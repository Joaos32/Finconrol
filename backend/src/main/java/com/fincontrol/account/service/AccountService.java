package com.fincontrol.account.service;

import com.fincontrol.account.dto.AccountDtos;
import com.fincontrol.account.entity.AccountEntity;
import com.fincontrol.account.mapper.AccountMapper;
import com.fincontrol.account.repository.AccountBalanceProjection;
import com.fincontrol.account.repository.AccountRepository;
import com.fincontrol.category.repository.CategoryRepository;
import com.fincontrol.shared.error.ApiException;
import com.fincontrol.user.repository.UserRepository;
import com.fincontrol.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AccountService {
    private final AccountRepository accounts;
    private final UserRepository users;
    private final TransactionRepository transactions;
    private final AccountMapper mapper;

    public AccountService(AccountRepository accounts, UserRepository users, TransactionRepository transactions,
                          AccountMapper mapper) {
        this.accounts = accounts;
        this.users = users;
        this.transactions = transactions;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AccountDtos.Response> list(UUID userId) {
        List<AccountEntity> entities = accounts.findAllByUserIdOrderByNameAsc(userId);
        Map<UUID, BigDecimal> balances = accounts.findBalancesByUserId(userId).stream()
                .collect(Collectors.toMap(AccountBalanceProjection::getAccountId, AccountBalanceProjection::getCurrentBalance));
        return entities.stream().map(account -> response(account, balances.getOrDefault(account.getId(), account.getInitialBalance()))).toList();
    }

    @Transactional(readOnly = true)
    public AccountDtos.Response get(UUID userId, UUID accountId) {
        AccountEntity account = findOwned(userId, accountId);
        BigDecimal balance = accounts.findBalancesByUserId(userId).stream()
                .filter(row -> row.getAccountId().equals(accountId))
                .map(AccountBalanceProjection::getCurrentBalance)
                .findFirst().orElse(account.getInitialBalance());
        return response(account, balance);
    }

    @Transactional
    public AccountDtos.Response create(UUID userId, AccountDtos.Request request) {
        AccountEntity entity = new AccountEntity(users.getReferenceById(userId), request.name().trim(),
                request.type(), request.initialBalance());
        AccountEntity saved = accounts.save(entity);
        return response(saved, saved.getInitialBalance());
    }

    @Transactional
    public AccountDtos.Response update(UUID userId, UUID accountId, AccountDtos.Request request) {
        AccountEntity entity = findOwned(userId, accountId);
        entity.update(request.name().trim(), request.type(), request.initialBalance());
        accounts.flush();
        return response(entity, getBalance(userId, accountId, entity.getInitialBalance()));
    }

    @Transactional
    public void delete(UUID userId, UUID accountId) {
        AccountEntity entity = findOwned(userId, accountId);
        if (transactions.existsByAccountIdAndUserId(accountId, userId)) {
            throw ApiException.conflict("Não é possível excluir uma conta vinculada a transações.");
        }
        accounts.delete(entity);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID userId, UUID accountId) {
        AccountEntity account = findOwned(userId, accountId);
        return getBalance(userId, accountId, account.getInitialBalance());
    }

    private BigDecimal getBalance(UUID userId, UUID accountId, BigDecimal initialBalance) {
        return accounts.findBalancesByUserId(userId).stream()
                .filter(row -> row.getAccountId().equals(accountId))
                .map(AccountBalanceProjection::getCurrentBalance)
                .findFirst().orElse(initialBalance);
    }

    private AccountEntity findOwned(UUID userId, UUID accountId) {
        return accounts.findByIdAndUserId(accountId, userId).orElseThrow(() -> ApiException.notFound("Conta"));
    }

    private AccountDtos.Response response(AccountEntity entity, BigDecimal currentBalance) {
        AccountDtos.Details details = mapper.toDetails(entity);
        return new AccountDtos.Response(details.id(), details.name(), details.type(), details.initialBalance(),
                currentBalance, details.createdAt(), details.updatedAt());
    }
}
