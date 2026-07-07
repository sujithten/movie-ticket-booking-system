# Skills & Tools Used During Development

This project was built using **Claude Code** (Anthropic's CLI coding agent, running Claude Sonnet 5) as an AI pair-programmer across the full development lifecycle — from reviewing the initial spec through implementation, testing, and debugging.

## Tools / Capabilities Used

- **File read/write/edit tools** — scaffolding and editing all Java source (entities, repositories, services, controllers, DTOs, config, exceptions), Flyway migrations, `application.yml`, and tests.
- **Bash execution** — running Docker, Maven, Homebrew, and git commands directly.
- **Task tracking** (todo-list style) — the build was broken into 7 tracked phases (scaffolding → schema/entities/auth → seat locking → pricing/payment/booking → cancellation/refunds → notifications → validation/error-handling/README), each marked in-progress/completed as work landed.
- **Docker** — used two ways: (1) as the project's actual Postgres dependency (`docker-compose.yml`, per the spec), and (2) temporarily, as a way to compile/test the project via the official Maven image before Java/Maven were installed locally.
- **Homebrew** — installed Java 17 and Maven natively once containerized Maven hit a Testcontainers/Docker Desktop compatibility wall (documented below).
- **Interactive clarification prompts** — used at a few genuine decision points (e.g., how to proceed when Docker Desktop blocked nested Testcontainers access; whether to install Java/Maven natively) rather than guessing.

## AI-Assisted Workflow

1. **Spec review** — reviewed the user's own draft spec (`movie-ticket-booking-spec.md`) against the assignment PDF and flagged concrete gaps: no authentication mechanism, a multi-seat lock deadlock risk, an ambiguous pricing-tier design, a missing `GET /movies` endpoint, and a payment/idempotency design that was just a comment.
2. **Iterative spec refinement** — the user made a call on each gap; the spec was rewritten section-by-section to reflect those decisions (HTTP Basic auth + seeded admin, sorted-lock-order multi-seat locking, decoupled seat-type/weekend pricing, mocked `PaymentGateway` with a deterministic failure mode, a narrowly-scoped `Idempotency-Key` design).
3. **Implementation plan** — turned the finalized spec into a phase-ordered build checklist (`PLAN.md`).
4. **Phased implementation** — built the plan phase-by-phase, keeping the seat-locking concurrency core (`SeatLockService`, sorted lock ordering, inline expiry reclaim) as the centerpiece, consistent with the assignment's own emphasis.
5. **Continuous verification** — compiled and ran the unit test suite after each phase (18/18 passing throughout); investigated a Testcontainers/Docker Desktop socket incompatibility for the integration suite across several rounds of hypothesis-testing (container nesting, Enhanced Container Isolation, socket permissions, HTTP headers, library versions) before concluding it's an environment-level compatibility gap, not a project defect.
6. **Follow-up refactors on request** — e.g. extracting a `UserService` and renaming `AuthController` → `UserController`; a broader pass moving all direct repository access out of every controller into a proper service layer, applied consistently across all 11 affected controllers.

## Note on Scope

No named Claude Code "Skill" plugins (e.g. `/code-review`, `/security-review`) were invoked for this project — all engineering work was done through direct conversation with Claude Code.