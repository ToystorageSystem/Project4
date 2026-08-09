package com.toystorage.backend.repository.auth;

import com.toystorage.backend.entity.auth.UserRoles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository
        extends JpaRepository<UserRoles, Long> {

    List<UserRoles> findByUser_Id(Long userId);
}