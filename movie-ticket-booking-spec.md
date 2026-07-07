# Movie Ticket Booking System — Build Specification

## Assignment Context
This is a 48-hour SDE-2 take-home assignment for a Java Developer role. Build a movie ticket booking system supporting multiple cities, theaters, screens, shows, and seat-level booking with correct concurrency handling (no double-booking), pricing tiers, discounts, cancellations with refunds, and non-blocking notifications.

**Note on scope**: This spec deliberately excludes UI, deployment/CI-CD, distributed systems/microservices, advanced auth (OAuth/SSO/MFA), and production-grade observability, per assignment constraints.

---

## 1. Tech Stack
- **Language**: Java 17 (LTS — required for Spring Boot 3.x)
- **Framework**: Spring Boot 3.x
- **Security**: Spring Security — HTTP Basic Auth + method-level RBAC (see §7). BCrypt for password hashing.
- **Database**: PostgreSQL 16, run via Docker
- **ORM**: Spring Data JPA / Hibernate
- **Build tool**: Maven
- **Testing**: JUnit 5 + Mockito for unit tests; Spring Boot Test + Testcontainers (Postgres module) for integration tests with real concurrency (see §14)
- **Scheduling**: Spring `@Scheduled` for the hold-expiry sweeper (hygiene only, not correctness-critical — see §5)
- **Async**: Spring `@Async` for notifications (mocked/logged, no real message broker)
- **Locking**: Database-level only (pessimistic row locks + composite primary key). **No Redis** — explicitly decided against, to be documented in README (see §13 for rationale to include)

---

## 2. Docker Setup

**docker-compose.yml** (place at repo root):
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16
    container_name: movie-booking-db
    environment:
      POSTGRES_DB: movie_booking
      POSTGRES_USER: booking_user
      POSTGRES_PASSWORD: booking_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

Run with: `docker compose up -d`

**application.yml**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/movie_booking
    username: booking_user
    password: booking_pass
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate   # schema managed via Flyway migrations, not auto-DDL
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

Use **Flyway** for schema migrations (`src/main/resources/db/migration/V1__init_schema.sql`, `V2__seed_reference_data.sql`, etc.) rather than Hibernate auto-DDL, so the schema is explicit and reviewable.

---

## 3. Domain Model / Entities

- **City** — id, name
- **Theater** — id, name, cityId (FK), address
- **Screen** — id, theaterId (FK), name, totalSeats
- **Seat** — id, screenId (FK), seatNumber, seatType (REGULAR / PREMIUM / RECLINER) — physical, permanent, created once per screen
- **Movie** — id, title, durationMinutes, language, genre
- **Show** — id, movieId (FK), screenId (FK), startTime, endTime — no direct pricing FK; price is computed per seat (see §10)
- **SeatTypePrice** — id, seatType (REGULAR / PREMIUM / RECLINER, unique), basePrice — admin-configurable base price per seat category
- **WeekendPricingRule** — id, multiplier — single admin-configurable factor applied when a show falls on a weekend
- **MovieShowSeat** — composite key (showId, seatId); userId (nullable); status (AVAILABLE / LOCKED / CONFIRMED); expiresAt (nullable). **This is the seat map for a given show** — see §5 for full design rationale.
- **Booking** — id, userId (FK), showId (FK), status (CONFIRMED / CANCELLED), totalAmount, discountCodeId (nullable FK), idempotencyKey (nullable, unique per user), createdAt
- **BookingSeat** — join table: bookingId (FK), seatId (FK) — supports multi-seat bookings
- **DiscountCode** — id, code, type (FLAT / PERCENTAGE), value, minOrderAmount, validFrom, validTo
- **RefundPolicy** — id, cutoffHoursBeforeShow, refundPercentage
- **User** — id, name, email, passwordHash, role (ADMIN / CUSTOMER)

**Not a persisted entity**: `PaymentGateway` is a mocked service (see §11), not a database-backed model — consistent with notifications being mocked/logged rather than stored.

---

## 4. Database Schema (Flyway migration)

