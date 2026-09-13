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

    List<DiscrepancyReports>
    findByReportedByIdOrderByCreatedAtDesc(
            Long userId
    );

}