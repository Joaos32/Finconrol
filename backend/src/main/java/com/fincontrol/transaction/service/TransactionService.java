package com.fincontrol.transaction.service;

import com.fincontrol.account.entity.AccountEntity;
import com.fincontrol.account.repository.AccountRepository;
import com.fincontrol.category.entity.CategoryEntity;
import com.fincontrol.category.repository.CategoryRepository;
import com.fincontrol.shared.error.ApiException;
import com.fincontrol.transaction.dto.TransactionDtos;
import com.fincontrol.transaction.entity.TransactionEntity;
import com.fincontrol.transaction.entity.TransactionType;
import com.fincontrol.transaction.mapper.TransactionMapper;
import com.fincontrol.transaction.repository.TransactionRepository;
import com.fincontrol.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {
    private final TransactionRepository transactions;
    private final AccountRepository accounts;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final TransactionMapper mapper;

    public TransactionService(TransactionRepository transactions, AccountRepository accounts, CategoryRepository categories,
                              UserRepository users, TransactionMapper mapper) {
        this.transactions = transactions;
        this.accounts = accounts;
        this.categories = categories;
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public TransactionDtos.PageResponse<TransactionDtos.Response> list(
            UUID userId, LocalDate startDate, LocalDate endDate, TransactionType type,
            UUID categoryId, UUID accountId, int page, int size) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw ApiException.badRequest("A data inicial deve ser anterior ou igual à data final.");
        }
        List<Specification<TransactionEntity>> filters = new ArrayList<>();
        filters.add((root, query, cb) -> cb.equal(root.get("user").get("id"), userId));
        if (startDate != null) filters.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
        if (endDate != null) filters.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("transactionDate"), endDate));
        if (type != null) filters.add((root, query, cb) -> cb.equal(root.get("type"), type));
        if (categoryId != null) filters.add((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        if (accountId != null) filters.add((root, query, cb) -> cb.equal(root.get("account").get("id"), accountId));
        Specification<TransactionEntity> specification = filters.getFirst();
        for (int index = 1; index < filters.size(); index++) {
            specification = specification.and(filters.get(index));
        }
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "transactionDate").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<TransactionEntity> result = transactions.findAll(specification, pageable);
        Page<TransactionDtos.Response> mapped = result.map(mapper::toResponse);
        return new TransactionDtos.PageResponse<>(mapped.getContent(), mapped.getNumber(), mapped.getSize(),
                mapped.getTotalElements(), mapped.getTotalPages(), mapped.isFirst(), mapped.isLast());
    }

    @Transactional(readOnly = true)
    public TransactionDtos.Response get(UUID userId, UUID transactionId) {
        return mapper.toResponse(findOwned(userId, transactionId));
    }

    @Transactional
    public TransactionDtos.Response create(UUID userId, TransactionDtos.Request request) {
        AccountEntity account = findAccount(userId, request.accountId());
        CategoryEntity category = findCategory(userId, request.categoryId());
        validateCategoryType(request.type(), category);
        TransactionEntity entity = new TransactionEntity(users.getReferenceById(userId), account, category,
                request.description().trim(), request.amount(), request.type(), request.transactionDate());
        return mapper.toResponse(transactions.save(entity));
    }

    @Transactional
    public TransactionDtos.Response update(UUID userId, UUID transactionId, TransactionDtos.Request request) {
        TransactionEntity entity = findOwned(userId, transactionId);
        AccountEntity account = findAccount(userId, request.accountId());
        CategoryEntity category = findCategory(userId, request.categoryId());
        validateCategoryType(request.type(), category);
        entity.update(account, category, request.description().trim(), request.amount(), request.type(), request.transactionDate());
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(UUID userId, UUID transactionId) {
        transactions.delete(findOwned(userId, transactionId));
    }

    @Transactional(readOnly = true)
    public List<TransactionDtos.Response> recent(UUID userId, int limit) {
        return transactions.findAllByUserIdOrderByTransactionDateDescCreatedAtDesc(userId, PageRequest.of(0, limit))
                .map(mapper::toResponse).getContent();
    }

    private TransactionEntity findOwned(UUID userId, UUID transactionId) {
        return transactions.findOne((root, query, cb) -> cb.and(
                        cb.equal(root.get("id"), transactionId),
                        cb.equal(root.get("user").get("id"), userId)))
                .orElseThrow(() -> ApiException.notFound("Transação"));
    }

    private AccountEntity findAccount(UUID userId, UUID accountId) {
        return accounts.findByIdAndUserId(accountId, userId).orElseThrow(() -> ApiException.notFound("Conta"));
    }

    private CategoryEntity findCategory(UUID userId, UUID categoryId) {
        return categories.findByIdAndUserId(categoryId, userId).orElseThrow(() -> ApiException.notFound("Categoria"));
    }

    private void validateCategoryType(TransactionType type, CategoryEntity category) {
        if (!category.getType().name().equals(type.name())) {
            throw ApiException.badRequest("Categoria incompatível com o tipo da transação.");
        }
    }
}
