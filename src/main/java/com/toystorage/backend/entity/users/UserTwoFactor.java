
package com.toystorage.backend.entity.users;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_two_factor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTwoFactor {

    // Mỗi tài khoản có một cấu hình 2FA duy nhất.
    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Lưu secret đã mã hóa, không lưu secret dạng văn bản.
    @Column(
            name = "encrypted_secret",
            columnDefinition = "VARBINARY(512)"
    )
    private byte[] encryptedSecret;

    // Chỉ bật sau khi người dùng xác minh OTP thiết lập thành công.
    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    // Ghi nhận chu kỳ OTP đã sử dụng để ngăn sử dụng lại.
    @Column(name = "last_accepted_step")
    private Long lastAcceptedStep;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