```sql
CREATE TABLE city (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE theater (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    city_id BIGINT NOT NULL REFERENCES city(id),
    address VARCHAR(255)
);

CREATE TABLE screen (
    id BIGSERIAL PRIMARY KEY,
    theater_id BIGINT NOT NULL REFERENCES theater(id),
    name VARCHAR(50) NOT NULL,
    total_seats INT NOT NULL
);

CREATE TABLE seat (
    id BIGSERIAL PRIMARY KEY,
    screen_id BIGINT NOT NULL REFERENCES screen(id),
    seat_number VARCHAR(10) NOT NULL,
    seat_type VARCHAR(20) NOT NULL
);

CREATE TABLE movie (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    duration_minutes INT NOT NULL,
    language VARCHAR(50),
    genre VARCHAR(50)
);

-- Base price per seat category. One row per seat_type, admin-configurable.
CREATE TABLE seat_type_price (
    id BIGSERIAL PRIMARY KEY,
    seat_type VARCHAR(20) UNIQUE NOT NULL,
    base_price NUMERIC(10,2) NOT NULL
);

-- Single admin-configurable multiplier applied when a show's date is a Saturday/Sunday.
-- Deliberately a separate axis from seat_type_price — see §10.
CREATE TABLE weekend_pricing_rule (
    id BIGSERIAL PRIMARY KEY,
    multiplier NUMERIC(4,2) NOT NULL DEFAULT 1.00
);

CREATE TABLE show (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL REFERENCES movie(id),
    screen_id BIGINT NOT NULL REFERENCES screen(id),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL
);
-- No pricing FK here: price is computed at read/booking time from
-- seat_type_price x (weekend_pricing_rule if start_time is a weekend). See §10.

-- The seat map for a given show. One row per (show, seat), provisioned at show-creation time.
CREATE TABLE movie_show_seat (
    show_id BIGINT NOT NULL REFERENCES show(id),
    seat_id BIGINT NOT NULL REFERENCES seat(id),
    user_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    expires_at TIMESTAMP,
    PRIMARY KEY (show_id, seat_id)
);

CREATE TABLE discount_code (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    type VARCHAR(20) NOT NULL,
    value NUMERIC(10,2) NOT NULL,
    min_order_amount NUMERIC(10,2) DEFAULT 0,
    valid_from TIMESTAMP,
    valid_to TIMESTAMP
);

CREATE TABLE refund_policy (
    id BIGSERIAL PRIMARY KEY,
    cutoff_hours_before_show INT NOT NULL,
    refund_percentage INT NOT NULL
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE booking (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    show_id BIGINT NOT NULL REFERENCES show(id),
    status VARCHAR(20) NOT NULL,
    total_amount NUMERIC(10,2) NOT NULL,
    discount_code_id BIGINT REFERENCES discount_code(id),
    idempotency_key VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, idempotency_key)
);

CREATE TABLE booking_seat (
    booking_id BIGINT NOT NULL REFERENCES booking(id),
    seat_id BIGINT NOT NULL REFERENCES seat(id),
    PRIMARY KEY (booking_id, seat_id)
);
```

**Seed data** (`V2__seed_reference_data.sql`): one row per seat type in `seat_type_price` (e.g. REGULAR 150.00, PREMIUM 300.00, RECLINER 450.00), one row in `weekend_pricing_rule` (e.g. multiplier `1.20`), and one seeded `ADMIN` user with a documented dev-only password (see §7) — admins are provisioned this way, not via self-registration.

---

## 5. Concurrency Design — Seat Locking (core of the evaluation)

### Design decision: single row per (show, seat), no history table, no Redis
`movie_show_seat` has exactly one row per `(show_id, seat_id)` — created once at show-creation time, always present, and updated in place through its lifecycle (`AVAILABLE → LOCKED → CONFIRMED`, or back to `AVAILABLE` on expiry/cancellation). This avoids an entire class of bugs around stale rows conflicting with unique indexes, and removes the need for a separate insert-race to reason about.

### Single-seat locking flow
```java
@Transactional
public MovieShowSeat lockSeat(Long showId, Long seatId, Long userId) {
    MovieShowSeat row = movieShowSeatRepository.findByShowIdAndSeatIdForUpdate(showId, seatId);
    // SELECT ... WHERE show_id=? AND seat_id=? FOR UPDATE

    boolean available = row.getStatus() == SeatStatus.AVAILABLE
        || (row.getStatus() == SeatStatus.LOCKED && row.getExpiresAt().isBefore(Instant.now()));

    if (!available) {
        throw new SeatUnavailableException(seatId);
    }

    row.setStatus(SeatStatus.LOCKED);
    row.setUserId(userId);
    row.setExpiresAt(Instant.now().plus(LOCK_DURATION));
    return movieShowSeatRepository.save(row);
}
```

