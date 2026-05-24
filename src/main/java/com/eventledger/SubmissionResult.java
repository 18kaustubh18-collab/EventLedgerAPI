package com.eventledger;

public record SubmissionResult(TransactionEvent event, boolean created) {
}
