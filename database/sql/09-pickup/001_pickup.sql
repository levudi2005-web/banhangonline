CREATE TABLE pickup (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  pickup_code_hash CHAR(64) NOT NULL,
  expires_at DATETIME(6) NULL,
  verified_by_user_id BIGINT NULL,
  picked_up_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uq_pickup_order UNIQUE (order_id),
  KEY idx_pickup_verified_by (verified_by_user_id),
  CONSTRAINT fk_pickup_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
  CONSTRAINT fk_pickup_verified_by FOREIGN KEY (verified_by_user_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
