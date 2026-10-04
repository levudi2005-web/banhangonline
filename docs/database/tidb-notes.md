# TiDB Cloud implementation notes

## Connection and runtime

The production-compatible datasource is MySQL Connector/J. Spring builds its
URL from:

```text
jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?sslMode=${DB_SSL_MODE}
```

The required environment names are `DB_HOST`, `DB_PORT`, `DB_NAME`,
`DB_USERNAME`, `DB_PASSWORD`, and `DB_SSL_MODE`. `.env.example` contains
placeholders only. `DB_PORT=4000` and `DB_SSL_MODE=REQUIRED` are prepared
defaults/examples from the requested TiDB Cloud setup; confirm the actual
cluster connection instructions before use.

No local DB or H2 fallback is configured. Do not connect or validate credentials
until the owner has provisioned TiDB and explicitly starts that step.

## Schema and SQL compatibility

- TiDB's MySQL-compatible DDL uses InnoDB, `BIGINT AUTO_INCREMENT`, `DATETIME(6)`,
  `DECIMAL`, `CHAR/VARCHAR`, indexes, foreign keys, and JSON in this proposal.
- Confirm the TiDB cluster version's foreign-key enforcement, generated-ID
  behavior, JSON behavior, collation support, and index/constraint limits
  against current TiDB documentation and the target region/edition before
  approving the physical model.
- Current schemas use `utf8mb4_unicode_ci`; confirm desired case sensitivity
  for username, email, SKU and category slug. Username normalization is
  lowercase in Java, but email/SKU normalization is not uniformly explicit.
- `sslMode=REQUIRED` encrypts the connection without requiring identity
  verification. Follow the TiDB Cloud-provided CA and JDBC instructions if
  stricter verification is required; do not disable TLS.
- Do not use Hibernate schema generation. Current setting is
  `spring.jpa.hibernate.ddl-auto=validate`. Schema DDL remains a separately
  reviewed/manual operation.
- `spring.flyway.enabled=false`; migration SQL under
  `backend/src/main/resources/db/migration/` is historical source material and
  is not automatically run.
- The SQL bootstrap is not idempotent. Do not run it against a database with
  existing tables or combine it with applying Flyway V1/V2. Reconcile the
  PowerDesigner MPD, target schema, and current data first.

## Open design review items

- Confirm the currency policy/scale (proposed `DECIMAL(19,4)` plus ISO currency).
- Confirm deletion/retention behavior for accounts, orders, chats, audit and
  payment records before deployment.
- Define exact status vocabularies and transitions in Java service specs.
- Decide whether staff-to-store assignment requires a dedicated relationship;
  current `user_roles` has no store scope.
- Decide whether store registration approval should create a `stores` row;
  current approval SQL only updates request/user status and role assignment.
- Confirm whether a cart may have more than one active row per customer/store;
  the proposal leaves lifecycle uniqueness to a cart service.
