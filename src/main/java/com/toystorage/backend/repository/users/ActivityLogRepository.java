package com.toystorage.backend.repository.users;

import com.toystorage.backend.entity.users.ActivityLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityLogRepository
        extends JpaRepository<ActivityLogs, Long> {
}