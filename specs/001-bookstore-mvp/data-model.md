# Data Model: Chapter Bookstore MVP

## Persistent entities

All identifiers are UUIDs unless stated otherwise. Money uses integer VND amounts stored as
BIGINT and checked arithmetic in Java; negative amounts and overflow are rejected. Instants
are UTC; customer-facing dates use Asia/Ho_Chi_Minh. Text is trimmed and length-limited.

| Entity | Fields and rules | Relationships |
| --- | --- | --- |
| Category | id; name (1-100 chars, unique ignoring case); createdAt | One-to-many books; deletion restricted while referenced |
| Book | id; title (1-255); author (1-255); description (1-10000); coverPath (optional application-local image); priceVnd >= 0; stock integer >= 0; active; version; timestamps | Required category; referenced by order lines |
| AdminUser | id; normalized username (3-100, unique); passwordHash; enabled; role ADMIN | No public registration |
| CustomerOrder | id; reference (random unique); checkoutKey (unique); requestFingerprint; ownerTokenHash; recipientName (1-100); phone (0 followed by 9 digits); street (1-255); ward/district/province (1-100 each); subtotalVnd; deliveryVnd=0; totalVnd; status; timestamps; version | One-to-many immutable order lines |
| OrderLine | id; orderId; bookId; titleSnapshot; authorSnapshot; unitPriceVnd >= 0; quantity > 0; lineTotalVnd | Referenced book is archived, never hard-deleted |

Foreign keys, not-null fields, unique keys and nonnegative stock/price constraints are enforced
in migrations. Orders require at least one line. subtotal equals sum of line totals; total equals
subtotal plus delivery; services enforce cross-row invariants in one transaction.
Inventory counts are limited to Java int range; reject overflowing quantity additions.

## Session and form objects

- Cart: map of book ID to quantity, maximum 100 distinct lines. Cookie identifies a server
  session; no customer PII or authoritative prices are stored in the cookie. Survives reloads
  for the lifetime of that session (default 30-minute inactivity timeout); no account sync.
- Checkout quote: random single-purchase key, session owner secret, immutable cart quantity
  snapshot and reviewed prices. One active quote per checkout; mutations invalidate it.
- DeliveryForm: recipient name, phone and structured address; preserved on validation failure.
- Confirmation ownership: session retains high-entropy receipt secret; database stores its hash.
  A human-readable order reference alone grants no access. Losing the session loses guest access.

## Checkout transaction

1. Validate CSRF, quote/session ownership, field limits and input formats. Freeze a submitted
   request snapshot; fingerprint canonical book IDs, quantities, reviewed prices and delivery data.
2. Serialize submissions for the same checkout key using a PostgreSQL transaction-scoped
   advisory lock. Check for an existing order after obtaining the lock. Same owner/key/payload
   returns the original order; a changed payload with the same key returns a conflict.
3. Lock referenced book rows pessimistically in sorted ID order. Recheck active flags,
   stock and prices against the quote. Any mismatch rolls back and requires renewed review;
   input and cart remain available. Cart quantities also must match the frozen quote.
4. Write the order and line snapshots and deduct stock in the same transaction. A unique
   checkout-key constraint is the final duplicate guard. Return after successful commit.
5. Clear only the submitted, unchanged session cart after commit. Receipt state must be
   recoverable by a retry using the retained checkout key and owner secret after a lost response.
   An unrelated later cart mutation must not be erased by an earlier checkout response.

Deadlocks, lock timeouts and failures roll back all changes and give a retryable message.
Never use a JVM-only mutex as the correctness boundary. Admin inventory edits participate in
the same row locking/version rules so they cannot overwrite a committed stock deduction.

## Order state transitions

```text
PENDING -> CONFIRMED -> SHIPPING -> COMPLETED
PENDING -> CANCELLED
CONFIRMED -> CANCELLED
```

COMPLETED and CANCELLED are terminal; repeated requests for the same current state are no-ops.
Other transitions are rejected. Lock the order then all affected books in sorted order;
cancel and replenish stock atomically. A retry must not restore stock twice. Status updates
never modify purchased line descriptions or prices. Checkout never locks existing orders,
avoiding an order/book lock cycle with cancellations.

## Privacy and migration notes

No public order listing, customer accounts or upload tables. Session cookies are HttpOnly,
SameSite=Lax and Secure under HTTPS. Password hashes use Spring Security's adaptive encoder;
bootstrap admin credentials come from environment variables, with no shared production default.
Flyway creates the schema; JPA validates it. Demo fixtures run only in the local profile and
contain fictional data. No secrets, recipient forms or private tokens are written to logs.
Initial MVP has no customer-data export or analytics; a production retention/deletion policy
must be defined before handling real customer records.
