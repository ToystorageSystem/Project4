package com.toystorage.backend.repository.inventories.discrepancy;

import com.toystorage.backend.entity.inventories.DiscrepancyReports;
import com.toystorage.backend.enums.inventories.DiscrepancyReferenceType;
import com.toystorage.backend.enums.inventories.DiscrepancyStatus;
import com.toystorage.backend.enums.inventories.DiscrepancyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscrepancyReportRepository
        extends JpaRepository<DiscrepancyReports, Long> {

    List<DiscrepancyReports>
    findByWarehouseIdAndStatusInOrderByCreatedAtDesc(
            Long warehouseId,
            List<DiscrepancyStatus> statuses
    );

    List<DiscrepancyReports>
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

    List<DiscrepancyReports>
    findByReportedByIdOrderByCreatedAtDesc(
            Long userId
    );

    List<DiscrepancyReports>
    findByWarehouseIdAndResponsiblePartyAndStatusOrderByCreatedAtDesc(
            Long warehouseId,
            String responsibleParty,
            DiscrepancyStatus status
    );

    List<DiscrepancyReports>
    findByWarehouseIdAndStatusOrderByCreatedAtDesc(
            Long warehouseId,
            DiscrepancyStatus status
    );

    // Business Manager lấy các báo cáo được chuyển cho mình.
    List<DiscrepancyReports>
    findByResponsiblePartyOrderByCreatedAtDesc(
            String responsibleParty
    );
}