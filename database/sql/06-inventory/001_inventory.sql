-- Reconciled to the TiDB schema export for: inventory.

-- Logical model types use BIGINT, VARCHAR, CHAR, DECIMAL, DATETIME, INT, TINYINT, TEXT, and JSON.

-- inventory
CREATE TABLE `inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `quantity` int NOT NULL DEFAULT '0',
  `reserved_quantity` int NOT NULL DEFAULT '0',
  `reorder_level` int NOT NULL DEFAULT '0',
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`) ,
  UNIQUE KEY `uq_inventory_store_product` (`store_id`,`product_id`),
  KEY `idx_inventory_product` (`product_id`),
  CONSTRAINT `fk_inventory_store` FOREIGN KEY (`store_id`) REFERENCES `stores` (`id`),
  CONSTRAINT `fk_inventory_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
