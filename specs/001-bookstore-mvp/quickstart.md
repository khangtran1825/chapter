# Quickstart and Validation — Chapter Bookstore

The bookstore is implemented. Run from the chapter repository root using JDK 21 and Docker
Desktop in Linux-container mode. Maven Wrapper supplies Maven 3.9.16; first-time dependency
and browser installation needs network access.

## Start locally

```powershell
.\Run-Local.ps1
```

The script prepares ignored .env settings with random local passwords, starts the PostgreSQL
18.7 Compose service and sets the corresponding Spring shell environment explicitly before
starting the application. It never evaluates .env content as code or prints passwords.
If PowerShell blocks script execution, use the process-local invocation documented in
[README.md](../../README.md); no machine-wide execution-policy change is required.

Visit http://127.0.0.1:8080/books and http://127.0.0.1:8080/admin/login. Find local administrator
credentials in .env under CHAPTER_ADMIN_USERNAME and CHAPTER_ADMIN_PASSWORD. The credentials
bootstrap a user only if that username is absent; changing the bootstrap password does not
reset an existing user's password. Do not commit .env or use real customer data for checks.

Local database uses 127.0.0.1:15432, avoiding the Windows restriction on port 5432 observed
during setup. The app listens only on localhost in the local profile. Sixteen fictional books
are seeded only when catalog/categories are empty. Production has no fixture seeding or
shared administrator password; use HTTPS and explicit database environment variables.

For a packaged run, prepare the environment and database in the same PowerShell process:

```powershell
.\Run-Local.ps1 -PrepareOnly
.\mvnw.cmd package -DskipTests
java -jar .\target\bookstore-0.0.1-SNAPSHOT.jar
```

The current review process is already running locally; do not start a second instance on
the same port. Its process ID is in ignored target/local-app.pid. Stop that specific review
process with `Stop-Process -Id ([int](Get-Content .\target\local-app.pid))` when finished.
For foreground runs use Ctrl+C. `docker compose stop db` preserves the local volume.

## Automated checks

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd -Pbrowser-tests test-compile exec:java '-Dexec.mainClass=com.microsoft.playwright.CLI' '-Dexec.classpathScope=test' '-Dexec.args=install chromium'
.\mvnw.cmd -Pbrowser-tests verify
.\mvnw.cmd -Pperformance-tests verify
```

Unit tests run with Surefire. Failsafe runs real PostgreSQL Testcontainers integrations;
unavailable Docker is a failure, not a silent skip. Browser/profile tests start an isolated
application and database automatically, with no dependence on the local review process or
Compose customer data. Chromium must be installed once before browser or performance checks.
Screenshots on failure and successful viewport captures go to target/screenshots/. Detailed
reports are in target/surefire-reports/ and target/failsafe-reports/. The permanent measured
performance sample is linked from [validation.md](validation.md).

## Acceptance walkthrough

1. Search a known title/author with varied case and surrounding spaces; combine a category,
   clear filters, paginate, and view empty/missing/stock-zero states (FR-001-003).
2. Add two books, repeat an add, edit and remove quantities; reload and use another session.
   Reject zero, negative, fractional, overflow and above-stock quantities (FR-004-005).
3. Submit invalid then valid delivery details. Verify retained inputs, COD, zero delivery fee,
   exact totals and a session-private confirmation. Retry submission without creating another
   order or clearing newer cart changes (FR-006-008).
4. Change price/stock after review, verify conflict and explicit renewed review. Run concurrent
   final-copy/same-key requests and multi-line rollback checks against real PostgreSQL.
5. Try every admin route anonymously and with a non-admin role, and omit CSRF on mutations.
   Log in, edit/archive books, adjust inventory, manage categories and process/cancel orders.
   Retry cancellation and verify stock restoration occurs once (FR-009-011).
6. Use keyboard navigation and 360/768/1440px viewports for shopping pages. Check labels,
   focus, error summaries, long text, missing cover fallback and no horizontal overflow (FR-012).

## Measured and human checks

Performance uses 1,000 fictional books and 20 isolated browser contexts. Each context warms
up once; scheduled navigations then start near-simultaneously on loopback. Samples use browser
navigation DOMContentLoaded and first-contentful-paint timings, not only HTTP response time.
The verified run measured p95 388 ms against the 2,000 ms target. This is local evidence,
not a production-network SLA. See [performance-results.json](performance-results.json).

SC-001/002 require five actual participants. Follow [usability-results.md](usability-results.md);
T049 stays unchecked until observations exist. Automated journeys do not satisfy that study.

Session inactivity expiry is 30 minutes. A lost session loses the cart and guest receipt
access; retain the order reference to contact the store. One application instance is supported.
Define a customer-data retention/deletion policy before real sales. No hosting deployment
has been performed by this implementation run.
