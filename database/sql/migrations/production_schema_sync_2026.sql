-- Production already has these nullable columns, but they were added as DOUBLE.
-- Store maps both values as BigDecimal with precision 10 and scale 7.
-- Convert in place; do not add duplicate columns or remove existing data.
ALTER TABLE `stores`
  MODIFY COLUMN `latitude` DECIMAL(10,7) NULL,
  MODIFY COLUMN `longitude` DECIMAL(10,7) NULL;

-- UserAddress also maps nullable coordinates; production does not have these columns.
ALTER TABLE `user_addresses`
  ADD COLUMN IF NOT EXISTS `latitude` DECIMAL(10,7) NULL,
  ADD COLUMN IF NOT EXISTS `longitude` DECIMAL(10,7) NULL;

-- These indexes support OTP quota queries by destination and source IP.
CREATE INDEX IF NOT EXISTS `idx_verification_destination_purpose_created`
  ON `verification_codes` (`destination_hash`,`purpose`,`created_at`);
CREATE INDEX IF NOT EXISTS `idx_verification_ip_created`
  ON `verification_codes` (`ip_address`,`created_at`);
