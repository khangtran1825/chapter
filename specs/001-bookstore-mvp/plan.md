# Implementation Plan: Chapter Bookstore MVP

**Branch**: None (repository has no Git extension) | **Date**: 2026-11-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-bookstore-mvp/spec.md`.

## Summary

Build a Vietnamese physical-book storefront with search, categories, browser-session cart,
guest COD checkout and authenticated administration. Use a single Java application serving
HTML pages, with transactional PostgreSQL persistence for inventory and orders.
Scope assumptions remain those recorded in the specification; this plan does not expand them.

## Technical Context

**Language/Version**: Java 21 (local installation reports 21.0.11).

**Primary Dependencies**: Spring Boot 4.1.2; Boot-managed MVC, Thymeleaf, Security,
Validation, Data JPA; PostgreSQL driver; Flyway with its PostgreSQL support module.
Use Maven Wrapper with a pinned Maven 3.9 release. Let the Boot BOM manage dependency
versions where available; resolve the exact wrapper and browser-test versions at scaffolding.

**Storage**: PostgreSQL 18, Flyway versioned migrations, JPA schema validation only.
Server-side HTTP session stores cart and guest receipt ownership; one app instance for MVP.

**Testing**: Boot-managed test tooling, JUnit, MockMvc, Spring Security tests,
PostgreSQL Testcontainers, and Playwright Java for real-browser acceptance checks.

**Target Platform**: Windows development; JVM deployment with a PostgreSQL service.
Modern desktop/mobile browsers; local development defaults to localhost only.

**Project Type**: Server-rendered monolithic web application, feature-oriented packages.

**Performance Goals**: 95% of catalog/search interactions visible within two seconds with
1,000 books and 20 shoppers; document hardware, browser, network and warm-up when measuring.

**Constraints**: Vietnamese UI, VND integer amounts, zero delivery charge, COD only,
360-1440px responsive layouts, keyboard access, protected admin routes and private receipts.

**Scale/Scope**: One bookstore and one application instance; four user stories, no external
payment, carrier service, customer accounts, file uploads or public order lookup.

**Repository evidence**: Current chapter directory contains only `.agents`, `.specify`,
and `specs`; no pom.xml, source, Maven Wrapper or existing Java architecture was found.
Java is present; Maven is not on PATH; Docker executable is present but daemon readiness
has not been checked. Earlier IDE hints of Maven were not corroborated in this directory.

## Constitution Check

| Principle | Before research | After design and planned evidence |
| --- | --- | --- |
| Maintainable and scoped | Pass: one application, no extra services | Pass: feature packages, spec-to-test mapping |
| Correct orders/inventory | Pass: transactional checkout required | Pass: row locking, immutable snapshots, unique checkout key; concurrency tests planned |
| Privacy/access control | Pass: admin-only operations and private receipts | Pass: session authorization, CSRF, password hashing, log redaction |
| Verify business rules | Pass: integration tests in scope | Pass: real PostgreSQL tests for stock, retries, cancellations and permissions |
| Usable storefront | Pass: responsive and keyboard requirements retained | Pass: browser checks at 360/768/1440px plus manual usability scenarios |

These are design compliance gates; none claims running-software tests have passed.
No exceptions are required. Before implementation completion, rerun against actual behavior.

## Project Structure

### Documentation (this feature)

```text
specs/001-bookstore-mvp/
  spec.md
  plan.md
  research.md
  data-model.md
  quickstart.md
  contracts/http.md
  checklists/requirements.md
  tasks.md                  # subsequent task-generation phase
```

### Source Code (repository root)

```text
pom.xml
mvnw
mvnw.cmd
.mvn/wrapper/
compose.yaml
.env.example
README.md
src/main/java/com/chapter/bookstore/
  BookstoreApplication.java
  catalog/                 # category/book domain, queries, public pages
  cart/                    # session cart and quantity validation
  checkout/                # quote validation and transactional order creation
  order/                   # snapshots, order status transitions
  admin/                   # protected management pages and forms
  security/                # authentication and session policy
  shared/                  # errors, formatting, common configuration
src/main/resources/
  application.yml
  application-local.yml
  db/migration/
  templates/{catalog,cart,checkout,admin,fragments,error}/
  static/{css,js,images}/
src/test/java/com/chapter/bookstore/
  unit/
  integration/
  browser/
```

**Structure Decision**: Proposed layout, not existing source. Thymeleaf and small progressive
enhancements avoid a second build/deployment pipeline. Controllers bind validated forms;
services enforce rules; repositories own persistence queries. No separate public JSON API.

## Delivery and Verification Sequence

1. Scaffold Maven project, local PostgreSQL, migrations, security defaults and fixtures.
2. Deliver catalog and search (FR-001-003), then cart (FR-004-005).
3. Deliver checkout and private confirmation (FR-006-008), including retry/concurrency tests.
4. Deliver administrator sign-in, catalog maintenance and order workflow (FR-009-011).
5. Validate responsive/keyboard behavior (FR-012), measure performance and run usability checks.

Phase 0 research and Phase 1 design are recorded in the accompanying files. Task generation
is the next phase; no application implementation or deployment is part of this planning run.

## Complexity Tracking

No constitution violations or additional architectural exceptions.
