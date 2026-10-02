package com.fincontrol.creditcard.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Splits a merchant purchase into exact-cent charges across consecutive invoice cycles. */
public final class CreditCardInstallmentSchedule {
    public static final int MAX_INSTALLMENTS = 24;

    private CreditCardInstallmentSchedule() { }

    public static List<Installment> create(BigDecimal total, int count, YearMonth firstClosingMonth) {
        Objects.requireNonNull(total, "total must not be null");
        Objects.requireNonNull(firstClosingMonth, "firstClosingMonth must not be null");
        if (total.signum() <= 0 || count < 1 || count > MAX_INSTALLMENTS) {
            throw new IllegalArgumentException("A positive amount and 1 to 24 installments are required");
        }

        BigInteger totalCents;
        try {
            totalCents = total.movePointRight(2).toBigIntegerExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("The total must be representable in cents", exception);
        }
        BigInteger[] split = totalCents.divideAndRemainder(BigInteger.valueOf(count));
        BigInteger regularCents = split[0];
        BigInteger remainderCents = split[1];
        if (regularCents.signum() == 0) {
            throw new IllegalArgumentException("Each installment must be at least one cent");
        }
        List<Installment> schedule = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            BigInteger cents = regularCents.add(index == count - 1 ? remainderCents : BigInteger.ZERO);
            schedule.add(new Installment(index + 1, new BigDecimal(cents, 2), firstClosingMonth.plusMonths(index)));
        }
        return List.copyOf(schedule);
    }

    public record Installment(int number, BigDecimal amount, YearMonth closingMonth) { }
}
