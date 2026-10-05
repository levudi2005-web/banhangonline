# Entity-relationship specification

This is a textual MCD specification for recreating/reviewing the model in
PowerDesigner. It does not represent an exported PowerDesigner model file.
Unless marked optional, child-to-parent relationships are mandatory.

## Accounts and access

- `users` 1 — 0..N `user_addresses`
- `users` 1 — 0..N `sessions`
- `users` 1 — 0..N `password_reset_tokens`
- `users` 1 — 0..N `verification_codes`
- `users` N — M `roles` through `user_roles`
- `roles` N — M `permissions` through `role_permissions`
- `users` 1 — 0..N `store_registration_requests` as legacy applicant/owner
- `users` 1 — 0..N `stores` as owner
- `stores` 1 — 0..N `store_staff`
- `users` 1 — 0..N `store_staff`
- `store_staff` N — M `permissions` through `store_staff_permissions`
- `users` 1 — 0..N `store_staff_permissions` as permission grantor

## Catalog and stock

- `categories` 1 — 0..N `products`
- `stores` 1 — 0..N `inventory`
- `products` 1 — 0..N `inventory`
- `stores` 1 — 0..N `carts`
- `users` 1 — 0..N `carts`
- `carts` 1 — 0..N `cart_items`
- `products` 1 — 0..N `cart_items`

## Click-and-collect order

- `users` 1 — 0..N `orders` as customer
- `stores` 1 — 0..N `orders` as pickup/preparation location
- `orders` 1 — 1..N `order_items`
- `products` 1 — 0..N `order_items`; item name/SKU/price are snapshots
- `orders` 1 — 1..N `order_status_history`
- `users` 0..1 — 0..N `order_status_history` as actor; null means system
- `orders` 1 — 0..1 `pickup`
- `users` 0..1 — 0..N `pickup` as verifier
- `orders` 1 — 0..1 `payments`
- `users` 1 — 0..N `notifications`

The deployed schema records a single status and note per history row;
`OrderService`, not SQL, validates allowed transitions and cancellation policy.

## Store chat and audit

- `stores` 1 — 0..N `chat_conversations`
- `chat_conversations` 1 — 1..N `chat_participants`
- `users` 1 — 0..N `chat_participants`
- `chat_conversations` 1 — 0..N `chat_messages`
- `users` 1 — 0..N `chat_messages` as sender
- `users` 0..1 — 0..N `audit_logs`; null permits system events

Customer-to-AI conversations/messages are deliberately not included. Add
separate AI-owned persistence only when a real AI service requires it; do not
make AI impersonate store participants.

## MLD-to-MPD notes

Existing primary keys, constraints, and attributes follow the local TiDB
schema exports except for the intentionally unchanged chat model. The additive
staff model uses `BIGINT`, `VARCHAR`, and
`DATETIME(6)` attributes, with unique pairs and named foreign keys. Verify all
exports against the live database before deriving deployment SQL. See
`table-specification.md` and `foreign-key-dependencies.md` for table details.
