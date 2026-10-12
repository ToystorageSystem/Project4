package com.toystorage.backend.repository.users;

import com.toystorage.backend.entity.users.SuspiciousActivityAlerts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SuspiciousActivityAlertRepository
        extends JpaRepository<SuspiciousActivityAlerts, Long>,
        JpaSpecificationExecutor<SuspiciousActivityAlerts> {


    // =========================================================
    // FIND BY ALERT CODE
    // =========================================================

    Optional<SuspiciousActivityAlerts>
    findBySuspiciousActivityAlertCodeIgnoreCase(
            String suspiciousActivityAlertCode
    );


    // =========================================================
    // CHECK ALERT CODE
    // =========================================================

    boolean existsBySuspiciousActivityAlertCodeIgnoreCase(
            String suspiciousActivityAlertCode
    );
}