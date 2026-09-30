package com.fincontrol.transaction.service;

import com.fincontrol.account.entity.AccountEntity;
import com.fincontrol.account.repository.AccountRepository;
import com.fincontrol.category.entity.CategoryEntity;
import com.fincontrol.category.repository.CategoryRepository;
import com.fincontrol.creditcard.domain.CreditCardBillingCycle;
import com.fincontrol.creditcard.entity.CreditCardEntity;
import com.fincontrol.creditcard.entity.CreditCardInvoiceEntity;
import com.fincontrol.creditcard.repository.CreditCardInvoicePaymentRepository;
import com.fincontrol.creditcard.repository.CreditCardInvoiceRepository;
import com.fincontrol.creditcard.repository.CreditCardRepository;
import com.fincontrol.shared.error.ApiException;
import com.fincontrol.transaction.dto.TransactionDtos;
import com.fincontrol.transaction.entity.TransactionEntity;
import com.fincontrol.transaction.entity.TransactionType;
import com.fincontrol.transaction.mapper.TransactionMapper;
import com.fincontrol.transaction.repository.TransactionRepository;
import com.fincontrol.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

@Service
public class TransactionService {
    private final TransactionRepository transactions;
    private final AccountRepository accounts;
    private final CategoryRepository categories;
    private final CreditCardRepository creditCards;
    private final CreditCardInvoiceRepository invoices;
    private final CreditCardInvoicePaymentRepository invoicePayments;
    private final UserRepository users;
    private final TransactionMapper mapper;

