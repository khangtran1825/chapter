# Feature Specification: Chapter Bookstore MVP

**Feature Branch**: Not created; no Git extension configured.

**Created**: 2026-11-13

**Status**: Draft

**Input**: User requested a bookstore website in the chapter project and execution of
Spec Kit specification. The detailed MVP scope below is a proposed working assumption.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Discover Books (Priority: P1)
Visitors browse available books, search by title or author, filter by category and inspect
a book's cover, description, author, price and availability.

**Why this priority**: Visitors need to find and evaluate books before purchasing.

**Independent Test**: With a prepared catalog, find a known book and inspect its details.

**Acceptance Scenarios**:

1. **Given** a catalog of books, **When** a visitor opens the storefront, **Then** listed books
   show title, author, cover, price in VND and availability, with navigation between result pages.
2. **Given** known titles and authors, **When** a visitor searches with different letter cases
   or extra surrounding spaces, **Then** matching books are shown.
3. **Given** a category filter and search, **When** both are selected, **Then** results satisfy
   both and the visitor can clear either selection.
4. **Given** a visible book, **When** its details are opened, **Then** its description and
   purchase information are displayed; unavailable books cannot be added to the cart.
5. **Given** no matching books, **When** results appear, **Then** an empty state explains how
   to clear the search or filter.

### User Story 2 - Manage a Shopping Cart (Priority: P2)
Visitors prepare a purchase by adding, updating and removing books in a cart.

**Why this priority**: A reviewable cart lets shoppers control what they will purchase.

**Independent Test**: With seeded books, add two different books, change quantities and remove one.

**Acceptance Scenarios**:

1. **Given** an available book, **When** it is added twice, **Then** one cart line shows quantity
   two and its subtotal; quantities cannot exceed current stock.
2. **Given** a nonempty cart, **When** quantities change or a line is removed, **Then** all
   subtotals and the total are recalculated accurately.
3. **Given** a cart, **When** the page reloads in the same browser, **Then** the cart remains;
   changed availability is shown before purchase.
4. **Given** an empty cart, **When** viewed, **Then** checkout is unavailable and a browse action
   is presented.

### User Story 3 - Place a COD Order (Priority: P2)
A shopper provides delivery details, reviews the final total and places an order paid on delivery.

**Why this priority**: Ordering completes the primary purchase journey.

**Independent Test**: With a prepared cart, submit valid delivery details and verify the resulting
order and stock; repeat with invalid details and insufficient stock.

**Acceptance Scenarios**:

1. **Given** a valid cart, **When** checkout opens, **Then** name, phone number and delivery
   address are requested, and books, quantities, subtotal, delivery charge and total are shown.
2. **Given** missing required details or an invalid phone number, **When** submitted, **Then**
   the order is rejected with field-specific messages and valid entries remain.
3. **Given** valid delivery details and available stock, **When** confirmed, **Then** exactly
   one order is created with a reference, purchased prices and Pending status; stock is reduced
   exactly once and the cart clears only after success.
4. **Given** changed stock or price, **When** checkout is submitted, **Then** no stale order is
   accepted and the shopper reviews updated values before confirming again.
5. **Given** a repeated confirmation of the same purchase, **When** submitted again, **Then**
   the existing outcome is returned without creating a second order.

### User Story 4 - Administer the Store (Priority: P3)
An administrator manages books and categories, adjusts inventory and processes orders.

**Why this priority**: Store staff need to keep listings accurate and fulfil purchases.

**Independent Test**: With an authorized administrator and prepared orders, change a listing,
update inventory and move an order through permitted states; repeat without authorization.

**Acceptance Scenarios**:

1. **Given** an unauthenticated visitor, **When** any administrative read or write is attempted,
   **Then** access is denied; valid administrator sign-in permits access and sign-out revokes it.
2. **Given** an administrator, **When** books or categories are created or edited, **Then**
   valid changes appear in the storefront; missing titles, negative prices or noninteger/negative
   stock are rejected. A category in use cannot be deleted until books are reassigned.
3. **Given** a purchased book, **When** its listing is archived or repriced, **Then** it is hidden
   from new purchases while existing order descriptions and purchased prices stay unchanged.
4. **Given** a Pending order, **When** processed, **Then** staff can move it through Confirmed,
   Shipping and Completed in that order, or cancel it before Shipping.
