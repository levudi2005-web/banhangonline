# Existing Flyway findings

## Current files

- `V1__create_auth_tables.sql` defines nine application tables:
  `users`, `roles`, `permissions`, `user_roles`, `role_permissions`,
  `user_addresses`, `sessions`, `password_reset_tokens`, and
  `store_registration_requests`.
- V1 adds unique keys for usernames/emails/phones, role/permission names,
  session/reset token hashes; relationship indexes; and foreign keys with
  `ON DELETE CASCADE` for user-owned/link rows.
- `V2__seed_roles_permissions.sql` inserts `CUSTOMER`, `STAFF`, and `OWNER`;
  the application's current 13 named permissions; all permissions for `OWNER`;
  and a subset for `STAFF`. It creates reference/authorization rows, not
  customer or commerce sample data.

The exact table count created in V1 is **9**:
`users`, `roles`, `permissions`, `user_roles`, `role_permissions`,
`user_addresses`, `sessions`, `password_reset_tokens`, and
`store_registration_requests`.

## Runtime state

The Spring Boot Maven dependencies no longer include Flyway, and
`spring.flyway.enabled=false` is explicit. V1/V2 remain in source as historical
SQL but are not auto-executed. Hibernate is configured with
`spring.jpa.hibernate.ddl-auto=validate`; it must not create or update schema.

## Relationship to the proposed SQL

The proposed DDL redefines the same nine existing tables so it can form one
standalone bootstrap for an empty database, while adding future commerce
tables. Therefore applying V1 and then the proposed full schema will produce
duplicate-table conflicts. Applying V2 after its rows already exist may
produce duplicate-key conflicts. Neither path is automatic; do not apply
either blindly.

The proposed auth DDL retains current entity column names/types and existing
cascade relationships. It adds an index on `user_roles.role_id` and
`role_permissions.permission_id` inline in their definitions, equivalent to
the V1 standalone index statements. To use one primary-key strategy, the
proposed DDL changes both V1 junction tables from composite primary keys to
`BIGINT AUTO_INCREMENT` primary keys plus unique FK pairs. This is a deliberate
physical schema difference and requires PowerDesigner review.

PowerDesigner-generated SQL is the intended authority. Before a deployment,
compare its tables, indexes, FK actions and seeds with both the legacy V1/V2
and the proposed SQL; choose a single reviewed source. This analysis is static
and no SQL was executed.
