-- Duyệt đăng ký cửa hàng thủ công (chạy trong TiDB Cloud SQL Editor).
-- 1) Xem các yêu cầu đang chờ:
--    SELECT r.id, r.store_name, u.full_name, u.email, u.phone
--    FROM store_registration_requests r JOIN users u ON u.id = r.owner_user_id WHERE r.status = 'PENDING';
-- 2) Thay 123 bằng id yêu cầu cần duyệt rồi chạy khối dưới.
START TRANSACTION;
UPDATE store_registration_requests SET status='APPROVED', reviewed_at=NOW(6), updated_at=NOW(6) WHERE id=123 AND status='PENDING';
UPDATE users SET status='ACTIVE', updated_at=NOW(6) WHERE id=(SELECT owner_user_id FROM store_registration_requests WHERE id=123);
INSERT INTO user_roles (user_id, role_id)
SELECT r.owner_user_id, ro.id FROM store_registration_requests r JOIN roles ro ON ro.name='OWNER' WHERE r.id=123;
COMMIT;
