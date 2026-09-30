package com.toystorage.backend.repository.users;

import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.enums.users.ActivityEntityType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository
        extends JpaRepository<ActivityLogs, Long>,
        JpaSpecificationExecutor<ActivityLogs> {

    // =========================================================
    // ENTITY HISTORY
    // =========================================================

    List<ActivityLogs>
    findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            ActivityEntityType entityType,
            Long entityId
    );


    // =========================================================
    // ACTIONS PERFORMED BY USER
    // =========================================================

    List<ActivityLogs>
    findByUser_IdOrderByCreatedAtDesc(
            Long userId
    );
}