package com.fincontrol.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

public final class BudgetDtos {
    private BudgetDtos() { }

    public record Request(
            @NotNull(message = "Selecione uma categoria de despesa.")
            UUID categoryId,
            @NotBlank(message = "Informe o mês do orçamento.")
            @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Informe o mês no formato AAAA-MM.")
            String month,
            @NotNull(message = "Informe o limite do orçamento.")
            @DecimalMin(value = "0.01", message = "O limite deve ser maior que zero.")
            @Digits(integer = 17, fraction = 2, message = "O limite deve ter no máximo duas casas decimais.")
            BigDecimal limitAmount
    ) { }

    public record Response(
            UUID id,
            UUID categoryId,
            String categoryName,
            String month,
            BigDecimal limitAmount,
            BigDecimal spentAmount,
            BigDecimal remainingAmount,
            BigDecimal percentageUsed
    ) { }
}
