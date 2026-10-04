CREATE TABLE categories (
  id BIGINT NOT NULL AUTO_INCREMENT,
  parent_category_id BIGINT NULL,
  name VARCHAR(120) NOT NULL,
  slug VARCHAR(140) NOT NULL,
  description VARCHAR(500) NULL,
  status VARCHAR(24) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uq_categories_slug UNIQUE (slug),
  KEY idx_categories_parent (parent_category_id),
  KEY idx_categories_status (status),
  CONSTRAINT fk_categories_parent FOREIGN KEY (parent_category_id) REFERENCES categories (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
