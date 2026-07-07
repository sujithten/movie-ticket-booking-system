# Movie Ticket Booking System

A Spring Boot 3 / Java 17 movie ticket booking system: multiple cities, theaters, screens, and shows, with seat-level booking, time-bound holds, tiered pricing, discount codes, payment (mocked), cancellations with refunds, and non-blocking notifications. Built for a 48-hour SDE-2 take-home assignment.

**Author**: Sujith Tenali <tsujithonline@gmail.com>

Full design spec: [`movie-ticket-booking-spec.md`](movie-ticket-booking-spec.md). Implementation plan: [`PLAN.md`](PLAN.md).

## Overview & Architecture Decisions

- **Stack**: Spring Boot 3.3, Java 17, PostgreSQL 16 (Docker), Spring Data JPA/Hibernate, Flyway migrations (no Hibernate auto-DDL), Spring Security (HTTP Basic), Maven.
- **Layering**: `controller` → `service` → `repository` → `entity`, with a `dto` package for request/response shapes so entities never leak directly over HTTP. `exception` holds custom exceptions + a single `@RestControllerAdvice`. `security` holds the auth principal/user-details wiring. `payment` holds the mocked payment gateway.
- **No Redis, no microservices, no message broker** — deliberately. See "Concurrency design rationale" below for why, and "Out of scope" for the full list.
- **Two collaborating beans for booking confirmation and cancellation** (`BookingService` + `BookingWriter`/`CancellationWriter`): the outer `BookingService` is *not* `@Transactional` itself, so it can (a) catch a unique-constraint violation from *outside* the writer's transaction boundary (needed for the idempotency-key race, see below) and (b) fire the async notification only *after* the writer's transaction has actually committed, not from partway through it.

## Concurrency Design Rationale (the core of this assignment)

**Single row per (show, seat), no history table.** `movie_show_seat` has exactly one row per `(show_id, seat_id)`, created once when a show is created, and updated in place through `AVAILABLE → LOCKED → CONFIRMED` (or back to `AVAILABLE` on expiry/cancellation). This avoids an entire class of bugs around stale rows conflicting with unique indexes — there's no insert race to reason about, only ever one row to update.

**Row-level pessimistic locking.** Locking a seat takes a `SELECT ... FOR UPDATE` on that single row (`SeatLockService`, backed by `MovieShowSeatRepository.findByShowIdAndSeatIdForUpdate`). Only one transaction can be evaluating/updating a given `(showId, seatId)` at a time — competing requests queue behind the row lock, and by the time each one proceeds it sees the true, committed state left by whoever went before it.

**Multi-seat locking — deadlock avoidance.** Locking several seats in one transaction risks a classic deadlock: if request A locks `{5, 7}` while request B locks `{7, 5}` concurrently, each can end up waiting on a row the other already holds. `SeatLockService.lockSeats` sorts seat IDs before acquiring locks, so any two overlapping multi-seat requests always take `FOR UPDATE` locks in the same order — they queue behind each other instead of deadlocking. `BookingWriter.confirmBooking` re-validates locks using the same sorted order for the same reason. Proven by `MultiSeatLockDeadlockIT`.

**Expiry reclaimed inline, not dependent on a background job.** A stale `LOCKED` row (`expiresAt` in the past) is treated as available and reclaimed directly inside the same locked transaction that's trying to acquire it (`SeatLockService.lockOne`). The `@Scheduled` sweeper (`SeatLockSweeper`, every 30s) exists only so read-only seat-map queries look accurate without every read needing an `expiresAt` check — actual lock-acquisition correctness never depends on it having run recently.

**Explicitly not using Redis.** A single DB transaction with row-level locking fully satisfies "no double-allocation" at this scale; Redis would add a dual-write consistency problem (lock succeeding in Redis while the DB write fails, or vice versa) and a second stateful system for no correctness benefit in a single-instance app. Worth revisiting only if/when this needs to scale horizontally across multiple app instances.

## Pricing — Two Independent Axes

Seat quality and day-of-week are independent dimensions, not variants of the same enum (an earlier draft of the spec conflated them into one `PricingTier.type`; fixed before implementation):

- **Seat type** → `SeatTypePrice.basePrice`, one row per `seatType` (REGULAR/PREMIUM/RECLINER), admin-configurable via `/seat-type-prices`.
- **Day type** → `WeekendPricingRule.multiplier`, a single admin-configurable factor applied when `show.startTime` falls on a Saturday/Sunday — computed from the date at read/booking time, never persisted per-show.

`seatPrice = seatTypePrice(seat.seatType) × (isWeekend(show) ? weekendMultiplier : 1)`. Booking total = sum of per-seat prices, then the discount code applied on top.

## Payment (Mocked) & Idempotency

No real payment gateway (explicitly out of scope). `PaymentGateway`/`MockPaymentGateway` is called synchronously inside booking confirmation, after seats are re-validated as `LOCKED`/unexpired/owned but before they flip to `CONFIRMED`. Failure is **deterministic**, driven by a `simulatePaymentFailure` flag on the request — never random — so tests are reproducible. On failure, the transaction rolls back: no `Booking` row is created, and the seats stay `LOCKED` under the same user for whatever's left of the hold window, so a retry doesn't require re-locking. Response is `402 Payment Required`, distinct from the `409 Conflict` a lost seat lock returns.

