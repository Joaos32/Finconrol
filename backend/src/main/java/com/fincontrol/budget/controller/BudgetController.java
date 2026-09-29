package com.fincontrol.budget.controller;

import com.fincontrol.budget.dto.BudgetDtos;
import com.fincontrol.budget.service.BudgetService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {
    private final BudgetService service;
    private final CurrentUser currentUser;

    public BudgetController(BudgetService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<BudgetDtos.Response> list(@RequestParam String month) {
        return service.list(currentUser.id(), month);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetDtos.Response create(@Valid @RequestBody BudgetDtos.Request request) {
        return service.create(currentUser.id(), request);
    }

    @PutMapping("/{id}")
    public BudgetDtos.Response update(@PathVariable UUID id, @Valid @RequestBody BudgetDtos.Request request) {
        return service.update(currentUser.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(currentUser.id(), id);
    }
}
