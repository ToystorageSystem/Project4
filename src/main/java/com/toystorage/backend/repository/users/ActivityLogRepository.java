package com.toystorage.backend.repository.users;

import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository
        extends JpaRepository<ActivityLogs, Long>,
        JpaSpecificationExecutor<ActivityLogs> {

    Page<ActivityLogs> findByEntityTypeAndEntityId(
            ActivityEntityType entityType,
            Long entityId,
            Pageable pageable
    );

    List<ActivityLogs>
    findByEntityTypeAndEntityIdAndActionOrderByCreatedAtDesc(
            ActivityEntityType entityType,
            Long entityId,
            ActivityAction action
    );

    List<ActivityLogs>
    findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            ActivityEntityType entityType,
            Long entityId
    );

    List<ActivityLogs>
    findByUser_IdOrderByCreatedAtDesc(
            Long userId
    );
}