# HTTP and Page Contracts

Server-rendered HTML, same-origin forms, UTF-8 Vietnamese UI. Successful mutations use
POST/Redirect/GET (303). All mutation forms, including login/logout, carry CSRF tokens.
No GET route changes state. Invalid forms render errors with retained valid fields (422);
unauthorized admin navigation redirects to login and forbidden authenticated access returns 403.
Missing resources return 404. Conflicts return 409 with an actionable explanation.

## Public storefront

| Method/path | Input | Outcome |
| --- | --- | --- |
| GET / | None | Redirect to /books |
| GET /books | q <= 200 chars, optional category UUID, page >= 0 | Active books; 12/page ordered title then ID; title/author case-insensitive literal substring search; combined filters |
| GET /books/{id} | Book UUID | Active book details or 404; stock=0 remains visible with purchase disabled |
| GET /cart | Session | Lines, current price/stock warnings, subtotal and browse/checkout action |
| POST /cart/items | bookId, quantity integer 1..available | Add to existing quantity, enforcing stock and 100 distinct lines; redirect to cart |
| POST /cart/items/{bookId}/quantity | quantity integer >= 1 | Replace quantity within available stock |
| POST /cart/items/{bookId}/remove | None | Remove line idempotently; redirect to cart |
| GET /checkout | Nonempty session cart | Review authoritative quote and delivery fields; issue server-bound purchase key |
| POST /checkout | purchaseKey, recipientName, phone, street, ward, district, province | Validate immutable quote and delivery fields; atomic order/stock commit; redirect to owned receipt |
| GET /orders/{reference}/confirmation | Receipt owner session | Private order summary; 404 for missing/wrong owner |

Trim search and form input. Unknown category/book IDs yield a useful empty state or 404.
Malformed IDs, pagination and oversized inputs return 400 rather than server errors.
Queries treat '%' and '_' literally. Default search is case-insensitive but accent-sensitive.
checkoutKey and owner secrets are generated server-side, never accepted as authority alone.
Totals, prices, roles and order status supplied by public forms are never trusted.
Private pages use Cache-Control: no-store. Redact PII and tokens in request/error logs.
When a stale quote is rejected, show changed items and require a new explicit confirmation.
The old key cannot be reused to create an order with changed contents.

## Administration

Every /admin route requires ADMIN, including reads. Login is rate-limited by a bounded
per-origin/username failure counter with a short lockout; errors do not reveal account existence.
Login rotates the session identifier, preserves cart, and logout invalidates the session.

| Method/path | Purpose |
| --- | --- |
| GET /admin/login; POST /admin/login | Sign-in form and authentication |
| POST /admin/logout | Invalidate authenticated session |
| GET /admin/books | Paginated active/archived catalog management |
| GET /admin/books/new; POST /admin/books | Create validated book |
| GET /admin/books/{id}/edit; POST /admin/books/{id} | Update metadata/price/category using expected version |
| POST /admin/books/{id}/inventory | Set nonnegative integer stock with expected version; conflict on concurrent change |
| POST /admin/books/{id}/archive | Hide listing, preserve historical references |
| POST /admin/books/{id}/activate | Republish valid listing |
| GET /admin/categories; POST /admin/categories | List/create categories |
| POST /admin/categories/{id} | Rename category |
| POST /admin/categories/{id}/delete | Delete only when no books reference it |
| GET /admin/orders | Paginated orders; optional known status filter; latest first |
| GET /admin/orders/{id} | Recipient, immutable purchase lines and current status |
| POST /admin/orders/{id}/status | Expected current status and target; enforce state machine atomically |

Book forms include title, author, description, coverPath, priceVnd, categoryId and version
when editing. Cover paths are application-local, with fallback artwork; no arbitrary HTML,
remote file fetch or file upload. Templates escape all supplied content.
Concurrent edits return 409 without overwriting changes; validation errors preserve input.
Status and cancellation behavior follows [data-model.md](../data-model.md).

## UI acceptance contract

All pages provide a shared navigation, descriptive title, one main heading and visible focus.
Catalog uses cover/title/author/price/availability cards; detail has an explicit add control.
Cart uses labeled quantity/remove controls and total summary. Checkout labels all fields,
shows inline errors plus an error summary, and clearly identifies COD and zero delivery fee.
Empty/error pages provide recovery navigation. Use responsive layouts at 360, 768 and 1440px;
long Vietnamese text wraps without obscuring controls. Validation and errors never rely only
on color. JavaScript enhancement is optional for normal catalog/cart/checkout navigation.
