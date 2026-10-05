-- Activate a submitted store only after its details have been reviewed.
-- Owner accounts are provisioned internally; owners create a DRAFT store after login
-- and submit it to PENDING_REVIEW. Replace 123 with the reviewed store id.
UPDATE stores
SET status='ACTIVE', updated_at=NOW(6)
WHERE id=123 AND status IN ('PENDING_REVIEW', 'PENDING');

-- The application exposes only ACTIVE stores to customers:
-- SELECT id, owner_user_id, name, status FROM stores WHERE id=123;
