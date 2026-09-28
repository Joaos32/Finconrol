package com.fincontrol.account.service;

import com.fincontrol.account.entity.AccountEntity;
import com.fincontrol.account.entity.AccountType;
import com.fincontrol.account.repository.AccountBalanceProjection;
import com.fincontrol.account.repository.AccountRepository;
import com.fincontrol.account.mapper.AccountMapper;
import com.fincontrol.category.repository.CategoryRepository;
import com.fincontrol.shared.error.ApiException;
import com.fincontrol.transaction.repository.TransactionRepository;
import com.fincontrol.user.entity.UserEntity;
import com.fincontrol.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock private AccountRepository accounts;
    @Mock private UserRepository users;
    @Mock private TransactionRepository transactions;
    @Mock private AccountMapper mapper;
    @InjectMocks private AccountService service;

    @Test
    void calculatesBalanceFromTheRepositoryAggregateForOwnedAccount() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        AccountEntity account = new AccountEntity(new UserEntity("Demo", "demo@example.com", "hash"),
                "Principal", AccountType.CHECKING, new BigDecimal("100.00"));
        AccountBalanceProjection projection = mock(AccountBalanceProjection.class);
        when(projection.getAccountId()).thenReturn(accountId);
        when(projection.getCurrentBalance()).thenReturn(new BigDecimal("135.25"));
        when(accounts.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(account));
        when(accounts.findBalancesByUserId(userId)).thenReturn(List.of(projection));

        BigDecimal balance = service.getBalance(userId, accountId);

        assertEquals(new BigDecimal("135.25"), balance);
        verify(accounts).findByIdAndUserId(accountId, userId);
        verify(accounts).findBalancesByUserId(userId);
    }

    @Test
    void hidesAccountsOwnedByAnotherUser() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(accounts.findByIdAndUserId(accountId, userId)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> service.getBalance(userId, accountId));

        assertEquals(404, exception.getStatus().value());
    }
}
