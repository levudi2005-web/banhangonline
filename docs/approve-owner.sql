-- Owners are provisioned through the approved internal account process.
-- There is no public owner registration endpoint. Create the account securely,
-- store only a BCrypt password hash, and ensure the user is ACTIVE before this step.
-- Review the target user and role before replacing 123 with the user's id.
SELECT id, full_name, username, status
FROM users
WHERE id=123 AND status='ACTIVE';

START TRANSACTION;
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, ro.id
FROM users u
JOIN roles ro ON ro.name='OWNER'
WHERE u.id=123 AND u.status='ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id=u.id AND ur.role_id=ro.id);
COMMIT;
