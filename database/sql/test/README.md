# Test accounts and store fixture

These are disposable test credentials for a local or otherwise isolated
TiDB/MySQL-compatible database. **Do not run them against production.**

## Prepare an isolated database

1. Create a separate database named `banhangonline_test`.
2. Apply the reviewed schema from `../tidb_full_schema.sql` to that empty
   database. The schema file is a baseline for a fresh database; do not apply
   it to an existing database.
3. Apply `../01-auth/900_seed_core_roles_permissions.sql` if the role and
   permission reference rows are not already present.
4. Run `001_seed_test_accounts.sql`. It explicitly selects
   `banhangonline_test`, inserts missing fixture rows, and does not delete or
   overwrite existing rows.
5. Point the local application at `banhangonline_test` before signing in.
6. Run `002_verify_test_accounts.sql` to inspect the inserted accounts,
   store, staff access, and customer addresses.

All four accounts use the same intentionally public test password:
`Test@12345`. The database stores its BCrypt hash; the raw password is not
stored in SQL. Change or remove these accounts before exposing a test
environment to a network.

| Role | Username | Email | Phone |
| --- | --- | --- | --- |
| Store owner | `demo.owner` | `owner@banhangonline.test` | `0000000001` |
| Store staff | `demo.staff` | `staff@banhangonline.test` | `0000000002` |
| Customer | `demo.customer` | `customer@banhangonline.test` | `0000000003` |
| Customer | `demo.customer2` | `customer2@banhangonline.test` | `0000000004` |

The fixture creates one ACTIVE demo store, grants the staff account a limited
set of store-scoped permissions, and adds sample pickup/customer addresses.
It does not create sessions, orders, or production credentials.
