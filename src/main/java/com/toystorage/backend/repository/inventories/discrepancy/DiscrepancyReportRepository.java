package com.toystorage.backend.repository.inventories.discrepancy;

import com.toystorage.backend.enums.inventories.DiscrepancyReferenceType;
import com.toystorage.backend.enums.inventories.DiscrepancyStatus;
import com.toystorage.backend.enums.inventories.DiscrepancyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscrepancyReportRepository
        extends JpaRepository<
        com.toystorage.backend.entity.inventories.DiscrepancyReports,
        Long
        > {

    List<com.toystorage.backend.entity.inventories.DiscrepancyReports>
    findByWarehouseIdAndStatusInOrderByCreatedAtDesc(
            Long warehouseId,
            List<DiscrepancyStatus> statuses
    );

    List<com.toystorage.backend.entity.inventories.DiscrepancyReports>
    findByReferenceTypeAndReferenceId(
            DiscrepancyReferenceType referenceType,
            Long referenceId
    );

    boolean existsByReferenceTypeAndReferenceIdAndDiscrepancyTypeAndStatusIn(
            DiscrepancyReferenceType referenceType,
            Long referenceId,
            DiscrepancyType discrepancyType,
            List<DiscrepancyStatus> statuses
    );

    boolean existsByReferenceTypeAndReferenceIdAndProductIdAndDiscrepancyTypeAndStatusIn(
            DiscrepancyReferenceType referenceType,
            Long referenceId,
            Long productId,
            DiscrepancyType discrepancyType,
            List<DiscrepancyStatus> statuses
    );

    List<com.toystorage.backend.entity.inventories.DiscrepancyReports>
    findByReportedByIdOrderByCreatedAtDesc(
            Long userId
    );

    List<com.toystorage.backend.entity.inventories.DiscrepancyReports>
    findByWarehouseIdAndResponsiblePartyAndStatusOrderByCreatedAtDesc(
            Long warehouseId,
            String responsibleParty,
            DiscrepancyStatus status
    );

    List<com.toystorage.backend.entity.inventories.DiscrepancyReports>
    findByWarehouseIdAndStatusOrderByCreatedAtDesc(
            Long warehouseId,
            DiscrepancyStatus status
    );
}
