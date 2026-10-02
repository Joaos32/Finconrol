package com.fincontrol.creditcard.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreditCardInstallmentScheduleTest {
    @Test
    void distributesRemainderToLastInstallmentAndAdvancesOneBillingMonthAtATime() {
        var schedule = CreditCardInstallmentSchedule.create(new BigDecimal("100.00"), 3,
                YearMonth.of(2026, 6));

        assertEquals(3, schedule.size());
        assertEquals(new BigDecimal("33.33"), schedule.get(0).amount());
        assertEquals(new BigDecimal("33.33"), schedule.get(1).amount());
        assertEquals(new BigDecimal("33.34"), schedule.get(2).amount());
        assertEquals(YearMonth.of(2026, 6), schedule.get(0).closingMonth());
        assertEquals(YearMonth.of(2026, 7), schedule.get(1).closingMonth());
        assertEquals(YearMonth.of(2026, 8), schedule.get(2).closingMonth());
        assertEquals(new BigDecimal("100.00"), schedule.stream()
                .map(CreditCardInstallmentSchedule.Installment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void supportsTwoThroughTwentyFourInstallmentsAndPreservesWholeAmountForOne() {
        assertEquals(new BigDecimal("10.00"), CreditCardInstallmentSchedule
                .create(new BigDecimal("10.00"), 1, YearMonth.of(2026, 12)).getFirst().amount());
        assertEquals(24, CreditCardInstallmentSchedule
                .create(new BigDecimal("24.00"), 24, YearMonth.of(2026, 12)).size());
    }

    @Test
    void rejectsAmountsThatCannotBeRepresentedInCentsAndInvalidCounts() {
        assertThrows(IllegalArgumentException.class, () -> CreditCardInstallmentSchedule
                .create(new BigDecimal("10.001"), 3, YearMonth.of(2026, 6)));
        assertThrows(IllegalArgumentException.class, () -> CreditCardInstallmentSchedule
                .create(new BigDecimal("10.00"), 0, YearMonth.of(2026, 6)));
        assertThrows(IllegalArgumentException.class, () -> CreditCardInstallmentSchedule
                .create(new BigDecimal("10.00"), 25, YearMonth.of(2026, 6)));
        assertThrows(IllegalArgumentException.class, () -> CreditCardInstallmentSchedule
                .create(new BigDecimal("0.01"), 2, YearMonth.of(2026, 6)));
    }
}
