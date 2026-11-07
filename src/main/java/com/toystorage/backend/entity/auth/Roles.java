package com.toystorage.backend.entity.auth;

import jakarta.persistence.*;
import com.toystorage.backend.enums.auth.RoleStatus;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "roles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_roles_role_name", columnNames = "role_name"),
                @UniqueConstraint(name = "uk_roles_code", columnNames = "roles_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Roles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false, length = 80)
    private String roleName;

    @Column(name = "roles_code", nullable = false, length = 50)
    private String roleCode;

    // Trạng thái của vai trò trong DB; role INACTIVE không được cấp quyền.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoleStatus status;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserRoles> userRoles = new HashSet<>();

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RolePermissions> rolePermissions = new HashSet<>();
}
