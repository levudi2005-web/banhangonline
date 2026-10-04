# Entity-relationship specification

This is a textual MCD specification for recreating/reviewing the model in
PowerDesigner. It does not represent an exported PowerDesigner model file.
Unless marked optional, child-to-parent relationships are mandatory.

## Accounts and access

- `users` 1 — 0..N `user_addresses`
- `users` 1 — 0..N `sessions`
- `users` 1 — 0..N `password_reset_tokens`
- `users` N — M `roles` through `user_roles`
- `roles` N — M `permissions` through `role_permissions`
- `users` 1 — 0..N `store_registration_requests` as applicant/owner
- `users` 1 — 0..N `stores` as owner

## Catalog and stock

- `categories` 1 — 0..N `products`
- `categories` 1 — 0..N `categories` as parent/child; parent is optional
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
- `orders` 1 — 0..N `payments`
- `users` 1 — 0..N `notifications`
- `users` 0..1 — 0..N `orders` as cancellation actor

The database records status snapshots/history. `OrderService`, not SQL,
validates allowed status transitions and cancellation policy.

## Store chat and audit

- `stores` 1 — 0..N `chat_conversations`
- `orders` 0..1 — 0..N `chat_conversations`; a conversation may concern an order
- `users` 1 — 0..N `chat_conversations` as creator
- `chat_conversations` 1 — 1..N `chat_participants`
- `users` 1 — 0..N `chat_participants`
- `chat_conversations` 1 — 0..N `chat_messages`
- `users` 1 — 0..N `chat_messages` as sender
- `users` 0..1 — 0..N `audit_logs`; null permits system events

Customer-to-AI conversations/messages are deliberately not included. Add
separate AI-owned persistence only when a real AI service requires it; do not
make AI impersonate store participants.

## MLD-to-MPD notes

Each table, including N:M junctions, has a `BIGINT AUTO_INCREMENT` surrogate
primary key; each junction also has a unique constraint on its FK pair. All
relationships become named foreign keys
with explicit supporting indexes. Monetary attributes use fixed-precision
decimal plus currency code; status attributes remain service-validated text.
See `table-specification.md` and `foreign-key-dependencies.md` for the physical
table and creation details.
