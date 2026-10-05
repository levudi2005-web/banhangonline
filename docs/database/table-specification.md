# Table specification

The 26 existing table definitions in `database/sql/` reflect the local TiDB
schema exports except for the intentionally unchanged chat tables, and are not
yet verified against the current remote database. The
two store-staff tables are new and exist only in the additive manual migration
`database/sql/05-store/002_store_staff_membership_and_permissions.sql`.
Consult the DDL for authoritative columns, nullability, indexes, constraints,
and defaults. Do not execute the combined bootstrap on an existing database.

| Table | Primary key | Important references / constraints | Current semantics |
|---|---|---|---|
| `users` | `id` | unique `username`, `email`, `phone` | Credentials stored as `password_hash` |
| `roles` | `id` | unique `name` | Customer, staff, and owner roles |
| `permissions` | `id` | unique `name` | Existing named permission catalog |
| `user_roles` | `id` | user and role FKs | User-role association |
| `role_permissions` | `id` | role and permission FKs | Role-wide permissions |
| `sessions` | `id` | user FK; unique `token_hash` | Session token hashes and expiry |
| `password_reset_tokens` | `id` | user FK; unique `token_hash` | Hashed, single-use reset credentials |
| `verification_codes` | `id` | user FK | Dormant OTP verification data |
| `user_addresses` | `id` | user FK | Customer address/contact records |
| `categories` | `id` | unique `slug` | No parent-category column or self-reference |
| `products` | `id` | category FK; unique SKU and slug | Catalog details, price, currency, image URL |
| `stores` | `id` | owner user FK | Physical store/address/status; default `PENDING` |
| `store_registration_requests` | `id` | owner user FK | Existing schema table; no public owner-registration flow |
| `store_staff` | `id` | store and user FKs; unique `(store_id,user_id)` | Store-specific staff membership/status |
| `store_staff_permissions` | `id` | membership, permission, grantor FKs; unique pair | Owner-granted permission scoped to one membership |
| `inventory` | `id` | store/product FKs; unique pair | Quantity, reserved quantity, reorder level, status |
| `carts` | `id` | user/store FKs; unique user/store/status | Store-scoped cart |
| `cart_items` | `id` | cart/product FKs; unique pair | Quantity and fixed-precision unit-price snapshot |
| `orders` | `id` | user/store FKs; unique `order_code` | `subtotal`, `total_amount`, `note`, status |
| `order_items` | `id` | order/product FKs | Product, SKU, quantity, and price snapshots |
| `order_status_history` | `id` | order FK; optional actor FK | One `status` and optional `note` per event |
| `pickup` | `id` | unique order FK; optional confirmer FK | Hashed pickup code, expiry, pickup timestamp |
| `payments` | `id` | unique order FK; provider/payment uniqueness | One payment row per order in the current schema |
| `notifications` | `id` | user FK | `title`, `message`, polymorphic reference, read flag/time |
| `chat_conversations` | `id` | store FK | Existing chat shell schema; untouched by feature work |
| `chat_participants` | `id` | conversation/user FKs; unique pair | Existing participant membership |
| `chat_messages` | `id` | conversation/sender FKs | Existing message records |
| `audit_logs` | `id` | optional actor FK | Action/entity, JSON details, request metadata |

No AI persistence tables or delivery/shipping tables are specified. Chat/AI
backend work is explicitly out of scope.

## Application-owned values

- Order lifecycle is service-owned: `PENDING` → `CONFIRMED` → `PREPARING` →
  `READY_FOR_PICKUP` → `COMPLETED`; cancellations are validated server-side.
  The existing history schema stores `status` and `note`, not prior/new status
  columns.
- Store onboarding is `DRAFT` → `PENDING_REVIEW` → `ACTIVE`; the admin-only
  activation script changes submitted stores to active. Only active stores are
  visible to customers.
- Owners are provisioned internally; there is no public owner registration.
  A logged-in owner creates a store draft and submits it for review.
- Staff authorization is checked against active membership and that
  membership's permission rows; role-wide grants do not replace ownership
  checks.
- The cart schema's unique `(user_id, store_id, status)` key permits one
  persistent cart per state. Checkout preserves the `ACTIVE` cart and clears
  its items so later orders do not conflict with historical carts.

## Storage and modeling notes

- Preserve existing export types and constraints when importing them into
  PowerDesigner. Common model domains include `BIGINT`, `VARCHAR`, `CHAR`,
  `INT`, `TINYINT`, `DECIMAL`, `DATETIME`, `TEXT`, and `JSON`.
- New staff IDs use `BIGINT AUTO_INCREMENT`; permission grants are unique per
  staff membership and permission.
- Monetary values use `DECIMAL(19,4)` and a `CHAR(3)` currency code.
- Passwords, session tokens, reset tokens, and pickup codes are never stored
  in plaintext. The OTP infrastructure is dormant.
- Validate the exported schema against the live TiDB database before applying
  any migration. The bootstrap DDL is for modeling/recreation only.
