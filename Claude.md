# AI Workflow — Claude Code

This document describes how Claude Code (Claude Sonnet 5) was used to build this project, as a narrative of the actual collaboration — what was asked, what Claude proposed, where the human course-corrected, and how problems were worked through. See `skills.md` for the plain list of tools/capabilities used.

## 1. Spec review, not spec generation

Development did not start from a blank page or a one-shot "build me a booking system" prompt. The user had already written a draft spec (`movie-ticket-booking-spec.md`) based on the assignment PDF and asked Claude to **review it**, not rewrite it. Claude read both documents side-by-side and reported back concrete, prioritized gaps rather than a rewrite:

- No authentication mechanism specified anywhere, despite RBAC being required.
- A deadlock risk in multi-seat locking that the single-seat locking example didn't address.
- `PricingTier` conflating two independent dimensions (seat quality vs. day-of-week) into one enum.
- A missing `GET /movies` browse endpoint.
- Payment reduced to a comment (`→ simulated payment →`) with no failure path, and no idempotency guard on booking creation.

This review-first step mattered: it meant every subsequent design decision traced back to a specific, named problem rather than being an unprompted addition.

## 2. Decisions made by the user, implemented by Claude

For each gap, the user made the actual call, and Claude implemented and documented it:

- **Auth**: user asked for "a user's table for RBAC" — Claude proposed HTTP Basic Auth (over JWT/OAuth) as the minimal option consistent with the assignment's exclusion of OAuth/SSO, and implemented it.
- **Multi-seat locking**: user asked for "the fix" — Claude added sorted-lock-order acquisition to `SeatLockService`, the same pattern later reused in `BookingWriter` when re-validating locks at confirmation time.
- **Pricing**: the user specified the fix directly ("decouple seat type pricing from day type pricing entirely... premiumBasePrice × weekendMultiplier") — Claude translated that into `SeatTypePrice` + `WeekendPricingRule` as two independent, admin-configurable entities.
- **Idempotency**: Claude's first draft was broader than needed; the user pushed back with a specific 5-point scope ("Accept an optional `Idempotency-Key` header... UNIQUE(user_id, idempotency_key)... one test"). Claude adopted that scope exactly, including a follow-up correction when the user caught a real bug in Claude's own explanation — that idempotency keys must be generated once per booking *intent*, not per HTTP request, or the guard does nothing.

This back-and-forth is the throughline of the whole session: Claude proposed, the user tightened or corrected, Claude implemented the corrected version.

## 3. Plan, then phased implementation

Once the spec was finalized, Claude generated `PLAN.md` — a phase-ordered checklist (scaffolding → schema/entities/auth → seat locking → pricing/payment/booking → cancellation/refunds → notifications → validation/error-handling/README) — before writing any code. Implementation then followed that plan phase-by-phase using an internal task list to track progress, with the seat-locking concurrency core (`SeatLockService`) treated as the centerpiece, matching the assignment's own framing of concurrency as "the core of the evaluation."

## 4. Verify, don't assume

After each phase, Claude compiled the project and ran the unit test suite rather than moving on trusting the code was correct. This surfaced real bugs before they compounded — e.g. a lambda capturing a reassigned variable that only failed at compile time, and a Lombok/JDK version incompatibility (see below) that would have silently produced broken entities (no getters/setters at all) if untested.

## 5. An honest debugging saga: Docker Desktop vs. Testcontainers

Java/Maven weren't installed on the user's machine, so Claude first ran builds via the official Maven Docker image. This worked for compiling and unit tests, but the Testcontainers-based integration test suite (which needs to spin up a real Postgres container) kept failing with `Could not find a valid Docker environment`.

Rather than declaring the integration tests "done" without running them, Claude worked through this as a real debugging problem across several rounds:
- Confirmed the socket was reachable and Docker Desktop itself was healthy (`curl` against the socket returned full real data).
- Tested and ruled out, one at a time: Docker Desktop's "allow default socket" setting, Enhanced Container Isolation, the HTTP `Host` header, the `User-Agent` header, and the Testcontainers library version.
- Discovered along the way that the *native* Maven install (via Homebrew) picked up a very new JDK that broke Lombok's annotation processing silently — a distinct bug caught by the same "always verify" discipline, fixed by pinning `JAVA_HOME` to a Homebrew-installed JDK 17.
- Ultimately narrowed the remaining integration-test failure to an unresolved `docker-java`/this-specific-Docker-Desktop-version incompatibility, and reported that honestly rather than papering over it — the unit test suite (18/18) is verified green; the integration suite is written and logically reviewed but not yet verified to execute in this environment.

At each dead end, Claude surfaced findings and asked the user how to proceed (e.g., whether to keep debugging Docker Desktop settings or switch to a native Java/Maven install) instead of silently picking a path.

## 6. Refactors driven by explicit direction

Two structural refactors happened mid-build, both initiated by the user reading the code rather than by Claude proactively "cleaning up":
- Extracting registration logic out of `AuthController` into a `UserService`, renaming the controller to `UserController`.
- A broader architectural rule — "controllers should only communicate with services" — applied consistently across all 11 controllers that had been directly injecting repositories, each given a matching service class.

Both were verified by full recompilation and a full unit-test re-run afterward, not assumed correct.

## Summary

The working pattern throughout was: **review before building, decide before implementing, verify before moving on, and report problems honestly instead of hiding them.** Every non-trivial design choice in this codebase (auth mechanism, lock ordering, pricing model, idempotency scope, service-layer boundaries) traces back to an explicit human decision in this conversation, not an unprompted AI choice.