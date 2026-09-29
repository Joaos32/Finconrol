package com.fincontrol.budget.repository;

import com.fincontrol.budget.entity.BudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<BudgetEntity, UUID> {
    @Query("""
            select b from BudgetEntity b join fetch b.category
            where b.user.id = :userId and b.periodStart = :periodStart
            order by b.category.name
            """)
    List<BudgetEntity> findAllByUserIdAndPeriodStartOrderByCategoryName(
            @Param("userId") UUID userId, @Param("periodStart") LocalDate periodStart);

    @Query("select b from BudgetEntity b where b.id = :budgetId and b.user.id = :userId")
    Optional<BudgetEntity> findByIdAndUserId(@Param("budgetId") UUID budgetId, @Param("userId") UUID userId);

    @Query("""
            select b from BudgetEntity b
            where b.user.id = :userId and b.category.id = :categoryId and b.periodStart = :periodStart
            """)
    Optional<BudgetEntity> findByUserIdAndCategoryIdAndPeriodStart(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("periodStart") LocalDate periodStart);

    @Query("""
            select case when count(b) > 0 then true else false end from BudgetEntity b
            where b.user.id = :userId and b.category.id = :categoryId
            """)
    boolean existsByCategoryIdAndUserId(@Param("categoryId") UUID categoryId, @Param("userId") UUID userId);
}
