# Tasks: Chapter Bookstore MVP

**Input**: Design documents from `specs/001-bookstore-mvp/`.
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [HTTP contracts](contracts/http.md), [quickstart.md](quickstart.md),
and `.specify/memory/constitution.md`.
**Status**: 49/50 tasks implemented and verified 2026-11-13. T049 awaits five real participants; see validation.md and usability-results.md.

**Tests**: Included because FR-007, SC-003/004/005 and constitution IV explicitly require
verification of orders, stock, permissions and user journeys. Write the listed tests before
their implementations, observe meaningful behavior failures, then make them pass. Do not
mistake compilation/setup failures for verification of a missing behavior.

**Organization**: Seven phases; four independently verifiable user-story increments.

## Format: `[ID] [P?] [Story] Description`

Every task uses an unchecked checkbox and unique sequential ID. `[P]` marks file-disjoint
work that can run concurrently within its stated dependency wave; it does not waive prerequisites.
Story tasks carry `[US1]` through `[US4]`. All paths are relative to the chapter repository root.
Do not implement in the unrelated todo-list-app workspace. No task authorizes publication.

## Path Conventions

Java packages live under `src/main/java/com/chapter/bookstore/`. Tests live under
`src/test/java/com/chapter/bookstore/`. Templates, static files and migrations live under
`src/main/resources/`. Paths below are proposed implementation files, not existing source.
Tests use fictional data. Keep unchecked tasks unchecked until implementation and verification
are actually complete. Any blocked manual measurement must remain explicitly pending.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the Java application and reproducible local build environment.

- [X] T001 Scaffold Java 21/Spring Boot 4.1.2 with MVC, Thymeleaf, Security, Validation, JPA, PostgreSQL and Flyway PostgreSQL support in `pom.xml` and `src/main/java/com/chapter/bookstore/BookstoreApplication.java`; use Boot-managed dependency versions and resolve artifact availability before proceeding.
- [X] T002 Pin a reviewed Maven 3.9 release and distribution checksum in `.mvn/wrapper/maven-wrapper.properties`; add `mvnw` and `mvnw.cmd` and verify `mvnw.cmd --version` uses Java 21 without requiring global Maven.
- [X] T003 [P] Define a localhost-bound PostgreSQL 18 service with health check and persistent volume in `compose.yaml`, pin a reviewed image patch/digest, and document fictional local settings in `.env.example`; exclude `.env`, build outputs and browser artifacts in `.gitignore`.
- [X] T004 [P] Configure environment-backed database credentials, Flyway, JPA schema validation, 30-minute sessions, production Secure cookies and localhost-only local defaults in `src/main/resources/application.yml` and `src/main/resources/application-local.yml`; do not embed bootstrap secrets.
- [X] T005 Configure Surefire `*Test`, Failsafe `*IT`, security test support and PostgreSQL Testcontainers in `pom.xml`; create `src/test/java/com/chapter/bookstore/integration/PostgresTestSupport.java`, pin required tooling, and fail integration checks clearly when Docker is unavailable rather than skipping them.

**Checkpoint**: Wrapper resolves the project; database is reachable; test infrastructure can start
an isolated PostgreSQL instance. T003 and T004 can run after T001 alongside T002; T005 follows
T001/T002 and the database configuration. Do not claim Docker readiness before checking it.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish common security, formatting, page structure and database verification.
Complete T001-T005 before this phase; complete this phase before user-story work.

- [X] T006 [P] Implement checked integer-VND multiplication/addition, nonnegative amount validation, UUID helpers and UTC/Asia_Ho_Chi_Minh presentation in `src/main/java/com/chapter/bookstore/shared/Money.java` and `src/test/java/com/chapter/bookstore/unit/MoneyTest.java`; reject overflow instead of wrapping.
- [X] T007 [P] Add default-deny `/admin/**` authorization with an explicit public exception for GET/POST `/admin/login`, public catalog/cart/checkout routes, CSRF for unsafe methods including login/logout, HttpOnly/SameSite=Lax cookies and session-ID rotation configuration in `src/main/java/com/chapter/bookstore/security/SecurityConfig.java`; leave administrator provisioning to US4.
- [X] T008 [P] Create escaped Vietnamese layout, navigation, accessible focus styling, VND formatting, labeled controls and responsive base styles in `src/main/resources/templates/fragments/layout.html` and `src/main/resources/static/css/site.css`; use no external frontend build pipeline.
- [X] T009 [P] Implement safe 400/403/404/409/422/error responses, private-page `Cache-Control: no-store` support and redacted logging in `src/main/java/com/chapter/bookstore/shared/WebErrorHandler.java` and `src/main/resources/templates/error/error.html`; never log recipient forms, credentials or tokens.
- [X] T010 Add security/session/CSRF smoke tests and a migration-versus-JPA validation check in `src/test/java/com/chapter/bookstore/integration/FoundationIT.java`; verify public access, denied admin access, consistent error rendering and no PII in errors after T006-T009.