**Why this is correct under concurrency**: the `FOR UPDATE` lock on the row means only one transaction can be evaluating/updating a given `(showId, seatId)` at a time — competing requests queue behind the lock, and by the time each one proceeds, it sees the true, committed state left by whoever went before it. No unique-index safety net is needed because there's no insert race — only ever one row to update.

**Expiry handling — reclaim inline, don't rely on a background job**: the `expiresAt.isBefore(now())` check above means a stale `LOCKED` row is treated as available and immediately reclaimed within the same locked transaction — correctness never depends on a sweeper having run recently.

### Multi-seat locking flow — deadlock avoidance
`POST /shows/{id}/seats/lock` accepts one or more seat IDs. Locking several rows with `FOR UPDATE` inside one transaction introduces a deadlock risk: if two concurrent requests lock the same pair of seats in opposite orders (request A: seat 5 then seat 7; request B: seat 7 then seat 5), each can end up waiting on a row the other already holds. Fixed by always acquiring locks in a fixed, deterministic order:

```java
@Transactional
public List<MovieShowSeat> lockSeats(Long showId, List<Long> seatIds, Long userId) {
    List<Long> orderedSeatIds = seatIds.stream().sorted().toList();
    // Sorting seat IDs before locking means any two overlapping multi-seat
    // requests always acquire FOR UPDATE locks in the same order, so they
    // queue behind each other instead of deadlocking.

    List<MovieShowSeat> locked = new ArrayList<>();
    for (Long seatId : orderedSeatIds) {
        MovieShowSeat row = movieShowSeatRepository.findByShowIdAndSeatIdForUpdate(showId, seatId);
        boolean available = row.getStatus() == SeatStatus.AVAILABLE
            || (row.getStatus() == SeatStatus.LOCKED && row.getExpiresAt().isBefore(Instant.now()));
        if (!available) {
            throw new SeatUnavailableException(seatId);
            // Transaction rolls back here — every FOR UPDATE lock taken so far
            // in this call is released, so no seat is left partially locked.
        }
        row.setStatus(SeatStatus.LOCKED);
        row.setUserId(userId);
        row.setExpiresAt(Instant.now().plus(LOCK_DURATION));
        locked.add(row);
    }
    return movieShowSeatRepository.saveAll(locked);
}
```

All-or-nothing behavior falls out of `@Transactional` for free: any `SeatUnavailableException` mid-loop rolls back the entire transaction, so no seat is ever left locked while a sibling in the same request failed.

### Sweeper job (hygiene only, not correctness-critical)
```java
@Scheduled(fixedRate = 30000)
@Transactional
public void releaseExpiredLocks() {
    List<MovieShowSeat> expired = movieShowSeatRepository
        .findByStatusAndExpiresAtBefore(SeatStatus.LOCKED, Instant.now());
    expired.forEach(s -> {
        s.setStatus(SeatStatus.AVAILABLE);
        s.setUserId(null);
        s.setExpiresAt(null);
    });
    movieShowSeatRepository.saveAll(expired);
}
```
This exists to keep read-only display queries (e.g., simple seat-map polling) accurate without every query needing the `expiresAt` check — but actual lock-acquisition correctness does not depend on it.

### Booking confirmation
Re-validate every `MovieShowSeat` row is `LOCKED`, unexpired, and owned by the requesting user before transitioning to `CONFIRMED` — inside the same locked-row transaction pattern, and only after payment succeeds (see §11).

### Explicitly not using Redis
Decided against Redis-based locking (single-node or otherwise) for this assignment. Rationale to include in README:
- Single DB transaction with row-level locking fully satisfies the "no double-allocation" requirement at this scale
- Avoids the dual-write consistency problem (Redis lock succeeding while DB write fails or vice versa)
- Assignment explicitly scopes out distributed-systems complexity; a second stateful system is unnecessary infrastructure for a single-instance app
- Mention as a documented "future consideration" for horizontal scaling, not something the assignment requires

---

## 6. Show Creation — Seat Provisioning

