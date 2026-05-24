package com.eventledger;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

public record TransactionEventRequest(
        @Schema(example = "evt-001")
        String eventId,

        @Schema(example = "acct-123")
        String accountId,

        @Schema(example = "CREDIT", allowableValues = {"CREDIT", "DEBIT"})
        String type,

        @Schema(example = "150.00")
        BigDecimal amount,

        @Schema(example = "USD")
        String currency,

        @Schema(example = "2026-05-15T14:02:11Z")
        String eventTimestamp,

        @Schema(example = "{\"source\":\"mainframe-batch\",\"batchId\":\"B-9042\"}")
        Map<String, Object> metadata
) {
}
