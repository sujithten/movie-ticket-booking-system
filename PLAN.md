# Implementation Plan — Movie Ticket Booking System

Derived from `movie-ticket-booking-spec.md` (§ references below point there for full design rationale). Phase-ordered checklist for the 48-hour build. Phases 1–2 match the spec's Suggested Build Order (§16); this plan just breaks each step into concrete files/tasks and folds in the five recent changes: RBAC via a real `users` table, the multi-seat lock deadlock fix, seat-type/weekend pricing decoupling, `GET /movies`, and the unit/integration test split plus mocked payment and the scoped idempotency guard.

---

## Phase 0 — Scaffolding
- [ ] Spring Boot 3.x project (Maven), Java 17
- [ ] `docker-compose.yml` — Postgres 16 (§2)
- [ ] `application.yml` — datasource, `ddl-auto: validate`, PostgreSQL dialect (§2)
- [ ] Flyway dependency + `src/main/resources/db/migration/`

## Phase 1 — Schema, seed data, base CRUD, auth
*(Build Order step 1 — auth comes early since seat locking needs a real authenticated `userId`, not a client-supplied one)*
- [ ] `V1__init_schema.sql` — all tables in §4: `city`, `theater`, `screen`, `seat`, `movie`, `seat_type_price`, `weekend_pricing_rule`, `show`, `movie_show_seat`, `discount_code`, `refund_policy`, `users`, `booking` (with `UNIQUE(user_id, idempotency_key)`), `booking_seat`
- [ ] `V2__seed_reference_data.sql` — `seat_type_price` rows per seat type, one `weekend_pricing_rule` row, one seeded `ADMIN` user (BCrypt hash) — §4
- [ ] Entities: City, Theater, Screen, Seat, Movie, SeatTypePrice, WeekendPricingRule, Show, MovieShowSeat, DiscountCode, RefundPolicy, User, Booking, BookingSeat — §3
- [ ] Repositories, incl. `movieShowSeatRepository.findByShowIdAndSeatIdForUpdate` using `@Lock(PESSIMISTIC_WRITE)`
- [ ] Admin CRUD controllers: `/cities`, `/theaters`, `/screens`, `/seats`, `/movies`, `/shows`, `/seat-type-prices`, `/discount-codes`, `/refund-policies`, `PATCH /pricing-rules/weekend` — §8
- [ ] `SecurityConfig` (HTTP Basic), `UserDetailsService` backed by `users`, `BCryptPasswordEncoder`, `POST /auth/register` (forces `role=CUSTOMER` server-side, ignoring the request body) — §7
- [ ] `@PreAuthorize("hasRole('ADMIN')")` on every admin endpoint; `permitAll()` on browse endpoints (`/cities`, `/theaters`, `/movies`, `/shows`, `/shows/{id}/seats`) — §7

## Phase 2 — Seat locking (the centerpiece)
*(Build Order step 2 — build and prove this first)*
- [ ] `SeatLockService.lockSeat(showId, seatId, userId)` — §5
- [ ] `SeatLockService.lockSeats(showId, seatIds, userId)` — **sort seat IDs before acquiring `FOR UPDATE` locks** to avoid deadlock on overlapping multi-seat requests; all-or-nothing via `@Transactional` rollback — §5
- [ ] `POST /shows/{id}/seats/lock` — `userId` taken from the authenticated principal, never the request body — §7, §8
- [ ] `SeatUnavailableException` → `409` in `@ControllerAdvice`
- [ ] Sweeper: `@Scheduled releaseExpiredLocks()` — §5
- [ ] Integration test: concurrent seat-lock test — N threads on the same `(showId, seatId)`, exactly 1 succeeds — §14 unit/integration #1
- [ ] Integration test: multi-seat deadlock test — two threads locking overlapping seat sets in reversed order, no Postgres deadlock error — §14 #2
- [ ] Integration test: expired-lock reclaim test — §14 #3

