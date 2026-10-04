INSERT INTO roles (name, description) VALUES
  ('CUSTOMER', 'Khách hàng mua sắm'),
  ('STAFF', 'Nhân viên cửa hàng'),
  ('OWNER', 'Chủ cửa hàng');

INSERT INTO permissions (name, description) VALUES
  ('VIEW_ORDERS', 'Xem đơn hàng'),
  ('MANAGE_ORDERS', 'Xử lý đơn hàng'),
  ('CONFIRM_PICKUP', 'Xác nhận khách nhận hàng'),
  ('VIEW_PRODUCTS', 'Xem sản phẩm'),
  ('MANAGE_PRODUCTS', 'Quản lý sản phẩm'),
  ('VIEW_INVENTORY', 'Xem tồn kho'),
  ('MANAGE_INVENTORY', 'Quản lý tồn kho'),
  ('VIEW_STAFF', 'Xem nhân viên'),
  ('MANAGE_STAFF', 'Quản lý nhân viên'),
  ('VIEW_STORES', 'Xem cửa hàng'),
  ('MANAGE_STORES', 'Quản lý cửa hàng'),
  ('VIEW_REPORTS', 'Xem báo cáo'),
  ('MANAGE_SETTINGS', 'Quản lý cài đặt');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'OWNER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'STAFF'
  AND p.name IN ('VIEW_ORDERS','MANAGE_ORDERS','CONFIRM_PICKUP','VIEW_PRODUCTS','VIEW_INVENTORY','VIEW_STORES');
