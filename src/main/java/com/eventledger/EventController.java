package com.eventledger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Event Ledger")
public class EventController {
    private static final String ACCOUNT_NOT_FOUND = "Account no. doesn't exist";

    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    @PostMapping("/events")
    @Operation(summary = "Submit a transaction event")
    @ApiResponse(responseCode = "201", description = "Event created")
    @ApiResponse(responseCode = "409", description = "Event ID already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<?> submitEvent(@RequestBody TransactionEventRequest payload) {
        SubmissionResult result = service.submit(payload);
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result.event());
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("event id already exists"));
    }

    @GetMapping("/events/{id}")
    @Operation(summary = "Retrieve a single event by ID")
    @ApiResponse(responseCode = "200", description = "Event found")
    @ApiResponse(responseCode = "404", description = "Event not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<?> getEvent(@PathVariable String id) {
        TransactionEvent event = service.getEvent(id);
        if (event == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("event not found"));
        }
        return ResponseEntity.ok(event);
    }

    @GetMapping("/events")
    @Operation(summary = "List events for an account ordered by event timestamp")
    @ApiResponse(responseCode = "200", description = "Events for the account")
    @ApiResponse(responseCode = "404", description = "Account not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<?> listEvents(@RequestParam String account,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        List<TransactionEvent> events = service.listEvents(account, page, size);
        if (events.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ACCOUNT_NOT_FOUND));
        }
        return ResponseEntity.ok(events);
    }

    @GetMapping("/accounts/{accountId}/balance")
    @Operation(summary = "Get the current computed balance for an account")
    public BalanceResponse getBalance(@PathVariable String accountId) {
        Map<String, BigDecimal> balances = service.getBalances(accountId);
        BigDecimal balance = balances.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BalanceResponse(accountId, balance, new LinkedHashMap<>(balances));
    }
}
