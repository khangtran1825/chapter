# Current implementation status

49/50 tasks complete. T049 awaits five real participants. Local review: http://127.0.0.1:8080/books.

Final verification: 6 unit tests + 36 integration/browser tests passed. Local browser performance p95: 388 ms.

The Phase 1 section below is historical; the completed implementation report follows it.

---

# Phase 1 Validation — Chapter Bookstore

**Date**: 2026-11-13 (Asia/Bangkok)
**Authorized scope**: T001-T005 only. Stop before Phase 2.

## Results

| Task | Result | Evidence |
| --- | --- | --- |
| T001 | Complete | Java 21/Spring Boot 4.1.2 dependencies resolved from Maven Central; application and test source compile; executable JAR built |
| T002 | Complete | Official Maven Wrapper 3.3.4 only-script distribution; Maven 3.9.16 `--version` reports Java 21.0.11; Maven archive verified with Central SHA-512 and wrapper distribution pinned with SHA-256 |
| T003 | Complete | PostgreSQL 18.7 image pinned; Compose config valid; healthy database on 127.0.0.1:15432; `select current_database(), version()` returns chapter and PostgreSQL 18.7 |
| T004 | Complete | Packaged app starts against Compose with local profile; login responds HTTP 200; integration assertions verify JPA validate, default Secure/HttpOnly cookies and 30-minute timeout |
| T005 | Complete | Surefire/Failsafe configured; real PostgreSQL Testcontainers 2.0.5 integration runs 1 test, 0 failures, 0 errors, 0 skipped; Flyway, JPA and JDBC wiring verified |

The built-in spec-quality checklist passed 16/16 before implementation and was not modified.
No extensions.yml was present, so no pre/post implementation hooks were configured.

## Commands executed

- `mvnw.cmd --version`: Maven 3.9.16, Java 21.0.11; global Maven not required.
- `mvnw.cmd -B -ntp test`: BUILD SUCCESS; no `*Test` unit tests exist yet. This verifies compilation,
  not business behavior. Integration tests are intentionally not run by this lifecycle command.
- `docker compose config --quiet`: success with an ignored local .env.
- `docker compose up -d --wait db`: healthy.
- `docker compose exec -T db psql -U chapter -d chapter -c "select current_database(), version();"`:
  confirms the real database and PostgreSQL 18.7.
- `mvnw.cmd -B -ntp verify`: BUILD SUCCESS; final run reports 1 integration test, 0 failures,
  0 errors, 0 skipped. Testcontainers uses a separate disposable chapter_test database.
- Packaged JAR launched with the local profile on temporary localhost port 18080 against the
  Compose database; GET /login returned 200. The temporary Java process was then stopped.

Detailed build/test artifacts are under ignored `target/`: phase1-verify.log,
failsafe-reports/, phase1-startup.log and phase1-startup-error.log.

## Environment adjustment

Docker Desktop was initially stopped and was started for verification. Windows refused the
default host port 5432. The local defaults and quickstart now consistently use 15432; the
container retains internal port 5432 and is bound only on 127.0.0.1.
No other service was stopped or reconfigured. Compose data persists in chapter_chapter_postgres.
The database remains available for the next development session; stop it with
`docker compose stop db` if no longer needed. No data-removal command was run.

A random local database password was generated into ignored .env and was not printed.
.env.example contains only a replacement marker. Spring Boot still needs explicit shell
environment variables; it does not automatically import .env. See README.md for setup.

One verification invocation was interrupted because Windows PowerShell treated a JVM/Mockito
stderr warning as a command error. Re-running with native stdout/stderr log redirection
completed successfully. The warning is not a test failure. No check was disabled to pass.

## Scope and remaining work

- No catalog, cart, checkout, administrator domain logic, custom security policy or Phase 2
  source was implemented. Spring Security's scaffold defaults remain in force.
- The Flyway migration directory is intentionally empty until US1; JPA schema validation
  is configured, but no domain schema or domain validation is claimed.
- Missing Docker causes the integration test to fail; Testcontainers is not configured with
  disabledWithoutDocker=true, and Failsafe fails if no integration tests are discovered.
- Browser tests, concurrency/order tests, usability and performance measurements remain later tasks.
- No deployment, repository initialization or commit was performed. T006-T050 remain unchecked.

## Constitution review for this scope

Maintainability: one Maven application and Boot-managed dependencies. Data correctness: no
business implementation yet; real PostgreSQL test infrastructure established. Privacy:
environment-backed secrets, ignored local files, secure-by-default session configuration.
Verification: compiled source, real container test and packaged startup recorded accurately.
Usability: no product pages yet; responsive/keyboard obligations remain pending.

---

# Implementation and Final Validation — T006–T050

**Date**: 2026-11-13 (Asia/Bangkok)
**Scope**: Continue remaining implementation tasks; retain T049 as pending real participants.

## Delivered behavior

- T006–T010: Checked VND arithmetic, date/UUID helpers, authenticated admin boundary, CSRF,
  private cache headers, safe errors and shared responsive/keyboard-accessible layout.
- T011–T018: Book/category migration and entities, literal case-insensitive title/author search,
  combined category filter, stable pagination, private-safe errors, escaped content, local-only
  fictional catalog and cover fallback.
- T019–T024: Session-isolated cart with 100-line bound, positive/overflow/stock validation,
  reload retention, current-price totals, stale-stock warnings and add/update/remove actions.
- T025–T034: COD checkout with validated delivery, frozen quote, authoritative price/stock
  checks, transactional sorted row locks and purchase-key advisory locks, immutable order
  snapshots, private receipt ownership and retry-safe cart clearing.
