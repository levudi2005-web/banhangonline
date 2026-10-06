-- Apply manually after reviewing the PowerDesigner MPD and target database.
-- Products remain backward-compatible through products.image_url; sort_order 0 is the main image.
CREATE TABLE `product_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` tinyint unsigned NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_product_images_product_order` (`product_id`,`sort_order`),
  UNIQUE KEY `uq_product_images_product_url` (`product_id`,`image_url`),
  CONSTRAINT `ck_product_images_sort_order` CHECK (`sort_order` BETWEEN 0 AND 7),
  CONSTRAINT `fk_product_images_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `product_images` (`product_id`, `image_url`, `sort_order`)
SELECT `id`, `image_url`, 0
FROM `products`
WHERE `image_url` IS NOT NULL AND `image_url` <> '';
