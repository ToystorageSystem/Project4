package com.toystorage.backend.repository.users;

import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.enums.users.ActivityEntityType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository
        extends JpaRepository<ActivityLogs, Long> {

    /*
     * Lịch sử của một entity.
     *
     * Với User Management:
     *
     * entityType = USER
     * entityId   = userId
     */
    List<ActivityLogs>
    findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            ActivityEntityType entityType,
            Long entityId
    );


    /*
     * Xem toàn bộ hành động do một user thực hiện.
     */
    List<ActivityLogs>
    findByUser_IdOrderByCreatedAtDesc(
            Long userId
    );
}