# Foreign-key dependencies and DDL order

Every referenced table must exist before its referencing table. This order is
used by `database/sql/tidb_full_schema.sql`; SQL module folder numbering is a
catalog, not a global execution order.

| Order | SQL module/file | Tables introduced | References already available |
|---:|---|---|---|
| 1 | `01-auth/001_auth_core.sql` | `users`, `roles`, `permissions` | None |
| 2 | `01-auth/002_auth_links_and_tokens.sql` | `user_roles`, `role_permissions`, `sessions`, `password_reset_tokens` | `users`, `roles`, `permissions` |
| 3 | `02-user/001_user_addresses.sql` | `user_addresses` | `users` |
| 4 | `04-category/001_categories.sql` | `categories` | Self-reference within `categories` |
| 5 | `03-product/001_products.sql` | `products` | `categories` |
| 6 | `05-store/001_stores_and_registration_requests.sql` | `stores`, `store_registration_requests` | `users` |
| 7 | `06-inventory/001_inventory.sql` | `inventory` | `stores`, `products` |
| 8 | `07-cart/001_carts_and_items.sql` | `carts`, `cart_items` | `users`, `stores`, `products`, `carts` |
| 9 | `08-order/001_orders_items_and_status_history.sql` | `orders`, `order_items`, `order_status_history` | `users`, `stores`, `products`, `orders` |
| 10 | `09-pickup/001_pickup.sql` | `pickup` | `orders`, `users` |
| 11 | `10-payment/001_payments.sql` | `payments` | `orders` |
| 12 | `11-notification/001_notifications.sql` | `notifications` | `users` |
| 13 | `12-chat/001_chat_tables.sql` | `chat_conversations`, `chat_participants`, `chat_messages` | `stores`, `orders`, `users`, then conversations |
| 14 | `13-audit/001_audit_logs.sql` | `audit_logs` | `users` |
| 15 | `01-auth/900_seed_core_roles_permissions.sql` | No tables; role/permission reference rows | Auth tables and links |

Every table, including junction tables, has a `BIGINT AUTO_INCREMENT` primary
key; unique constraints retain pair uniqueness. `categories` must precede
`products`, even though its module directory number is after product. Tables
are only listed once in the combined file. The seed
script contains role and permission reference data, not sample users or
business transactions.
