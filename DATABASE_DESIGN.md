# Parking Lot Database Design

## Interview opening

A strong way to begin the database discussion is:

> I would use a relational database such as PostgreSQL because parking, ticketing, spot allocation, and payments involve structured relationships, transactions, constraints, and strong consistency.

The database should support the following core workflows:

1. Configure parking lots, floors, gates, and spots.
2. Atomically allocate an available compatible spot.
3. Create a parking session and issue a ticket.
4. Calculate the fee when the customer exits.
5. Record one or more payment attempts.
6. Close the parking session and release the spot.
7. Show near-real-time availability on display boards.

## High-level data model

```text
parking_lot
    |
    +--- parking_floor
    |        |
    |        +--- parking_spot
    |
    +--- gate

vehicle
    |
    +--- parking_session
             |
             +--- payment
             |
             +--- applied pricing information
```

## Core tables

### `parking_lot`

Stores a physical parking facility.

```sql
CREATE TABLE parking_lot (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    address         TEXT,
    timezone        VARCHAR(50) NOT NULL,
    status          VARCHAR(30) NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);
```

Possible statuses include:

- `ACTIVE`
- `TEMPORARILY_CLOSED`
- `UNDER_MAINTENANCE`

The timezone matters because pricing, reporting, and business-day boundaries use local time.

### `parking_floor`

A parking lot can contain multiple floors.

```sql
CREATE TABLE parking_floor (
    id              BIGSERIAL PRIMARY KEY,
    parking_lot_id  BIGINT NOT NULL REFERENCES parking_lot(id),
    floor_number    INTEGER NOT NULL,
    name            VARCHAR(50),
    status          VARCHAR(30) NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,

    UNIQUE (parking_lot_id, floor_number)
);
```

The unique constraint prevents duplicate floor numbers within one parking lot.

### `parking_spot`

Represents each physical parking space.

```sql
CREATE TABLE parking_spot (
    id                  BIGSERIAL PRIMARY KEY,
    parking_floor_id    BIGINT NOT NULL REFERENCES parking_floor(id),
    spot_number         VARCHAR(30) NOT NULL,
    spot_type           VARCHAR(30) NOT NULL,
    status              VARCHAR(30) NOT NULL,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL,

    UNIQUE (parking_floor_id, spot_number)
);
```

Possible spot types:

- `MOTORCYCLE`
- `COMPACT`
- `LARGE`
- `ACCESSIBLE`
- `ELECTRIC`

Possible spot statuses:

- `AVAILABLE`
- `OCCUPIED`
- `RESERVED`
- `OUT_OF_SERVICE`

The `version` column can support **optimistic locking**. Optimistic locking detects that another transaction changed the row after it was read.

Occupancy could be derived exclusively from active parking sessions. However, keeping a status on `parking_spot` makes allocation and availability queries faster. This creates duplicated state, so the spot status and parking session must be changed in the same database transaction.

### `gate`

Represents an entrance or exit panel.

```sql
CREATE TABLE gate (
    id              BIGSERIAL PRIMARY KEY,
    parking_lot_id  BIGINT NOT NULL REFERENCES parking_lot(id),
    gate_number     VARCHAR(30) NOT NULL,
    gate_type       VARCHAR(20) NOT NULL,
    status          VARCHAR(30) NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,

    UNIQUE (parking_lot_id, gate_number)
);
```

`gate_type` can be `ENTRANCE`, `EXIT`, or `BOTH`. Gate status can be `ACTIVE`, `CLOSED`, or `OUT_OF_SERVICE`.

### `vehicle`

Stores reusable vehicle information.

```sql
CREATE TABLE vehicle (
    id                  BIGSERIAL PRIMARY KEY,
    license_number      VARCHAR(30) NOT NULL,
    registration_region VARCHAR(30),
    vehicle_type        VARCHAR(30) NOT NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL,

    UNIQUE (license_number, registration_region)
);
```

Possible vehicle types include `MOTORCYCLE`, `CAR`, `VAN`, and `TRUCK`.

