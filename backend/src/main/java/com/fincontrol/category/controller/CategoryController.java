package com.fincontrol.category.controller;

import com.fincontrol.category.dto.CategoryDtos;
import com.fincontrol.category.entity.CategoryType;
import com.fincontrol.category.service.CategoryService;
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
@RequestMapping("/api/categories")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {
    private final CategoryService service;
    private final CurrentUser currentUser;

    public CategoryController(CategoryService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<CategoryDtos.Response> list(@RequestParam(required = false) CategoryType type) {
        return service.list(currentUser.id(), type);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDtos.Response create(@Valid @RequestBody CategoryDtos.Request request) {
        return service.create(currentUser.id(), request);
    }

    @PutMapping("/{id}")
    public CategoryDtos.Response update(@PathVariable UUID id, @Valid @RequestBody CategoryDtos.Request request) {
        return service.update(currentUser.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(currentUser.id(), id);
    }
}
