package com.toystorage.backend.services.inventories.discrepancy;

import com.toystorage.backend.dto.request.inventories.discrepancy.EscalateDiscrepancyRequest;
import com.toystorage.backend.dto.request.inventories.discrepancy.ResolveDiscrepancyRequest;
import com.toystorage.backend.dto.response.inventories.discrepancy.DiscrepancyItemResponse;
import com.toystorage.backend.dto.response.inventories.discrepancy.DiscrepancyReportResponse;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptItemRepository;
import com.toystorage.backend.services.receipts.incidents.ReceivingIncidentReportService;
import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.inventories.DiscrepancyReports;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseTaskClaim;
import com.toystorage.backend.enums.inventories.DiscrepancyReferenceType;
import com.toystorage.backend.enums.inventories.DiscrepancyStatus;
import com.toystorage.backend.enums.inventories.DiscrepancyType;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.mapper.inventories.discrepancy.DiscrepancyReportMapper;
import com.toystorage.backend.repository.inventories.discrepancy.DiscrepancyReportRepository;
import com.toystorage.backend.repository.receipts.receiving.ReceiptInspectionRepository;
import com.toystorage.backend.dto.request.inventories.discrepancy.UpdateAcceptedQuantityRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceivingDiscrepancyService {

    private final DiscrepancyReportRepository discrepancyReportRepository;
    private final ReceiptInspectionRepository receiptInspectionRepository;
    private final DiscrepancyReportMapper discrepancyReportMapper;
    private final DiscrepancyValidationService validationService;
    private final DiscrepancyResolutionService resolutionService;
    private final GoodsReceiptItemRepository goodsReceiptItemRepository;
    private final ReceivingIncidentReportService receivingIncidentReportService;

    @Transactional(readOnly = true)
    public List<DiscrepancyReportResponse> getReceivingDiscrepancies() {
        Users manager = validationService.getCurrentUser();
        Long warehouseId = validationService.getWarehouseId(manager);

        return discrepancyReportRepository
                .findByWarehouseIdAndStatusInOrderByCreatedAtDesc(
                        warehouseId,
                        List.of(DiscrepancyStatus.OPEN, DiscrepancyStatus.INVESTIGATING))
                .stream()
                .filter(r -> r.getReferenceType() == DiscrepancyReferenceType.GOODS_RECEIPT)
                .map(this::buildResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DiscrepancyReportResponse getDiscrepancy(Long discrepancyId) {
        DiscrepancyReports report = getReport(discrepancyId);
        Users manager = validationService.getCurrentUser();
        validationService.validateSameWarehouse(manager, report);
        validationService.validateGoodsReceipt(report);
        return buildResponse(report);
    }

    @Transactional
    public DiscrepancyReportResponse startHandling(Long discrepancyId) {
        DiscrepancyReports report = getReport(discrepancyId);
        Users manager = validationService.getCurrentUser();
        validationService.validateSameWarehouse(manager, report);
        validationService.validateGoodsReceipt(report);

        if (report.getStatus() != DiscrepancyStatus.OPEN) {
            throw new BadRequest("Only OPEN discrepancy can be handled");
        }

        report.setStatus(DiscrepancyStatus.INVESTIGATING);
        report.setReviewedBy(manager);
        report.setReviewedAt(LocalDateTime.now());
        report.setResponsibleParty("WAREHOUSE_MANAGER");
        return buildResponse(discrepancyReportRepository.save(report));
    }

    @Transactional
    public DiscrepancyReportResponse acceptActualQuantity(
            Long discrepancyId,
            ResolveDiscrepancyRequest request) {

        DiscrepancyReports report = getReport(discrepancyId);
        Users manager = validationService.getCurrentUser();
        validationService.validateSameWarehouse(manager, report);
        validationService.validateGoodsReceipt(report);

        return buildResponse(resolutionService.acceptActualQuantity(
                report, manager, request.getResolutionNote()));
    }

    @Transactional
    public DiscrepancyReportResponse requestRecount(
            Long discrepancyId,
            ResolveDiscrepancyRequest request) {

        DiscrepancyReports report = getReport(discrepancyId);
        Users manager = validationService.getCurrentUser();
        validationService.validateSameWarehouse(manager, report);
        validationService.validateGoodsReceipt(report);

        return buildResponse(resolutionService.requestRecount(
                report, manager, request.getResolutionNote()));
    }

    @Transactional
    public DiscrepancyReportResponse escalateToBusinessManager(
            Long discrepancyId,
            EscalateDiscrepancyRequest request) {

        DiscrepancyReports report = getReport(discrepancyId);
        Users manager = validationService.getCurrentUser();
        validationService.validateSameWarehouse(manager, report);
        validationService.validateGoodsReceipt(report);

        if (report.getStatus() == DiscrepancyStatus.RESOLVED
                || report.getStatus() == DiscrepancyStatus.CANCELLED) {
            throw new BadRequest("Resolved discrepancy cannot be escalated");
        }

        report.setReviewedBy(manager);
        report.setReviewedAt(LocalDateTime.now());
        report.setStatus(DiscrepancyStatus.INVESTIGATING);
        report.setResponsibleParty("BUSINESS_MANAGER");
        report.setResolutionNote(request.getReason().trim());

        return buildResponse(discrepancyReportRepository.save(report));
    }

    @Transactional
    public void syncFromFinishedInspection(
            GoodsReceipts receipt,
            WarehouseTaskClaim taskClaim,
            Users staff) {

        if (receipt == null || receipt.getId() == null) {
            throw new BadRequest("Goods receipt is required");
        }
        if (taskClaim == null || taskClaim.getId() == null) {
            throw new BadRequest("Receiving task claim is required");
        }

        List<ReceiptInspections> problems =
                receiptInspectionRepository.findByTaskClaimId(taskClaim.getId())
                        .stream()
                        .filter(i -> i.getInspectedResult() != null
                                && !"MATCHED".equals(i.getInspectedResult().name()))
                        .toList();

        // A new attempt supersedes unresolved reports from the previous attempt.
        List<DiscrepancyReports> previous =
                discrepancyReportRepository.findByReferenceTypeAndReferenceId(
                        DiscrepancyReferenceType.GOODS_RECEIPT, receipt.getId());

        for (DiscrepancyReports old : previous) {
            if (old.getStatus() == DiscrepancyStatus.OPEN
                    || old.getStatus() == DiscrepancyStatus.INVESTIGATING) {
                old.setStatus(DiscrepancyStatus.RESOLVED);
                old.setResponsibleParty("SYSTEM");
                old.setResolutionNote("Superseded by a newer receiving inspection attempt");
                old.setResolvedBy(staff);
                old.setResolvedAt(LocalDateTime.now());
                discrepancyReportRepository.save(old);
            }
        }

        for (ReceiptInspections inspection : problems) {
            DiscrepancyType type = toDiscrepancyType(
                    inspection.getInspectedResult().name());

            boolean exists =
                    discrepancyReportRepository
                            .existsByReferenceTypeAndReferenceIdAndDiscrepancyTypeAndStatusIn(
                                    DiscrepancyReferenceType.GOODS_RECEIPT,
                                    receipt.getId(),
                                    type,
                                    List.of(DiscrepancyStatus.OPEN,
                                            DiscrepancyStatus.INVESTIGATING));

            if (exists) {
                continue;
            }

            String code = generateReportCode();

            DiscrepancyReports report = DiscrepancyReports.builder()
                    .reportCode(code)
                    .discrepancyReportsCode(code)
                    .referenceType(DiscrepancyReferenceType.GOODS_RECEIPT)
                    .referenceId(receipt.getId())
                    .warehouse(receipt.getWarehouse())
                    .discrepancyType(type)
                    .status(DiscrepancyStatus.OPEN)
                    .reportedBy(staff)
                    .responsibleParty("WAREHOUSE_MANAGER")
                    .description(buildDescription(inspection))
                    .build();

            discrepancyReportRepository.save(report);
        }
    }

    @Transactional(readOnly = true)
    public void assertCanConfirm(Long receiptId) {
        boolean unresolved =
                discrepancyReportRepository
                        .findByReferenceTypeAndReferenceId(
                                DiscrepancyReferenceType.GOODS_RECEIPT,
                                receiptId)
                        .stream()
                        .anyMatch(r ->
                                r.getStatus() == DiscrepancyStatus.OPEN
                                        || r.getStatus() == DiscrepancyStatus.INVESTIGATING);

        if (unresolved) {
            throw new BadRequest(
                    "Receiving cannot be confirmed while discrepancy is unresolved");
        }
    }

    @Transactional
    public void markReinspectionRequested(
            Long receiptId,
            Users manager,
            String reason) {

        if (reason == null || reason.isBlank()) {
            throw new BadRequest("Re-inspection reason is required");
        }

        List<DiscrepancyReports> reports =
                discrepancyReportRepository.findByReferenceTypeAndReferenceId(
                        DiscrepancyReferenceType.GOODS_RECEIPT, receiptId);

        for (DiscrepancyReports report : reports) {
            if (report.getStatus() == DiscrepancyStatus.OPEN
                    || report.getStatus() == DiscrepancyStatus.INVESTIGATING) {

                validationService.validateSameWarehouse(manager, report);
                validationService.validateGoodsReceipt(report);

                report.setStatus(DiscrepancyStatus.INVESTIGATING);
                report.setReviewedBy(manager);
                report.setReviewedAt(LocalDateTime.now());
                report.setResponsibleParty("WAREHOUSE_STAFF");
                report.setResolutionNote(reason.trim());
                discrepancyReportRepository.save(report);
            }
        }
    }

    private DiscrepancyType toDiscrepancyType(String result) {
        try {
            return DiscrepancyType.valueOf(result);
        } catch (IllegalArgumentException ex) {
            return DiscrepancyType.OTHER;
        }
    }

    private String buildDescription(ReceiptInspections inspection) {
        String product = inspection.getProduct() != null
                ? inspection.getProduct().getName()
                : "Unknown product";

        return "Receiving discrepancy for " + product
                + ". Expected: " + inspection.getExpectedQuantity()
                + ", actual: " + inspection.getActualQuantity()
                + ", result: " + inspection.getInspectedResult().name();
    }

    private String generateReportCode() {
        return "DR-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }

    private DiscrepancyReportResponse buildResponse(DiscrepancyReports report) {
        List<DiscrepancyItemResponse> items = List.of();

        if (report.getReferenceType() == DiscrepancyReferenceType.GOODS_RECEIPT) {
            items = getProblemInspections(report.getReferenceId())
                    .stream()
                    .map(discrepancyReportMapper::toItemResponse)
                    .toList();
        }

        Users reviewedBy = report.getReviewedBy();
        Users resolvedBy = report.getResolvedBy();

        return DiscrepancyReportResponse.builder()
                .id(report.getId())
                .reportCode(report.getReportCode())
                .discrepancyType(report.getDiscrepancyType().name())
                .status(report.getStatus().name())
                .description(report.getDescription())
                .goodsReceiptId(report.getReferenceId())
                .warehouseId(report.getWarehouse().getId())
                .responsibleParty(report.getResponsibleParty())
                .resolutionAction(report.getResolutionAction() != null
                        ? report.getResolutionAction().name() : null)
                .resolutionNote(report.getResolutionNote())
                .reviewedBy(reviewedBy != null ? reviewedBy.getId() : null)
                .reviewedByName(reviewedBy != null ? reviewedBy.getName() : null)
                .reviewedAt(report.getReviewedAt())
                .resolvedBy(resolvedBy != null ? resolvedBy.getId() : null)
                .resolvedByName(resolvedBy != null ? resolvedBy.getName() : null)
                .resolvedAt(report.getResolvedAt())
                .createdAt(report.getCreatedAt())
                .items(items)
                .build();
    }

    private List<ReceiptInspections> getProblemInspections(Long receiptId) {
        return receiptInspectionRepository.findByGoodsReceiptId(receiptId)
                .stream()
                .filter(i -> i.getInspectedResult() != null
                        && !"MATCHED".equals(i.getInspectedResult().name()))
                .toList();
    }

    private DiscrepancyReports getReport(Long discrepancyId) {
        return discrepancyReportRepository.findById(discrepancyId)
                .orElseThrow(() ->
                        new NotFound("Discrepancy report not found: " + discrepancyId));
    }
    @Transactional
    public DiscrepancyReportResponse updateAcceptedQuantity(
            Long discrepancyId,
            Long productId,
            UpdateAcceptedQuantityRequest request
    ) {

        DiscrepancyReports report =
                getReport(discrepancyId);

        Users manager =
                validationService.getCurrentUser();

        validationService.validateSameWarehouse(
                manager,
                report
        );

        validationService.validateGoodsReceipt(
                report
        );

        if (report.getStatus() == DiscrepancyStatus.RESOLVED
                || report.getStatus() == DiscrepancyStatus.CANCELLED) {

            throw new BadRequest(
                    "Resolved discrepancy cannot be modified"
            );
        }

        if (report.getReferenceType()
                != DiscrepancyReferenceType.GOODS_RECEIPT) {

            throw new BadRequest(
                    "Discrepancy is not associated with a goods receipt"
            );
        }

        Integer acceptedQuantity =
                request.getAcceptedQuantity();

        if (acceptedQuantity == null
                || acceptedQuantity < 0) {

            throw new BadRequest(
                    "Accepted quantity cannot be negative"
            );
        }

        String reason =
                request.getReason() == null
                        ? ""
                        : request.getReason().trim();

        if (reason.isEmpty()) {
            throw new BadRequest(
                    "Resolution reason is required"
            );
        }

        Long receiptId =
                report.getReferenceId();

        GoodsReceiptItems receiptItem =
                goodsReceiptItemRepository
                        .findByGoodsReceiptIdAndProductId(
                                receiptId,
                                productId
                        )
                        .orElseThrow(
                                () -> new NotFound(
                                        "Goods receipt item not found"
                                                + " for receipt "
                                                + receiptId
                                                + " and product "
                                                + productId
                                )
                        );

        /*
         * IMPORTANT:
         *
         * actualQuantity:
         * Staff inspection result.
         * DO NOT CHANGE.
         *
         * acceptedQuantity:
         * Manager decision.
         */
        receiptItem.setAcceptedQuantity(
                acceptedQuantity
        );

        goodsReceiptItemRepository.save(
                receiptItem
        );

        LocalDateTime now =
                LocalDateTime.now();

        /*
         * Manager has reviewed and resolved
         * this discrepancy.
         */
        report.setReviewedBy(manager);

        if (report.getReviewedAt() == null) {
            report.setReviewedAt(now);
        }

        report.setResolvedBy(manager);
        report.setResolvedAt(now);

        report.setResolutionNote(reason);

        report.setResponsibleParty(
                "WAREHOUSE_MANAGER"
        );

        report.setStatus(
                DiscrepancyStatus.RESOLVED
        );

        DiscrepancyReports saved =
                discrepancyReportRepository.save(
                        report
                );
        receivingIncidentReportService
                .generateFromWarehouseAccept(
                        saved,
                        receiptItem,
                        manager,
                        request
                );

        return buildResponse(saved);
    }
}
