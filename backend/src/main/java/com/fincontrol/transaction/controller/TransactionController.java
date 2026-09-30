package com.fincontrol.transaction.controller;

import com.fincontrol.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.fincontrol.transaction.dto.TransactionDtos;
import com.fincontrol.transaction.entity.TransactionType;
import com.fincontrol.transaction.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/transactions")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {
    private final TransactionService service;
    private final CurrentUser currentUser;

    public TransactionController(TransactionService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public TransactionDtos.PageResponse<TransactionDtos.Response> list(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID cardId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(currentUser.id(), startDate, endDate, type, categoryId, accountId, cardId, page, size);
    }

    @GetMapping("/{id}")
    public TransactionDtos.Response get(@PathVariable UUID id) {
        return service.get(currentUser.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionDtos.Response create(@Valid @RequestBody TransactionDtos.Request request) {
        return service.create(currentUser.id(), request);
    }

    @PutMapping("/{id}")
    public TransactionDtos.Response update(@PathVariable UUID id, @Valid @RequestBody TransactionDtos.Request request) {
        return service.update(currentUser.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(currentUser.id(), id);
    }
}
