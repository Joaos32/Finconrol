package com.fincontrol.transaction.repository;

import java.math.BigDecimal;

public interface TransactionCategoryTotalProjection {
    String getCategoryName();
    BigDecimal getAmount();
}
