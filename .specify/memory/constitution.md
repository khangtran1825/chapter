# Chapter Constitution

## Core Principles

### I. Maintainable and Scoped Changes
Changes MUST implement a documented user need. Responsibilities MUST be separated clearly,
and existing project conventions MUST be reused where present. New dependencies MUST have
a stated purpose. Prefer the smallest implementation that satisfies acceptance criteria.

### II. Correct Orders and Inventory
Prices, quantities, totals and available stock MUST be validated by the authoritative system
before an order is accepted. An accepted order MUST preserve its purchased prices and items.
Repeated submission MUST NOT create duplicate orders or deduct stock twice.

### III. Customer Privacy and Access Control
Administrative actions MUST require authenticated administrator access. Customers MUST NOT
access other customers' orders. Secrets MUST stay outside source control, passwords MUST NOT
be stored in plaintext, and logs MUST omit passwords and customer contact/address details.

### IV. Verification of Business Rules
Changes to checkout, inventory, totals or administrative permissions MUST include meaningful
automated verification of successful and rejected operations. Each completed feature MUST
record the checks performed and any remaining limitations. Never report unrun checks as passed.

### V. Usable and Accessible Storefront
Browsing, cart and checkout MUST work at mobile and desktop widths without horizontal overflow.
Controls MUST have accessible names and support keyboard operation. Empty, loading and error
states MUST explain what the user can do next; validation MUST preserve valid entered values.

## Product Constraints
The initial product is a single-store physical-book shop. Product scope assumptions belong
in the feature specification and may be revised explicitly. Technology choices belong in
the implementation plan. Real credentials and customer data MUST NOT be used as demo fixtures.

## Development Workflow
Define observable acceptance criteria before implementation. Produce the specification,
implementation plan and actionable tasks in that order. Review changed behavior against the
specification and these principles before reporting completion. Document setup and validation
commands so another developer can reproduce them. Publication requires a user instruction.

## Governance
This constitution governs project plans and implementation. Amendments MUST state the reason,
affected principles and impact on existing work. Use major versions for incompatible principle
changes, minor versions for added obligations and patch versions for wording clarifications.
Each plan and final verification MUST review compliance and explain any proposed exception.
The user may revise these initial working principles at any time.

**Version**: 1.0.0 | **Ratified**: 2026-11-13 | **Last Amended**: 2026-11-13
