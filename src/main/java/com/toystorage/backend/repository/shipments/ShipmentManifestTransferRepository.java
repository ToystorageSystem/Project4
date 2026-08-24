package com.toystorage.backend.repository.shipments;

import com.toystorage.backend.entity.shipments.ShipmentManifestTransfer;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipmentManifestTransferRepository
        extends JpaRepository<ShipmentManifestTransfer, Long> {

    /*
     * Luồng cũ:
     * tìm bảng kê chứa một phiếu điều chuyển.
     */
    Optional<ShipmentManifestTransfer>
    findByTransferId(
            Long transferId
    );

    /*
     * Task #14:
     * lấy toàn bộ phiếu điều chuyển thuộc một bảng kê.
     *
     * Fetch luôn StockTransfer và items để mapper có thể
     * tính totalProducts và totalRequestedQuantity.
     */
    @EntityGraph(attributePaths = {
            "transfer",
            "transfer.items"
    })
    List<ShipmentManifestTransfer>
    findByManifest_Id(
            Long manifestId
    );

    /*
     * Task #14:
     * đếm số phiếu điều chuyển thuộc bảng kê.
     */
    long countByManifest_Id(
            Long manifestId
    );
}