**Idempotency** is scoped narrowly: an optional `Idempotency-Key` header, a `UNIQUE(user_id, idempotency_key)` DB constraint, and on a duplicate key, look up and return the existing booking instead of re-creating/re-charging. The uniqueness is enforced by the database constraint, not a check-then-insert in application code — `BookingWriter` attempts the insert directly, and `BookingService` catches the resulting `DataIntegrityViolationException` from *outside* the writer's transaction (a Hibernate persistence context is unusable for further work after a failed flush, so the catch can't live inside the same transactional method) and falls back to a fresh lookup. The client is responsible for generating the key once per booking *intent* (not once per HTTP request) and resending the same value on retries/double-clicks.

## Assumptions

- **Lock duration**: 10 minutes (`booking.lock-duration-minutes`).
- **Cancellation cutoff**: bookings can't be cancelled within 1 hour of show start (`booking.cancellation-min-hours-before-show`); refund percentage below that is 0% via the same mechanism as any other tier miss.
- **Refund tiers**: no default rows are seeded — an admin configures `RefundPolicy` rows (cutoff hours → refund %) via `/refund-policies`. If no tier's cutoff is met, refund is 0%.
- **Discount codes**: one code per booking, FLAT or PERCENTAGE, no stacking.
- **Payment failure trigger**: an explicit `simulatePaymentFailure: true` boolean on the booking request (deterministic, not a magic amount) — purely for demoing/testing the failure path.
- **Authentication**: HTTP Basic over Spring Security, not OAuth/SSO/JWT (excluded by the assignment). Customers self-register via `POST /auth/register` (role is always forced to `CUSTOMER` server-side); the one admin account is seeded via Flyway (`admin@example.com` / `AdminPass123!` — **dev-only credential, not for production use**).
- **Seat-lock ownership**: `userId` for locking/booking always comes from the authenticated principal (`CurrentUser.id()`), never a client-supplied field — otherwise a client could lock/book on another user's behalf.
- **Weekend multiplier**: a single global value (seeded at `1.20`), not per-theater or per-movie.
- **Booking totals**: computed from live seat-type price × weekend multiplier at confirmation time, not frozen at lock time — so a mid-hold pricing-config change is reflected in the final charge.

## Explicitly Out of Scope

- UI/frontend, deployment/containerization beyond the Postgres dev container, CI/CD, distributed systems/microservices.
- OAuth/SSO/MFA — HTTP Basic + role-based access control only.
- Real payment gateway integration, dynamic/surge pricing beyond the seat-type × weekend model, seat auto-recommendation.
- Redis/distributed locking (see Concurrency Design Rationale for why).
- Production-grade observability/monitoring/alerting.

## How to Run Locally

```bash
# 1. Start Postgres
docker compose up -d

# 2. Run the app (Flyway migrations run automatically on startup)
./mvnw spring-boot:run
# — or, without installing Maven locally, via Docker:
docker run --rm -v "$(pwd)":/app -w /app -p 8080:8080 \
  --network host \
  maven:3.9-eclipse-temurin-17 mvn spring-boot:run
```

The app listens on `:8080`. Seeded admin: `admin@example.com` / `AdminPass123!`. Register a customer via `POST /auth/register`.

## Testing Approach

Unit and integration tests are kept in **separate packages** (`unit` vs. `integration`), matching the split in the spec:

- **Unit tests** (`src/test/java/.../unit`, JUnit 5 + Mockito, no DB): pricing calculation across seat types/weekend-vs-weekday, discount code validation (expired/below-minimum/unknown/math), refund calculation across cutoff tiers and the exact boundary, the mock payment gateway's success/decline branches, the seat-lock availability/expiry predicate in isolation, and the show scheduling-conflict check. **18 tests, all passing.**
- **Integration tests** (`src/test/java/.../integration`, Testcontainers real Postgres, full Spring context): the concurrent seat-lock test (N threads on one seat, exactly 1 wins), the multi-seat deadlock test (reversed lock order, no DB deadlock), expired-lock direct reclaim, end-to-end booking confirmation (payment success and payment failure paths), idempotency (same key fired twice → one booking), cancellation + refund, RBAC (`403`/`401`), and standard CRUD + validation error shape.

**Running tests:**
```bash
./mvnw test                              # everything
./mvnw -Dtest='**/unit/**' test           # unit only, no Docker needed
./mvnw -Dtest='**/integration/**' test    # integration only, needs Docker
```

Without Maven installed locally, the same commands work via the official Maven Docker image, e.g.:
```bash
docker run --rm -v "$(pwd)":/app -w /app -v /var/run/docker.sock:/var/run/docker.sock \
  maven:3.9-eclipse-temurin-17 mvn test
```
Note: on some Docker Desktop configurations, running Maven *itself* inside a container (rather than natively) while also needing Testcontainers to reach the host Docker daemon can hit Docker Desktop's socket-access restrictions for nested/"Docker-outside-of-Docker" access. If `mvn test` inside a container reports `Could not find a valid Docker environment` despite Docker Desktop running normally, either run Maven natively (`brew install openjdk@17 maven`) or check Docker Desktop's socket-sharing settings — this is an environment/tooling nuance, not a project issue. Compilation and the full unit test suite (18/18) are verified green in this containerized-Maven setup; the integration suite is verified by design/code review and is expected to pass under a normal (non-nested) `mvn test` invocation.
