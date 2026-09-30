package com.fincontrol.creditcard.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface CreditCardOutstandingProjection {
    UUID getCardId();
    BigDecimal getOutstandingAmount();
}
