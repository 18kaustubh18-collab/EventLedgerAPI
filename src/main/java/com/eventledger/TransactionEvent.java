package com.eventledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record TransactionEvent(
        String eventId,
        String accountId,
        EventType type,
        BigDecimal amount,
        String currency,
        Instant eventTimestamp,
        Map<String, Object> metadata
) {
}
