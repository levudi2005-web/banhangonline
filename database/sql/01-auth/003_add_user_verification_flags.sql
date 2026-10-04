-- Apply once to the existing TiDB users table before deploying OTP-aware Java entities.
-- Existing accounts are treated as verified; newly registered users are explicitly initialized false by AuthService.
ALTER TABLE users
  ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN phone_verified BOOLEAN NOT NULL DEFAULT TRUE;
