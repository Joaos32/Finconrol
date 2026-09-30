package com.fincontrol.dashboard.service;

import com.fincontrol.account.repository.AccountRepository;
import com.fincontrol.account.repository.AccountBalanceProjection;
import com.fincontrol.transaction.entity.TransactionType;
import com.fincontrol.transaction.repository.TransactionRepository;
import com.fincontrol.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock private AccountRepository accounts;
    @Mock private TransactionRepository transactions;
    @Mock private TransactionService transactionService;
    @InjectMocks private DashboardService service;

    @Test
    void calculatesCashBalanceAndMonthlyResultFromAggregates() {
        UUID userId = UUID.randomUUID();
        when(transactions.sumByUserAndTypeForPeriod(eq(userId), eq(TransactionType.INCOME), any(), any()))
                .thenReturn(new BigDecimal("2100.00"));
        when(transactions.sumByUserAndTypeForPeriod(eq(userId), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal("800.00"));
        AccountBalanceProjection balance = org.mockito.Mockito.mock(AccountBalanceProjection.class);
        when(balance.getCurrentBalance()).thenReturn(new BigDecimal("8600.00"));
        when(accounts.findBalancesByUserId(userId)).thenReturn(List.of(balance));

        var summary = service.summary(userId);

        assertEquals(new BigDecimal("8600.00"), summary.currentBalance());
        assertEquals(new BigDecimal("2100.00"), summary.monthlyIncome());
        assertEquals(new BigDecimal("800.00"), summary.monthlyExpense());
        assertEquals(new BigDecimal("1300.00"), summary.monthlyResult());
    }
}
