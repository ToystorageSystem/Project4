
package com.toystorage.backend.repository.businessstaff;

import com.toystorage.backend.entity.users.ActivityLogs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessSupplierPriceDecisionAuditRepository
        extends JpaRepository<ActivityLogs, Long> {

    @Query(value = """
            SELECT COUNT(*)
            FROM activity_logs al
            WHERE al.entity_type = 'PURCHASE_ORDER'
              AND al.entity_id = :purchaseOrderId
              AND al.action IN ('APPROVE', 'REJECT')
              AND (
                  CASE WHEN JSON_VALID(al.new_value)
                  THEN JSON_UNQUOTE(
                      JSON_EXTRACT(al.new_value, '$.eventType')
                  )
                  ELSE NULL END
              ) = 'SUPPLIER_PRICE_DECISION'
              AND (
                  CASE WHEN JSON_VALID(al.new_value)
                  THEN CAST(
                      JSON_UNQUOTE(
                          JSON_EXTRACT(
                              al.new_value,
                              '$.purchaseOrderItemId'
                          )
                      ) AS UNSIGNED
                  )
                  ELSE NULL END
              ) = :purchaseOrderItemId
              AND (
                  CASE WHEN JSON_VALID(al.new_value)
                  THEN JSON_UNQUOTE(
                      JSON_EXTRACT(
                          al.new_value,
                          '$.confirmedUnitPrice'
                      )
                  )
                  ELSE NULL END
              ) = :confirmedUnitPrice
            """, nativeQuery = true)
    long countExistingDecision(
            @Param("purchaseOrderId") Long purchaseOrderId,
            @Param("purchaseOrderItemId") Long purchaseOrderItemId,
            @Param("confirmedUnitPrice") String confirmedUnitPrice
    );

    @Modifying
    @Query(value = """
            INSERT INTO activity_logs (
                activity_logs_code,
                user_id,
                action,
                entity_type,
                entity_id,
                old_value,
                new_value,
                created_at
            )
            VALUES (
                :activityCode,
                :userId,
                :action,
                'PURCHASE_ORDER',
                :purchaseOrderId,
                :oldValue,
                :newValue,
                NOW(6)
            )
            """, nativeQuery = true)
    int insertDecisionLog(
            @Param("activityCode") String activityCode,
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("purchaseOrderId") Long purchaseOrderId,
            @Param("oldValue") String oldValue,
            @Param("newValue") String newValue
    );
}