## Phase 3 — Pricing, booking confirmation, payment, discounts
*(Build Order step 3)*
- [ ] `PricingService.seatPrice(seat, show)` = `seatTypePrice(seat.seatType) × (isWeekend(show.startTime) ? weekendMultiplier : 1)` — two independent axes, computed at read time, never persisted per-show — §10
- [ ] `GET /shows/{id}/seats` returns live status + computed per-seat price — §8
- [ ] `GET /movies?cityId=&theaterId=` — §8
- [ ] `PaymentGateway` interface + `MockPaymentGateway` — deterministic failure trigger (e.g. `simulatePaymentFailure` flag on the request), not random — §11
- [ ] `BookingService.confirmBooking(...)`: re-validate seats are `LOCKED`/unexpired/owned → charge → on success, confirm seats + create `Booking`; on failure, throw `PaymentFailedException` (`402`), transaction rolls back so seats stay `LOCKED` — §5, §11
- [ ] Idempotency: accept optional `Idempotency-Key` header; rely on the `UNIQUE(user_id, idempotency_key)` constraint (attempt insert, catch the violation, return the existing booking) rather than check-then-insert — §11
- [ ] Discount code validation (type, `minOrderAmount`, validity window) applied on top of the summed seat prices — §10
- [ ] Unit tests: pricing calc across seat types/weekend-vs-weekday, discount validation, `PaymentGateway` mock branches — §14
- [ ] Integration tests: end-to-end booking (payment success), end-to-end booking (payment failure), idempotency (same key fired twice → one row, matching responses) — §14

## Phase 4 — Cancellation + refunds
*(Build Order step 4)*
- [ ] `POST /bookings/{id}/cancel` — cutoff check, refund % via `RefundPolicy.cutoffHoursBeforeShow` — §9
- [ ] Reject cancelling an already-cancelled booking (state transition validation)
- [ ] Release seats back to `AVAILABLE` on cancellation
- [ ] Unit tests: refund calc across cutoff tiers + exact boundary; booking state transition validation — §14
- [ ] Integration test: cancellation + refund end-to-end — §14 #6

## Phase 5 — Async notifications
*(Build Order step 5)*
- [ ] `@Async` `NotificationService` — log-only confirmation/cancellation notification — §12
- [ ] `@Scheduled` reminder job — upcoming shows + confirmed bookings within a reminder window — §12
- [ ] `AsyncConfig` (executor) — §1, §15

## Phase 6 — Polish
*(Build Order step 7)*
- [ ] Global `@ControllerAdvice` — validation errors → `400` structured body; `SeatUnavailableException` → `409`; `PaymentFailedException` → `402`
- [ ] Bean Validation annotations on request DTOs
- [ ] RBAC integration tests: customer hitting an admin endpoint → `403`; unauthenticated request to a protected endpoint → `401` — §14 #7
- [ ] Standard CRUD + input validation integration tests — §14 #9
- [ ] `README.md` — all sections from §13
- [ ] `Claude.md` / `Agents.md` — AI workflow documentation
- [ ] Loom video (max 10 min)

---

## Recent design decisions baked into this plan
1. **RBAC** — a real `users` table + HTTP Basic Auth (BCrypt, `UserDetailsService`), not a stub; seat-lock/booking `userId` comes from the authenticated principal, never a client-supplied field.
2. **Multi-seat locking** — seat IDs sorted before acquiring `FOR UPDATE` locks, closing a deadlock risk the original single-seat-only flow didn't cover.
3. **Pricing** — `SeatTypePrice` (seat quality) and `WeekendPricingRule` (day type) are separate, independently configurable axes instead of one conflated `PricingTier` enum.
4. **`GET /movies`** — added so customers can discover what's playing before filtering shows by `movieId`.
5. **Testing & payment** — unit tests (mocked deps) and integration tests (Testcontainers) are explicit, separate lists; payment has a real success/failure branch (`402` vs. `409`) instead of being a comment; the idempotency guard is scoped narrowly (`Idempotency-Key` header, `UNIQUE(user_id, idempotency_key)`, lookup-on-duplicate, one test) rather than over-built.