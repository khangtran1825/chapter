# Research and Decisions: Chapter Bookstore MVP

**Date**: 2026-11-13. Unknowns resolved for planning; no application tests run.

## Existing repository and runtime

**Decision**: Treat this as a new application in the existing Spec Kit directory.
**Rationale**: Filesystem inspection found only .agents, .specify and specs. No pom.xml,
source or framework configuration exists in the current chapter path. Java reports 21.0.11;
Docker executable exists; Maven is absent from PATH. Docker daemon readiness is unverified.
**Alternatives considered**: Extending an existing Spring application; ruled out by present
filesystem evidence. Do not assume the earlier IDE HELP.md refers to code available here.

## Java web stack

**Decision**: Java 21, Spring Boot 4.1.2, Maven Wrapper, MVC/Thymeleaf and Boot-managed
Security/Data JPA/Validation. Use a single deployable application.
**Rationale**: Official requirements list Boot 4.1.2 as stable, compatible with Java 17-27
and Maven 3.6.3+. Java 21 is already installed. A server-rendered monolith reduces setup and
deployment work for this small bookstore; that simplicity assessment is a design judgment.
**Alternatives considered**: React plus REST adds a second build pipeline without a present
independent-client requirement. Node.js is viable but would not use the local Java baseline.
**Sources**: [Boot requirements](https://docs.spring.io/spring-boot/system-requirements.html),
[stable versions](https://docs.spring.io/spring-boot/).

## Database and migrations

**Decision**: PostgreSQL 18 in development and production; Flyway migrations and JPA validation.
Use official PostgreSQL container images pinned to a reviewed patch/digest during scaffolding.
**Rationale**: Relational constraints and transactional row locks fit order/inventory rules.
Real PostgreSQL test containers verify the same locking semantics used by the application.
**Alternatives considered**: H2 is unsuitable as sole evidence for PostgreSQL concurrency;
automatic destructive schema updates would lose migration history.
**Sources**: [PostgreSQL locking](https://www.postgresql.org/docs/current/explicit-locking.html),
[Boot Testcontainers support](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html).

## Checkout and cancellation correctness

**Decision**: Transactional services, sorted pessimistic book-row locks, authoritative price
and stock checks, immutable purchased details, and a unique purchase key. Serialize same-key
submissions with transaction-scoped advisory locks; include ownership and payload fingerprint.
Cancellation locks the order and inventory and changes status/restores stock once atomically.
**Rationale**: A lost response or competing shopper must not duplicate an order or oversell.
The specific algorithm is our design derived from the documented database mechanisms.
**Alternatives considered**: JVM-only locks cannot protect multiple processes. Optimistic-only
checkout is possible but needs explicit retries; deterministic row locking is easier to reason
about for this low-volume MVP. The unique key remains a final persistent duplicate guard.
**Sources**: [JPA locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html),
[service transactions](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html),
[PostgreSQL explicit locks](https://www.postgresql.org/docs/current/explicit-locking.html).

## Authentication, carts and receipts

**Decision**: Server-side session admin authentication, CSRF-protected forms and POST logout.
Keep cart in the session, retain guest receipt ownership with a random secret and stored hash,
and never authorize receipt access using the human-readable reference alone.
**Rationale**: Same-origin HTML forms do not require a token-based public API. CSRF protection
and session identity fit both administration and the single-instance guest workflow.
**Alternatives considered**: JWT adds lifecycle and client-storage complexity; customer accounts
and receipt links via email expand the agreed scope. Session loss means loss of guest receipt
access; that limitation is documented and public tracking remains outside the MVP.
**Source**: [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

## Verification and user experience

**Decision**: Unit tests for arithmetic/validation/state transitions; MockMvc security/form
tests; real PostgreSQL integration tests for races, retries and rollback; Playwright Java
browser tests plus actual usability and browser-visible performance measurements.
**Rationale**: Each check proves a different acceptance outcome. A database race test cannot
establish keyboard usability, and document review cannot establish runtime performance.
**Alternatives considered**: Mock-only checkout tests miss database behavior; screenshots alone
cannot prove authorization or inventory integrity. Use plain CSS and modest JavaScript rather
than adding a frontend component build pipeline before a need exists.

## Resolution status

No unresolved planning questions. Business choices are explicit working assumptions in
spec.md, not user-confirmed requirements. Exact tooling/container patch pins and environment
readiness are implementation checks, not reasons to invent existing source or claim tests pass.
