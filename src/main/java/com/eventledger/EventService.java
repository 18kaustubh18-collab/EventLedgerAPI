package com.eventledger;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EventService {
    private final EventRepository repository;

    public EventService(EventRepository repository) {
        this.repository = repository;
    }

    public SubmissionResult submit(TransactionEventRequest payload) {
        TransactionEvent event = parseAndValidate(payload);
        return repository.saveIfAbsent(event);
    }

    public TransactionEvent getEvent(String eventId) {
        return repository.findById(eventId).orElse(null);
    }

    public List<TransactionEvent> listEvents(String accountId) {
        requireNonBlank(accountId, "account query parameter is required");
        return repository.findByAccount(accountId);
    }

    public Map<String, BigDecimal> getBalances(String accountId) {
        requireNonBlank(accountId, "accountId is required");
        return repository.balancesByCurrency(accountId);
    }

    private TransactionEvent parseAndValidate(TransactionEventRequest payload) {
        if (payload == null) {
            throw new ValidationException("request body is required");
        }
        String eventId = requiredString(payload.eventId(), "eventId");
        String accountId = requiredString(payload.accountId(), "accountId");
        String typeValue = requiredString(payload.type(), "type");
        BigDecimal amount = requiredAmount(payload.amount());
        String currency = requiredString(payload.currency(), "currency");
        String timestampValue = requiredString(payload.eventTimestamp(), "eventTimestamp");
        EventType type = parseType(typeValue);
        Instant timestamp = parseTimestamp(timestampValue);

        Map<String, Object> metadata = payload.metadata() != null
                ? coerceObjectMap(payload.metadata())
                : Map.of();

        return new TransactionEvent(
                eventId,
                accountId,
                type,
                amount,
                currency,
                timestamp,
                metadata);
    }

    private String requiredString(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
        return value;
    }

    private BigDecimal requiredAmount(BigDecimal amount) {
        if (amount == null) {
            throw new ValidationException("amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount must be greater than 0");
        }
        return amount;
    }

    private Map<String, Object> coerceObjectMap(Map<?, ?> input) {
        Map<String, Object> output = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new ValidationException("metadata keys must be strings");
            }
            output.put(key, entry.getValue());
        }
        return output;
    }

    private EventType parseType(String value) {
        try {
            return EventType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("type must be CREDIT or DEBIT");
        }
    }

    private Instant parseTimestamp(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw new ValidationException("eventTimestamp must be a valid ISO 8601 instant");
        }
    }

    private void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
    }
}
