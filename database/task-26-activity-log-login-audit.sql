-- ============================================================
-- TASK #26 - LOGIN AUDIT SUPPORT
--
-- Cho phép ghi LOGIN_FAILED khi email đăng nhập
-- không thuộc bất kỳ user nào.
-- ============================================================

ALTER TABLE activity_logs
MODIFY COLUMN user_id BIGINT NULL;