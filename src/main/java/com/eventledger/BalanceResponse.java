package com.eventledger;

import java.math.BigDecimal;
import java.util.Map;

public record BalanceResponse(
        String accountId,
        BigDecimal balance,
        Map<String, BigDecimal> balances
) {
}
