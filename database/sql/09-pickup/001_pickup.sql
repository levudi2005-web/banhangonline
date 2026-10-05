-- Reconciled to the TiDB schema export for: pickup.

-- Logical model types use BIGINT, VARCHAR, CHAR, DECIMAL, DATETIME, INT, TINYINT, TEXT, and JSON.

-- pickup
CREATE TABLE `pickup` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `pickup_code_hash` char(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'WAITING',
  `expires_at` datetime(6) DEFAULT NULL,
  `picked_up_at` datetime(6) DEFAULT NULL,
  `confirmed_by_user_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`) ,
  UNIQUE KEY `uq_pickup_order` (`order_id`),
  KEY `idx_pickup_code_hash` (`pickup_code_hash`),
  KEY `idx_pickup_confirmed_by` (`confirmed_by_user_id`),
  CONSTRAINT `fk_pickup_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pickup_confirmed_by` FOREIGN KEY (`confirmed_by_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
