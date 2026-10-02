package com.fincontrol.transaction.dto;

import com.fincontrol.transaction.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
            UUID accountId,
            UUID cardId,
            @NotNull(message = "Selecione a categoria.")
            UUID categoryId,
            @NotNull(message = "Informe a data da transação.")
            LocalDate transactionDate,
            @Min(value = 1, message = "A compra deve ter entre 1 e 24 parcelas.")
            @Max(value = 24, message = "A compra deve ter entre 1 e 24 parcelas.")
            Integer installmentCount
    ) {
        public Request(String description, BigDecimal amount, TransactionType type, UUID accountId,
                       UUID cardId, UUID categoryId, LocalDate transactionDate) {
            this(description, amount, type, accountId, cardId, categoryId, transactionDate, null);
        }

        public Request(String description, BigDecimal amount, TransactionType type, UUID accountId,
                       UUID categoryId, LocalDate transactionDate) {
            this(description, amount, type, accountId, null, categoryId, transactionDate, null);
        }
    }

    public record Response(UUID id, String description, BigDecimal amount, TransactionType type,
                           UUID accountId, String accountName, UUID cardId, String cardName,
                           UUID categoryId, String categoryName,
                           LocalDate transactionDate, Instant createdAt, Instant updatedAt,
                           int installmentCount) { }

    public record PageResponse<T>(java.util.List<T> content, int page, int size, long totalElements,
                                  int totalPages, boolean first, boolean last) { }
}
