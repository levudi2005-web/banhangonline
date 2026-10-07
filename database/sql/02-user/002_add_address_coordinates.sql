ALTER TABLE `user_addresses`
  ADD COLUMN IF NOT EXISTS `latitude` decimal(10,7) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `longitude` decimal(10,7) DEFAULT NULL;