When a `Show` is created, provision one `MovieShowSeat` row (status `AVAILABLE`) for every physical `Seat` on that screen:

```java
@Transactional
public Show createShow(ShowRequest request) {
    Show show = showRepository.save(new Show(request.getMovieId(),
        request.getScreenId(), request.getStartTime(), request.getEndTime()));

    List<Seat> seatsForScreen = seatRepository.findByScreenId(request.getScreenId());

    List<MovieShowSeat> rows = seatsForScreen.stream()
        .map(seat -> new MovieShowSeat(show.getId(), seat.getId(),
            null, SeatStatus.AVAILABLE, null))
        .toList();

    movieShowSeatRepository.saveAll(rows);
    return show;
}
```

Also validate at creation time that the new show's time window doesn't overlap an existing show on the same screen (basic scheduling-conflict check).

---

## 7. Authentication & Authorization

**Mechanism**: Spring Security with HTTP Basic Auth — stateless, no session or token infrastructure to build. Passwords stored as BCrypt hashes in `users.password_hash`. A custom `UserDetailsService` loads a user by email and maps `role` to a Spring Security authority (`ROLE_ADMIN` / `ROLE_CUSTOMER`).

**Endpoints**:
- `POST /auth/register` — public; customer self-registration. `role` is forced to `CUSTOMER` server-side regardless of what the request body contains.
- Admin accounts are **not** self-registered — they're seeded via `V2__seed_reference_data.sql` (§4). Document the seeded admin email/password in README as a dev-only convenience.

**Enforcement**:
- `@PreAuthorize("hasRole('ADMIN')")` on every admin CRUD endpoint (§8)
- Customer endpoints require authentication but no elevated role; pure browse endpoints (`GET /cities`, `/theaters`, `/movies`, `/shows`, `/shows/{id}/seats`) are `permitAll()` since the requirement doesn't gate browsing
- The authenticated principal's user ID — **never** a client-supplied `userId` field — is what flows into `lockSeat`/`lockSeats` and `POST /bookings`. This closes a spoofing gap: without this, a client could pass an arbitrary `userId` in the request body and lock/book seats as someone else.

**Why HTTP Basic over JWT/OAuth**: the assignment explicitly excludes OAuth/SSO/MFA. HTTP Basic gives real, testable RBAC (`401` unauthenticated, `403` wrong role) with the least moving parts, keeping the time budget on concurrency/booking logic rather than token infrastructure.

---

## 8. Core APIs by Role

### Auth
- `POST /auth/register` — customer self-registration

### Customer
- `GET /cities`
- `GET /theaters?cityId=`
- `GET /movies?cityId=&theaterId=` — browse movies currently showing, optionally scoped to a city/theater
- `GET /shows?theaterId=&movieId=&date=`
- `GET /shows/{id}/seats` — full seat map with live status and computed per-seat price (§10)
- `POST /shows/{id}/seats/lock` — lock one or more seats (`seatIds[]`); userId taken from the authenticated principal; returns lock expiry
- `POST /bookings` — confirm booking from locked seats (optional discount code, optional `Idempotency-Key` header) → payment (§11) → confirmation
- `POST /bookings/{id}/cancel` — cancel + trigger refund per policy
- `GET /bookings` — booking history for the current user

### Admin
- CRUD: `/cities`, `/theaters`, `/screens`, `/seats`, `/movies`, `/shows`, `/seat-type-prices`, `/discount-codes`, `/refund-policies`
- `PATCH /pricing-rules/weekend` — update the single weekend multiplier

---

## 9. Booking Lifecycle
`AVAILABLE → LOCKED → CONFIRMED`, with `LOCKED` auto-reverting to `AVAILABLE` on expiry (inline reclaim or sweeper). A failed payment does **not** introduce a new seat status — see §11 for why. Cancellation only allowed before a configurable cutoff relative to show start time; refund computed via `RefundPolicy.refundPercentage` based on `cutoffHoursBeforeShow`.

---

## 10. Pricing & Discounts

**Two independent axes** — previously conflated into a single `PricingTier.type` (REGULAR/PREMIUM/WEEKEND) enum on `Show`, which mixed seat quality and day-of-week into one field. Fixed by separating them:

