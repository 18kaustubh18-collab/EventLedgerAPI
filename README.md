<<<<<<< HEAD
# Event Ledger API

A Spring Boot implementation of the take-home Event Ledger API. It accepts financial transaction events, deduplicates repeated `eventId` submissions, tolerates out-of-order arrival, and computes account balances from the accepted ledger.

## Why This Approach

1. I used Spring Boot with Maven for standard build, run, API, and Swagger support.
2. I store events in memory with a `ConcurrentHashMap` where key  is `eventId`. This gives duplicate protection through `putIfAbsent`, which follows the major idempotency requirement.
3. Event listing sorts by `eventTimestamp` every time it is read. That makes response order independent of ingestion order.
4. Amounts use `BigDecimal` instead of floating point to avoid rounding errors in financial values.
5. Balances are grouped by currency in the `balances` field. A top-level `balance` is also returned for the simple single-currency case .

## Prerequisites

- JDK 17 or newer
- Maven 3.9 or newer

## Setup & Installation

1. Install the prerequisites if you don't already have them (macOS example):

```sh
# Java (via Homebrew)
brew install openjdk@17

# Maven
brew install maven
```

2. From the project root, download and build dependencies:

```sh
mvn clean install -DskipTests
```

This downloads all Maven dependencies and builds the project artifacts.

## Run

```sh
./scripts/run.sh
```

The server starts on `http://localhost:8080`. You can pass another port:

```sh
./scripts/run.sh 9090
```

Equivalent Maven command:

```sh
mvn spring-boot:run
```

Swagger UI is available at:

```text
http://localhost:8080/swagger
```

The OpenAPI document is available at:

```text
http://localhost:8080/v3/api-docs
```

## Test

```sh
./scripts/test.sh
```

Equivalent Maven command:

```sh
mvn test
```

The tests cover idempotency, out-of-order arrival, balance calculation, and validation failures.

## Endpoints

### Submit an Event

```http
POST /events
```

Example:

```sh
curl -i -X POST http://localhost:8080/events \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId": "evt-001",
    "accountId": "acct-123",
    "type": "CREDIT",
    "amount": 150.00,
    "currency": "USD",
    "eventTimestamp": "2026-05-15T14:02:11Z",
    "metadata": {
      "source": "mainframe-batch",
      "batchId": "B-9042"
    }
  }'
```

Returns `201 Created` for a new event. 
A duplicate `eventId` returns `200 OK` with the original event and does not change balance.

### Get an Event

```http
GET /events/{id}
```

### List Events for an Account

```http
GET /events?account={accountId}&page=0&size=10
```

Events are returned in chronological order by `eventTimestamp`.

The default page size is `10`, and pages are zero-based.

### Get Account Balance

```http
GET /accounts/{accountId}/balance
```

Example response:

```json
{
  "accountId": "acct-123",
  "balance": 150,
  "balances": {
    "USD": 150
  }
}
```

## Validation

The API returns `400 Bad Request` with a clear error message for missing required fields, non-positive amounts, unknown event types, and invalid timestamps.

