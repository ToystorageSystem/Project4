package com.toystorage.backend.repository.auth;

import com.toystorage.backend.entity.auth.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRepository
        extends JpaRepository<Roles, Long> {

    boolean existsByRoleCode(String roleCode);

    boolean existsByRoleNameIgnoreCase(String roleName);

    boolean existsByRoleNameIgnoreCaseAndIdNot(
            String roleName,
            Long id
    );

    @Query("""
        SELECT r
        FROM Roles r
        WHERE LOWER(r.roleCode)
                  LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(r.roleName)
                  LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(COALESCE(r.description, ''))
                  LIKE LOWER(CONCAT('%', :keyword, '%'))
        ORDER BY r.id ASC
        """)
    List<Roles> searchRoles(
            @Param("keyword") String keyword
    );
}