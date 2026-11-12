package com.toystorage.backend.repository.auth;

import com.toystorage.backend.entity.auth.UserRoles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.toystorage.backend.enums.users.UserStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface UserRoleRepository
        extends JpaRepository<UserRoles, Long> {

    // Lấy tất cả role của user
    List<UserRoles> findByUser_Id(Long userId);

    // Kiểm tra user đã có role này chưa
    boolean existsByUser_IdAndRole_Id(
            Long userId,
            Long roleId
    );
    List<UserRoles> findByRole_Id(Long roleId);

    boolean existsByRole_Id(Long roleId);

    boolean existsByUser_IdAndRole_RoleCode(
            Long userId,
            String roleCode
    );

    @Query("""
    SELECT COUNT(DISTINCT ur.user.id)
    FROM UserRoles ur
    WHERE ur.role.roleCode = :roleCode
      AND ur.user.status = :status
    """)
    long countUsersByRoleCodeAndStatus(
            @Param("roleCode") String roleCode,
            @Param("status") UserStatus status
    );
}