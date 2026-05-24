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

    public List<TransactionEvent> findByAccount(String accountId, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("page must be >= 0 and size must be > 0");
        }
        List<TransactionEvent> all = findByAccount(accountId);
        if (all.isEmpty()) {
            return List.of();
        }
        int from = page * size;
        if (from >= all.size()) {
            throw new PageOutOfRangeException("this many record doesn't exist");
        }
        int to = Math.min(from + size, all.size());
        return new ArrayList<>(all.subList(from, to));
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
