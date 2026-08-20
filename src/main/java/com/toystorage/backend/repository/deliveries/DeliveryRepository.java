package com.toystorage.backend.repository.deliveries;

import com.toystorage.backend.entity.deliveries.Deliveries;

import com.toystorage.backend.enums.deliveries.DeliveryStatus;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeliveryRepository
        extends JpaRepository<Deliveries, Long> {


    // =====================================================
    // DRIVER DELIVERY LIST
    // =====================================================

    List<Deliveries>
    findByDriverIdAndDeliveryStatusInOrderByCreatedAtDesc(
            Long driverId,
            Collection<DeliveryStatus> statuses
    );


    // =====================================================
    // DRIVER DELIVERY DETAIL
    // =====================================================

    Optional<Deliveries>
    findByIdAndDriverId(
            Long deliveryId,
            Long driverId
    );


    // =====================================================
    // WORKLOAD
    // =====================================================

    long countByDriverIdAndDeliveryStatusIn(
            Long driverId,
            Collection<DeliveryStatus> statuses
    );


    boolean existsByDriverIdAndDeliveryStatusIn(
            Long driverId,
            Collection<DeliveryStatus> statuses
    );


    // =====================================================
    // ACCEPT
    // =====================================================

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
        update Deliveries d
           set d.deliveryStatus = :newStatus,
               d.acceptedAt = :acceptedAt,
               d.rejectedAt = null,
               d.rejectionReason = null,
               d.updatedAt = :acceptedAt
         where d.id = :deliveryId
           and d.driver.id = :driverId
           and d.deliveryStatus = :currentStatus
    """)
    int acceptAssignedDelivery(

            @Param("deliveryId")
            Long deliveryId,

            @Param("driverId")
            Long driverId,

            @Param("currentStatus")
            DeliveryStatus currentStatus,

            @Param("newStatus")
            DeliveryStatus newStatus,

            @Param("acceptedAt")
            LocalDateTime acceptedAt
    );


    // =====================================================
    // REJECT
    // =====================================================

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
        update Deliveries d
           set d.deliveryStatus = :newStatus,
               d.rejectionReason = :reason,
               d.rejectedAt = :rejectedAt,
               d.updatedAt = :rejectedAt
         where d.id = :deliveryId
           and d.driver.id = :driverId
           and d.deliveryStatus = :currentStatus
    """)
    int rejectAssignedDelivery(

            @Param("deliveryId")
            Long deliveryId,

            @Param("driverId")
            Long driverId,

            @Param("reason")
            String reason,

            @Param("currentStatus")
            DeliveryStatus currentStatus,

            @Param("newStatus")
            DeliveryStatus newStatus,

            @Param("rejectedAt")
            LocalDateTime rejectedAt
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select d
    from Deliveries d
    where d.id = :deliveryId
""")
    Optional<Deliveries> findByIdForUpdate(
            @Param("deliveryId")
            Long deliveryId
    );
}