-- ============================================================
-- TASK #26 - ACTIVITY LOG MONITORING
-- Bootstrap permissions for ADMIN
-- Safe to run multiple times
-- ============================================================

START TRANSACTION;


-- ============================================================
-- 1. CREATE ROLE_MANAGE IF NOT EXISTS
-- ============================================================

INSERT INTO permissions (
    permission_code,
    module_name,
    action_name,
    description,
    status,
    created_at,
    updated_at
)
SELECT
    'ROLE_MANAGE',
    'ROLE_PERMISSION',
    'MANAGE',
    'Manage roles and permissions',
    'ACTIVE',
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions
    WHERE permission_code = 'ROLE_MANAGE'
);


-- ============================================================
-- 2. CREATE AUDIT_READ IF NOT EXISTS
-- ============================================================

INSERT INTO permissions (
    permission_code,
    module_name,
    action_name,
    description,
    status,
    created_at,
    updated_at
)
SELECT
    'AUDIT_READ',
    'AUDIT',
    'READ',
    'View activity and security logs',
    'ACTIVE',
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions
    WHERE permission_code = 'AUDIT_READ'
);


-- ============================================================
-- 3. RESOLVE IDS
-- ============================================================

SET @admin_role_id = (
    SELECT id
    FROM roles
    WHERE roles_code = 'ROLE-ADMIN'
    LIMIT 1
);

SET @admin_user_id = (
    SELECT id
    FROM users
    WHERE user_code = 'DEV002'
    LIMIT 1
);

SET @role_manage_id = (
    SELECT id
    FROM permissions
    WHERE permission_code = 'ROLE_MANAGE'
    LIMIT 1
);

SET @audit_read_id = (
    SELECT id
    FROM permissions
    WHERE permission_code = 'AUDIT_READ'
    LIMIT 1
);


-- ============================================================
-- 4. GRANT ROLE_MANAGE TO ADMIN
-- ============================================================

INSERT INTO role_permissions (
    granted_at,
    granted_by,
    permission_id,
    role_id
)
SELECT
    NOW(6),
    @admin_user_id,
    @role_manage_id,
    @admin_role_id
WHERE
    @admin_role_id IS NOT NULL
    AND @role_manage_id IS NOT NULL
    AND NOT EXISTS (
        SELECT 1
        FROM role_permissions
        WHERE role_id = @admin_role_id
          AND permission_id = @role_manage_id
    );


-- ============================================================
-- 5. GRANT AUDIT_READ TO ADMIN
-- ============================================================

INSERT INTO role_permissions (
    granted_at,
    granted_by,
    permission_id,
    role_id
)
SELECT
    NOW(6),
    @admin_user_id,
    @audit_read_id,
    @admin_role_id
WHERE
    @admin_role_id IS NOT NULL
    AND @audit_read_id IS NOT NULL
    AND NOT EXISTS (
        SELECT 1
        FROM role_permissions
        WHERE role_id = @admin_role_id
          AND permission_id = @audit_read_id
    );


COMMIT;


-- ============================================================
-- VERIFY
-- ============================================================

SELECT
    r.id AS role_id,
    r.roles_code,
    p.id AS permission_id,
    p.permission_code,
    p.module_name,
    p.action_name,
    p.status
FROM role_permissions rp
JOIN roles r
    ON r.id = rp.role_id
JOIN permissions p
    ON p.id = rp.permission_id
WHERE r.roles_code = 'ROLE-ADMIN'
  AND p.permission_code IN (
      'ROLE_MANAGE',
      'AUDIT_READ'
  )
ORDER BY p.permission_code;