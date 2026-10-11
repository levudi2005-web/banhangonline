-- TEST DATA ONLY. This script is pinned to the isolated test database.
-- It inserts only missing rows and never deletes or updates user data.
USE `banhangonline_test`;

START TRANSACTION;

-- Shared test-only password: Test@12345 (Spring Security BCrypt, cost 10).
SET @test_password_hash = '$2a$10$r0q5noEI0qZC3KEbGRLd/OsEo6Om5sMNdD8V4o/rQ3PoJ2VibFg9u';

INSERT INTO roles (name, description)
SELECT 'OWNER', 'Chủ cửa hàng'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'OWNER');

INSERT INTO roles (name, description)
SELECT 'STAFF', 'Nhân viên cửa hàng'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'STAFF');

INSERT INTO roles (name, description)
SELECT 'CUSTOMER', 'Khách hàng mua sắm'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'CUSTOMER');

INSERT INTO permissions (name, description)
SELECT 'VIEW_ORDERS', 'Xem đơn hàng'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'VIEW_ORDERS');

INSERT INTO permissions (name, description)
SELECT 'MANAGE_ORDERS', 'Xử lý đơn hàng'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'MANAGE_ORDERS');

INSERT INTO permissions (name, description)
SELECT 'CONFIRM_PICKUP', 'Xác nhận khách nhận hàng'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'CONFIRM_PICKUP');

INSERT INTO permissions (name, description)
SELECT 'VIEW_PRODUCTS', 'Xem sản phẩm'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'VIEW_PRODUCTS');

INSERT INTO permissions (name, description)
SELECT 'VIEW_INVENTORY', 'Xem tồn kho'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'VIEW_INVENTORY');

INSERT INTO users
    (full_name, username, email, phone, password_hash, status, email_verified)
SELECT
    'Chủ cửa hàng Demo', 'demo.owner', 'owner@banhangonline.test', '0000000001',
    @test_password_hash, 'ACTIVE', 1
WHERE NOT EXISTS (
    SELECT 1 FROM users
    WHERE username = 'demo.owner'
       OR email = 'owner@banhangonline.test'
       OR phone = '0000000001'
);

INSERT INTO users
    (full_name, username, email, phone, password_hash, status, email_verified)
SELECT
    'Nhân viên Demo', 'demo.staff', 'staff@banhangonline.test', '0000000002',
    @test_password_hash, 'ACTIVE', 1
WHERE NOT EXISTS (
    SELECT 1 FROM users
    WHERE username = 'demo.staff'
       OR email = 'staff@banhangonline.test'
       OR phone = '0000000002'
);

INSERT INTO users
    (full_name, username, email, phone, password_hash, status, email_verified)
SELECT
    'Khách hàng Demo', 'demo.customer', 'customer@banhangonline.test', '0000000003',
    @test_password_hash, 'ACTIVE', 1
WHERE NOT EXISTS (
    SELECT 1 FROM users
    WHERE username = 'demo.customer'
       OR email = 'customer@banhangonline.test'
       OR phone = '0000000003'
);

INSERT INTO users
    (full_name, username, email, phone, password_hash, status, email_verified)
SELECT
    'Khách hàng Demo Hai', 'demo.customer2', 'customer2@banhangonline.test', '0000000004',
    @test_password_hash, 'ACTIVE', 1
WHERE NOT EXISTS (
    SELECT 1 FROM users
    WHERE username = 'demo.customer2'
       OR email = 'customer2@banhangonline.test'
       OR phone = '0000000004'
);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'OWNER'
WHERE u.username = 'demo.owner'
  AND u.email = 'owner@banhangonline.test'
  AND u.phone = '0000000001'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'STAFF'
WHERE u.username = 'demo.staff'
  AND u.email = 'staff@banhangonline.test'
  AND u.phone = '0000000002'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'CUSTOMER'