**Checkpoint**: Shared checks pass. Protected routes remain protected even before US4 exposes
the administrator login flow. Book/category/order migrations arrive in the relevant stories.

## Phase 3: User Story 1 - Discover Books (Priority: P1) — First Demo

**Goal**: Visitors can browse, search, filter and inspect books without checkout or admin features.
**Independent Test**: Load a fictional catalog, locate a known title/author, combine a category,
inspect an available and unavailable book, and recover from no results or unknown IDs.

### Tests for User Story 1

- [X] T011 [P] [US1] Write PostgreSQL catalog query checks in `src/test/java/com/chapter/bookstore/integration/CatalogQueryIT.java` for trimmed case-insensitive/accent-sensitive title/author search, literal `%`/`_`, category intersection, 12-item stable title/ID pagination, archived exclusion and stock-zero visibility (FR-001-003).
- [X] T012 [P] [US1] Write page contracts in `src/test/java/com/chapter/bookstore/integration/CatalogWebIT.java` for `/`, `/books`, `/books/{id}`, invalid query/UUID/page inputs (400), missing details (404), empty results, escaped text and disabled purchase controls for unavailable books.

### Implementation for User Story 1

- [X] T013 [US1] Create `src/main/resources/db/migration/V1__catalog.sql`, `src/main/java/com/chapter/bookstore/catalog/Category.java` and `src/main/java/com/chapter/bookstore/catalog/Book.java`: UUID IDs; category `name (1-100 chars, unique ignoring case); createdAt`; book `title (1-255); author (1-255); description (1-10000); coverPath (optional application-local image); priceVnd >= 0; stock integer >= 0; active; version; timestamps`; required category, restricted deletion, not-null constraints and stock limited to Java int range.
- [X] T014 [US1] Implement `src/main/java/com/chapter/bookstore/catalog/BookRepository.java`, `src/main/java/com/chapter/bookstore/catalog/CategoryRepository.java` and `src/main/java/com/chapter/bookstore/catalog/CatalogService.java` with escaped literal search, combined filtering and deterministic pagination after T013.
- [X] T015 [US1] Implement catalog routing and validated search `q <= 200 chars`, optional category UUID and `page >= 0` in `src/main/java/com/chapter/bookstore/catalog/CatalogController.java`; follow `contracts/http.md` for 400/404 and root redirect behavior.
- [X] T016 [US1] Build cover/title/author/VND/availability cards, category/search controls, pagination, details and clear-filter empty states in `src/main/resources/templates/catalog/list.html` and `src/main/resources/templates/catalog/detail.html`; add local fallback artwork at `src/main/resources/static/images/book-placeholder.svg` and wrap long Vietnamese text.
- [X] T017 [US1] Add repeatable fictional catalog data only for the local profile in `src/main/java/com/chapter/bookstore/catalog/LocalCatalogSeeder.java`; do not mutate production inventory or create real customer data.
- [X] T018 [US1] Run T011/T012 and the standalone discovery walkthrough, recording exact commands/results and any SC-001 human-check limitation in `specs/001-bookstore-mvp/validation.md`; fix regressions before marking US1 complete.

**Checkpoint**: Catalog-only demo ready. Cart/checkout links must not lead to broken pages;
introduce those links when their stories are delivered. This is the smallest useful demo,
not the full purchasing MVP.

## Phase 4: User Story 2 - Manage a Shopping Cart (Priority: P2)

**Goal**: Add/update/remove books, review accurate totals and keep cart across same-session reloads.
**Independent Test**: With seeded books, add two books, repeat an add, change quantities, remove
a line and reload; reject invalid quantities and verify empty-cart recovery.
**Prerequisites**: Foundation and US1 catalog entities/services; no US3/US4 required.

### Tests for User Story 2

