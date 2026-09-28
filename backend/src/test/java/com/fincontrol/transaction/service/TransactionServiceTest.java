package com.fincontrol.transaction.service;

import com.fincontrol.account.entity.AccountEntity;
import com.fincontrol.account.entity.AccountType;
import com.fincontrol.account.repository.AccountRepository;
import com.fincontrol.category.entity.CategoryEntity;
import com.fincontrol.category.entity.CategoryType;
import com.fincontrol.category.repository.CategoryRepository;
import com.fincontrol.shared.error.ApiException;
import com.fincontrol.transaction.dto.TransactionDtos;
import com.fincontrol.transaction.entity.TransactionType;
import com.fincontrol.transaction.mapper.TransactionMapper;
import com.fincontrol.transaction.repository.TransactionRepository;
import com.fincontrol.user.repository.UserRepository;
import com.fincontrol.user.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock private TransactionRepository transactions;
    @Mock private AccountRepository accounts;
    @Mock private CategoryRepository categories;
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
        when(transactions.findOne(any())).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> service.get(userId, UUID.randomUUID()));

        assertEquals(404, exception.getStatus().value());
        verify(mapper, never()).toResponse(any());
    }

    private TransactionDtos.Request request(TransactionType type) {
        return new TransactionDtos.Request("Compra", new BigDecimal("10.00"), type, accountId, categoryId, LocalDate.now());
    }
}