5. **Given** an order cancelled before Shipping, **When** cancellation is applied or retried,
   **Then** its stock is restored exactly once; Completed and Cancelled orders cannot transition.

### Edge Cases

- Two shoppers attempt the last copy: at most one order succeeds and stock never becomes negative.
- A book is archived while in a cart: checkout explains its removal and requires renewed review.
- Checkout fails or connectivity is lost: cart and valid delivery inputs remain; a safe retry
  cannot create duplicate orders.
- Long titles, missing cover images and unknown book links receive readable fallbacks.
- Unexpected checkout failures show an actionable message without exposing customer data.
- Private confirmations and order details are not exposed through guessable references.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Visitors MUST browse a paginated catalog and view book details (Story 1.1, 1.4).
- **FR-002**: Visitors MUST search title/author and combine category filtering (Story 1.2, 1.3).
- **FR-003**: The storefront MUST identify empty results and unavailable books (Story 1.4, 1.5).
- **FR-004**: Visitors MUST add, update and remove cart lines with positive integer quantities
  bounded by stock; the cart MUST survive same-browser reloads (Story 2.1-2.3).
- **FR-005**: Cart and checkout MUST show accurate line totals and final purchase total;
  empty carts MUST NOT be submitted (Story 2.2, 2.4, 3.1).
- **FR-006**: Checkout MUST validate recipient name, phone and delivery address and preserve
  valid inputs on rejection (Story 3.1, 3.2).
- **FR-007**: Checkout MUST revalidate stock and prices, prevent duplicate orders, record
  immutable purchased line details and deduct stock only for successful orders (Story 3.3-3.5).
- **FR-008**: Successful checkout MUST show an order reference and clear the cart (Story 3.3).
- **FR-009**: Administrative reads and writes MUST require authenticated administrator access,
  with sign-in and sign-out supported (Story 4.1).
- **FR-010**: Administrators MUST manage books, categories and inventory with validation;
  archiving MUST preserve historical orders (Story 4.2, 4.3).
- **FR-011**: Administrators MUST list and inspect orders, follow the defined status transitions
  and restore stock once on permitted cancellation (Story 4.4, 4.5).
- **FR-012**: Catalog, cart and checkout MUST support keyboard operation, labeled inputs and
  widths from 360 to 1440 pixels without horizontal overflow (all shopper scenarios).

### Key Entities *(include if feature involves data)*

- **Book**: Title, author, description, cover, category, VND price, stock and active/archive state.
- **Category**: Named grouping used for discovery; referenced categories cannot be deleted.
- **Cart / Cart Line**: Browser-specific intended purchase with books and quantities.
- **Order / Order Line**: Reference, recipient contact/address, status, purchased descriptions,
  unit prices, quantities, delivery charge and total; historical purchase data is preserved.
- **Administrator**: Authorized staff identity with store management permissions.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In a five-person usability check, at least four participants locate a named book
  and its price within 60 seconds without assistance.
- **SC-002**: At least four of five participants complete a valid prepared-cart checkout within
  three minutes without assistance.
- **SC-003**: All acceptance checks for totals, invalid inputs, insufficient stock, repeated
  submission and cancellation pass; no negative stock or duplicate orders occur.
- **SC-004**: All unauthorized administrative access checks are denied; no customer delivery
  information appears in publicly accessible pages or error messages.
- **SC-005**: At mobile and desktop widths, all shopper scenarios are completable by keyboard
  with no horizontal overflow. With 1,000 catalog books and 20 simultaneous shoppers in the
  agreed test environment, 95% of catalog/search interactions display results within two seconds.

## Assumptions

- Proposed scope for review: one store, physical books, Vietnamese UI and VND prices.
- Guest checkout is supported; customer accounts, reviews, discounts, online payments,
  multi-seller features and carrier integrations are outside this first version.
- Delivery charge is zero in the MVP and is explicitly displayed before confirmation.
- Delivery requires recipient name, a 10-digit Vietnamese phone number starting with 0, and
  a nonempty street/ward/district/province address. No external address verification is required.
- Orders are handled manually by authenticated staff. A privately issued confirmation is the
  guest's receipt; public order lookup and customer order history are outside this version.
- Product descriptions, covers, prices and initial stock must be supplied by store staff;
  development demonstrations use clearly identified fictional catalog data.
- Performance targets are provisional and will be validated against a documented environment
  during implementation. No technology stack is mandated by this specification.
