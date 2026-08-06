package com.toystorage.backend.entity.auth;

import com.toystorage.backend.entity.users.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "role_permissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_role_permissions_role_permission",
                columnNames = {"role_id", "permission_id"}
        ),
        indexes = {
                @Index(name = "idx_role_permissions_role_id", columnList = "role_id"),
                @Index(name = "idx_role_permissions_permission_id", columnList = "permission_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_role_permissions_role"))
    private Roles role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_role_permissions_permission"))
    private Permissions permission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by",
            foreignKey = @ForeignKey(name = "fk_role_permissions_granted_by"))
    private Users grantedBy;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    @PrePersist
    protected void onCreate() {
        if (grantedAt == null) grantedAt = LocalDateTime.now();
    }
}
