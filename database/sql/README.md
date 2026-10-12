# SQL bootstrap files

`mariadb_full_schema.sql` is the current full bootstrap for MariaDB 10.11.x.
It creates the `usadshop` database, tables, constraints, indexes, and initial
roles/permissions. Run it only against a fresh database; it is not a backup,
does not contain application data, and is not a migration for an existing
database.

`tidb_full_schema.sql` and the numbered SQL files are retained as historical
TiDB/schema-design material. Do not use them as the current MariaDB bootstrap.
Spring Boot does not execute any of these files automatically; Hibernate is
validation-only and Flyway is disabled. Back up existing MariaDB data with
`mariadb-dump` before changing a database. Existing installations missing the
chat unread cursor can apply
`migrations/chat_last_read_at_mariadb_10_11.sql` manually after backing up.
