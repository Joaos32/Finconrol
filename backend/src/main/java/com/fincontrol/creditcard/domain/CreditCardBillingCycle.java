package com.fincontrol.creditcard.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

/**
 * The statement period and dates assigned to a credit card purchase.
 */
public final class CreditCardBillingCycle {
    private final YearMonth closingMonth;
    private final LocalDate periodStart;
    private final LocalDate closingDate;
    private final LocalDate dueDate;

    private CreditCardBillingCycle(YearMonth closingMonth, int closingDay, int dueDay) {
        this.closingMonth = closingMonth;
        this.periodStart = closingMonth.minusMonths(1).atDay(closingDay).plusDays(1);
        this.closingDate = closingMonth.atDay(closingDay);
        this.dueDate = closingMonth.plusMonths(1).atDay(dueDay);
    }

    public static CreditCardBillingCycle forPurchase(LocalDate purchaseDate, int closingDay, int dueDay) {
        Objects.requireNonNull(purchaseDate, "purchaseDate must not be null");
        validateDay("closingDay", closingDay);
        validateDay("dueDay", dueDay);

        YearMonth closingMonth = YearMonth.from(purchaseDate);
        if (purchaseDate.getDayOfMonth() > closingDay) {
            closingMonth = closingMonth.plusMonths(1);
        }
        return new CreditCardBillingCycle(closingMonth, closingDay, dueDay);
    }

    private static void validateDay(String name, int day) {
        if (day < 1 || day > 28) {
            throw new IllegalArgumentException(name + " must be between 1 and 28");
        }
    }

    public YearMonth closingMonth() {
        return closingMonth;
    }

    public LocalDate periodStart() {
        return periodStart;
    }

    public LocalDate closingDate() {
        return closingDate;
    }

    public LocalDate dueDate() {
        return dueDate;
    }
}
