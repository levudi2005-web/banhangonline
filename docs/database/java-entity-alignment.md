# Java entity and proposed schema alignment

Comparison is against the current Java entities and historical Flyway V1, not a
live TiDB database. The new module SQL preserves the mapped columns of current
entities where they overlap. No Java entity was changed for this schema
proposal.

| Java entity | Table | Current mapped fields/relations | Proposed SQL comparison |
|---|---|---|---|
| `User` | `users` | `id`, full name, username, email, phone, password hash, status, created/updated times; `user_roles` relation | Matches V1 names, types/lengths, nullability, unique identifiers and identity strategy |
| `Role` | `roles` | `id`, name, description; `role_permissions` relation | Matches V1 |
| `Permission` | `permissions` | `id`, name, description | Matches V1 |
| `UserAddress` | `user_addresses` | `id`, user, recipient/contact/address fields, default flag and timestamps | Matches V1 columns; FK and index are equivalent |
| `Session` | `sessions` | `id`, token hash, user, expiry/creation, optional IP and user agent | Matches V1 columns and unique token hash |
| `PasswordResetToken` | `password_reset_tokens` | `id`, user, token hash, expiry, used/creation times | Matches V1 columns and unique token hash |
| `StoreRegistrationRequest` | `store_registration_requests` | `id`, owner, store/contact/address details, status/review and timestamps | Matches V1; request is distinct from the proposed `stores` entity/table |

## Findings and mismatches

- Existing `StoreRegistrationRequest` is not a `Store`. The current manual
  approval SQL activates the owner and assigns `OWNER`, but does not create a
  row in the proposed `stores` table. A future approval workflow must decide
  whether/how to create a physical store; do not infer one in SQL.
- No Java entities currently map `categories`, `products`, `stores`,
  `inventory`, `carts`, `cart_items`, `orders`, `order_items`,
  `order_status_history`, `pickup`, `payments`, `notifications`, chat, or audit
  tables. These are forward schema proposals, not runtime features.
- There is no current AI persistence entity or requirement; no AI tables are
  proposed.
- Existing entities use `GenerationType.IDENTITY`, matching proposed
  `BIGINT AUTO_INCREMENT`.
- Password columns hold hashes, not raw credentials. The session/reset token
  entities map only hashed token fields.
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
  the auth/user SQL retains that behavior. New modules use UTC `DATETIME(6)`
  defaults; confirm Hibernate validation and write semantics when those
  entities are implemented.

Do not treat Hibernate `validate` as a substitute for a manual mapping review:
new entity column precision, enum length, association join names, and timestamp
semantics must be compared with the approved physical model before enabling
each feature.
