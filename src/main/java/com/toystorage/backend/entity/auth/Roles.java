package com.toystorage.backend.entity.auth;

import com.toystorage.backend.enums.auth.RoleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "roles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_roles_role_name",
                        columnNames = "role_name"
                ),
                @UniqueConstraint(
                        name = "uk_roles_code",
                        columnNames = "roles_code"
                )
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

    @Column(
            name = "role_name",
            nullable = false,
            length = 80
    )
    private String roleName;

    @Column(
            name = "roles_code",
            nullable = false,
            length = 50
    )
    private String roleCode;

    @Column(
            name = "description",
            length = 255
    )
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private RoleStatus status = RoleStatus.ACTIVE;

    @OneToMany(
            mappedBy = "role",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private Set<UserRoles> userRoles = new HashSet<>();

    @OneToMany(
            mappedBy = "role",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private Set<RolePermissions> rolePermissions = new HashSet<>();

    @PrePersist
    protected void onCreate() {

        if (status == null) {
            status = RoleStatus.ACTIVE;
        }
    }
}