For a small system, the license number could be stored directly in the parking session. A separate vehicle table becomes useful for returning customers, subscriptions, blacklisting, loyalty programs, and parking history.

#### Mapping Java inheritance

The Java model may contain `Car`, `Truck`, `Van`, and `Motorcycle` subclasses. This does not require four database tables. Since these classes currently have the same persistent attributes, one `vehicle` table with a `vehicle_type` discriminator is simpler. Separate subtype tables make sense only when the subtypes have significantly different persistent data.

### `parking_session`

`parking_session` is a more accurate name than `parking_ticket` because the row represents the complete lifecycle from entry to exit.

```sql
CREATE TABLE parking_session (
    id                  BIGSERIAL PRIMARY KEY,
    ticket_number       VARCHAR(50) NOT NULL UNIQUE,
    parking_lot_id      BIGINT NOT NULL REFERENCES parking_lot(id),
    vehicle_id          BIGINT NOT NULL REFERENCES vehicle(id),
    parking_spot_id     BIGINT NOT NULL REFERENCES parking_spot(id),
    entrance_gate_id    BIGINT REFERENCES gate(id),
    exit_gate_id        BIGINT REFERENCES gate(id),

    entry_time          TIMESTAMP NOT NULL,
    payment_time        TIMESTAMP,
    exit_time           TIMESTAMP,

    status              VARCHAR(30) NOT NULL,
    calculated_amount   DECIMAL(12, 2),
    currency            CHAR(3) NOT NULL DEFAULT 'INR',
    pricing_policy_id   BIGINT,

    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL
);
```

A typical state transition is:

```text
ACTIVE -> PAYMENT_PENDING -> PAID -> COMPLETED
```

Exceptional states can include:

- `PAYMENT_FAILED`
- `LOST_TICKET`
- `CANCELLED`

Payment time and exit time should be separate because a customer may pay at a kiosk and take several minutes to reach the exit. The business could allow, for example, 15 minutes to exit after payment before extra charges apply.

### `payment`

Payment should be stored separately because one parking session can have multiple payment attempts.

```sql
CREATE TABLE payment (
    id                      BIGSERIAL PRIMARY KEY,
    parking_session_id      BIGINT NOT NULL REFERENCES parking_session(id),
    payment_reference       VARCHAR(100) NOT NULL UNIQUE,
    idempotency_key         VARCHAR(100) NOT NULL UNIQUE,
    payment_method          VARCHAR(30) NOT NULL,
    status                  VARCHAR(30) NOT NULL,
    amount                  DECIMAL(12, 2) NOT NULL,
    currency                CHAR(3) NOT NULL,
    provider_transaction_id VARCHAR(100),
    failure_reason          VARCHAR(255),
    initiated_at            TIMESTAMP NOT NULL,
    completed_at            TIMESTAMP,
    created_at              TIMESTAMP NOT NULL
);
```

Payment methods can include:

- `CASH`
- `CREDIT_CARD`
- `DEBIT_CARD`
- `UPI`
- `WALLET`
- `COUPON`

Payment states can include `PENDING`, `COMPLETED`, `FAILED`, and `REFUNDED`.

#### Why maintain an idempotency key?

If an exit panel sends a payment request, times out, and retries, the customer must not be charged twice. Reusing the same idempotency key tells the payment service that the retry represents the same logical operation.

Never store a raw card number or CVV. Store only a token or transaction reference supplied by the payment provider.

## Pricing tables

Pricing rules should not be permanently hardcoded because rates may need to change without deploying the application.

### `pricing_policy`

```sql
CREATE TABLE pricing_policy (
    id              BIGSERIAL PRIMARY KEY,
    parking_lot_id  BIGINT NOT NULL REFERENCES parking_lot(id),
    name            VARCHAR(100) NOT NULL,
    effective_from  TIMESTAMP NOT NULL,
    effective_to    TIMESTAMP,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL
);
```

### `pricing_rule`