    public TransactionService(TransactionRepository transactions, AccountRepository accounts, CategoryRepository categories,
                              CreditCardRepository creditCards, CreditCardInvoiceRepository invoices,
                              CreditCardInvoicePaymentRepository invoicePayments, UserRepository users, TransactionMapper mapper) {
        this.transactions = transactions;
        this.accounts = accounts;
        this.categories = categories;
        this.creditCards = creditCards;
        this.invoices = invoices;
        this.invoicePayments = invoicePayments;
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public TransactionDtos.PageResponse<TransactionDtos.Response> list(
            UUID userId, LocalDate startDate, LocalDate endDate, TransactionType type,
            UUID categoryId, UUID accountId, UUID cardId, int page, int size) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw ApiException.badRequest("A data inicial deve ser anterior ou igual à data final.");
        }
        List<Specification<TransactionEntity>> filters = new ArrayList<>();
        filters.add((root, query, cb) -> cb.equal(root.get("user").get("id"), userId));
        if (startDate != null) filters.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
        if (endDate != null) filters.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("transactionDate"), endDate));
        if (type != null) filters.add((root, query, cb) -> cb.equal(root.get("type"), type));
        if (categoryId != null) filters.add((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        if (accountId != null) filters.add((root, query, cb) -> cb.equal(root.get("account").get("id"), accountId));
        if (cardId != null) filters.add((root, query, cb) -> cb.equal(root.get("card").get("id"), cardId));
        Specification<TransactionEntity> specification = filters.getFirst();
        for (int index = 1; index < filters.size(); index++) {
            specification = specification.and(filters.get(index));
        }
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "transactionDate").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<TransactionEntity> result = transactions.findAll(specification, pageable);
        Page<TransactionDtos.Response> mapped = result.map(mapper::toResponse);
        return new TransactionDtos.PageResponse<>(mapped.getContent(), mapped.getNumber(), mapped.getSize(),
                mapped.getTotalElements(), mapped.getTotalPages(), mapped.isFirst(), mapped.isLast());
    }

    @Transactional(readOnly = true)
    public TransactionDtos.Response get(UUID userId, UUID transactionId) {
        return mapper.toResponse(findOwned(userId, transactionId));
    }

    @Transactional
    public TransactionDtos.Response create(UUID userId, TransactionDtos.Request request) {
        AccountEntity account = null;
        CreditCardEntity card = null;
        CreditCardInvoiceEntity invoice = null;
        if (request.accountId() != null && request.cardId() == null) {
            account = findAccount(userId, request.accountId());
        } else if (request.accountId() == null && request.cardId() != null && request.type() == TransactionType.EXPENSE) {
            card = creditCards.findByIdAndUserId(request.cardId(), userId)
                    .orElseThrow(() -> ApiException.notFound("Cartão"));
            invoice = invoiceForPurchase(card, userId, request.transactionDate());
        } else {
            throw ApiException.badRequest("Selecione uma conta ou um cartão para a transação.");
        }
        CategoryEntity category = findCategory(userId, request.categoryId());
        validateCategoryType(request.type(), category);
        TransactionEntity entity = new TransactionEntity(users.getReferenceById(userId), account, card, invoice, category,
                request.description().trim(), request.amount(), request.type(), request.transactionDate());
        return mapper.toResponse(transactions.save(entity));
    }

    @Transactional
    public TransactionDtos.Response update(UUID userId, UUID transactionId, TransactionDtos.Request request) {
        TransactionEntity entity = findOwned(userId, transactionId);
        lockCardsForUpdate(userId, entity, request);
        ensureInvoiceIsUnpaid(userId, entity);
        AccountEntity account = null;
        CreditCardEntity card = null;
        CreditCardInvoiceEntity invoice = null;
        if (request.accountId() != null && request.cardId() == null) {
            account = findAccount(userId, request.accountId());
        } else if (request.accountId() == null && request.cardId() != null && request.type() == TransactionType.EXPENSE) {
            card = creditCards.findByIdAndUserId(request.cardId(), userId)
                    .orElseThrow(() -> ApiException.notFound("Cartão"));
            invoice = invoiceForPurchase(card, userId, request.transactionDate());
        } else {
            throw ApiException.badRequest("Selecione uma conta ou um cartão para a transação.");
        }
        CategoryEntity category = findCategory(userId, request.categoryId());
        validateCategoryType(request.type(), category);
        CreditCardInvoiceEntity previousInvoice = entity.getInvoice();
        entity.update(account, card, invoice, category, request.description().trim(), request.amount(), request.type(), request.transactionDate());
        if (previousInvoice != null && (invoice == null || !previousInvoice.getId().equals(invoice.getId()))) {
            removeInvoiceIfEmpty(userId, previousInvoice);
        }
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(UUID userId, UUID transactionId) {
        TransactionEntity entity = findOwned(userId, transactionId);
        lockCardsForUpdate(userId, entity, null);
        ensureInvoiceIsUnpaid(userId, entity);
        CreditCardInvoiceEntity invoice = entity.getInvoice();
        transactions.delete(entity);
        if (invoice != null) removeInvoiceIfEmpty(userId, invoice);
    }

    @Transactional(readOnly = true)
    public List<TransactionDtos.Response> recent(UUID userId, int limit) {
        return transactions.findAllByUserIdOrderByTransactionDateDescCreatedAtDesc(userId, PageRequest.of(0, limit))
                .map(mapper::toResponse).getContent();
    }

    private TransactionEntity findOwned(UUID userId, UUID transactionId) {
        return transactions.findOne((root, query, cb) -> cb.and(
                        cb.equal(root.get("id"), transactionId),
                        cb.equal(root.get("user").get("id"), userId)))
                .orElseThrow(() -> ApiException.notFound("Transação"));
    }

    private AccountEntity findAccount(UUID userId, UUID accountId) {
        return accounts.findByIdAndUserId(accountId, userId).orElseThrow(() -> ApiException.notFound("Conta"));
    }

    private CategoryEntity findCategory(UUID userId, UUID categoryId) {
        return categories.findByIdAndUserId(categoryId, userId).orElseThrow(() -> ApiException.notFound("Categoria"));
    }

    private CreditCardInvoiceEntity invoiceForPurchase(CreditCardEntity card, UUID userId, LocalDate purchaseDate) {
        CreditCardEntity lockedCard = creditCards.findOwnedForUpdate(card.getId(), userId)
                .orElseThrow(() -> ApiException.notFound("Cartão"));
        var cycleSettings = creditCards.findCycleSettingsForUpdate(lockedCard.getId(), userId)
                .orElseThrow(() -> ApiException.notFound("Cartão"));
        CreditCardBillingCycle cycle = CreditCardBillingCycle.forPurchase(
                purchaseDate, cycleSettings.getClosingDay(), cycleSettings.getDueDay());
        LocalDate closingMonth = cycle.closingMonth().atDay(1);
        CreditCardInvoiceEntity invoice = invoices.findForUpdateByCardAndMonth(lockedCard.getId(), userId, closingMonth)
                .orElseGet(() -> invoices.save(new CreditCardInvoiceEntity(lockedCard, lockedCard.getUser(), closingMonth,
                        cycle.periodStart(), cycle.closingDate(), cycle.dueDate())));
        if (invoicePayments.existsByInvoiceId(invoice.getId())) {
            throw ApiException.conflict("Não é possível adicionar uma compra a uma fatura já paga.");
        }
        return invoice;
    }

    private void lockCardsForUpdate(UUID userId, TransactionEntity existing, TransactionDtos.Request request) {
        TreeSet<UUID> cardIds = new TreeSet<>();
        if (existing.getCard() != null) cardIds.add(existing.getCard().getId());
        if (request != null && request.cardId() != null) cardIds.add(request.cardId());
        for (UUID cardId : cardIds) {
            creditCards.findOwnedForUpdate(cardId, userId).orElseThrow(() -> ApiException.notFound("Cartão"));
        }
    }

    private void removeInvoiceIfEmpty(UUID userId, CreditCardInvoiceEntity invoice) {
        transactions.flush();
        if (transactions.countByInvoiceIdAndUserId(invoice.getId(), userId) == 0) {
            invoices.delete(invoice);
        }
    }

    private void ensureInvoiceIsUnpaid(UUID userId, TransactionEntity transaction) {
        if (transaction.getInvoice() == null) return;
        CreditCardInvoiceEntity invoice = transaction.getInvoice();
        invoices.findOwnedForUpdate(invoice.getId(), invoice.getCard().getId(), userId)
                .orElseThrow(() -> ApiException.notFound("Fatura"));
        if (invoicePayments.existsByInvoiceId(invoice.getId())) {
            throw ApiException.conflict("Não é possível alterar uma compra de uma fatura já paga.");
        }
    }

    private void validateCategoryType(TransactionType type, CategoryEntity category) {
        if (!category.getType().name().equals(type.name())) {
            throw ApiException.badRequest("Categoria incompatível com o tipo da transação.");
        }
    }
}
