package com.fincontrol.account.controller;

import com.fincontrol.account.dto.AccountDtos;
import com.fincontrol.account.entity.AccountType;
import com.fincontrol.account.service.AccountService;
import com.fincontrol.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {
    private final AccountService service;
    private final CurrentUser currentUser;

    public AccountController(AccountService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<AccountDtos.Response> list() {
        return service.list(currentUser.id());
    }

    @GetMapping("/{id}")
    public AccountDtos.Response get(@PathVariable UUID id) {
        return service.get(currentUser.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountDtos.Response create(@Valid @RequestBody AccountDtos.Request request) {
        return service.create(currentUser.id(), request);
    }

    @PutMapping("/{id}")
    public AccountDtos.Response update(@PathVariable UUID id, @Valid @RequestBody AccountDtos.Request request) {
        return service.update(currentUser.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(currentUser.id(), id);
    }

    @GetMapping("/{id}/balance")
    public BalanceResponse balance(@PathVariable UUID id) {
        return new BalanceResponse(id, service.getBalance(currentUser.id(), id));
    }

    public record BalanceResponse(UUID accountId, BigDecimal currentBalance) { }
}
