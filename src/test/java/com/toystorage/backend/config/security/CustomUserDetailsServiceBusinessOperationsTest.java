
package com.toystorage.backend.config.security;

import com.toystorage.backend.entity.auth.Permissions;
import com.toystorage.backend.entity.auth.RolePermissions;
import com.toystorage.backend.entity.auth.Roles;
import com.toystorage.backend.entity.auth.UserRoles;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.auth.PermissionStatus;
import com.toystorage.backend.enums.auth.RoleStatus;
import com.toystorage.backend.enums.users.UserStatus;
import com.toystorage.backend.repository.auth.UserRoleRepository;
import com.toystorage.backend.repository.users.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceBusinessOperationsTest {

    private static final String EMAIL =
            "bizops-test@example.invalid";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    // Gia lap tai khoan va cac role, khong doc MySQL.
    private Set<String> loadAuthorities(UserRoles... assignments) {

        Users user = Users.builder()
                .id(777L)
                .email(EMAIL)
                .password("test-password")
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(userRoleRepository.findByUser_Id(777L))
                .thenReturn(List.of(assignments));

        return service.loadUserByUsername(EMAIL)
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private UserRoles assign(Roles role) {
        return UserRoles.builder()
                .role(role)
                .build();
    }

    @Test
    void onlyBusinessOperationsRoleIsSufficient() {

        // Role moi duoc gan quyen phê duyet PO.
        Permissions permission = Permissions.builder()
                .permissionCode("PURCHASE_ORDER_APPROVE")
                .status(PermissionStatus.ACTIVE)
                .build();

        RolePermissions grant = RolePermissions.builder()
                .permission(permission)
                .build();

        Roles operations = Roles.builder()
                .roleCode("ROLE-BIZ-OPS")
                .status(RoleStatus.ACTIVE)
                .rolePermissions(Set.of(grant))
                .build();

        Set<String> authorities = loadAuthorities(
                assign(operations)
        );

        // Khong can role Staff hay Manager cu.
        assertEquals(
                Set.of(
                        "ROLE_ROLE-BIZ-OPS",
                        "PURCHASE_ORDER_APPROVE"
                ),
                authorities
        );
    }

    @Test
    void inactiveLegacyRolesAreIgnored() {

        Roles operations = Roles.builder()
                .roleCode("ROLE-BIZ-OPS")
                .status(RoleStatus.ACTIVE)
                .build();

        Roles staff = Roles.builder()
                .roleCode("ROLE-BIZ-STAFF")
                .status(RoleStatus.INACTIVE)
                .build();

        Roles manager = Roles.builder()
                .roleCode("ROLE-BIZ-MANAGER")
                .status(RoleStatus.INACTIVE)
                .build();

        // Gia lap sau khi ngung hai role cu.
        Set<String> authorities = loadAuthorities(
                assign(operations),
                assign(staff),
                assign(manager)
        );

        assertEquals(
                Set.of("ROLE_ROLE-BIZ-OPS"),
                authorities
        );
    }

    @Test
    void inactivePermissionIsNotGranted() {

        Permissions permission = Permissions.builder()
                .permissionCode("PURCHASE_ORDER_APPROVE")
                .status(PermissionStatus.INACTIVE)
                .build();

        RolePermissions grant = RolePermissions.builder()
                .permission(permission)
                .build();

        Roles operations = Roles.builder()
                .roleCode("ROLE-BIZ-OPS")
                .status(RoleStatus.ACTIVE)
                .rolePermissions(Set.of(grant))
                .build();

        Set<String> authorities = loadAuthorities(
                assign(operations)
        );

        // Role van hoat dong, nhung permission da inactive.
        assertEquals(
                Set.of("ROLE_ROLE-BIZ-OPS"),
                authorities
        );
    }
}
