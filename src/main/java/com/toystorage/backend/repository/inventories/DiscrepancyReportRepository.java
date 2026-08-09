package com.toystorage.backend.repository.inventories;



import com.toystorage.backend.entity.inventories.DiscrepancyReports;
import com.toystorage.backend.enums.inventories.DiscrepancyReferenceType;
import com.toystorage.backend.enums.inventories.DiscrepancyStatus;
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
}