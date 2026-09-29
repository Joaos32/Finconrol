package com.fincontrol.category.service;

import com.fincontrol.budget.repository.BudgetRepository;
import com.fincontrol.category.dto.CategoryDtos;
import com.fincontrol.category.entity.CategoryEntity;
import com.fincontrol.category.entity.CategoryType;
import com.fincontrol.category.mapper.CategoryMapper;
import com.fincontrol.category.repository.CategoryRepository;
import com.fincontrol.shared.error.ApiException;
import com.fincontrol.transaction.repository.TransactionRepository;
import com.fincontrol.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {
    private final CategoryRepository categories;
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final UserRepository users;
    private final CategoryMapper mapper;

    public CategoryService(CategoryRepository categories, TransactionRepository transactions, BudgetRepository budgets,
                           UserRepository users, CategoryMapper mapper) {
        this.categories = categories;
        this.transactions = transactions;
        this.budgets = budgets;
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryDtos.Response> list(UUID userId, CategoryType type) {
        List<CategoryEntity> result = type == null
                ? categories.findAllByUserIdOrderByTypeAscNameAsc(userId)
                : categories.findAllByUserIdAndTypeOrderByNameAsc(userId, type);
        return result.stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public CategoryDtos.Response create(UUID userId, CategoryDtos.Request request) {
        ensureUnique(userId, request.type(), request.name(), null);
        CategoryEntity saved = categories.save(new CategoryEntity(users.getReferenceById(userId),
                request.name().trim(), request.type()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public CategoryDtos.Response update(UUID userId, UUID categoryId, CategoryDtos.Request request) {
        CategoryEntity entity = findOwned(userId, categoryId);
        ensureUnique(userId, request.type(), request.name(), categoryId);
        if (entity.getType() != request.type() && (transactions.existsByCategoryIdAndUserId(categoryId, userId)
                || budgets.existsByCategoryIdAndUserId(categoryId, userId))) {
            throw ApiException.conflict("Não é possível alterar o tipo de uma categoria vinculada a transações ou orçamentos.");
        }
        entity.update(request.name().trim(), request.type());
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(UUID userId, UUID categoryId) {
        CategoryEntity entity = findOwned(userId, categoryId);
        if (transactions.existsByCategoryIdAndUserId(categoryId, userId)) {
            throw ApiException.conflict("Não é possível excluir uma categoria vinculada a transações.");
        }
        if (budgets.existsByCategoryIdAndUserId(categoryId, userId)) {
            throw ApiException.conflict("Não é possível excluir uma categoria vinculada a orçamentos.");
        }
        categories.delete(entity);
    }

    private void ensureUnique(UUID userId, CategoryType type, String name, UUID exceptId) {
        boolean duplicate = categories.findAllByUserIdAndTypeOrderByNameAsc(userId, type).stream()
                .anyMatch(category -> !category.getId().equals(exceptId)
                        && category.getName().equalsIgnoreCase(name.trim()));
        if (duplicate) {
            throw ApiException.conflict("Já existe uma categoria com esse nome e tipo.");
        }
    }

    private CategoryEntity findOwned(UUID userId, UUID categoryId) {
        return categories.findByIdAndUserId(categoryId, userId).orElseThrow(() -> ApiException.notFound("Categoria"));
    }
}
