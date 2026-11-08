package com.toystorage.backend.entity.users;

import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.users.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_code", nullable = false, unique = true, length =50)
    private String userCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 150)
    private String email;

    @JsonIgnore
    @Column(nullable = false, length = 150)
    private String password;

    @Column(name = "identity_card", nullable = false, unique = true, length = 12)
    private String identityCard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouses warehouse;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword = false;

    @Column(name = "phone", length = 20)
    private String phone;

    @JsonIgnore
    @Column(name = "totp_secret", length = 255)
    private String totpSecret;

    @Builder.Default
    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled = false;

    @JsonIgnore
    @Builder.Default
    @Column(name = "mfa_last_used_step", nullable = false)
    private long mfaLastUsedStep = -1;

    @JsonIgnore
    @Builder.Default
    @Column(name = "mfa_failed_attempts", nullable = false)
    private int mfaFailedAttempts = 0;

    @JsonIgnore
    @Column(name = "mfa_locked_until")
    private Instant mfaLockedUntil;

    @JsonIgnore
    @Column(name = "mfa_recovery_hashes", columnDefinition = "text")
    private String mfaRecoveryHashes;

    @JsonIgnore
    @Builder.Default
    @Column(name = "mfa_reenroll_required", nullable = false)
    private boolean mfaReenrollRequired = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isMustChangePassword() {
        return Boolean.TRUE.equals(mustChangePassword);
    }

}