# Java entity and proposed schema alignment

Comparison is against current Java mappings and reviewed SQL definitions, not a
live TiDB database. OTP introduces entity and SQL mappings that require
explicit manual schema changes on an existing TiDB database; no database was
contacted or SQL executed.

| Java entity | Table | Current mapped fields/relations | Proposed SQL comparison |
|---|---|---|---|
| `User` | `users` | `id`, full name, username, email, phone, password hash, status, email verification flag, created/updated times; `user_roles` relation | Email verification requires manual SQL 003 on the existing schema; phone is contact information only |
| `Role` | `roles` | `id`, name, description; `role_permissions` relation | Matches V1 |
| `Permission` | `permissions` | `id`, name, description | Matches V1 |
| `UserAddress` | `user_addresses` | `id`, user, recipient/contact/address fields, default flag and timestamps | Matches V1 columns; FK and index are equivalent |
| `Session` | `sessions` | `id`, token hash, user, expiry/creation, optional IP and user agent | Matches V1 columns and unique token hash |
| `PasswordResetToken` | `password_reset_tokens` | `id`, user, token hash, expiry, used/creation times | Matches V1 columns and unique token hash |
| `VerificationCode` | `verification_codes` | `id`, user, channel, purpose, HMAC destination hash, BCrypt code hash, expiry, attempts, use/invalidation and creation times | Added with manual SQL 004; maps to module and combined DDL |
| `StoreRegistrationRequest` | `store_registration_requests` | `id`, owner, store/contact/address details, status/review and timestamps | Matches V1; request is distinct from a runtime store record |
| `Category` | `categories` | `id`, name, slug, description, status and timestamps | Mapped by JPA |
| `Product` | `products` | `id`, category, SKU, name, slug, description, price, currency, status, image URL and timestamps | Mapped by JPA |
| `Store` | `stores` | Store owner, contact/address details, status and timestamps | Mapped by JPA |
| `Inventory` | `inventory` | Store/product, quantity, reserved quantity, reorder level, status and update time | Mapped by JPA |
| `StoreStaff` / `StoreStaffPermission` | `store_staff` / `store_staff_permissions` | Store membership, status and individually granted permissions | Mapped by JPA |
| Click-and-collect cart/order/pickup/payment records | `carts`, `cart_items`, `orders`, `order_items`, `order_status_history`, `pickup`, `payments` | Read and written transactionally through `JdbcTemplate` in the order service | SQL-backed; these tables do not have JPA entities |

## Findings and mismatches

- Existing `StoreRegistrationRequest` is not a `Store`. The current manual
  approval SQL activates the owner and assigns `OWNER`, but does not create a
  row in the proposed `stores` table. A future approval workflow must decide
  whether/how to create a physical store; do not infer one in SQL.
- `categories`, `products`, `stores`, and `inventory` are mapped by JPA.
  Click-and-collect cart, order, pickup, payment, and notification operations
  use SQL through `JdbcTemplate`; chat and AI chat are outside these mappings.
- There is no current AI persistence entity or requirement; no AI tables are
  proposed.
- Existing entities use `GenerationType.IDENTITY`, matching proposed
  `BIGINT AUTO_INCREMENT`.
- Password columns hold hashes, not raw credentials. The session/reset token
  entities map only hashed token fields.
- Email verification is represented by `users.email_verified`. New
  registrations start with email unverified; customers activate after email
  verification, while staff registrations remain pending until manual
  approval. Phone is contact information only and has no verification state.
- OTPs are BCrypt-hashed; email destination lookups use HMAC-SHA-256 keyed by
  `OTP_HASH_SECRET` when set, otherwise by the configured SMTP password. Email
  OTP is the only verification channel. Keep the key stable while unexpired
  codes exist.
- Current associations use cascade-delete foreign keys for role links,
  addresses, sessions, reset tokens and store registration. The proposed
  definitions preserve these current V1 delete behaviors for those existing
  relationships.
- Existing V1 defines `user_roles` and `role_permissions` with composite
  primary keys. To meet the one-ID-strategy requirement, the proposed DDL gives
  each junction a `BIGINT AUTO_INCREMENT` primary key and preserves pair
  uniqueness with a separate unique constraint. Current JPA many-to-many
  mappings do not expose these junction rows as entities, so they do not map
  the added key. This intentional physical-model difference needs review
  against the approved PowerDesigner model.
- Existing JPA timestamps are `Instant` with application callbacks/services.
  Existing SQL uses required `DATETIME(6)` columns without database defaults;
  the auth/user SQL retains that behavior. Several SQL modules use UTC
  `DATETIME(6)` defaults. Hibernate uses validation only; verify mappings
  against the approved model and deployed schema before rollout.

Do not treat Hibernate `validate` as a substitute for a manual mapping review:
new entity column precision, enum length, association join names, and timestamp
semantics must be compared with the approved physical model before enabling
each feature.
