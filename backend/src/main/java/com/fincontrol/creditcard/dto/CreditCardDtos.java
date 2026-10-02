package com.fincontrol.creditcard.dto;

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
import java.util.List;
import java.util.UUID;

public final class CreditCardDtos {
    private CreditCardDtos() { }

    public record Request(
            @NotBlank(message = "Informe o nome do cartão.")
            @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
            String name,
            @NotNull(message = "Informe o limite do cartão.")
            @DecimalMin(value = "0.01", message = "O limite deve ser maior que zero.")
            @Digits(integer = 17, fraction = 2, message = "O limite aceita até duas casas decimais.")
            BigDecimal creditLimit,
            @Min(value = 1, message = "O fechamento deve ser entre os dias 1 e 28.")
            @Max(value = 28, message = "O fechamento deve ser entre os dias 1 e 28.")
            int closingDay,
            @Min(value = 1, message = "O vencimento deve ser entre os dias 1 e 28.")
            @Max(value = 28, message = "O vencimento deve ser entre os dias 1 e 28.")
            int dueDay
    ) { }

    public record Response(UUID id, String name, BigDecimal creditLimit, int closingDay, int dueDay,
                           BigDecimal outstandingAmount, BigDecimal availableLimit,
                           Instant createdAt, Instant updatedAt) { }

    public record PaymentRequest(@NotNull(message = "Selecione a conta de pagamento.") UUID accountId) { }

    public record InvoiceItem(String description, BigDecimal amount,
                              Integer installmentNumber, Integer installmentCount) { }

    public record InvoiceResponse(UUID id, UUID cardId, String cardName, String month,
                                  LocalDate periodStart, LocalDate closingDate, LocalDate dueDate,
                                  BigDecimal totalAmount, boolean paid, Instant paidAt,
                                  UUID paymentAccountId, String paymentAccountName,
                                  List<InvoiceItem> items) { }
}
