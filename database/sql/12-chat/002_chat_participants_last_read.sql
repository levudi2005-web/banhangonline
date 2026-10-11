ALTER TABLE chat_participants
  ADD COLUMN last_read_at DATETIME(6) NULL AFTER left_at;