- T035–T044: Database-backed adaptive-password admin authentication, bounded failure throttling,
  session rotation and logout, catalog/category/inventory management, stale-edit protection,
  order status workflow and atomic once-only replenishment on cancellation.
- T045–T048: Pinned Playwright/Chromium tests, viewport/keyboard/privacy/admin journeys,
  real browser-visible load measurement with 1,000 books and 20 contexts.
- T050: Reproducible README/Run-Local/quickstart, final automated verification, actual packaged
  startup against Compose, and this documented review. T049 is deliberately incomplete.

## Verification results

| Check | Result |
| --- | --- |
| Unit suite | 6 passed; overflow, invalid quantities, cart mutation, capacity and arithmetic |
| Final default integrations | 33 PostgreSQL/MVC integrations included in the final combined run |
| Chromium journeys | 3 tests passed, including shopper flows at 360/768/1440px and admin workflow |
| Final browser-profile verify | BUILD SUCCESS; 36 integration/browser tests, 0 failures/errors/skips, plus 6 unit tests |
| Dedicated performance verify | BUILD SUCCESS; 1 performance test and 6 unit tests passed |
| Performance sample | 1,000 books, 20 isolated browser contexts, p95 388 ms; target <=2,000 ms |
| Real local startup | 16 fictional catalog books, 1 administrator, /books HTTP 200 at 127.0.0.1:8080 |
| Human usability | Not performed; T049/SC-001/SC-002 remain pending |

Key checks prove final-copy contention accepts one purchase, concurrent same-key submission
creates one order, a second-line conflict rolls back earlier stock changes, retries reject
changed delivery/owner and preserve newer cart state, and concurrent cancellation restores stock
once. Inventory edit-versus-checkout conflicts cannot overwrite deductions. All administrative
read/write route families are checked for authentication, role authorization and CSRF.
Login session-ID rotation and cart preservation are checked in MVC and an actual Chromium session.
Another browser session receives 404 for the private confirmation and cannot see delivery data.

## Evidence and execution

- Meaningful red-stage behavior was observed before implementations: arithmetic overflow/input
  checks failed, then missing catalog/cart/checkout/admin routes failed their expected behavior.
  Implementations and regression fixes followed. Compilation/setup errors were not counted
  as evidence that a business behavior test failed.
- Final command: `mvnw.cmd -B -ntp clean -Pbrowser-tests verify`.
- Performance command: `mvnw.cmd -B -ntp -Pperformance-tests verify`.
- Logs retained in ignored root files: foundation-red.log was under target before clean;
  catalog-check.log, cart-check.log, checkout-check.log, admin-check.log, browser-check.log,
  performance-check.log and final-check.log capture later checkpoints.
- Final test reports and viewport screenshots are in ignored target/. Source code was formatted
  using google-java-format 2.0.0 and Prettier 3.10.1 before the final verification.
- Permanent performance evidence: [performance-results.json](performance-results.json),
  copied verbatim from the recorded CATALOG_PERFORMANCE JSON in performance-check.log.
  Later clean builds removed temporary target/performance output; the measured record is retained
  here and restored under target/performance/catalog.json for convenience.
- Visual inspection included desktop catalog and mobile checkout captures. A clipped caption
  on the cream cover was reduced to fit before final screenshots were regenerated.

Performance environment: Windows 11, Java 21.0.11, 16 logical processors, reported RAM
16,849,264,640 bytes (about 15.7 GiB), Docker Desktop 29.6.2 with about 7.6 GiB available,
PostgreSQL 18.7, Chromium 158.0.8127.18, Playwright 1.65.0. Each browser context loaded the
catalog once for warm-up; timed search navigations were scheduled together with a three-second
delay. The 20 samples span 252–388 ms; timed execution including scheduling took 4.55 seconds.
The metric is max(DOMContentLoaded, first-contentful-paint), sampled in the browser on loopback.
No production latency, sustained throughput or cold-start performance claim is made.

## Current local review

The application is running only on http://127.0.0.1:8080/books. Admin login is /admin/login.
Local credentials are stored in ignored .env, not this report or logs. The process ID is in
target/local-app.pid. Run-Local.ps1 was exercised with -PrepareOnly, then the packaged JAR was
started with those environment values and migrations were applied to the local Compose database.
Demo seeding creates data only if both catalog tables are empty. The review process and database
are left running for the user; no external hosting or other service was changed.

## Final constitution and requirements review

Maintainability: one feature-oriented Maven application, Boot-managed dependencies, formatted
source and reproducible run/test commands. Correctness: authoritative stock/prices, transaction
rollback, persistent duplicate keys, immutable snapshots and verified status transitions.
Privacy: database-backed adaptive hashes, environment-only bootstrap, CSRF, session ownership,
private no-store pages and no public order lookup. Verification: real PostgreSQL and Chromium
results distinguished from manual checks. Usability: responsive/keyboard journeys pass the
automated checks; five-person completion-rate claims remain unverified.

FR-001–FR-012 have implementation and corresponding tests. SC-003/004 pass their automated
acceptance checks; SC-005 passes the recorded local browser/layout and timing checks.
SC-001/002 await [the participant study](usability-results.md). T049 stays unchecked; 49 of
50 tasks are completed, with the manual external dependency explicitly recorded.

MVP limitations remain explicit: one instance, COD/free delivery, no account/email/payment/carrier
integration. Session expiry loses guest cart/receipt access; preserve reference for store contact.
A customer-data retention/deletion policy is required before real sales. No deployment or commit
was performed during the implementation run; Git publication is handled separately. No extension hooks were configured.
