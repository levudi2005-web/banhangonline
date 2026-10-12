-- Required by ChatService for unread counts and marking conversations as read.
-- Apply manually to an existing MariaDB database after taking a backup.
-- Safe to re-run on MariaDB 10.11; existing rows are initialized to the migration time.
ALTER TABLE `chat_participants`
  ADD COLUMN IF NOT EXISTS `last_read_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
