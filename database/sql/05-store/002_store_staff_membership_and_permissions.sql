-- Additive manual migration: store-scoped staff membership and permissions.
-- Apply once only after confirming the live TiDB schema matches the reviewed export.
-- No existing tables or rows are changed.

CREATE TABLE store_staff (
  id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uq_store_staff_store_user UNIQUE (store_id, user_id),
  KEY idx_store_staff_user_status (user_id, status),
  CONSTRAINT fk_store_staff_store FOREIGN KEY (store_id) REFERENCES stores (id),
  CONSTRAINT fk_store_staff_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE store_staff_permissions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  store_staff_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  granted_by_user_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uq_store_staff_permission UNIQUE (store_staff_id, permission_id),
  KEY idx_store_staff_permissions_permission (permission_id),
  KEY idx_store_staff_permissions_granted_by (granted_by_user_id),
  CONSTRAINT fk_store_staff_permissions_membership FOREIGN KEY (store_staff_id) REFERENCES store_staff (id) ON DELETE CASCADE,
  CONSTRAINT fk_store_staff_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id),
  CONSTRAINT fk_store_staff_permissions_grantor FOREIGN KEY (granted_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
