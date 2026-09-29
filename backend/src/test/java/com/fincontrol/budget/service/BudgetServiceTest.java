package com.fincontrol.budget.service;

import com.fincontrol.budget.dto.BudgetDtos;
import com.fincontrol.budget.entity.BudgetEntity;
import com.fincontrol.budget.repository.BudgetRepository;
import com.fincontrol.budget.repository.BudgetExpenseTotalProjection;
import com.fincontrol.category.entity.CategoryEntity;
import com.fincontrol.category.entity.CategoryType;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {
    @Mock private BudgetRepository budgets;
    @Mock private CategoryRepository categories;
    @Mock private TransactionRepository transactions;
    @Mock private UserRepository users;
    @InjectMocks private BudgetService service;

    @Test
    void listsBudgetsWithDatabaseAggregatesForTheRequestedMonth() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryEntity category = mock(CategoryEntity.class);
        when(category.getId()).thenReturn(categoryId);
        when(category.getName()).thenReturn("Mercado");
        BudgetEntity budget = mock(BudgetEntity.class);
        when(budget.getCategory()).thenReturn(category);
        when(budget.getPeriodStart()).thenReturn(LocalDate.of(2026, 6, 1));
        when(budget.getLimitAmount()).thenReturn(new BigDecimal("100.00"));
        BudgetExpenseTotalProjection total = mock(BudgetExpenseTotalProjection.class);
        when(total.getCategoryId()).thenReturn(categoryId);
        when(total.getSpentAmount()).thenReturn(new BigDecimal("35.50"));
        when(budgets.findAllByUserIdAndPeriodStartOrderByCategoryName(userId, LocalDate.of(2026, 6, 1)))
                .thenReturn(List.of(budget));
        when(transactions.budgetExpenseTotals(userId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1)))
                .thenReturn(List.of(total));

        List<BudgetDtos.Response> result = service.list(userId, "2026-06");

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("100.00"), result.getFirst().limitAmount());
        assertEquals(new BigDecimal("35.50"), result.getFirst().spentAmount());
        assertEquals(new BigDecimal("64.50"), result.getFirst().remainingAmount());
        assertEquals(new BigDecimal("35.5"), result.getFirst().percentageUsed());
        verify(transactions).budgetExpenseTotals(userId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1));
    }

    @Test
    void rejectsIncomeCategoriesWhenCreatingABudget() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryEntity category = new CategoryEntity(new UserEntity("Pessoa", "pessoa@example.com", "hash"),
                "Salário", CategoryType.INCOME);
        when(categories.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(category));

        ApiException error = assertThrows(ApiException.class, () -> service.create(userId,
                new BudgetDtos.Request(categoryId, "2026-06", new BigDecimal("100.00"))));

        assertEquals(400, error.getStatus().value());
        verify(budgets, never()).save(any(BudgetEntity.class));
    }

    @Test
    void hidesCategoriesOwnedByAnotherUser() {
        UUID userId = UUID.randomUUID();
        UUID foreignCategoryId = UUID.randomUUID();
        when(categories.findByIdAndUserId(foreignCategoryId, userId)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class, () -> service.create(userId,
                new BudgetDtos.Request(foreignCategoryId, "2026-06", new BigDecimal("100.00"))));

        assertEquals(404, error.getStatus().value());
        verify(budgets, never()).save(any(BudgetEntity.class));
    }

    @Test
    void rejectsDuplicateBudgetForTheSameCategoryAndMonth() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryEntity category = mock(CategoryEntity.class);
        when(category.getId()).thenReturn(categoryId);
        when(category.getType()).thenReturn(CategoryType.EXPENSE);
        BudgetEntity existing = mock(BudgetEntity.class);
        when(existing.getId()).thenReturn(UUID.randomUUID());
        when(categories.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(category));
        when(budgets.findByUserIdAndCategoryIdAndPeriodStart(userId, categoryId, LocalDate.of(2026, 6, 1)))
                .thenReturn(Optional.of(existing));

        ApiException error = assertThrows(ApiException.class, () -> service.create(userId,
                new BudgetDtos.Request(categoryId, "2026-06", new BigDecimal("100.00"))));

        assertEquals(409, error.getStatus().value());
        verify(budgets, never()).save(any(BudgetEntity.class));
    }

    @Test
    void rejectsMalformedMonthBeforeAccessingRepositories() {
        ApiException error = assertThrows(ApiException.class, () -> service.list(UUID.randomUUID(), "2026-13"));

        assertEquals(400, error.getStatus().value());
        verify(budgets, never()).findAllByUserIdAndPeriodStartOrderByCategoryName(any(), any());
    }
}
