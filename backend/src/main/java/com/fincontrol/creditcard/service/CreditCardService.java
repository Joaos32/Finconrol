package com.fincontrol.creditcard.service;

import com.fincontrol.account.entity.AccountEntity;
import com.fincontrol.account.repository.AccountRepository;
import com.fincontrol.creditcard.domain.CreditCardBillingCycle;
import com.fincontrol.creditcard.dto.CreditCardDtos;
import com.fincontrol.creditcard.entity.CreditCardEntity;
import com.fincontrol.creditcard.entity.CreditCardInvoiceEntity;
import com.fincontrol.creditcard.entity.CreditCardInvoicePaymentEntity;
import com.fincontrol.creditcard.repository.CreditCardInvoicePaymentRepository;
import com.fincontrol.creditcard.repository.CreditCardInvoiceRepository;
import com.fincontrol.creditcard.repository.CreditCardRepository;
import com.fincontrol.shared.error.ApiException;
import com.fincontrol.transaction.repository.TransactionRepository;
import com.fincontrol.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CreditCardService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final CreditCardRepository cards;
    private final CreditCardInvoiceRepository invoices;
    private final CreditCardInvoicePaymentRepository payments;
    private final TransactionRepository transactions;
    private final AccountRepository accounts;
    private final UserRepository users;

    public CreditCardService(CreditCardRepository cards, CreditCardInvoiceRepository invoices,
                             CreditCardInvoicePaymentRepository payments, TransactionRepository transactions,
                             AccountRepository accounts,
                             UserRepository users) {
        this.cards = cards;
        this.invoices = invoices;
        this.payments = payments;
        this.transactions = transactions;
        this.accounts = accounts;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<CreditCardDtos.Response> list(UUID userId) {
        Map<UUID, BigDecimal> outstanding = cards.outstandingByUser(userId).stream()
                .collect(Collectors.toMap(row -> row.getCardId(), row -> zeroIfNull(row.getOutstandingAmount())));
        return cards.findAllByUserIdOrderByNameAsc(userId).stream()
                .map(card -> cardResponse(card, outstanding.getOrDefault(card.getId(), ZERO)))
                .toList();
    }

    @Transactional
    public CreditCardDtos.Response create(UUID userId, CreditCardDtos.Request request) {
        CreditCardEntity card = cards.save(new CreditCardEntity(users.getReferenceById(userId), request.name().trim(),
                request.creditLimit(), request.closingDay(), request.dueDay()));
        return cardResponse(card, ZERO);
    }

    @Transactional
    public CreditCardDtos.Response update(UUID userId, UUID cardId, CreditCardDtos.Request request) {
        CreditCardEntity card = findOwnedCardForUpdate(userId, cardId);
        if (card.getClosingDay() != request.closingDay() && invoices.existsByCardIdAndUserId(cardId, userId)) {
            throw ApiException.conflict("Não é possível alterar o dia de fechamento depois da criação de uma fatura.");
        }
        if (card.getDueDay() != request.dueDay() && invoices.existsUnpaidByCardAndUser(cardId, userId)) {
            throw ApiException.conflict("Não é possível alterar o vencimento enquanto houver uma fatura em aberto.");
        }
        card.update(request.name().trim(), request.creditLimit(), request.closingDay(), request.dueDay());
        return cardResponse(card, invoices.outstandingByCardAndUser(cardId, userId));
    }

    @Transactional
    public void delete(UUID userId, UUID cardId) {
        CreditCardEntity card = findOwnedCardForUpdate(userId, cardId);
        if (transactions.existsByCardIdAndUserId(cardId, userId) || payments.existsByCardIdAndUserId(cardId, userId)) {
            throw ApiException.conflict("Não é possível excluir um cartão que já possui faturas ou compras.");
        }
        invoices.deleteAllByCardIdAndUserId(cardId, userId);
        cards.delete(card);
    }

    @Transactional(readOnly = true)
    public CreditCardDtos.InvoiceResponse invoice(UUID userId, UUID cardId, String monthValue) {
        CreditCardEntity card = findOwnedCard(userId, cardId);
        YearMonth month = parseMonth(monthValue);
        return invoices.findByCardIdAndUserIdAndClosingMonth(cardId, userId, month.atDay(1))
                .map(invoice -> invoiceResponse(invoice,
                        invoices.totalByInvoiceAndUser(invoice.getId(), userId), payments.findByInvoiceId(invoice.getId()).orElse(null)))
                .orElseGet(() -> {
                    CreditCardBillingCycle cycle = CreditCardBillingCycle.forPurchase(
                            month.atDay(1), card.getClosingDay(), card.getDueDay());
                    return new CreditCardDtos.InvoiceResponse(null, cardId, card.getName(), month.toString(),
                            cycle.periodStart(), cycle.closingDate(), cycle.dueDate(), ZERO,
                            false, null, null, null);
                });
    }

    @Transactional
    public CreditCardDtos.InvoiceResponse pay(UUID userId, UUID cardId, UUID invoiceId,
                                              CreditCardDtos.PaymentRequest request) {
        CreditCardEntity card = findOwnedCard(userId, cardId);
        CreditCardInvoiceEntity invoice = invoices.findOwnedForUpdate(invoiceId, cardId, userId)
                .orElseThrow(() -> ApiException.notFound("Fatura"));
        if (payments.existsByInvoiceId(invoiceId)) {
            throw ApiException.conflict("Esta fatura já foi paga.");
        }
        BigDecimal total = invoices.totalByInvoiceAndUser(invoiceId, userId);
        if (total.signum() <= 0) {
            throw ApiException.badRequest("Não é possível pagar uma fatura sem compras.");
        }
        AccountEntity account = accounts.findByIdAndUserId(request.accountId(), userId)
                .orElseThrow(() -> ApiException.notFound("Conta"));
        CreditCardInvoicePaymentEntity payment = payments.save(new CreditCardInvoicePaymentEntity(
                invoice, card, users.getReferenceById(userId), account, total));
        return invoiceResponse(invoice, total, payment);
    }

    public CreditCardEntity findOwnedCard(UUID userId, UUID cardId) {
        return cards.findByIdAndUserId(cardId, userId).orElseThrow(() -> ApiException.notFound("Cartão"));
    }

    private CreditCardEntity findOwnedCardForUpdate(UUID userId, UUID cardId) {
        return cards.findOwnedForUpdate(cardId, userId).orElseThrow(() -> ApiException.notFound("Cartão"));
    }

    private CreditCardDtos.Response cardResponse(CreditCardEntity card, BigDecimal outstanding) {
        return new CreditCardDtos.Response(card.getId(), card.getName(), card.getCreditLimit(),
                card.getClosingDay(), card.getDueDay(), outstanding, card.getCreditLimit().subtract(outstanding),
                card.getCreatedAt(), card.getUpdatedAt());
    }

    private CreditCardDtos.InvoiceResponse invoiceResponse(CreditCardInvoiceEntity invoice, BigDecimal total,
                                                            CreditCardInvoicePaymentEntity payment) {
        return new CreditCardDtos.InvoiceResponse(invoice.getId(), invoice.getCard().getId(),
                invoice.getCard().getName(), YearMonth.from(invoice.getClosingMonth()).toString(), invoice.getPeriodStart(),
                invoice.getClosingDate(), invoice.getDueDate(), total, payment != null,
                payment == null ? null : payment.getPaidAt(),
                payment == null ? null : payment.getAccount().getId(),
                payment == null ? null : payment.getAccount().getName());
    }

    private YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw ApiException.badRequest("Informe o mês no formato AAAA-MM.");
        }
    }

    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? ZERO : amount;
    }
}
