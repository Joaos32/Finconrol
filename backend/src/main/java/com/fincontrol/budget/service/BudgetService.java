package com.fincontrol.budget.service;

import com.fincontrol.budget.dto.BudgetDtos;
import com.fincontrol.budget.entity.BudgetEntity;
import com.fincontrol.budget.repository.BudgetExpenseTotalProjection;
import com.fincontrol.budget.repository.BudgetRepository;
import com.fincontrol.category.entity.CategoryEntity;
import com.fincontrol.category.entity.CategoryType;
import com.fincontrol.category.repository.CategoryRepository;
import com.fincontrol.shared.error.ApiException;
import com.fincontrol.transaction.repository.TransactionRepository;
import com.fincontrol.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BudgetService {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BudgetRepository budgets;
    private final CategoryRepository categories;
    private final TransactionRepository transactions;
    private final UserRepository users;

    public BudgetService(BudgetRepository budgets, CategoryRepository categories,
                         TransactionRepository transactions, UserRepository users) {
        this.budgets = budgets;
        this.categories = categories;
        this.transactions = transactions;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<BudgetDtos.Response> list(UUID userId, String monthValue) {
        LocalDate periodStart = parseMonth(monthValue).atDay(1);
        Map<UUID, BigDecimal> spent = spentByCategory(userId, periodStart);
        return budgets.findAllByUserIdAndPeriodStartOrderByCategoryName(userId, periodStart).stream()
                .map(budget -> response(budget, spent.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional
    public BudgetDtos.Response create(UUID userId, BudgetDtos.Request request) {
        LocalDate periodStart = parseMonth(request.month()).atDay(1);
        CategoryEntity category = findExpenseCategory(userId, request.categoryId());
        ensureUnique(userId, category.getId(), periodStart, null);
        BudgetEntity saved = budgets.save(new BudgetEntity(users.getReferenceById(userId), category,
                periodStart, request.limitAmount()));
        return response(saved, spentForCategory(userId, category.getId(), periodStart));
    }

    @Transactional
    public BudgetDtos.Response update(UUID userId, UUID budgetId, BudgetDtos.Request request) {
        BudgetEntity budget = findOwned(userId, budgetId);
        LocalDate periodStart = parseMonth(request.month()).atDay(1);
        CategoryEntity category = findExpenseCategory(userId, request.categoryId());
        ensureUnique(userId, category.getId(), periodStart, budgetId);
        budget.update(category, periodStart, request.limitAmount());
        return response(budget, spentForCategory(userId, category.getId(), periodStart));
    }

    @Transactional
    public void delete(UUID userId, UUID budgetId) {
        budgets.delete(findOwned(userId, budgetId));
    }

    private CategoryEntity findExpenseCategory(UUID userId, UUID categoryId) {
        CategoryEntity category = categories.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> ApiException.notFound("Categoria"));
        if (category.getType() != CategoryType.EXPENSE) {
            throw ApiException.badRequest("O orçamento só pode usar categorias de despesa.");
        }
        return category;
    }

    private void ensureUnique(UUID userId, UUID categoryId, LocalDate periodStart, UUID exceptBudgetId) {
        budgets.findByUserIdAndCategoryIdAndPeriodStart(userId, categoryId, periodStart)
                .filter(existing -> !existing.getId().equals(exceptBudgetId))
                .ifPresent(existing -> { throw ApiException.conflict("Já existe um orçamento para essa categoria neste mês."); });
    }

    private BudgetEntity findOwned(UUID userId, UUID budgetId) {
        return budgets.findByIdAndUserId(budgetId, userId).orElseThrow(() -> ApiException.notFound("Orçamento"));
    }

    private Map<UUID, BigDecimal> spentByCategory(UUID userId, LocalDate periodStart) {
        LocalDate nextPeriodStart = periodStart.plusMonths(1);
        return transactions.budgetExpenseTotals(userId, periodStart, nextPeriodStart).stream()
                .collect(Collectors.toMap(BudgetExpenseTotalProjection::getCategoryId,
                        BudgetExpenseTotalProjection::getSpentAmount));
    }

    private BigDecimal spentForCategory(UUID userId, UUID categoryId, LocalDate periodStart) {
        return spentByCategory(userId, periodStart).getOrDefault(categoryId, BigDecimal.ZERO);
    }

    private BudgetDtos.Response response(BudgetEntity budget, BigDecimal spentAmount) {
        BigDecimal limitAmount = budget.getLimitAmount();
        BigDecimal percentageUsed = spentAmount.multiply(HUNDRED).divide(limitAmount, 1, RoundingMode.HALF_UP);
        return new BudgetDtos.Response(budget.getId(), budget.getCategory().getId(), budget.getCategory().getName(),
                YearMonth.from(budget.getPeriodStart()).toString(), limitAmount, spentAmount,
                limitAmount.subtract(spentAmount), percentageUsed);
    }

    private YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw ApiException.badRequest("Informe o mês no formato AAAA-MM.");
        }
    }
}
