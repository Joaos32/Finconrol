package com.fincontrol.dashboard.dto;

import com.fincontrol.transaction.dto.TransactionDtos;

import java.math.BigDecimal;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() { }

    public record Summary(BigDecimal currentBalance, BigDecimal monthlyIncome,
                          BigDecimal monthlyExpense, BigDecimal monthlyResult) { }
    public record ExpenseCategory(String category, BigDecimal amount, BigDecimal percentage) { }
    public record MonthlyPoint(String month, BigDecimal income, BigDecimal expense) { }
    public record RecentTransactions(List<TransactionDtos.Response> items) { }
}
