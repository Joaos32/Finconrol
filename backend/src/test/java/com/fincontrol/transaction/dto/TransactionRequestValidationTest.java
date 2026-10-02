package com.fincontrol.transaction.dto;

import com.fincontrol.transaction.entity.TransactionType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsNonPositiveTransactionAmounts() {
        TransactionDtos.Request request = new TransactionDtos.Request("Compra", new BigDecimal("-1.00"),
                TransactionType.EXPENSE, UUID.randomUUID(), UUID.randomUUID(), LocalDate.now());

        assertTrue(validator.validate(request).stream().anyMatch(error -> error.getMessage().contains("maior que zero")));
    }

    @Test
    void rejectsInstallmentCountsAboveTheSupportedLimit() {
        TransactionDtos.Request request = new TransactionDtos.Request("Compra", new BigDecimal("120.00"),
                TransactionType.EXPENSE, null, UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), 25);

        assertTrue(validator.validate(request).stream().anyMatch(error -> error.getMessage().contains("24")));
    }
}
