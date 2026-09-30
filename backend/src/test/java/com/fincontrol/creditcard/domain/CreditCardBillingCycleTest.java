package com.fincontrol.creditcard.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreditCardBillingCycleTest {
    @Test
    void purchaseOnClosingDayBelongsToTheCycleClosingThatDay() {
        CreditCardBillingCycle cycle = CreditCardBillingCycle.forPurchase(
                LocalDate.of(2026, 11, 28), 28, 10);

        assertEquals(YearMonth.of(2026, 11), cycle.closingMonth());
        assertEquals(LocalDate.of(2026, 10, 29), cycle.periodStart());
        assertEquals(LocalDate.of(2026, 11, 28), cycle.closingDate());
        assertEquals(LocalDate.of(2026, 12, 10), cycle.dueDate());
    }

    @Test
    void purchaseAfterClosingDayBelongsToTheNextCycle() {
        CreditCardBillingCycle cycle = CreditCardBillingCycle.forPurchase(
                LocalDate.of(2026, 12, 29), 28, 10);

        assertEquals(YearMonth.of(2027, 1), cycle.closingMonth());
        assertEquals(LocalDate.of(2026, 12, 29), cycle.periodStart());
        assertEquals(LocalDate.of(2027, 1, 28), cycle.closingDate());
        assertEquals(LocalDate.of(2027, 2, 10), cycle.dueDate());
    }

    @Test
    void calculatesCycleSpanningFebruary() {
        CreditCardBillingCycle cycle = CreditCardBillingCycle.forPurchase(
                LocalDate.of(2026, 2, 27), 28, 5);

        assertEquals(YearMonth.of(2026, 2), cycle.closingMonth());
        assertEquals(LocalDate.of(2026, 1, 29), cycle.periodStart());
        assertEquals(LocalDate.of(2026, 2, 28), cycle.closingDate());
        assertEquals(LocalDate.of(2026, 3, 5), cycle.dueDate());
    }

    @Test
    void dueDateFallsOnConfiguredDayInTheMonthAfterClosing() {
        CreditCardBillingCycle cycle = CreditCardBillingCycle.forPurchase(
                LocalDate.of(2026, 12, 1), 20, 28);

        assertEquals(LocalDate.of(2027, 1, 28), cycle.dueDate());
    }

    @Test
    void rejectsClosingAndDueDaysOutsideTheSupportedRange() {
        assertThrows(IllegalArgumentException.class, () ->
                CreditCardBillingCycle.forPurchase(LocalDate.of(2026, 6, 1), 0, 10));
        assertThrows(IllegalArgumentException.class, () ->
                CreditCardBillingCycle.forPurchase(LocalDate.of(2026, 6, 1), 28, 29));
    }
}