- [X] T019 [P] [US2] Write cart rule tests in `src/test/java/com/chapter/bookstore/unit/CartTest.java` for positive integer quantities, combined add quantities, stock bounds, overflow, removal, `maximum 100 distinct lines`, empty cart and checked totals (FR-004-005).
- [X] T020 [P] [US2] Write session/form contracts in `src/test/java/com/chapter/bookstore/integration/CartWebIT.java` for add/update/remove routes, CSRF, same-session reload retention, separate-session isolation, stale/archived books and clear stock warnings without silently changing cart contents.

### Implementation for User Story 2

- [X] T021 [US2] Implement `src/main/java/com/chapter/bookstore/cart/Cart.java` and `src/main/java/com/chapter/bookstore/cart/CartService.java` as a `map of book ID to quantity, maximum 100 distinct lines`; use the session with `default 30-minute inactivity timeout`, current authoritative prices and a mutation version for later quote invalidation; keep PII/prices out of cookies and reject overflowing quantity additions.
- [X] T022 [US2] Implement GET `/cart` and POST add/quantity/remove form routes in `src/main/java/com/chapter/bookstore/cart/CartController.java` with `quantity integer 1..available`, trim/type checks, idempotent removal, 303 success redirects and retained inputs on errors.
- [X] T023 [US2] Build `src/main/resources/templates/cart/view.html` and connect add forms in `src/main/resources/templates/catalog/detail.html`; show labeled quantity/remove controls, current stock/price warnings, line totals, empty recovery and unavailable checkout for empty carts; enable checkout navigation only once US3 exists.
- [X] T024 [US2] Run cart tests and the seeded standalone cart walkthrough; append verified session retention, totals, invalid inputs and rollback evidence to `specs/001-bookstore-mvp/validation.md` without assuming checkout implementation.

**Checkpoint**: US1 remains green and cart is independently usable with fictional book fixtures.

## Phase 5: User Story 3 - Place a COD Order (Priority: P2)

**Goal**: Guest COD purchase with validated delivery, private receipt, stock correctness and safe retries.
**Independent Test**: Use a prepared session cart to submit delivery details; verify one order,
correct immutable lines and one stock deduction; repeat with invalid input and insufficient stock.
**Prerequisites**: US1 entities and US2 cart; no administrator UI required.

### Tests for User Story 3

- [X] T025 [P] [US3] Write real PostgreSQL concurrency/rollback tests in `src/test/java/com/chapter/bookstore/integration/CheckoutIT.java` for final-copy races, repeated concurrent purchase keys, same-key changed payload conflicts, stale price/stock, archived books, rolled-back writes and immutable line snapshots (FR-007, SC-003).
- [X] T026 [P] [US3] Write checkout/confirmation page tests in `src/test/java/com/chapter/bookstore/integration/CheckoutWebIT.java` for required delivery validation, CSRF, preserved fields/cart, empty-cart rejection, zero delivery, private-owner receipt/no-store, lost-response retry and a later cart mutation surviving an earlier response (FR-005-008, SC-004).

### Implementation for User Story 3

- [X] T027 [US3] Create `src/main/resources/db/migration/V2__orders.sql`, `src/main/java/com/chapter/bookstore/order/CustomerOrder.java` and `src/main/java/com/chapter/bookstore/order/OrderLine.java`: UUID IDs; `reference (random unique); checkoutKey (unique); requestFingerprint; ownerTokenHash; recipientName (1-100); phone (0 followed by 9 digits); street (1-255); ward/district/province (1-100 each); subtotalVnd; deliveryVnd=0; totalVnd; status; timestamps; version`; line `orderId; bookId; titleSnapshot; authorSnapshot; unitPriceVnd >= 0; quantity > 0; lineTotalVnd`; not-null/FK/unique constraints, BIGINT money and at least one immutable line per order.
- [X] T028 [US3] Implement `src/main/java/com/chapter/bookstore/checkout/DeliveryForm.java`, `src/main/java/com/chapter/bookstore/checkout/CheckoutQuote.java` and `src/main/java/com/chapter/bookstore/checkout/CheckoutSession.java`; enforce T027 lengths/phone, random server-generated single-purchase keys, session owner secrets, frozen quantities/reviewed prices, one active quote, invalidation on cart mutation and retained submitted quote/ownership for retry.
- [X] T029 [US3] Add `src/main/java/com/chapter/bookstore/order/OrderRepository.java` and `src/main/java/com/chapter/bookstore/checkout/CheckoutLockRepository.java`; extend `src/main/java/com/chapter/bookstore/catalog/BookRepository.java` for sorted pessimistic book locks and use transaction-scoped advisory locking per purchase key plus persistent unique-key duplicate protection.
- [X] T030 [US3] Implement atomic checkout in `src/main/java/com/chapter/bookstore/checkout/CheckoutService.java`: canonical request fingerprint/owner verification, existing-result retry before mutable cart checks, sorted row locks, price/stock/active revalidation, positive quantities, checked totals (`subtotal equals sum of line totals; total equals subtotal plus delivery`), immutable order/lines, PENDING status and stock deduction in one transaction; conflicts/failures roll back and require renewed review as specified.
- [X] T031 [US3] Implement GET/POST `/checkout` and session-owned GET `/orders/{reference}/confirmation` in `src/main/java/com/chapter/bookstore/checkout/CheckoutController.java`; preserve inputs on 422/409, use 303 after commit, return 404 for wrong receipt owner and clear only the submitted unchanged cart after commit without losing retry identity.
- [X] T032 [US3] Build labeled delivery/review/COD/error-summary pages in `src/main/resources/templates/checkout/form.html` and `src/main/resources/templates/checkout/confirmation.html`; display deliveryVnd=0 and immutable receipt totals, keep private pages uncached and enable checkout navigation in `src/main/resources/templates/cart/view.html`.
- [X] T033 [US3] Complete receipt-secret hashing, private cache headers and lock-timeout/deadlock recovery behavior in `src/main/java/com/chapter/bookstore/checkout/ReceiptAccess.java` and `src/main/java/com/chapter/bookstore/checkout/CheckoutService.java`; retain valid cart/input and never expose owner secrets, stack traces or recipient details in errors/logs.
- [X] T034 [US3] Run checkout tests including genuinely concurrent PostgreSQL requests and lost-response retries, then append exact evidence and remaining manual usability checks to `specs/001-bookstore-mvp/validation.md`; verify US1/US2 regressions remain green.

