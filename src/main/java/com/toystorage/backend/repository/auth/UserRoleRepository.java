package com.toystorage.backend.repository.auth;

import com.toystorage.backend.entity.auth.UserRoles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}