# Database architecture

## Status and source of truth

The existing-table DDL in `database/sql/` has been reconciled to the local
TiDB schema exports except for the chat tables, which are intentionally left
unchanged. The exports have not been verified against the currently deployed
TiDB database, and no SQL change has been applied to TiDB. Confirm the live
schema and take a backup before applying the additive staff migration.

The intended production path is:

```text
PowerDesigner MCD → MLD → MPD → reviewed SQL → manually provisioned TiDB Cloud
```

Native PowerDesigner files are not present. The numbered
`database/powerdesigner/` folders are placeholders only. Reverse-engineer the
reviewed DDL into an MPD, derive the MLD/MCD, and save actual PowerDesigner
model files there; the SQL and text specifications do not themselves constitute
a native PowerDesigner model. SQL types use common relational types suitable
for mapping into PowerDesigner domains.

Spring Boot uses only the configured MySQL-compatible TiDB datasource.
`spring.jpa.hibernate.ddl-auto=validate` prevents Hibernate from creating or
updating schema. Flyway is disabled in the application; it cannot apply the
legacy migrations at startup.

## Module ownership

| Module | Tables | Responsibility |
|---|---|---|
| Auth | `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `sessions`, `password_reset_tokens`, `verification_codes` | Identity credentials, role/permission links, sessions and hashed one-time credentials |
| User | `user_addresses` | Customer pickup/contact addresses |
| Product | `products` | Product catalog entries, SKU, price and lifecycle status |
| Category | `categories` | Product classification; the current table has no parent-category relation |
| Store | `stores`, `store_registration_requests` | Physical pickup locations; owner accounts are provisioned internally, then submit store drafts for manual review |
| Inventory | `inventory` | Per-store product quantity and reservation count |
| Cart | `carts`, `cart_items` | Customer cart scoped to a physical store |
| Order | `orders`, `order_items`, `order_status_history` | Click-and-collect order, immutable item/price snapshots and status audit trail |
| Pickup | `pickup` | Single pickup-verification record for an order; stores only a code hash |
| Payment | `payments` | Payment attempts and provider references for an order |
| Notification | `notifications` | User-facing notification records |
| Chat | `chat_conversations`, `chat_participants`, `chat_messages` | Existing customer-to-store communication schema; no chat server/API work is in this scope |
| Audit | `audit_logs` | Actor/action/entity audit records |
| Store staff | `store_staff`, `store_staff_permissions` | Store-scoped staff memberships and owner-granted permissions (additive manual migration) |
| AI | No tables | Reserved module; omitted until the application has an AI persistence requirement |

There are 26 existing tables in the local exports and two new store-staff
tables in the additive migration. No delivery, shipping, courier, or tracking
tables are included. AI chat is not mixed with customer-to-store chat.
`store_registration_requests` is retained as an existing table in the schema
export but is not used for public owner registration. Owner accounts are
provisioned through an internal process; a logged-in owner creates a `DRAFT`
store and explicitly submits it as `PENDING_REVIEW`. An administrator changes
the store to `ACTIVE` after review. Only `ACTIVE` stores are public.

## Cross-cutting data rules

- Existing primary keys and uniqueness constraints are preserved from the
  TiDB exports. The new staff tables use signed `BIGINT AUTO_INCREMENT`
  surrogate keys and enforce unique membership/permission pairs.
- All database identifiers use snake_case. Entity and table identifiers are
  singular where the existing application already uses that table name;
  otherwise plural names are consistent with current migrations.
- Money uses `DECIMAL(19,4)` and an explicit ISO-4217 `CHAR(3)` currency code.
  No floating-point money columns are used. VND is expected for the initial
  deployment, but currency selection/scale must be confirmed in the MPD.
- Passwords are represented only by `password_hash`; session, reset, pickup,
  and OTP verification secrets are represented by hashes. Email destination
  identifiers use keyed HMAC; `OTP_HASH_SECRET` can override its key, otherwise
  the application derives one from the configured SMTP password. No credentials,
  OTP codes, or sample accounts are in SQL.
- New lifecycle/status values are `VARCHAR`, not database enums. Java services
  own allowed transitions; the order service must record every state change in
  `order_status_history`.
- Store onboarding uses the existing `stores.status` column (`DRAFT` →
  `PENDING_REVIEW` → `ACTIVE`; administrators may keep a store `SUSPENDED`).
  The service explicitly writes `DRAFT`; the exported SQL default remains
  `PENDING` for compatibility with the observed schema.
- `created_at` and `updated_at` are microsecond `DATETIME(6)`. Immutable
  records omit `updated_at`. Existing JPA callbacks/services remain responsible
  for timestamps on mapped columns.
- Existing FK delete behavior is preserved from the exports. Review TiDB
  foreign-key enforcement/version and backup/restore procedure before migration.

## Design flow

Customer places an order against a physical store; the exported order tables
contain price/name/SKU snapshots, status history, and a single pickup record.
Inventory is per store/product. The schema does not implement or enforce the
order state machine: Java validates transitions and cancellation policy.

The intended order status vocabulary is `PENDING`, `CONFIRMED`, `PREPARING`,
`READY_FOR_PICKUP`, `COMPLETED`, and controlled terminal `CANCELLED`.
The existing schema stores one status plus a note in each history row;
previous/new status columns and cancellation-actor columns are not present.
Cancellation authorization, eligible prior states, inventory release, payment
reversal and notifications remain application rules.

## Database creation

Use the reconciled module DDL and `database/sql/tidb_full_schema.sql` as a
review/reverse-engineering baseline only. The combined file is a one-time empty
schema bootstrap and includes the new staff tables; never run it against an
existing database. Apply
`05-store/002_store_staff_membership_and_permissions.sql` once, manually, only
after confirming the live schema, backup, and migration target. It creates
tables only and does not update existing data.

The SQL files are not loaded as Spring resources and are not applied at build
or startup. Do not apply the legacy Flyway migrations as well as the proposed
schema: both define the same auth tables.

The combined schema is not a deployment script, is not loaded by Spring, and
must not be run over an existing database. Historical auth/OTP SQL remains
separate; no Flyway script is enabled at runtime.