```sql
CREATE TABLE pricing_rule (
    id                  BIGSERIAL PRIMARY KEY,
    pricing_policy_id   BIGINT NOT NULL REFERENCES pricing_policy(id),
    vehicle_type        VARCHAR(30),
    spot_type           VARCHAR(30),
    start_minute        INTEGER NOT NULL,
    end_minute          INTEGER,
    rate                DECIMAL(12, 2) NOT NULL,
    billing_unit        VARCHAR(20) NOT NULL
);
```

The sample tiered policy is:

| Period | Rate |
|---|---:|
| First hour | $4.00 |
| Second and third hours | $3.50 per hour |
| Every later hour | $2.50 per hour |

Conceptually, its rules are:

```text
0-60 minutes     -> 4.00 per block
61-180 minutes   -> 3.50 per hour
181+ minutes     -> 2.50 per hour
```

Pricing rules can change while a vehicle is parked. A completed session should therefore preserve:

- The final amount charged
- The applied policy ID or version
- Optionally, a fee-calculation breakdown

An optional JSON breakdown might look like:

```json
{
  "firstHour": 4.00,
  "nextTwoHours": 7.00,
  "remainingHours": 5.00,
  "total": 16.00
}
```

The final amount should still be stored in a numeric column for filtering, aggregation, and reporting.

## Display-board availability

Initially, availability can be calculated directly from the spot table:

```sql
SELECT spot_type, COUNT(*)
FROM parking_spot
WHERE parking_floor_id = ?
  AND status = 'AVAILABLE'
GROUP BY spot_type;
```

For a high-traffic facility, continuously executing this aggregation can become expensive. Availability counters can then be maintained in Redis, a materialized view, or a read-model table updated through events.

```sql
CREATE TABLE spot_availability (
    parking_floor_id    BIGINT NOT NULL,
    spot_type           VARCHAR(30) NOT NULL,
    available_count     INTEGER NOT NULL,
    updated_at          TIMESTAMP NOT NULL,

    PRIMARY KEY (parking_floor_id, spot_type)
);
```

The database spot/session state remains the source of truth. Display counters are derived data and may be eventually consistent by a small amount of time.

## Preventing double allocation

The main concurrency problem is two entrance gates attempting to allocate the final compatible spot:

```text
Gate A reads C-101 as AVAILABLE
Gate B reads C-101 as AVAILABLE
Gate A allocates C-101
Gate B also allocates C-101
```

This is a race condition. PostgreSQL row locking can prevent it:

```sql
BEGIN;

SELECT id
FROM parking_spot
WHERE parking_floor_id = ?
  AND spot_type IN ('COMPACT', 'LARGE')
  AND status = 'AVAILABLE'
ORDER BY
    CASE spot_type
        WHEN 'COMPACT' THEN 1
        WHEN 'LARGE' THEN 2
    END,
    id
FOR UPDATE SKIP LOCKED
LIMIT 1;
```

- `FOR UPDATE` locks the selected spot until the transaction completes.
- `SKIP LOCKED` makes another entrance request ignore a spot already being allocated instead of waiting for it.

The same transaction then occupies the spot and creates the session:

```sql
UPDATE parking_spot
SET status = 'OCCUPIED',
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = ?;

INSERT INTO parking_session (..., status)
VALUES (..., 'ACTIVE');

COMMIT;
```

If session creation fails, the transaction rolls back and the spot remains available.

PostgreSQL can also enforce a final safety constraint:

```sql
CREATE UNIQUE INDEX one_active_session_per_spot
ON parking_session(parking_spot_id)
WHERE status IN ('ACTIVE', 'PAYMENT_PENDING', 'PAID');
```

This partial unique index prevents two active sessions from referencing the same spot, even if application logic contains a bug.

## Exit and payment transaction

Do not keep a database transaction or row lock open while calling an external payment provider. Network calls can be slow or time out.

A safer flow is:

```text
1. Read the active parking session.
2. Calculate the amount.
3. Create a PENDING payment with an idempotency key.
4. Call the payment provider outside a database transaction.
5. Mark the payment COMPLETED after provider success.
6. In one short database transaction:
   - mark the parking session PAID/COMPLETED;
   - record payment and exit timestamps;
   - mark the parking spot AVAILABLE.
7. Open the exit gate.
```

