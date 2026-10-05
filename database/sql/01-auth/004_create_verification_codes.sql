-- Reconciled to the TiDB schema export for: verification_codes.

-- Logical model types use BIGINT, VARCHAR, CHAR, DECIMAL, DATETIME, INT, TINYINT, TEXT, and JSON.

-- verification_codes
CREATE TABLE `verification_codes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `channel` varchar(20) NOT NULL,
  `purpose` varchar(40) NOT NULL,
  `code_hash` varchar(64) NOT NULL,
  `destination_hash` varchar(64) NOT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `attempts` int NOT NULL DEFAULT '0',
  `max_attempts` int NOT NULL DEFAULT '5',
  `expires_at` datetime NOT NULL,
  `used_at` datetime DEFAULT NULL,
  `invalidated_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) ,
  KEY `idx_verification_codes_lookup` (`user_id`,`channel`,`purpose`,`destination_hash`,`created_at`),
  CONSTRAINT `fk_verification_codes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
