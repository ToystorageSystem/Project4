package com.toystorage.backend.repository.deliveries;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.enums.deliveries.DeliveryStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryRepository
        extends JpaRepository<Deliveries, Long> {


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
    // TASK #13 - IN TRANSIT SHIPMENT DETAIL
    // =====================================================

    /**
     * Load Delivery cùng các quan hệ chính cần cho màn detail.
     *
     * Transfers/packages/products sẽ được lấy bằng repository
     * chuyên biệt để tránh fetch nhiều collection trong cùng
     * một query.
     */
    @Override
    @EntityGraph(attributePaths = {
            "manifest",
            "fromWarehouse",
            "toWarehouse",
            "driver",
            "handedOverBy"
    })
    Optional<Deliveries> findById(
            Long id
    );
}