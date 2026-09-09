package com.toystorage.backend.repository.auth;

import com.toystorage.backend.entity.auth.UserRoles;
import com.toystorage.backend.enums.users.UserStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository
        extends JpaRepository<UserRoles, Long> {


    // =========================================================
    // FIND ROLES OF USER
    // =========================================================

    /*
     * Dùng cho:
     *
     * - Login
     * - User Management
     * - GET roles của User
     */
    List<UserRoles> findByUser_Id(
            Long userId
    );


    // =========================================================
    // FIND USERS OF ROLE
    // =========================================================

    /*
     * Dùng cho:
     *
     * - GET danh sách User thuộc Role
     * - Revoke session khi Role/Permission thay đổi
     */
    List<UserRoles> findByRole_Id(
            Long roleId
    );


    // =========================================================
    // CHECK ROLE IS USED BY USER
    // =========================================================

    /*
     * Dùng khi DELETE ROLE.
     *
     * Nếu Role đang được User sử dụng
     * thì không cho xóa.
     */
    boolean existsByRole_Id(
            Long roleId
    );


    // =========================================================
    // DELETE ALL ROLES OF USER
    // =========================================================

    /*
     * Giữ lại để tránh ảnh hưởng code cũ.
     */
    void deleteByUser_Id(
            Long userId
    );


    // =========================================================
    // CHECK USER HAS ROLE
    // =========================================================

    boolean existsByUser_IdAndRole_RoleCode(
            Long userId,
            String roleCode
    );


    // =========================================================
    // COUNT ACTIVE USERS BY ROLE
    // =========================================================

    /*
     * Dùng để bảo vệ Admin cuối cùng.
     */
    @Query("""
        SELECT COUNT(DISTINCT ur.user.id)
        FROM UserRoles ur
        WHERE ur.role.roleCode = :roleCode
          AND ur.user.status = :status
    """)
    long countUsersByRoleCodeAndStatus(

            @Param("roleCode")
            String roleCode,

            @Param("status")
            UserStatus status
    );
}