**Checkpoint**: Guest purchasing MVP works with prepared catalog data; no duplicate orders,
negative stock or cross-session receipts. Race tests must use separate real transactions.

## Phase 6: User Story 4 - Administer the Store (Priority: P3)

**Goal**: Authorized staff manage catalog/inventory and progress or cancel orders safely.
**Independent Test**: Using fixtures and an admin identity, edit/archive books, adjust stock,
process seeded orders and retry cancellation; verify unauthorized access is denied.
**Prerequisites**: Foundation, US1 catalog and US3 order entities. The UI can be verified against
seeded orders without exercising the checkout UI; it still reuses the delivered order model.

### Tests for User Story 4

- [X] T035 [P] [US4] Write admin route/authentication tests in `src/test/java/com/chapter/bookstore/integration/AdminSecurityIT.java` for every `/admin/**` read/write, CSRF/login/logout, session-ID rotation, cart preservation on login, disabled users, bounded failure throttling and generic authentication errors (FR-009, SC-004).
- [X] T036 [P] [US4] Write management integration tests in `src/test/java/com/chapter/bookstore/integration/AdminManagementIT.java` for catalog validation/version conflicts, category deletion restrictions, archive/reprice history preservation, stock edit-versus-checkout races, permitted/forbidden order transitions and once-only stock restoration under concurrent/retried cancellation (FR-010-011).

### Implementation for User Story 4

