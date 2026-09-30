package com.fincontrol.transaction.service;

import com.fincontrol.account.entity.AccountEntity;
import com.fincontrol.account.entity.AccountType;
import com.fincontrol.account.repository.AccountRepository;
import com.fincontrol.category.entity.CategoryEntity;
import com.fincontrol.category.entity.CategoryType;
import com.fincontrol.category.repository.CategoryRepository;
import com.fincontrol.creditcard.entity.CreditCardEntity;
import com.fincontrol.creditcard.entity.CreditCardInvoiceEntity;
import com.fincontrol.creditcard.repository.CreditCardInvoicePaymentRepository;
import com.fincontrol.creditcard.repository.CreditCardInvoiceRepository;
import com.fincontrol.creditcard.repository.CreditCardCycleSettingsProjection;
import com.fincontrol.creditcard.repository.CreditCardRepository;
import com.fincontrol.shared.error.ApiException;
import com.fincontrol.transaction.dto.TransactionDtos;
import com.fincontrol.transaction.entity.TransactionEntity;
import com.fincontrol.transaction.entity.TransactionType;
import com.fincontrol.transaction.mapper.TransactionMapper;
import com.fincontrol.transaction.repository.TransactionRepository;
import com.fincontrol.user.repository.UserRepository;
import com.fincontrol.user.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock private TransactionRepository transactions;
    @Mock private AccountRepository accounts;
    @Mock private CategoryRepository categories;
    @Mock private CreditCardRepository creditCards;
    @Mock private CreditCardInvoiceRepository invoices;
    @Mock private CreditCardInvoicePaymentRepository invoicePayments;
    @Mock private UserRepository users;
    @Mock private TransactionMapper mapper;
    @InjectMocks private TransactionService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    @Test
    void doesNotAllowAnAccountOwnedByAnotherUser() {
        when(accounts.findByIdAndUserId(accountId, userId)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> service.create(userId, request(TransactionType.EXPENSE)));

        assertEquals(404, exception.getStatus().value());
        verify(categories, never()).findByIdAndUserId(any(), any());
        verify(transactions, never()).save(any());
    }

    @Test
    void rejectsCategoryThatDoesNotMatchTransactionType() {
        AccountEntity account = new AccountEntity(null, "Carteira", AccountType.CASH, BigDecimal.ZERO);
        CategoryEntity category = new CategoryEntity(new UserEntity("Pessoa", "pessoa@example.com", "hash"),
                "Salário", CategoryType.INCOME);
        when(accounts.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(account));
        when(categories.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(category));

        ApiException exception = assertThrows(ApiException.class, () -> service.create(userId, request(TransactionType.EXPENSE)));

        assertEquals(400, exception.getStatus().value());
        assertEquals("Categoria incompatível com o tipo da transação.", exception.getMessage());
        verify(transactions, never()).save(any());
    }

    @Test
    void doesNotReturnTransactionOutsideAuthenticatedUsersScope() {
        when(transactions.findOne(ArgumentMatchers.<Specification<TransactionEntity>>any())).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> service.get(userId, UUID.randomUUID()));

        assertEquals(404, exception.getStatus().value());
        verify(mapper, never()).toResponse(any());
    }

    @Test
    void rejectsIncomeFromCreditCard() {
        TransactionDtos.Request request = new TransactionDtos.Request("Salário", new BigDecimal("10.00"),
                TransactionType.INCOME, null, UUID.randomUUID(), categoryId, LocalDate.now());

        ApiException exception = assertThrows(ApiException.class, () -> service.create(userId, request));

        assertEquals(400, exception.getStatus().value());
        verifyNoInteractions(transactions);
    }

    @Test
    void assignsCardPurchaseToTheBillingCycleContainingItsPurchaseDate() {
        LocalDate purchaseDate = LocalDate.of(2026, 11, 28);
        UUID cardId = UUID.randomUUID();
        CreditCardEntity card = mock(CreditCardEntity.class);
        CreditCardInvoiceEntity invoice = mock(CreditCardInvoiceEntity.class);
        CreditCardCycleSettingsProjection cycleSettings = mock(CreditCardCycleSettingsProjection.class);
        CategoryEntity category = new CategoryEntity(new UserEntity("Pessoa", "pessoa@example.com", "hash"),
                "Mercado", CategoryType.EXPENSE);
        when(creditCards.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(card));
        when(creditCards.findOwnedForUpdate(cardId, userId)).thenReturn(Optional.of(card));
        when(creditCards.findCycleSettingsForUpdate(cardId, userId)).thenReturn(Optional.of(cycleSettings));
        when(card.getId()).thenReturn(cardId);
        when(cycleSettings.getClosingDay()).thenReturn(28);
        when(cycleSettings.getDueDay()).thenReturn(10);
        when(card.getUser()).thenReturn(new UserEntity("Pessoa", "pessoa@example.com", "hash"));
        when(invoices.findForUpdateByCardAndMonth(cardId, userId, LocalDate.of(2026, 11, 1)))
                .thenReturn(Optional.of(invoice));
        when(categories.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(category));
        when(users.getReferenceById(userId)).thenReturn(new UserEntity("Pessoa", "pessoa@example.com", "hash"));
        when(transactions.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(userId, new TransactionDtos.Request("Mercado", new BigDecimal("35.50"),
                TransactionType.EXPENSE, null, cardId, categoryId, purchaseDate));

        var transaction = org.mockito.ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactions).save(transaction.capture());
        assertEquals(card, transaction.getValue().getCard());
        assertEquals(invoice, transaction.getValue().getInvoice());
        assertEquals(null, transaction.getValue().getAccount());
    }

    @Test
    void rejectsNewCardPurchaseWhenItsDestinationInvoiceWasAlreadyPaid() {
        LocalDate purchaseDate = LocalDate.of(2026, 11, 28);
        UUID cardId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        CreditCardEntity card = mock(CreditCardEntity.class);
        CreditCardInvoiceEntity invoice = mock(CreditCardInvoiceEntity.class);
        CreditCardCycleSettingsProjection cycleSettings = mock(CreditCardCycleSettingsProjection.class);
        when(creditCards.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(card));
        when(creditCards.findOwnedForUpdate(cardId, userId)).thenReturn(Optional.of(card));
        when(creditCards.findCycleSettingsForUpdate(cardId, userId)).thenReturn(Optional.of(cycleSettings));
        when(card.getId()).thenReturn(cardId);
        when(cycleSettings.getClosingDay()).thenReturn(28);
        when(cycleSettings.getDueDay()).thenReturn(10);
        when(card.getUser()).thenReturn(new UserEntity("Pessoa", "pessoa@example.com", "hash"));
        when(invoice.getId()).thenReturn(invoiceId);
        when(invoices.findForUpdateByCardAndMonth(cardId, userId, LocalDate.of(2026, 11, 1)))
                .thenReturn(Optional.of(invoice));
        when(invoicePayments.existsByInvoiceId(invoiceId)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> service.create(userId,
                new TransactionDtos.Request("Compra retroativa", new BigDecimal("10.00"), TransactionType.EXPENSE,
                        null, cardId, categoryId, purchaseDate)));

        assertEquals(409, exception.getStatus().value());
        verify(transactions, never()).save(any());
    }

    private TransactionDtos.Request request(TransactionType type) {
        return new TransactionDtos.Request("Compra", new BigDecimal("10.00"), type, accountId, categoryId, LocalDate.now());
    }
}
