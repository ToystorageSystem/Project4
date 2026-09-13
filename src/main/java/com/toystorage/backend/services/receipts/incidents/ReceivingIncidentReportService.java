package com.toystorage.backend.services.receipts.incidents;

import com.toystorage.backend.dto.request.inventories.discrepancy.UpdateAcceptedQuantityRequest;
import com.toystorage.backend.dto.request.receipts.incidents.UpdateReceivingIncidentReportItemRequest;
import com.toystorage.backend.dto.request.receipts.incidents.UpdateReceivingIncidentReportRequest;
import com.toystorage.backend.dto.response.receipts.incidents.ReceivingIncidentReportResponse;
import com.toystorage.backend.entity.inventories.DiscrepancyReports;
import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import com.toystorage.backend.entity.receipts.ReceivingIncidentReportItems;
import com.toystorage.backend.entity.receipts.ReceivingIncidentReports;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.receipts.ReceivingIncidentSourceDecision;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.ReceivingIncidentReportNotFound;
import com.toystorage.backend.mapper.receipts.incidents.ReceivingIncidentReportMapper;
import com.toystorage.backend.repository.receipts.incidents.ReceivingIncidentReportItemRepository;
import com.toystorage.backend.repository.receipts.incidents.ReceivingIncidentReportRepository;
import com.toystorage.backend.repository.receipts.receiving.ReceiptInspectionRepository;
import com.toystorage.backend.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceivingIncidentReportService {

    private final ReceivingIncidentReportRepository incidentReportRepository;
    private final ReceivingIncidentReportItemRepository incidentReportItemRepository;
    private final ReceiptInspectionRepository receiptInspectionRepository;
    private final UserRepository userRepository;
    private final ReceivingIncidentReportMapper mapper;

    /*
     * ONE RECEIPT + WAREHOUSE_ACCEPT = ONE REPORT.
     * Multiple faulty products are appended into the same report.
     */
    @Transactional
    public ReceivingIncidentReportResponse generateFromWarehouseAccept(
            DiscrepancyReports discrepancy,
            GoodsReceiptItems receiptItem,
            Users warehouseManager,
            UpdateAcceptedQuantityRequest request
    ) {
        Long receiptId = receiptItem.getGoodsReceipt().getId();

        ReceivingIncidentReportItems existingItem =
                incidentReportItemRepository
                        .findByDiscrepancyReportId(discrepancy.getId())
                        .orElse(null);

        if (existingItem != null) {
            existingItem.setExpectedQuantity(receiptItem.getExpectedQuantity());
            existingItem.setActualQuantity(receiptItem.getActualQuantity());
            existingItem.setAcceptedQuantity(receiptItem.getAcceptedQuantity());
            existingItem.setDamagedQuantity(receiptItem.getDamagedQuantity());
            existingItem.setShortageQuantity(receiptItem.getShortageQuantity());
            existingItem.setSurplusQuantity(receiptItem.getSurplusQuantity());
            existingItem.setReason(requireReason(request.getReason()));

            incidentReportItemRepository.save(existingItem);

            ReceivingIncidentReports existingReport = existingItem.getReport();
            existingReport.setWarehouseManager(warehouseManager);
            existingReport.setUpdatedAt(LocalDateTime.now());

            return mapper.toResponse(
                    incidentReportRepository.save(existingReport)
            );
        }

        Users warehouseStaff =
                getLatestInspectionStaff(
                        receiptId,
                        receiptItem.getProduct().getId()
                );

        ReceivingIncidentReports report =
                incidentReportRepository
                        .findByGoodsReceiptIdAndSourceDecision(
                                receiptId,
                                ReceivingIncidentSourceDecision.WAREHOUSE_ACCEPT
                        )
                        .orElseGet(
                                () -> createNewWarehouseReport(
                                        receiptItem,
                                        warehouseStaff,
                                        warehouseManager
                                )
                        );

        report.setWarehouseManager(warehouseManager);

        if (report.getWarehouseStaff() == null) {
            report.setWarehouseStaff(warehouseStaff);
        }

        ReceivingIncidentReportItems item =
                ReceivingIncidentReportItems.builder()
                        .receivingIncidentReportItemsCode(generateItemCode())
                        .discrepancyReport(discrepancy)
                        .product(receiptItem.getProduct())
                        .discrepancyType(discrepancy.getDiscrepancyType())
                        .expectedQuantity(receiptItem.getExpectedQuantity())
                        .actualQuantity(receiptItem.getActualQuantity())
                        .acceptedQuantity(receiptItem.getAcceptedQuantity())
                        .damagedQuantity(receiptItem.getDamagedQuantity())
                        .shortageQuantity(receiptItem.getShortageQuantity())
                        .surplusQuantity(receiptItem.getSurplusQuantity())
                        .reason(requireReason(request.getReason()))
                        .build();

        report.addItem(item);
        report.setUpdatedAt(LocalDateTime.now());

        return mapper.toResponse(
                incidentReportRepository.save(report)
        );
    }

    @Transactional(readOnly = true)
    public Page<ReceivingIncidentReportResponse> getMyWarehouseReports(
            String keyword,
            Pageable pageable
    ) {
        Users currentUser = getCurrentUser();
        Long warehouseId = requireWarehouseId(currentUser);

        return incidentReportRepository
                .findByWarehouseId(warehouseId, pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReceivingIncidentReportResponse> getByReceipt(
            Long receiptId,
            Pageable pageable
    ) {
        Users currentUser = getCurrentUser();
        Long warehouseId = requireWarehouseId(currentUser);

        return incidentReportRepository
                .findByGoodsReceiptId(receiptId, pageable)
                .map(report -> {
                    if (
                            report.getWarehouse() == null ||
                            !warehouseId.equals(report.getWarehouse().getId())
                    ) {
                        throw new Forbidden(
                                "You cannot view incident reports from another warehouse"
                        );
                    }

                    return mapper.toResponse(report);
                });
    }

    @Transactional(readOnly = true)
    public ReceivingIncidentReportResponse getDetail(Long id) {
        ReceivingIncidentReports report = getReport(id);
        validateSameWarehouse(getCurrentUser(), report);
        return mapper.toResponse(report);
    }

    /*
     * No digital signature / confirmation.
     * Manager can edit the stored report before printing.
     */
    @Transactional
    public ReceivingIncidentReportResponse updateReport(
            Long id,
            UpdateReceivingIncidentReportRequest request
    ) {
        ReceivingIncidentReports report = getReport(id);
        Users manager = getCurrentUser();

        validateSameWarehouse(manager, report);

        report.setWarehouseManager(manager);

        report.setPenaltyAction(
                normalizeNullable(request.getPenaltyAction())
        );

        report.setManagerNote(
                normalizeNullable(request.getManagerNote())
        );

        report.setUpdatedAt(LocalDateTime.now());

        return mapper.toResponse(
                incidentReportRepository.save(report)
        );
    }

    @Transactional
    public ReceivingIncidentReportResponse updateItem(
            Long reportId,
            Long itemId,
            UpdateReceivingIncidentReportItemRequest request
    ) {
        ReceivingIncidentReports report = getReport(reportId);
        Users manager = getCurrentUser();

        validateSameWarehouse(manager, report);

        ReceivingIncidentReportItems item =
                incidentReportItemRepository
                        .findByIdAndReportId(itemId, reportId)
                        .orElseThrow(
                                () -> new ReceivingIncidentReportNotFound(
                                        "Incident report item not found: " + itemId
                                )
                        );

        item.setReason(requireReason(request.getReason()));

        incidentReportItemRepository.save(item);

        report.setWarehouseManager(manager);
        report.setUpdatedAt(LocalDateTime.now());

        return mapper.toResponse(
                incidentReportRepository.save(report)
        );
    }

    private ReceivingIncidentReports createNewWarehouseReport(
            GoodsReceiptItems receiptItem,
            Users warehouseStaff,
            Users warehouseManager
    ) {
        LocalDateTime now = LocalDateTime.now();

        return ReceivingIncidentReports.builder()
                .reportCode(generateReportCode())
                .goodsReceipt(receiptItem.getGoodsReceipt())
                .warehouse(receiptItem.getGoodsReceipt().getWarehouse())
                .warehouseStaff(warehouseStaff)
                .warehouseManager(warehouseManager)
                .sourceDecision(
                        ReceivingIncidentSourceDecision.WAREHOUSE_ACCEPT
                )
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Users getLatestInspectionStaff(
            Long receiptId,
            Long productId
    ) {
        List<ReceiptInspections> inspections =
                receiptInspectionRepository
                        .findByGoodsReceiptId(receiptId)
                        .stream()
                        .filter(
                                inspection ->
                                        inspection.getProduct() != null &&
                                        inspection.getProduct()
                                                .getId()
                                                .equals(productId)
                        )
                        .sorted(
                                Comparator.comparing(
                                        ReceiptInspections::getInspectedAt
                                ).reversed()
                        )
                        .toList();

        if (
                inspections.isEmpty() ||
                inspections.get(0).getInspectedBy() == null
        ) {
            throw new ReceivingIncidentReportNotFound(
                    "Inspection staff could not be determined for receipt "
                            + receiptId
                            + ", product "
                            + productId
            );
        }

        return inspections.get(0).getInspectedBy();
    }

    private ReceivingIncidentReports getReport(Long id) {
        return incidentReportRepository
                .findDetailById(id)
                .orElseThrow(
                        () -> new ReceivingIncidentReportNotFound(
                                "Receiving incident report not found: " + id
                        )
                );
    }

    private Users getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (
                authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())
        ) {
            throw new Forbidden("User is not authenticated");
        }

        String email = authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(
                        () -> new ReceivingIncidentReportNotFound(
                                "Authenticated user not found: " + email
                        )
                );
    }

    private Long requireWarehouseId(Users user) {
        if (user.getWarehouse() == null) {
            throw new Forbidden(
                    "User is not assigned to any warehouse"
            );
        }

        return user.getWarehouse().getId();
    }

    private void validateSameWarehouse(
            Users user,
            ReceivingIncidentReports report
    ) {
        Long warehouseId = requireWarehouseId(user);

        if (
                report.getWarehouse() == null ||
                !warehouseId.equals(report.getWarehouse().getId())
        ) {
            throw new Forbidden(
                    "You cannot access an incident report from another warehouse"
            );
        }
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequest("Reason is required");
        }

        return reason.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private String generateReportCode() {
        return "BB-WH-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }

    private String generateItemCode() {
        return "BBI-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }
}