WHERE u.username IN ('demo.customer', 'demo.customer2')
  AND (
      (u.username = 'demo.customer'
       AND u.email = 'customer@banhangonline.test'
       AND u.phone = '0000000003')
      OR
      (u.username = 'demo.customer2'
       AND u.email = 'customer2@banhangonline.test'
       AND u.phone = '0000000004')
  )
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO stores
    (owner_user_id, name, phone, email, province, district, ward,
     address_detail, latitude, longitude, postal_code, description, status)
SELECT
    u.id, 'BanHangOnline Demo Store', '0000000000',
    'store@banhangonline.test', 'Thành phố Hồ Chí Minh', 'Quận 1',
    'Phường Bến Nghé', '123 Đường Demo', 10.7769000, 106.7009000,
    '700000', 'Cửa hàng giả lập dùng cho kiểm thử nội bộ.', 'ACTIVE'
FROM users u
WHERE u.username = 'demo.owner'
  AND u.email = 'owner@banhangonline.test'
  AND u.phone = '0000000001'
  AND NOT EXISTS (
      SELECT 1 FROM stores s
      WHERE s.owner_user_id = u.id AND s.name = 'BanHangOnline Demo Store'
  );

INSERT INTO store_staff (store_id, user_id, status)
SELECT s.id, u.id, 'ACTIVE'
FROM stores s
JOIN users owner ON owner.id = s.owner_user_id
JOIN users u ON u.username = 'demo.staff'
             AND u.email = 'staff@banhangonline.test'
             AND u.phone = '0000000002'
WHERE s.name = 'BanHangOnline Demo Store'
  AND owner.username = 'demo.owner'
  AND owner.email = 'owner@banhangonline.test'
  AND owner.phone = '0000000001'
  AND NOT EXISTS (
      SELECT 1 FROM store_staff ss
      WHERE ss.store_id = s.id AND ss.user_id = u.id
  );

INSERT INTO store_staff_permissions
    (store_staff_id, permission_id, granted_by_user_id)
SELECT ss.id, p.id, owner.id
FROM store_staff ss
JOIN stores s ON s.id = ss.store_id
JOIN users owner ON owner.id = s.owner_user_id
JOIN users staff ON staff.id = ss.user_id
CROSS JOIN permissions p
WHERE s.name = 'BanHangOnline Demo Store'
  AND owner.username = 'demo.owner'
  AND staff.username = 'demo.staff'
  AND p.name IN ('VIEW_ORDERS', 'MANAGE_ORDERS', 'CONFIRM_PICKUP',
                 'VIEW_PRODUCTS', 'VIEW_INVENTORY')
  AND NOT EXISTS (
      SELECT 1 FROM store_staff_permissions ssp
      WHERE ssp.store_staff_id = ss.id AND ssp.permission_id = p.id
  );

INSERT INTO user_addresses
    (user_id, recipient_name, phone, province, district, ward, address_line,
     latitude, longitude, is_default)
SELECT
    u.id, 'Khách hàng Demo', '0000000003', 'Thành phố Hồ Chí Minh',
    'Quận 1', 'Phường Bến Nghé', '45 Đường Nguyễn Huệ',
    10.7737000, 106.7030000, 1
FROM users u
WHERE u.username = 'demo.customer'
  AND u.email = 'customer@banhangonline.test'
  AND u.phone = '0000000003'
  AND NOT EXISTS (
      SELECT 1 FROM user_addresses a
      WHERE a.user_id = u.id AND a.address_line = '45 Đường Nguyễn Huệ'
  );

INSERT INTO user_addresses
    (user_id, recipient_name, phone, province, district, ward, address_line,
     latitude, longitude, is_default)
SELECT
    u.id, 'Khách hàng Demo Hai', '0000000004', 'Thành phố Hồ Chí Minh',
    'Quận 3', 'Phường Võ Thị Sáu', '88 Đường Demo',
    10.7823000, 106.6877000, 1
FROM users u
WHERE u.username = 'demo.customer2'
  AND u.email = 'customer2@banhangonline.test'
  AND u.phone = '0000000004'
  AND NOT EXISTS (
      SELECT 1 FROM user_addresses a
      WHERE a.user_id = u.id AND a.address_line = '88 Đường Demo'
  );

COMMIT;
