package com.toystorage.backend.entity.auth;


import com.toystorage.backend.entity.users.Users;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_roles",
        /**
         * uniqueConstraints được dùng khi cần UNIQUE trên nhiều cột
         * Nếu chỉ UNIQUE một cột thì dùng @Column(unique = true).
         */

        uniqueConstraints = {@UniqueConstraint(name = "uk_user_roles_1", columnNames = {"user_id", "role_id"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "role_id", nullable = false)
    private Roles role;

    @Column(name = "user_roles_code", nullable = false, length = 50)
    private String userRolesCode;

}
