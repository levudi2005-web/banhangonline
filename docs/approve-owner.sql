-- Duyệt đăng ký cửa hàng thủ công (chạy trong TiDB Cloud SQL Editor).
-- Xác minh thông tin liên hệ ngoài ứng dụng trước khi duyệt; email OTP hiện không được dùng.
-- 1) Xem các yêu cầu đang chờ:
--    SELECT r.id, r.store_name, u.full_name, u.email,
--           CONCAT(REPEAT('*', GREATEST(CHAR_LENGTH(u.phone)-4, 0)), RIGHT(u.phone, 4)) AS phone
--    FROM store_registration_requests r JOIN users u ON u.id = r.owner_user_id WHERE r.status = 'PENDING';
-- 2) Chỉ duyệt sau khi đã kiểm tra thủ công thông tin liên hệ và cửa hàng.
-- Thay 123 bằng id yêu cầu cần duyệt rồi chạy khối dưới.
START TRANSACTION;
UPDATE store_registration_requests r
JOIN users u ON u.id = r.owner_user_id
SET r.status='APPROVED', r.reviewed_at=NOW(6), r.updated_at=NOW(6)
WHERE r.id=123 AND r.status='PENDING';
UPDATE users u
JOIN store_registration_requests r ON r.owner_user_id=u.id
SET u.status='ACTIVE', u.updated_at=NOW(6)
WHERE r.id=123 AND r.status='APPROVED';
INSERT INTO user_roles (user_id, role_id)
SELECT r.owner_user_id, ro.id
FROM store_registration_requests r
JOIN users u ON u.id=r.owner_user_id
JOIN roles ro ON ro.name='OWNER'
WHERE r.id=123 AND r.status='APPROVED' AND u.status='ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id=u.id AND ur.role_id=ro.id);
COMMIT;
