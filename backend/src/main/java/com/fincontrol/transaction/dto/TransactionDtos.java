package com.fincontrol.transaction.dto;

import com.fincontrol.transaction.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class TransactionDtos {
    private TransactionDtos() { }

    public record Request(
            @NotBlank(message = "Informe a descrição da transação.")
            @Size(max = 180, message = "A descrição deve ter no máximo 180 caracteres.")
            String description,
            @NotNull(message = "Informe o valor.")
            @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
            @Digits(integer = 17, fraction = 2, message = "O valor aceita até duas casas decimais.")
            BigDecimal amount,
            @NotNull(message = "Selecione receita ou despesa.")
            TransactionType type,
            @NotNull(message = "Selecione a conta.")
            UUID accountId,
            @NotNull(message = "Selecione a categoria.")
            UUID categoryId,
            @NotNull(message = "Informe a data da transação.")
            LocalDate transactionDate
    ) { }

    public record Response(UUID id, String description, BigDecimal amount, TransactionType type,
                           UUID accountId, String accountName, UUID categoryId, String categoryName,
                           LocalDate transactionDate, Instant createdAt, Instant updatedAt) { }

    public record PageResponse<T>(java.util.List<T> content, int page, int size, long totalElements,
                                  int totalPages, boolean first, boolean last) { }
}
