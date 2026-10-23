package com.toystorage.backend.repository.deliveries;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
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

    Optional<Deliveries>
    findTopByManifestIdOrderByCreatedAtDesc(
            Long manifestId
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


    // Methods restored for legacy business APIs without replacing existing repository logic.

    // =====================================================
        // TASK #13 - IN TRANSIT SHIPMENT LIST
        // =====================================================

        /**
         * Business Staff xem danh sách các chuyến vận chuyển.
         *
         * Hỗ trợ tìm kiếm theo:
         * - mã chuyến giao
         * - mã bảng kê
         * - mã phiếu điều chuyển
         * - mã kiện hàng
         *
         * Hỗ trợ lọc theo:
         * - điểm xuất
         * - điểm nhận
         * - trạng thái vận chuyển
         */
        @EntityGraph(attributePaths = {
                "manifest",
                "fromWarehouse",
                "toWarehouse",
                "driver"
        })
        @Query("""
                select d
                from Deliveries d
                left join d.manifest m
                where
                    (
                        :keyword is null
                        or trim(:keyword) = ''

                        or lower(d.shipmentCode)
                            like lower(concat('%', :keyword, '%'))

                        or lower(m.manifestCode)
                            like lower(concat('%', :keyword, '%'))

                        or exists (
                            select mt.id
                            from ShipmentManifestTransfer mt
                            where mt.manifest = m
                              and lower(mt.transfer.transferCode)
                                  like lower(concat('%', :keyword, '%'))
                        )

                        or exists (
                            select dp.id
                            from DeliveryPackages dp
                            where dp.delivery = d
                              and lower(dp.packageEntity.packagesCode)
                                  like lower(concat('%', :keyword, '%'))
                        )
                    )

                    and (
                        :fromWarehouseId is null
                        or d.fromWarehouse.id = :fromWarehouseId
                    )

                    and (
                        :toWarehouseId is null
                        or d.toWarehouse.id = :toWarehouseId
                    )

                    and (
                        :deliveryStatus is null
                        or d.deliveryStatus = :deliveryStatus
                    )

                order by d.createdAt desc
                """)
        Page<Deliveries> searchInTransitShipments(
                @Param("keyword")
                String keyword,

                @Param("fromWarehouseId")
                Long fromWarehouseId,

                @Param("toWarehouseId")
                Long toWarehouseId,

                @Param("deliveryStatus")
                DeliveryStatus deliveryStatus,

                Pageable pageable
        );

    // =====================================================
        // EXISTING - LATEST DELIVERY OF MANIFEST
        // =====================================================

        /**
         * Lấy chuyến vận chuyển mới nhất của một bảng kê.
         *
         * Method này đang được ShipmentManifestQueryService sử dụng,
         * vì vậy giữ nguyên để không ảnh hưởng chức năng cũ.
         */
        Optional<Deliveries> findFirstByManifest_IdOrderByCreatedAtDesc(
                Long manifestId
        );
}