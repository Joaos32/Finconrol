package com.fincontrol.category.dto;

import com.fincontrol.category.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class CategoryDtos {
    private CategoryDtos() { }

    public record Request(
            @NotBlank(message = "Informe o nome da categoria.")
            @Size(max = 80, message = "O nome da categoria deve ter no máximo 80 caracteres.")
            String name,
            @NotNull(message = "Selecione o tipo da categoria.")
            CategoryType type
    ) { }

    public record Response(UUID id, String name, CategoryType type, Instant createdAt) { }
}
