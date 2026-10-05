# Foreign-key dependencies and DDL order

The combined DDL follows referenced-table order. Existing tables are ordered
from the local TiDB schema exports; store-scoped staff tables are a new
additive migration and must only be applied after confirming the live database.

| Order | SQL module/file | Tables | Required earlier tables |
|---:|---|---|---|
| 1 | `01-auth/001_auth_core.sql` | `users`, `roles`, `permissions` | None |
| 2 | `01-auth/002_auth_links_and_tokens.sql` | `user_roles`, `role_permissions`, `sessions`, `password_reset_tokens` | `users`, `roles`, `permissions` |
| 3 | `01-auth/004_create_verification_codes.sql` | `verification_codes` | `users` |
| 4 | `02-user/001_user_addresses.sql` | `user_addresses` | `users` |
| 5 | `04-category/001_categories.sql` | `categories` | None |
| 6 | `03-product/001_products.sql` | `products` | `categories` |
| 7 | `05-store/001_stores_and_registration_requests.sql` | `stores`, `store_registration_requests` | `users` |
| 8 | `05-store/002_store_staff_membership_and_permissions.sql` | `store_staff` | `stores`, `users` |
| 9 | `05-store/002_store_staff_membership_and_permissions.sql` | `store_staff_permissions` | `store_staff`, `permissions`, `users` |
| 10 | `06-inventory/001_inventory.sql` | `inventory` | `stores`, `products` |
| 11 | `07-cart/001_carts_and_items.sql` | `carts`, `cart_items` | `users`, `stores`, `products`, `carts` |
| 12 | `08-order/001_orders_items_and_status_history.sql` | `orders`, `order_items`, `order_status_history` | `users`, `stores`, `products`, `orders` |
| 13 | `09-pickup/001_pickup.sql` | `pickup` | `orders`, `users` |
| 14 | `10-payment/001_payments.sql` | `payments` | `orders` |
| 15 | `11-notification/001_notifications.sql` | `notifications` | `users` |
| 16 | `12-chat/001_chat_tables.sql` | `chat_conversations`, `chat_participants`, `chat_messages` | `stores`, `users`, `chat_conversations` |
| 17 | `13-audit/001_audit_logs.sql` | `audit_logs` | `users` |
| 18 | `01-auth/900_seed_core_roles_permissions.sql` | Reference role/permission rows | Auth tables |

The local export contains 26 existing tables; the additive migration adds two
more. Existing keys, columns, and relationships must not be inferred from the
proposed schema or this ordering table—use the DDL and verify it against the
live database. `tidb_full_schema.sql` is a one-time empty-schema bootstrap,
not a production migration or application startup script. It intentionally
has no `DROP TABLE` and is not idempotent.
