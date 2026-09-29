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
    boolean existsByCategoryIdAndUserId(UUID categoryId, UUID userId);

    Page<TransactionEntity> findAllByUserIdOrderByTransactionDateDescCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("""
            select coalesce(sum(t.amount), 0) from TransactionEntity t
            where t.user.id = :userId and t.type = :type
              and t.transactionDate >= :start and t.transactionDate < :end
            """)
    BigDecimal sumByUserAndTypeForPeriod(@Param("userId") UUID userId, @Param("type") TransactionType type,
                                         @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            select coalesce(sum(t.amount), 0) from TransactionEntity t
            where t.user.id = :userId and t.type = :type
            """)
    BigDecimal sumByUserAndType(@Param("userId") UUID userId, @Param("type") TransactionType type);

    @Query(value = """
            SELECT c.name AS "categoryName", SUM(t.amount) AS amount
            FROM transactions t JOIN categories c ON c.id = t.category_id AND c.user_id = t.user_id
            WHERE t.user_id = :userId AND t.type = 'EXPENSE'
              AND t.transaction_date >= :start AND t.transaction_date < :end
            GROUP BY c.name
            ORDER BY SUM(t.amount) DESC
            """, nativeQuery = true)
    List<TransactionCategoryTotalProjection> expenseTotalsByCategory(
            @Param("userId") UUID userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value = """
            SELECT date_trunc('month', transaction_date)::date AS month,
                   COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income,
                   COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense
            FROM transactions
            WHERE user_id = :userId AND transaction_date >= :start AND transaction_date < :end
            GROUP BY date_trunc('month', transaction_date)
            ORDER BY date_trunc('month', transaction_date)
            """, nativeQuery = true)
    List<MonthlyAggregateProjection> monthlyTotals(
            @Param("userId") UUID userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value = """
            SELECT category_id AS "categoryId", SUM(amount) AS "spentAmount"
            FROM transactions
            WHERE user_id = :userId AND type = 'EXPENSE'
              AND transaction_date >= :start AND transaction_date < :end
            GROUP BY category_id
            """, nativeQuery = true)
    List<BudgetExpenseTotalProjection> budgetExpenseTotals(
            @Param("userId") UUID userId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
