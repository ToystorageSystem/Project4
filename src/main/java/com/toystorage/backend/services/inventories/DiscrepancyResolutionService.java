package com.toystorage.backend.services.inventories;

import com.toystorage.backend.entity.inventories.DiscrepancyReports;
import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.inventories.DiscrepancyStatus;
import com.toystorage.backend.enums.inventories.DiscrepancyType;
import com.toystorage.backend.enums.inventories.ResolutionAction;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.repository.inventories.DiscrepancyReportRepository;
import com.toystorage.backend.repository.receipts.GoodsReceiptItemRepository;
import com.toystorage.backend.repository.receipts.ReceiptInspectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscrepancyResolutionService {

    private final DiscrepancyReportRepository discrepancyReportRepository;
    private final GoodsReceiptItemRepository goodsReceiptItemRepository;
    private final ReceiptInspectionRepository receiptInspectionRepository;

    @Transactional
    public DiscrepancyReports acceptActualQuantity(
            DiscrepancyReports report,
            Users manager,
            String resolutionNote
    ) {

        validateCanAcceptActual(report);

        Long receiptId =
                report.getReferenceId();

        List<ReceiptInspections> inspections =
                getProblemInspections(receiptId);

        if (inspections.isEmpty()) {
            throw new BadRequest(
                    "No discrepancy inspection records found"
            );
        }

        for (ReceiptInspections inspection : inspections) {

            GoodsReceiptItems item =
                    goodsReceiptItemRepository
                            .findByGoodsReceiptIdAndProductId(
                                    receiptId,
                                    inspection.getProduct().getId()
                            )
                            .orElseThrow(() ->
                                    new NotFound(
                                            "Goods receipt item not found"
                                    )
                            );

            updateReceiptItem(
                    item,
                    inspection
            );
        }

        report.setResolutionAction(
                ResolutionAction.ADJUST_INVENTORY
        );

        report.setResolutionNote(
                resolutionNote
        );

        report.setResolvedBy(manager);

        report.setResolvedAt(
                LocalDateTime.now()
        );

        report.setResponsibleParty(
                "WAREHOUSE_MANAGER"
        );

        report.setStatus(
                DiscrepancyStatus.RESOLVED
        );

        report.setUpdatedAt(
                LocalDateTime.now()
        );

        return discrepancyReportRepository
                .save(report);
    }

    @Transactional
    public DiscrepancyReports requestRecount(
            DiscrepancyReports report,
            Users manager,
            String note
    ) {

        if (report.getStatus() == DiscrepancyStatus.RESOLVED
                || report.getStatus()
                == DiscrepancyStatus.CANCELLED) {

            throw new BadRequest(
                    "Resolved discrepancy cannot be recounted"
            );
        }

        report.setResolutionAction(
                ResolutionAction.RECOUNT
        );

        report.setResolutionNote(note);

        report.setReviewedBy(manager);

        report.setReviewedAt(
                LocalDateTime.now()
        );

        report.setResponsibleParty(
                "WAREHOUSE_STAFF"
        );

        report.setStatus(
                DiscrepancyStatus.INVESTIGATING
        );

        report.setUpdatedAt(
                LocalDateTime.now()
        );

        return discrepancyReportRepository
                .save(report);
    }

    private void updateReceiptItem(
            GoodsReceiptItems item,
            ReceiptInspections inspection
    ) {

        int actual =
                inspection.getActualQuantity();

        int damaged =
                item.getDamagedQuantity() == null
                        ? 0
                        : item.getDamagedQuantity();

        int accepted =
                Math.max(
                        actual - damaged,
                        0
                );

        int expected =
                inspection.getExpectedQuantity();

        item.setActualQuantity(actual);

        item.setAcceptedQuantity(
                accepted
        );

        item.setShortageQuantity(
                Math.max(
                        expected - actual,
                        0
                )
        );

        item.setSurplusQuantity(
                Math.max(
                        actual - expected,
                        0
                )
        );

        goodsReceiptItemRepository
                .save(item);
    }

    private void validateCanAcceptActual(
            DiscrepancyReports report
    ) {

        if (report.getStatus()
                != DiscrepancyStatus.OPEN
                && report.getStatus()
                != DiscrepancyStatus.INVESTIGATING) {

            throw new BadRequest(
                    "Discrepancy has already been resolved"
            );
        }

        if (report.getDiscrepancyType()
                == DiscrepancyType.WRONG_PRODUCT
                || report.getDiscrepancyType()
                == DiscrepancyType.DAMAGED) {

            throw new BadRequest(
                    "Wrong product or damaged goods "
                            + "cannot be resolved by accepting quantity"
            );
        }
    }

    private List<ReceiptInspections>
    getProblemInspections(Long receiptId) {

        return receiptInspectionRepository
                .findByGoodsReceiptId(receiptId)
                .stream()
                .filter(i ->
                        !"MATCHED".equals(
                                i.getInspectedResult().name()
                        )
                )
                .toList();
    }
}