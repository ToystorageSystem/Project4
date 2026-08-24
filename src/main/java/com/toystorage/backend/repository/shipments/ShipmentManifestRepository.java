package com.toystorage.backend.repository.shipments;

import com.toystorage.backend.entity.shipments.ShipmentManifests;
import com.toystorage.backend.enums.deliveries.DeliveryStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ShipmentManifestRepository
        extends JpaRepository<ShipmentManifests, Long> {


    // =====================================================
    // BUSINESS - MANIFEST LIST
    // =====================================================

    /**
     * Business Staff xem danh sách bảng kê đi hàng.
     *
     * Hỗ trợ:
     * - tìm theo mã bảng kê
     * - tìm theo mã phiếu điều chuyển
     * - lọc điểm xuất
     * - lọc điểm nhận
     * - lọc trạng thái vận chuyển
     * - lọc khoảng thời gian tạo bảng kê
     * - phân trang
     */
    @EntityGraph(attributePaths = {
            "fromWarehouse",
            "toWarehouse",
            "createdBy"
    })
    @Query("""
            select m
            from ShipmentManifests m
            where
                (
                    :keyword is null
                    or trim(:keyword) = ''
                    or lower(m.manifestCode)
                        like lower(concat('%', :keyword, '%'))
                    or exists (
                        select mt.id
                        from ShipmentManifestTransfer mt
                        where mt.manifest = m
                          and lower(mt.transfer.transferCode)
                              like lower(concat('%', :keyword, '%'))
                    )
                )

                and (
                    :fromWarehouseId is null
                    or m.fromWarehouse.id = :fromWarehouseId
                )

                and (
                    :toWarehouseId is null
                    or m.toWarehouse.id = :toWarehouseId
                )

                and (
                    :createdFrom is null
                    or m.createdAt >= :createdFrom
                )

                and (
                    :createdToExclusive is null
                    or m.createdAt < :createdToExclusive
                )

                and (
                    :transportStatus is null

                    or exists (
                        select d.id
                        from Deliveries d
                        where d.manifest = m
                          and d.deliveryStatus = :transportStatus
                          and d.createdAt = (
                              select max(d2.createdAt)
                              from Deliveries d2
                              where d2.manifest = m
                          )
                    )
                )

            order by m.createdAt desc
            """)
    Page<ShipmentManifests> searchShipmentManifests(
            @Param("keyword")
            String keyword,

            @Param("fromWarehouseId")
            Long fromWarehouseId,

            @Param("toWarehouseId")
            Long toWarehouseId,

            @Param("transportStatus")
            DeliveryStatus transportStatus,

            @Param("createdFrom")
            LocalDateTime createdFrom,

            @Param("createdToExclusive")
            LocalDateTime createdToExclusive,

            Pageable pageable
    );


    // =====================================================
    // BUSINESS - MANIFEST DETAIL
    // =====================================================

    /**
     * Lấy thông tin chính của bảng kê cùng các quan hệ cần
     * để map response mà không gặp LazyInitializationException.
     *
     * Transfers và packages sẽ được lấy bằng repository riêng
     * vì đây là collection và còn cần map sâu xuống sản phẩm.
     */
    @Override
    @EntityGraph(attributePaths = {
            "fromWarehouse",
            "toWarehouse",
            "createdBy",
            "approvedBy"
    })
    Optional<ShipmentManifests> findById(
            Long id
    );
}