package com.fincontrol.budget.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface BudgetExpenseTotalProjection {
    UUID getCategoryId();
    BigDecimal getSpentAmount();
}
