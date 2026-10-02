package com.fincontrol.transaction.repository;

import com.fincontrol.transaction.entity.TransactionEntity;
import com.fincontrol.transaction.entity.TransactionType;
import com.fincontrol.budget.repository.BudgetExpenseTotalProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID>,
        JpaSpecificationExecutor<TransactionEntity> {
    boolean existsByAccountIdAndUserId(UUID accountId, UUID userId);
    boolean existsByCardIdAndUserId(UUID cardId, UUID userId);
    boolean existsByCategoryIdAndUserId(UUID categoryId, UUID userId);
    long countByInvoiceIdAndUserId(UUID invoiceId, UUID userId);

    @Query("select t from TransactionEntity t where t.invoice.id = :invoiceId and t.user.id = :userId " +
            "order by t.createdAt, t.id")
    List<TransactionEntity> findInvoiceTransactions(@Param("invoiceId") UUID invoiceId, @Param("userId") UUID userId);

    Page<TransactionEntity> findAllByUserIdOrderByTransactionDateDescCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("""
            select coalesce(sum(t.amount), 0) from TransactionEntity t
            where t.user.id = :userId and t.type = :type
              and t.transactionDate >= :start and t.transactionDate < :end
            """)
    BigDecimal sumByUserAndTypeForPeriod(@Param("userId") UUID userId, @Param("type") TransactionType type,
                                         @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value = """
            SELECT COALESCE(SUM(month_expenses.amount), 0)
            FROM (
                SELECT t.amount
                FROM transactions t
                WHERE t.user_id = :userId AND t.type = 'EXPENSE' AND t.installment_count = 1
                  AND t.transaction_date >= :start AND t.transaction_date < :end
                UNION ALL
                SELECT ci.amount
                FROM credit_card_installments ci
                JOIN credit_card_invoices i ON i.id = ci.invoice_id AND i.user_id = ci.user_id
                WHERE ci.user_id = :userId AND i.closing_month >= :start AND i.closing_month < :end
            ) month_expenses
            """, nativeQuery = true)
    BigDecimal sumExpensesByUserForPeriod(@Param("userId") UUID userId,
                                         @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            select coalesce(sum(t.amount), 0) from TransactionEntity t
            where t.user.id = :userId and t.type = :type
            """)
    BigDecimal sumByUserAndType(@Param("userId") UUID userId, @Param("type") TransactionType type);

    @Query(value = """
            SELECT monthly_expenses."categoryName", SUM(monthly_expenses.amount) AS amount
            FROM (
                SELECT c.name AS "categoryName", t.amount
                FROM transactions t JOIN categories c ON c.id = t.category_id AND c.user_id = t.user_id
                WHERE t.user_id = :userId AND t.type = 'EXPENSE' AND t.installment_count = 1
                  AND t.transaction_date >= :start AND t.transaction_date < :end
                UNION ALL
                SELECT c.name AS "categoryName", ci.amount
                FROM credit_card_installments ci
                JOIN transactions t ON t.id = ci.transaction_id AND t.user_id = ci.user_id
                JOIN credit_card_invoices i ON i.id = ci.invoice_id AND i.user_id = ci.user_id
                JOIN categories c ON c.id = t.category_id AND c.user_id = t.user_id
                WHERE ci.user_id = :userId AND i.closing_month >= :start AND i.closing_month < :end
            ) monthly_expenses
            GROUP BY monthly_expenses."categoryName"
            ORDER BY SUM(monthly_expenses.amount) DESC
            """, nativeQuery = true)
    List<TransactionCategoryTotalProjection> expenseTotalsByCategory(
            @Param("userId") UUID userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value = """
            SELECT date_trunc('month', activity.activity_date)::date AS month,
                   COALESCE(SUM(activity.income), 0) AS income,
                   COALESCE(SUM(activity.expense), 0) AS expense
            FROM (
                SELECT t.transaction_date AS activity_date,
                       CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END AS income,
                       CASE WHEN t.type = 'EXPENSE' AND t.installment_count = 1 THEN t.amount ELSE 0 END AS expense
                FROM transactions t
                WHERE t.user_id = :userId AND t.transaction_date >= :start AND t.transaction_date < :end
                UNION ALL
                SELECT i.closing_month AS activity_date, 0 AS income, ci.amount AS expense
                FROM credit_card_installments ci
                JOIN credit_card_invoices i ON i.id = ci.invoice_id AND i.user_id = ci.user_id
                WHERE ci.user_id = :userId AND i.closing_month >= :start AND i.closing_month < :end
            ) activity
            GROUP BY date_trunc('month', activity.activity_date)
            ORDER BY date_trunc('month', activity.activity_date)
            """, nativeQuery = true)
    List<MonthlyAggregateProjection> monthlyTotals(
            @Param("userId") UUID userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value = """
            SELECT monthly_expenses.category_id AS "categoryId", SUM(monthly_expenses.amount) AS "spentAmount"
            FROM (
                SELECT t.category_id, t.amount
                FROM transactions t
                WHERE t.user_id = :userId AND t.type = 'EXPENSE' AND t.installment_count = 1
                  AND t.transaction_date >= :start AND t.transaction_date < :end
                UNION ALL
                SELECT t.category_id, ci.amount
                FROM credit_card_installments ci
                JOIN transactions t ON t.id = ci.transaction_id AND t.user_id = ci.user_id
                JOIN credit_card_invoices i ON i.id = ci.invoice_id AND i.user_id = ci.user_id
                WHERE ci.user_id = :userId AND i.closing_month >= :start AND i.closing_month < :end
            ) monthly_expenses
            GROUP BY monthly_expenses.category_id
            """, nativeQuery = true)
    List<BudgetExpenseTotalProjection> budgetExpenseTotals(
            @Param("userId") UUID userId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
