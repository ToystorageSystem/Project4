
-- =====================================================
-- TASK: LOGIN + GOOGLE AUTHENTICATOR (2FA)
-- Table: user_two_factor
--
-- Luu cau hinh xac thuc hai yeu to cua moi tai khoan.
-- Khong xoa hoac thay doi du lieu trong bang users.
-- =====================================================

CREATE TABLE IF NOT EXISTS user_two_factor (

    user_id BIGINT NOT NULL,

    -- TOTP secret duoc ma hoa bang AES-256-GCM.
    encrypted_secret VARBINARY(512) NULL,

    -- Chi bat sau khi xac minh OTP thanh cong.
    enabled TINYINT(1) NOT NULL DEFAULT 0,

    -- Ngan su dung lai ma OTP da duoc chap nhan.
    last_accepted_step BIGINT NULL,

    created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    updated_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (user_id),

    CONSTRAINT fk_user_two_factor_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4;
