-- TEST DATA ONLY. This script is pinned to the isolated test database.
USE `banhangonline_test`;

SELECT
    u.id,
    u.full_name,
    u.username,
    u.email,
    u.phone,
    u.status,
    GROUP_CONCAT(r.name ORDER BY r.name SEPARATOR ', ') AS roles
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE u.username IN ('demo.owner', 'demo.staff', 'demo.customer', 'demo.customer2')
GROUP BY u.id, u.full_name, u.username, u.email, u.phone, u.status
ORDER BY u.username;

SELECT
    s.name AS store_name,
    s.status AS store_status,
    owner.username AS owner_username,
    staff.username AS staff_username,
    ss.status AS staff_status,
    GROUP_CONCAT(p.name ORDER BY p.name SEPARATOR ', ') AS staff_permissions
FROM stores s
JOIN users owner ON owner.id = s.owner_user_id
LEFT JOIN store_staff ss ON ss.store_id = s.id
LEFT JOIN users staff ON staff.id = ss.user_id
LEFT JOIN store_staff_permissions ssp ON ssp.store_staff_id = ss.id
LEFT JOIN permissions p ON p.id = ssp.permission_id
WHERE s.name = 'BanHangOnline Demo Store'
  AND owner.username = 'demo.owner'
GROUP BY s.id, s.name, s.status, owner.username, staff.username, ss.status;

SELECT
    u.username,
    a.recipient_name,
    a.phone,
    a.province,
    a.district,
    a.ward,
    a.address_line,
    a.latitude,
    a.longitude,
    a.is_default
FROM user_addresses a
JOIN users u ON u.id = a.user_id
WHERE u.username IN ('demo.customer', 'demo.customer2')
ORDER BY u.username, a.id;
