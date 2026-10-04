# Table specification

The detailed nullability, primary keys, unique constraints, indexes, defaults
and foreign keys are declared in the corresponding SQL files under
`database/sql/`. This catalog records ownership and key business semantics.
The full schema is proposed only; it is not deployed.

| Table | Primary key | Required references / unique rules | Status or important semantics |
|---|---|---|---|
| `users` | `id` | unique `username`, `email`, `phone` | Existing Java account states; `password_hash` only |
| `roles` | `id` | unique `name` | Existing authorization role |
| `permissions` | `id` | unique `name` | Existing permission key |
| `user_roles` | `id` | both FKs; pair unique | User-role link |
| `role_permissions` | `id` | both FKs; pair unique | Role-permission link |
| `sessions` | `id` | FK user; unique `token_hash` | Session token hash, expiry and request metadata |
| `password_reset_tokens` | `id` | FK user; unique `token_hash` | One-time hashed reset token and use time |
| `verification_codes` | `id` | FK user; indexed HMAC destination hash | OTP BCrypt hash, channel/purpose, expiry, attempts and one-time use |
| `user_addresses` | `id` | FK user | Existing customer address; `is_default` |
| `categories` | `id` | optional self-FK; unique `slug` | Parent category and lifecycle status |
| `products` | `id` | FK category; unique `sku` | Fixed-precision price and currency |
| `stores` | `id` | FK owner user | Physical store address/status for pickup |
| `store_registration_requests` | `id` | FK owner user | Existing pending/reviewed owner-registration record |
| `inventory` | `id` | FKs store/product; unique pair | On-hand and reserved integer quantities |
| `carts` | `id` | FKs user/store | Store-scoped cart lifecycle |
| `cart_items` | `id` | FKs cart/product; unique pair | Quantity and price/currency snapshot |
| `orders` | `id` | FKs customer/store; unique `order_number` | Status, subtotal/total, currency, cancellation fields |
| `order_items` | `id` | FKs order/product | Immutable product label/SKU/price and line total |
| `order_status_history` | `id` | FK order; optional actor user | Previous/new status and timestamp |
| `pickup` | `id` | FK order; unique order; optional verifier | Pickup code hash, expiry and verification time |
| `payments` | `id` | FK order | Amount/currency/provider/status/reference |
| `notifications` | `id` | FK recipient user | Type, content, optional JSON data/read time |
| `chat_conversations` | `id` | FK store/creator; optional order | Customer-to-store conversation |
| `chat_participants` | `id` | FKs conversation/user; unique pair | Participant role and membership dates |
| `chat_messages` | `id` | FKs conversation/sender | Message body and creation timestamp |
| `audit_logs` | `id` | optional FK actor user | Action/entity metadata and timestamp |

No `ai_conversations` or `ai_messages` are specified because no current
application component requires them. No shipping/delivery tables are
specified.

## Enum-like values owned by application services

- `orders.status`: `PENDING`, `CONFIRMED`, `PREPARING`, `READY_FOR_PICKUP`,
  `COMPLETED`, `CANCELLED`. Java `OrderService` validates transitions.
- `store_registration_requests.status`: current code uses `PENDING` and the
  manual approval SQL uses `APPROVED`. Other review outcomes are not defined
  until an application workflow requires them.
- Other product/store/cart/payment/notification/chat statuses are `VARCHAR`
  until service requirements define their exact allowed values.

## Storage decisions

- IDs: `BIGINT AUTO_INCREMENT` on every table, matching current JPA `IDENTITY`
  mappings. Junction tables also enforce uniqueness on their FK pairs.
- Timestamps: `DATETIME(6)` in UTC; mutable tables have `updated_at`.
- Monetary values: `DECIMAL(19,4)` with a `CHAR(3)` currency code. No `FLOAT`
  or `DOUBLE` for money.
- JSON is reserved for optional notification/audit metadata, not core
  relational fields.
- Credentials and one-time codes are hashes only. OTP destination hashes use
  HMAC-SHA-256 with an environment-provided secret.
