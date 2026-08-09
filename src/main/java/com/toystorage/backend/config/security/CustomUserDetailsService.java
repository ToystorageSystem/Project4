package com.toystorage.backend.config.security;

import com.toystorage.backend.entity.auth.RolePermissions;
import com.toystorage.backend.entity.auth.Roles;
import com.toystorage.backend.entity.auth.UserRoles;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.auth.PermissionStatus;
import com.toystorage.backend.repository.auth.UserRoleRepository;
import com.toystorage.backend.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Users user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found with email: " + email
                        )
                );

        List<UserRoles> userRoles =
                userRoleRepository.findByUser_Id(user.getId());

        Set<GrantedAuthority> authorities =
                new HashSet<>();

        for (UserRoles userRole : userRoles) {

            Roles role = userRole.getRole();

            if (role == null) {
                continue;
            }

            // =========================
            // ROLE
            // =========================

            authorities.add(
                    new SimpleGrantedAuthority(
                            "ROLE_" + role.getRoleCode()
                    )
            );

            // =========================
            // PERMISSIONS
            // =========================

            for (RolePermissions rolePermission :
                    role.getRolePermissions()) {

                if (rolePermission.getPermission() == null) {
                    continue;
                }

                if (rolePermission
                        .getPermission()
                        .getStatus()
                        != PermissionStatus.ACTIVE) {
                    continue;
                }

                String permissionCode =
                        rolePermission
                                .getPermission()
                                .getPermissionCode();

                authorities.add(
                        new SimpleGrantedAuthority(
                                permissionCode
                        )
                );
            }
        }

        return new CustomUserDetails(
                user,
                authorities
        );
    }
}