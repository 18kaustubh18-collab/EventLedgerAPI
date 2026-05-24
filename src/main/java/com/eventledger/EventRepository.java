package com.eventledger;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class EventRepository {
    private final ConcurrentHashMap<String, TransactionEvent> eventsById = new ConcurrentHashMap<>();

    public SubmissionResult saveIfAbsent(TransactionEvent event) {
        TransactionEvent existing = eventsById.putIfAbsent(event.eventId(), event);
        if (existing != null) {
            return new SubmissionResult(existing, false);
        }
        return new SubmissionResult(event, true);
    }

    public Optional<TransactionEvent> findById(String eventId) {
        return Optional.ofNullable(eventsById.get(eventId));
    }

    public List<TransactionEvent> findByAccount(String accountId) {
        List<TransactionEvent> events = new ArrayList<>();
        for (TransactionEvent event : eventsById.values()) {
            if (event.accountId().equals(accountId)) {
                events.add(event);
            }
        }
        events.sort(Comparator
                .comparing(TransactionEvent::eventTimestamp)
                .thenComparing(TransactionEvent::eventId));
        return events;
    }

    public Map<String, BigDecimal> balancesByCurrency(String accountId) {
        Map<String, BigDecimal> balances = new java.util.TreeMap<>();
        for (TransactionEvent event : eventsById.values()) {
            if (!event.accountId().equals(accountId)) {
                continue;
            }
            BigDecimal signedAmount = event.type() == EventType.CREDIT
                    ? event.amount()
                    : event.amount().negate();
            balances.merge(event.currency(), signedAmount, BigDecimal::add);
        }
        return balances;
    }
}
