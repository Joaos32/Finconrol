package com.fincontrol.dashboard.service;

import com.fincontrol.account.repository.AccountRepository;
import com.fincontrol.dashboard.dto.DashboardDtos;
import com.fincontrol.transaction.dto.TransactionDtos;
import com.fincontrol.transaction.entity.TransactionType;
import com.fincontrol.transaction.repository.MonthlyAggregateProjection;
import com.fincontrol.transaction.repository.TransactionCategoryTotalProjection;
import com.fincontrol.transaction.repository.TransactionRepository;
import com.fincontrol.transaction.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final TransactionService transactionService;

    public DashboardService(AccountRepository accounts, TransactionRepository transactions,
                            TransactionService transactionService) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.transactionService = transactionService;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.Summary summary(UUID userId) {
        YearMonth month = YearMonth.now(ZoneOffset.UTC);
        LocalDate start = month.atDay(1);
        LocalDate end = month.plusMonths(1).atDay(1);
        BigDecimal income = zeroIfNull(transactions.sumByUserAndTypeForPeriod(userId, TransactionType.INCOME, start, end));
        BigDecimal expense = zeroIfNull(transactions.sumByUserAndTypeForPeriod(userId, TransactionType.EXPENSE, start, end));
        BigDecimal currentBalance = accounts.findBalancesByUserId(userId).stream()
                .map(row -> zeroIfNull(row.getCurrentBalance()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardDtos.Summary(currentBalance,
                income, expense, income.subtract(expense));
    }

    @Transactional(readOnly = true)
    public List<DashboardDtos.ExpenseCategory> expensesByCategory(UUID userId) {
        YearMonth month = YearMonth.now(ZoneOffset.UTC);
        List<TransactionCategoryTotalProjection> rows = transactions.expenseTotalsByCategory(
                userId, month.atDay(1), month.plusMonths(1).atDay(1));
        BigDecimal total = rows.stream().map(TransactionCategoryTotalProjection::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() == 0) {
            return List.of();
        }
        return rows.stream().map(row -> new DashboardDtos.ExpenseCategory(row.getCategoryName(), row.getAmount(),
                row.getAmount().multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP))).toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardDtos.MonthlyPoint> monthlyEvolution(UUID userId) {
        YearMonth current = YearMonth.now(ZoneOffset.UTC);
        YearMonth first = current.minusMonths(5);
        Map<YearMonth, MonthlyAggregateProjection> values = transactions.monthlyTotals(
                        userId, first.atDay(1), current.plusMonths(1).atDay(1)).stream()
                .collect(Collectors.toMap(row -> YearMonth.from(row.getMonth()), Function.identity()));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        return java.util.stream.IntStream.rangeClosed(0, 5)
                .mapToObj(offset -> {
                    YearMonth month = first.plusMonths(offset);
                    MonthlyAggregateProjection row = values.get(month);
                    return new DashboardDtos.MonthlyPoint(month.format(formatter),
                            row == null ? ZERO : zeroIfNull(row.getIncome()),
                            row == null ? ZERO : zeroIfNull(row.getExpense()));
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionDtos.Response> recentTransactions(UUID userId) {
        return transactionService.recent(userId, 5);
    }

    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? ZERO : amount;
    }
}
