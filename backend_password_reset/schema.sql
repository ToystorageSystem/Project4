-- Run once on the same MySQL database as the backend.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    user_id BIGINT NOT NULL PRIMARY KEY,
    token_hash CHAR(64) NOT NULL,
    issued_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