If the provider succeeds but the database update fails, a reconciliation job can locate completed payments whose parking sessions were not closed and repair them safely.

## Important indexes

Indexes should match frequent lookup and allocation queries:

```sql
CREATE INDEX idx_available_spots
ON parking_spot(parking_floor_id, spot_type, status);

CREATE INDEX idx_active_vehicle_session
ON parking_session(vehicle_id, status);

CREATE INDEX idx_session_entry_time
ON parking_session(entry_time);

CREATE INDEX idx_payment_session
ON payment(parking_session_id);

CREATE INDEX idx_session_status
ON parking_session(parking_lot_id, status);
```

Do not index every column. Indexes improve reads but use storage and make inserts and updates more expensive.

## Data that should not be stored unnecessarily

- Do not store `parking_duration`; calculate it from entry and exit timestamps.
- Do not store only a global free-capacity count without individual spot records.
- Do not store raw card numbers or CVVs.
- Do not use binary floating-point types such as `FLOAT` or Java `double` for money.

Use a fixed decimal type:

```sql
DECIMAL(12, 2)
```

Alternatively, store money in integer minor units:

```text
INR 105.50 -> 10,550 paise
```

## Optional tables for expanded requirements

| Table | When it is needed |
|---|---|
| `reservation` | Customers can prebook a spot |
| `customer` | Registered users or loyalty programs |
| `subscription` | Monthly or annual parking plans |
| `vehicle_pass` | Employee or resident parking |
| `coupon` | Promotion and validation rules |
| `payment_refund` | Partial or complete refunds |
| `spot_status_history` | Auditing and utilization analytics |
| `parking_session_event` | Full lifecycle/event audit |
| `electric_charging_session` | EV charging and energy billing |
| `operator_account` | Administrator and attendant authentication |

## Important trade-offs to discuss

| Decision | Advantage | Trade-off |
|---|---|---|
| Store spot status | Fast allocation and display queries | Duplicates state represented by active sessions |
| Derive spot status | One logical source of truth | Allocation queries become more expensive |
| Relational database | Transactions, constraints, and strong consistency | Horizontal scaling is more involved than a simple key-value store |
| Database row locking | Strong protection against double allocation | Lock contention must be controlled under high traffic |
| Optimistic locking | Performs well when conflicts are rare | Conflicting requests must retry |
| Redis availability counters | Very fast display reads | Counters can briefly differ from the database |
| Versioned pricing tables | Rates change without deployments | Pricing evaluation and configuration become more complex |
| Separate payment attempts | Correct retries and complete audit history | More states and reconciliation logic |

## Interview-ready answer

> I would use PostgreSQL because spot allocation and ticket/payment updates require strong consistency and transactions. The core tables would be `parking_lot`, `parking_floor`, `parking_spot`, `gate`, `vehicle`, `parking_session`, `payment`, and versioned pricing-policy tables.
>
> `parking_spot` maintains the physical spot type and operational status. `parking_session` represents the ticket lifecycle and connects a vehicle with its spot, entry and exit gates, timestamps, status, and calculated amount. Payments are separate because one session can have multiple payment attempts.
>
> The critical concurrency problem is preventing two entrances from allocating the same spot. I would select a compatible available spot using `FOR UPDATE SKIP LOCKED`, update it to occupied, and create the parking session in the same transaction. I would add a partial unique index to guarantee that only one active session can exist for a spot.
>
> Display-board counts are derived from spot state. Initially I would query the database; at higher scale, I would maintain counters in Redis or a dedicated read model. Pricing rules would be versioned in separate tables so rates can change without a deployment, while every completed session stores the amount actually charged and its applied policy version.
>
> At exit, payment is recorded separately with an idempotency key to prevent duplicate charges. After payment succeeds, a short transaction closes the session and releases the spot. I would not hold a database lock while calling the external payment provider.

This answer demonstrates relational modeling, normalization, transactions, concurrency control, payment correctness, indexing, scalability, and awareness of real-world trade-offs.
