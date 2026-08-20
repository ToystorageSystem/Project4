package com.toystorage.backend.repository.inventories.discrepancy;

import com.toystorage.backend.entity.inventories.DiscrepancyItems;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscrepancyItemRepository
        extends JpaRepository<DiscrepancyItems, Long> {

    List<DiscrepancyItems>
    findByDiscrepancyReportId(
            Long discrepancyReportId
    );
}