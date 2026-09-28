package com.fincontrol.account.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccountBalanceProjection {
    UUID getAccountId();
    BigDecimal getCurrentBalance();
}
