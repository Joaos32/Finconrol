package com.fincontrol.account.dto;

import com.fincontrol.account.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class AccountDtos {
    private AccountDtos() { }

    public record Request(
            @NotBlank(message = "Informe o nome da conta.")
            @Size(max = 100, message = "O nome da conta deve ter no máximo 100 caracteres.")
            String name,
            @NotNull(message = "Selecione o tipo da conta.")
            AccountType type,
            @NotNull(message = "Informe o saldo inicial.")
            @DecimalMin(value = "0.00", message = "O saldo inicial não pode ser negativo.")
            @Digits(integer = 17, fraction = 2, message = "O saldo inicial aceita até duas casas decimais.")
            BigDecimal initialBalance
    ) { }

    public record Details(UUID id, String name, AccountType type, BigDecimal initialBalance,
                          Instant createdAt, Instant updatedAt) { }

    public record Response(UUID id, String name, AccountType type, BigDecimal initialBalance,
                           BigDecimal currentBalance, Instant createdAt, Instant updatedAt) { }
}
