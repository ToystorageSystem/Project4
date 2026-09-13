package com.toystorage.backend.repository.receipts.incidents;

import com.toystorage.backend.entity.receipts.ReceivingIncidentReportItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceivingIncidentReportItemRepository
        extends JpaRepository<ReceivingIncidentReportItems, Long> {

    Optional<ReceivingIncidentReportItems> findByDiscrepancyReportId(Long discrepancyReportId);

    Optional<ReceivingIncidentReportItems> findByIdAndReportId(Long itemId, Long reportId);
}
