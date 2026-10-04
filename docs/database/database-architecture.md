# Database architecture

## Status and source of truth

This is a proposed schema framework, not a deployed schema. No database was
contacted and no SQL in `database/sql/` has been executed.

The intended production path is:

```text
PowerDesigner MCD → MLD → MPD → reviewed SQL → manually provisioned TiDB Cloud
```

Native PowerDesigner files are not present. The numbered
`database/powerdesigner/` folders are placeholders only. The DDL is a
recreatable MPD/TiDB baseline to review and reconcile with the future
PowerDesigner export before anyone applies it.

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
| Category | `categories` | Hierarchical product classification |
| Store | `stores`, `store_registration_requests` | Physical pickup locations and the existing manual owner-approval workflow |
| Inventory | `inventory` | Per-store product quantity and reservation count |
| Cart | `carts`, `cart_items` | Customer cart scoped to a physical store |
| Order | `orders`, `order_items`, `order_status_history` | Click-and-collect order, immutable item/price snapshots and status audit trail |
| Pickup | `pickup` | Single pickup-verification record for an order; stores only a code hash |
| Payment | `payments` | Payment attempts and provider references for an order |
| Notification | `notifications` | User-facing notification records |
| Chat | `chat_conversations`, `chat_participants`, `chat_messages` | Customer-to-store communication |
| Audit | `audit_logs` | Actor/action/entity audit records |
| AI | No tables | Reserved module; omitted until the application has an AI persistence requirement |

There are 26 proposed tables, including the existing store-registration
workflow table and the OTP verification table. No delivery, shipping, courier, or tracking tables are
included. AI chat is not mixed with customer-to-store chat.

## Cross-cutting data rules

- Every table uses a signed `BIGINT AUTO_INCREMENT` surrogate primary key,
  including relationship tables. N:M relationships additionally have a unique
  constraint on the FK pair. This is compatible with TiDB and
  Hibernate/JPA `GenerationType.IDENTITY`; the current JPA-owned join tables
  have no Java entity for that surrogate key.
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
- `created_at` and `updated_at` are microsecond `DATETIME(6)`. Immutable
  records omit `updated_at`. Existing JPA callbacks/services remain responsible
  for timestamps on mapped columns.
- FK delete behavior preserves history: ownership/join rows can cascade where
  appropriate; catalog and completed business records generally restrict
  deletion. Review retention policy and TiDB foreign-key support/version before
  final MPD approval.

## Design flow

Customer places an order against a physical store; the order contains
price/name/SKU snapshots, records an initial status-history row, reserves
inventory through Java business logic, and has one pickup-verification row.
Payment attempts are linked to the order. The schema does not implement or
enforce the state machine: Java validates transitions and cancellation policy.

The order status vocabulary is `PENDING`, `CONFIRMED`, `PREPARING`,
`READY_FOR_PICKUP`, `COMPLETED`, and controlled terminal `CANCELLED`.
Cancellation authorization, eligible prior states, inventory release,
payment reversal and notifications remain application rules.

## Database creation

Use the module DDL and `database/sql/tidb_full_schema.sql` only after
PowerDesigner model review and explicit confirmation that the target database
does not already have these objects. The scripts intentionally contain no
`DROP TABLE` and no `IF NOT EXISTS`; the combined file is a one-time empty
schema bootstrap proposal, not an idempotent migration.

The SQL files are not loaded as Spring resources and are not applied at build
or startup. Do not apply the legacy Flyway migrations as well as the proposed
schema: both define the same auth tables.

For an existing TiDB database, review and manually apply
`01-auth/003_add_user_verification_flags.sql` and
`01-auth/004_create_verification_codes.sql` as a deliberate database change
before deploying the OTP-aware entities. The combined bootstrap schema is for
an empty schema only and must not be run over an existing database.
