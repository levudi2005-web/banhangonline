-- Apply once to the existing TiDB users table before deploying email OTP-aware Java entities.
-- Existing accounts are treated as verified; new accounts are initialized as unverified by AuthService.
ALTER TABLE users
  ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;
