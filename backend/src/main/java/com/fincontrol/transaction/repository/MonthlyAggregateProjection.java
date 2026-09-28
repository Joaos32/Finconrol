package com.fincontrol.transaction.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface MonthlyAggregateProjection {
    LocalDate getMonth();
    BigDecimal getIncome();
    BigDecimal getExpense();
}