- **Seat type** → `SeatTypePrice.basePrice`, one row per `seatType` (REGULAR / PREMIUM / RECLINER), admin-configurable via `/seat-type-prices`
- **Day type** → `WeekendPricingRule.multiplier`, a single admin-configurable factor (e.g. `1.20`) applied whenever the show's `startTime` falls on a Saturday or Sunday — computed from the date, never stored per-show

**Per-seat price**:
```
seatPrice(seat, show) = seatTypePrice(seat.seatType).basePrice
                        × (isWeekend(show.startTime) ? weekendPricingRule.multiplier : 1)
```

So a PREMIUM seat on a weekend show costs `premiumBasePrice × weekendMultiplier`; a REGULAR seat on a weekday costs just `regularBasePrice`. `isWeekend` is derived at read/booking time from `show.startTime.getDayOfWeek()` — not persisted — so changing the multiplier later never leaves stale, inconsistent data on existing shows.

**Booking total** = sum of `seatPrice(seat, show)` over every seat in the booking, then the discount applied on top.

- One discount code per booking, FLAT or PERCENTAGE type, validated against `minOrderAmount` and validity window — no stacking

---

## 11. Payment (mocked)

No real payment gateway integration (explicitly out of scope) — `PaymentGateway` is a service interface with one mock implementation, called synchronously inside the booking-confirmation transaction, after seats are re-validated as `LOCKED`/unexpired/owned-by-caller but before they're flipped to `CONFIRMED`:

```java
public interface PaymentGateway {
    PaymentResult charge(BigDecimal amount, Long userId);
}
```

**Mock failure mode**: deterministic, not random, so tests stay reproducible — e.g. an explicit `simulatePaymentFailure` boolean on the booking request, or a magic-amount trigger. Whichever is chosen, state it as an assumption in README.

**Behavior on success**: seats transition `LOCKED → CONFIRMED`, a `Booking` row is created with status `CONFIRMED`, the async confirmation notification fires (§12).

**Behavior on failure**: `PaymentFailedException` is thrown and the transaction rolls back — no `Booking` row is created, and because nothing committed, the seats remain `LOCKED` under the same user for whatever's left of the hold window, so the user can retry payment without re-locking. The response is `402 Payment Required`, distinct from the `409 Conflict` a `SeatUnavailableException` returns — the client can tell a payment failure from a lost-the-seat failure.

**Idempotency guard on `POST /bookings`** (scope kept deliberately small):
- Accept an optional `Idempotency-Key` header.
- `UNIQUE (user_id, idempotency_key)` constraint on `booking` (§4) — enforced by the DB, not a check-then-insert in application code, so two concurrent requests carrying the same key can't both slip through and double-create/double-charge.
- On a duplicate key: look up and return the existing booking instead of creating a new one, re-locking seats, or re-"charging."
- No key sent → unchanged behavior; this is purely additive, not a breaking change to the API contract.
- One test: fire the same booking request twice with the same key, assert only one `Booking` row exists and both responses match (§14).

The client, not the server, owns generating the key once per booking *intent* — not once per HTTP request — and resending that same value on every retry/double-click of that intent; a fresh key only appears if the user starts a genuinely new booking attempt.

---

## 12. Async Notifications
- On booking confirmation/cancellation, fire `@Async` mock notification (log statement simulating email/SMS) — must not block the HTTP response
- Reminder notifications: simple `@Scheduled` job querying upcoming shows + confirmed bookings within a reminder window, logging a mock reminder

---

## 13. README.md — Required Sections
- Overview and architecture decisions
- **Concurrency design rationale**: single-row `movie_show_seat` design, row-level locking, sorted-lock-order for multi-seat requests, inline expiry reclaim, why Redis was considered and explicitly not used
- Assumptions made for every ambiguous requirement — e.g.: lock duration length, refund cutoff defaults, discount stacking rules ("one code per booking, no stacking"), authentication mechanism chosen (HTTP Basic + seeded admin credentials), payment mock failure trigger, weekend multiplier default value
- Explicitly out-of-scope items (see below)
- How to run locally (Docker Postgres + Flyway + Spring Boot)
- Testing approach summary (unit vs. integration split, see §14)

