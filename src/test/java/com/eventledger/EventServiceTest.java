package com.eventledger;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventServiceTest {
    @Test
    void idempotentDuplicateSubmissionDoesNotChangeBalance() {
        EventService service = newService();
        TransactionEventRequest payload = event("evt-001", "acct-1", "CREDIT", "100.00", "2026-05-15T10:00:00Z");

        SubmissionResult first = service.submit(payload);
        SubmissionResult duplicate = service.submit(payload);

        assertTrue(first.created());
        assertFalse(duplicate.created());
        assertEquals("evt-001", duplicate.event().eventId());
        assertMoney("100.00", service.getBalances("acct-1").get("USD"));
    }

    @Test
    void outOfOrderEventsAreListedChronologically() {
        EventService service = newService();
        service.submit(event("evt-late", "acct-1", "CREDIT", "50.00", "2026-05-15T12:00:00Z"));
        service.submit(event("evt-early", "acct-1", "DEBIT", "10.00", "2026-05-15T09:00:00Z"));

        List<TransactionEvent> events = service.listEvents("acct-1");

        assertEquals("evt-early", events.get(0).eventId());
        assertEquals("evt-late", events.get(1).eventId());
    }

    @Test
    void balanceAddsCreditsAndSubtractsDebits() {
        EventService service = newService();
        service.submit(event("evt-1", "acct-2", "CREDIT", "150.00", "2026-05-15T10:00:00Z"));
        service.submit(event("evt-2", "acct-2", "DEBIT", "35.50", "2026-05-15T11:00:00Z"));
        service.submit(event("evt-3", "acct-2", "CREDIT", "4.50", "2026-05-15T12:00:00Z"));

        assertMoney("119.00", service.getBalances("acct-2").get("USD"));
    }

    @Test
    void validationRejectsBadInput() {
        EventService service = newService();
        assertValidation(() -> service.submit(new TransactionEventRequest(null, null, null, null, null, null, null)), "eventId is required");
        assertValidation(() -> service.submit(event("evt-bad-amount", "acct-3", "CREDIT", "0", "2026-05-15T10:00:00Z")), "amount must be greater than 0");
        assertValidation(() -> service.submit(event("evt-bad-type", "acct-3", "TRANSFER", "10", "2026-05-15T10:00:00Z")), "type must be CREDIT or DEBIT");
        assertValidation(() -> service.submit(event("evt-bad-time", "acct-3", "CREDIT", "10", "not-a-date")), "eventTimestamp must be a valid ISO 8601 instant");
    }

    private EventService newService() {
        return new EventService(new EventRepository());
    }

    private TransactionEventRequest event(String eventId, String accountId, String type, String amount, String timestamp) {
        return new TransactionEventRequest(
                eventId,
                accountId,
                type,
                new BigDecimal(amount),
                "USD",
                timestamp,
                Map.of("source", "test"));
    }

    private void assertValidation(Runnable action, String message) {
        ValidationException ex = assertThrows(ValidationException.class, action::run);
        assertEquals(message, ex.getMessage());
    }

    private void assertMoney(String expected, BigDecimal actual) {
        if (actual == null || actual.compareTo(new BigDecimal(expected)) != 0) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }
}