- [X] T037 [US4] Create `src/main/resources/db/migration/V3__administrators.sql`, `src/main/java/com/chapter/bookstore/security/AdminUser.java` and `src/main/java/com/chapter/bookstore/security/AdminUserRepository.java`: UUID ID; `normalized username (3-100, unique); passwordHash; enabled; role ADMIN`; required fields, no public registration and adaptive password hashes rather than plaintext.
- [X] T038 [US4] Implement environment-only one-time bootstrap and database-backed authentication in `src/main/java/com/chapter/bookstore/security/AdminBootstrap.java` and `src/main/java/com/chapter/bookstore/security/AdminDetailsService.java`; update `src/main/java/com/chapter/bookstore/security/SecurityConfig.java` for login/logout, session rotation/cart preservation and no shared default credentials; build `src/main/resources/templates/admin/login.html` and `src/main/java/com/chapter/bookstore/security/LoginAttemptGuard.java` with bounded per-origin/username failure counters and short lockout.
- [X] T039 [US4] Implement validated book metadata edits, archive/activate and row-locked expected-version stock adjustments in `src/main/java/com/chapter/bookstore/admin/AdminCatalogService.java` and `src/main/java/com/chapter/bookstore/admin/BookForm.java`; preserve the exact T013 constraints, reject arbitrary remote cover fetches/uploads, forbid hard deletion and prevent stock edits from overwriting checkout deductions.
- [X] T040 [US4] Implement category list/create/rename/delete with `name (1-100 chars, unique ignoring case)` and deletion restricted while referenced in `src/main/java/com/chapter/bookstore/admin/AdminCategoryController.java` and `src/main/resources/templates/admin/categories.html`; return retained validation errors or conflicts rather than bypassing foreign keys.
- [X] T041 [US4] Implement book list/new/edit/inventory/archive/activate HTTP contracts in `src/main/java/com/chapter/bookstore/admin/AdminBookController.java`, `src/main/resources/templates/admin/books.html` and `src/main/resources/templates/admin/book-form.html`; include CSRF and expected versions, stable pagination, 303 successes, 422 errors and 409 concurrent-edit recovery.
- [X] T042 [US4] Implement the exact `PENDING -> CONFIRMED -> SHIPPING -> COMPLETED`, `PENDING -> CANCELLED`, `CONFIRMED -> CANCELLED` state machine in `src/main/java/com/chapter/bookstore/order/OrderStatus.java` and `src/main/java/com/chapter/bookstore/order/OrderManagementService.java`; COMPLETED/CANCELLED are terminal, same-current-status retries are no-ops, other transitions fail, and cancellation locks order then sorted books and restores stock atomically once without changing snapshots.
- [X] T043 [US4] Implement order list/detail/status routes with known-status filtering, newest-first pagination, expected current status and protected recipient details in `src/main/java/com/chapter/bookstore/admin/AdminOrderController.java`, `src/main/resources/templates/admin/orders.html` and `src/main/resources/templates/admin/order-detail.html`.
- [X] T044 [US4] Run administrator/security/concurrency checks and a fixture-based management walkthrough; record results and denied access coverage in `specs/001-bookstore-mvp/validation.md`, and rerun earlier story regression checks before completing US4.

**Checkpoint**: Full staff workflow works against fixtures and purchased orders; cancellation
cannot replenish twice and catalog edits cannot alter historical purchase details.

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Validate the complete purchasing MVP and record reproducible evidence, not just
document completion. Requires all four story checkpoints; no new business features.

- [X] T045 Configure a pinned Playwright Java `browser-tests` Maven profile in `pom.xml` and `src/test/java/com/chapter/bookstore/browser/BrowserTestSupport.java`; start the test app with isolated PostgreSQL fixtures, install Chromium per `specs/001-bookstore-mvp/quickstart.md` and capture screenshots on failures without requiring a manually running application.
- [X] T046 [P] Add complete shopper keyboard/browser checks at 360/768/1440px in `src/test/java/com/chapter/bookstore/browser/StorefrontBrowserIT.java`; cover search, cart, checkout, confirmation, labels/focus, error summaries, long Vietnamese text and no horizontal overflow; fix issues in the owning templates and `src/main/resources/static/css/site.css` (FR-012).
- [X] T047 [P] Add admin and cross-session privacy browser checks in `src/test/java/com/chapter/bookstore/browser/AdminBrowserIT.java`; verify navigation/login/logout, mutation feedback, private receipts, escaped stored content, anonymous denied access and absence of customer data on public pages.
- [X] T048 Measure SC-005 with 1,000 fictional books and 20 distinct shopper sessions using `src/test/java/com/chapter/bookstore/browser/CatalogPerformanceIT.java`; run only in a dedicated `performance-tests` profile configured in `pom.xml`, measure browser-visible p95 against two seconds and record hardware/network/warm-up/duration in `specs/001-bookstore-mvp/validation.md`; optimize catalog queries/indexes only if evidence requires it.
- [ ] T049 Run five-participant discovery/checkout checks for SC-001/002 and record anonymized completion times, at least 4/5 discovery under 60 seconds and checkout under three minutes in `specs/001-bookstore-mvp/usability-results.md`; if participants are unavailable, leave this task unchecked and explicitly report it pending rather than fabricating observations.
- [X] T050 Run `mvnw.cmd test`, `mvnw.cmd verify`, browser-profile verify and the actual quickstart walkthrough; reconcile commands/environment requirements in `README.md` and `specs/001-bookstore-mvp/quickstart.md`, record results/limitations and a final constitution/FR/SC review in `specs/001-bookstore-mvp/validation.md`; document session-expiry receipt limitations and production retention-policy prerequisite, with no deployment performed.

## Dependencies & Execution Order

### Phase Dependencies