### Explicitly Out of Scope (state in README)
- Dynamic/surge pricing beyond the seat-type × weekend model, real payment gateway integration, seat auto-recommendation
- OAuth/SSO/MFA — HTTP Basic + role-based access control only
- UI/frontend, containerization beyond the Postgres dev container, CI/CD, microservices, Redis/distributed locking

---

## 14. Testing Strategy

### Unit tests (mocked repositories/dependencies, no DB)
1. Pricing calculation — `seatTypePrice × weekendMultiplier` for weekend vs. weekday shows, across all seat types
2. Discount code validation — expired code, below `minOrderAmount`, unknown code, correct FLAT vs. PERCENTAGE math
3. Refund calculation against `RefundPolicy` cutoff windows — multiple cutoff tiers, exactly-at-boundary case
4. Booking state transition validation — reject invalid transitions (e.g. cancel an already-cancelled booking)
5. `PaymentGateway` mock — service branches correctly on success vs. decline
6. Seat-lock expiry predicate in isolation (the `isExpired`/availability check)
7. Show scheduling-conflict check — overlapping time windows on the same screen

### Integration tests (Testcontainers-backed real Postgres, full Spring context)
1. **Concurrent seat-lock test** (highest priority) — N threads attempting to lock the same `(showId, seatId)` simultaneously; assert exactly 1 succeeds, N-1 throw `SeatUnavailableException`
2. **Multi-seat deadlock test** — two threads locking overlapping seat sets in reversed order concurrently; assert both complete without a Postgres deadlock error, resolved purely by the sorted-lock-order rule
3. Expired-lock reclaim test — create a `LOCKED` row with `expiresAt` in the past, verify a new lock attempt succeeds and correctly reclaims it (not just the sweeper path)
4. End-to-end booking — lock → confirm with payment success → seats `CONFIRMED`, booking `CONFIRMED`, notification fired
5. End-to-end booking with payment failure — lock → confirm attempt declines → seats remain `LOCKED` (not released), no booking row created, a retry before expiry succeeds
6. Cancellation + refund — confirm a booking, cancel before/after cutoff, assert correct refund percentage and seats released back to `AVAILABLE`
7. RBAC — customer hitting an admin endpoint → `403`; unauthenticated request to a protected endpoint → `401`
8. Idempotency — fire the same booking request twice with the same `Idempotency-Key` → only one `Booking` row created, both responses match
9. Standard CRUD + input validation error handling — malformed request → `400` with a structured error body (`@ControllerAdvice`)

---

## 15. Package Structure
```
com.example.movietickets
├── controller       (REST endpoints, one per resource)
├── service           (business logic — SeatLockService is the centerpiece; also PricingService, BookingService)
├── repository        (Spring Data JPA repositories)
├── entity            (JPA entities)
├── dto               (request/response objects)
├── exception         (SeatUnavailableException, PaymentFailedException, custom exceptions, @ControllerAdvice handler)
├── config            (async config, scheduling config, SecurityConfig)
├── security          (UserDetailsService impl, role-based method security)
└── payment           (PaymentGateway interface + mock implementation)
```

---

## 16. Suggested Build Order (protect time budget across 48 hours)
1. Docker Postgres up, Flyway migration + seed data, base entities + CRUD (city → theater → screen → seat → movie → show), basic auth (HTTP Basic, seeded admin, `/auth/register`) — auth comes early since seat locking needs a real authenticated `userId`, not a client-supplied one
2. **Seat locking logic (`MovieShowSeat` + `SeatLockService`, single- and multi-seat with sorted-lock-order) + concurrent JUnit test** — build and prove this first, it's the highlight of the evaluation
3. Pricing (seat-type × weekend multiplier) + booking confirmation flow + payment mock (success/fail path) + discount calculation
4. Cancellation + refund policy logic
5. Async notifications (mock)
6. Idempotency key on `POST /bookings`, if time allows
7. Polish: validation, global error handling, remaining unit/integration tests, README, Loom video

---

## 17. Submission Checklist
- [ ] GitHub repo with multiple commits spread across the 48 hours (not one final dump)
- [ ] `README.md` with all sections from §13
- [ ] `Claude.md` / `Agents.md` documenting the AI workflow used
- [ ] List of skills/tools used during development
- [ ] All raw files used during development included in the repo
- [ ] Loom video (max 10 min): lead with the concurrency design story, then tech stack reasoning, AI workflow, testing approach