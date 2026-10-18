
package com.toystorage.backend.repository.businessstaff;

import com.toystorage.backend.entity.users.ActivityLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessSupplierAuditRepository
        extends JpaRepository<ActivityLogs, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO activity_logs (
                user_id,
                action,
                entity_type,
                entity_id,
                old_value,
                new_value,
                created_at,
                activity_logs_code
            )
            VALUES (
                :userId,
                'UPDATE',
                'PURCHASE_ORDER',
                :purchaseOrderId,
                :oldValue,
                :newValue,
                NOW(6),
                :activityCode
            )
            """, nativeQuery = true)
    int insertFeedbackLog(
            @Param("userId") Long userId,
            @Param("purchaseOrderId") Long purchaseOrderId,
            @Param("oldValue") String oldValue,
            @Param("newValue") String newValue,
            @Param("activityCode") String activityCode
    );
}