```text
Setup T001-T005 -> Foundation T006-T010 -> US1 T011-T018
                                         |
                                         v
                                    US2 T019-T024
                                         |
                                         v
                                    US3 T025-T034
                                         |
                                         v
                                    US4 T035-T044
                                         |
                                         v
                                Polish T045-T050
```

US3 and US4 are both tied to common book/order models. The sequence above avoids overlapping
repository/schema edits; story independence means independent acceptance with fixtures,
not rebuilding shared entities separately. Never edit a shared file concurrently.

### Within Each Story

- Write the story's test wave first using the existing fixture infrastructure. Implement
  models/migrations, repositories/services, controllers/templates, then run its checkpoint.
- US1: T011/T012 together; T013 -> T014 -> T015 -> T016 -> T017 -> T018.
- US2: T019/T020 together; T021 -> T022 -> T023 -> T024.
- US3: T025/T026 together; T027 -> T028 -> T029 -> T030 -> T031 -> T032 -> T033 -> T034.
- US4: T035/T036 together; T037 -> T038 -> T039 -> T040 -> T041 -> T042 -> T043 -> T044.
- Polish: T045 -> T046/T047 together -> T048 -> T049 -> T050. If T049 is externally blocked,
  T050 may record the limitation while T049 and full usability success remain pending.
- No test task passes simply because it was written. Check behavior failures before the
  implementation and record successful execution afterward. Keep database integration tests real.

### Parallel Opportunities and Examples per Story

| Wave | Safe parallel work | Prerequisite |
| --- | --- | --- |
| Setup | T003 compose/environment sample; T004 application config | T001 |
| Foundation | T006 money; T007 security; T008 layout; T009 safe errors | T001-T005 |
| US1 | T011 catalog query checks; T012 catalog page checks | T006-T010 |
| US2 | T019 cart rules; T020 session/form checks | US1 complete |
| US3 | T025 PostgreSQL checkout checks; T026 page/privacy checks | US2 complete |
| US4 | T035 administrator security checks; T036 management checks | US3 complete |
| Polish | T046 storefront browser checks; T047 admin/privacy checks | T045 |

These examples describe optional file-disjoint execution, not a requirement to spawn agents.
When one worker performs implementation, use the sequential task order. Shared fixture,
pom.xml, BookRepository.java, SecurityConfig.java and validation.md edits stay sequential.

## Requirement Coverage

| Requirements | Main tasks | Proof/checkpoint |
| --- | --- | --- |
| FR-001, FR-002, FR-003 | T011-T018 | Catalog query/page checks and discovery demo |
| FR-004 | T019-T024 | Quantity limits, session reload and cart isolation |
| FR-005 | T006, T019-T026, T030-T034 | Checked totals, empty cart, checkout review |
| FR-006 | T026, T028, T031-T034 | Delivery validation and preserved input |
| FR-007 | T025, T027-T030, T033-T034 | Real stock/retry/rollback races |
| FR-008 | T026, T031-T034 | Private confirmation and commit-only cart clearing |
| FR-009 | T007, T010, T035, T037-T038, T044, T047 | Admin authorization, CSRF and sessions |
| FR-010 | T036, T039-T041, T044 | Catalog constraints, version conflicts, history |
| FR-011 | T036, T042-T044 | Order transitions and once-only replenishment |
| FR-012 | T008, T016, T023, T032, T045-T046 | Responsive keyboard journeys |
| SC-001/002 | T049 | Actual five-person measured usability |
| SC-003 | T025, T034, T036, T044, T050 | Order/inventory/validation evidence |
| SC-004 | T010, T026, T035, T044, T047, T050 | Permissions and PII isolation |
| SC-005 | T045-T046, T048 | Browser layout and measured catalog p95 |

## Implementation Strategy

### MVP First (User Story 1 Only)

Complete setup and foundation, then US1. Demonstrate a usable catalog locally and validate
its checkpoint before adding cart/checkout. This offers an early visible result without
claiming the full bookstore purchasing experience is finished.

### Incremental Delivery

Add US2 cart, US3 safe guest checkout, and US4 administration in that order. Run relevant
regressions at every checkpoint and mark only verified tasks complete. The purchasing MVP
requires US1-US4; final usability/performance claims require measured evidence from Phase 7.

### Completion Criteria

All 50 tasks have concrete paths, prerequisites and expected behavior. Generation validates
task format/coverage only. Application correctness remains unverified until implementation
and execution. Report any pending participant/environment work separately from completed code.
Business assumptions remain those in spec.md; do not silently introduce accounts, payments,
shipping integrations or public order lookup. Deployment is a separate user-authorized action.
