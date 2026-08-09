package com.toystorage.backend.services.inventories;

import com.toystorage.backend.dto.request.inventories.EscalateDiscrepancyRequest;
import com.toystorage.backend.dto.request.inventories.ResolveDiscrepancyRequest;
import com.toystorage.backend.dto.response.inventories.DiscrepancyItemResponse;
import com.toystorage.backend.dto.response.inventories.DiscrepancyReportResponse;

import com.toystorage.backend.entity.inventories.DiscrepancyReports;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.inventories.DiscrepancyReferenceType;
import com.toystorage.backend.enums.inventories.DiscrepancyStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.mapper.inventories.DiscrepancyReportMapper;

import com.toystorage.backend.repository.inventories.DiscrepancyReportRepository;
import com.toystorage.backend.repository.receipts.ReceiptInspectionRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceivingDiscrepancyService {

    private final DiscrepancyReportRepository
            discrepancyReportRepository;

    private final ReceiptInspectionRepository
            receiptInspectionRepository;

    private final DiscrepancyReportMapper
            discrepancyReportMapper;

    private final DiscrepancyValidationService
            validationService;

    private final DiscrepancyResolutionService
            resolutionService;


    // =====================================================
    // DANH SÁCH BÁO CÁO CẦN XỬ LÝ
    // =====================================================

    @Transactional(readOnly = true)
    public List<DiscrepancyReportResponse>
    getReceivingDiscrepancies() {

        Users manager =
                validationService.getCurrentUser();

        Long warehouseId =
                validationService.getWarehouseId(
                        manager
                );

        List<DiscrepancyReports> reports =
                discrepancyReportRepository
                        .findByWarehouseIdAndStatusInOrderByCreatedAtDesc(
                                warehouseId,
                                List.of(
                                        DiscrepancyStatus.OPEN,
                                        DiscrepancyStatus.INVESTIGATING
                                )
                        );

        return reports.stream()

                .filter(report ->
                        report.getReferenceType()
                                == DiscrepancyReferenceType.GOODS_RECEIPT
                )

                .map(this::buildResponse)

                .toList();
    }


    // =====================================================
    // XEM CHI TIẾT
    // =====================================================

    @Transactional(readOnly = true)
    public DiscrepancyReportResponse getDiscrepancy(
            Long discrepancyId
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

        return buildResponse(report);
    }


    // =====================================================
    // BẮT ĐẦU XỬ LÝ
    // OPEN -> INVESTIGATING
    // =====================================================

    @Transactional
    public DiscrepancyReportResponse startHandling(
            Long discrepancyId
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

        if (report.getStatus()
                != DiscrepancyStatus.OPEN) {

            throw new BadRequest(
                    "Only OPEN discrepancy can be handled"
            );
        }

        report.setStatus(
                DiscrepancyStatus.INVESTIGATING
        );

        report.setReviewedBy(
                manager
        );

        report.setReviewedAt(
                LocalDateTime.now()
        );

        report.setResponsibleParty(
                "WAREHOUSE_MANAGER"
        );

        report.setUpdatedAt(
                LocalDateTime.now()
        );

        DiscrepancyReports saved =
                discrepancyReportRepository.save(
                        report
                );

        return buildResponse(saved);
    }


    // =====================================================
    // CHẤP NHẬN SỐ LƯỢNG THỰC TẾ
    // =====================================================

    @Transactional
    public DiscrepancyReportResponse acceptActualQuantity(
            Long discrepancyId,
            ResolveDiscrepancyRequest request
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

        DiscrepancyReports saved =
                resolutionService
                        .acceptActualQuantity(
                                report,
                                manager,
                                request.getResolutionNote()
                        );

        return buildResponse(saved);
    }


    // =====================================================
    // YÊU CẦU KIỂM LẠI
    // =====================================================

    @Transactional
    public DiscrepancyReportResponse requestRecount(
            Long discrepancyId,
            ResolveDiscrepancyRequest request
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

        DiscrepancyReports saved =
                resolutionService
                        .requestRecount(
                                report,
                                manager,
                                request.getResolutionNote()
                        );

        return buildResponse(saved);
    }


    // =====================================================
    // CHUYỂN BUSINESS MANAGER
    // =====================================================

    @Transactional
    public DiscrepancyReportResponse escalateToBusinessManager(
            Long discrepancyId,
            EscalateDiscrepancyRequest request
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

        if (report.getStatus()
                == DiscrepancyStatus.RESOLVED
                || report.getStatus()
                == DiscrepancyStatus.CANCELLED) {

            throw new BadRequest(
                    "Resolved discrepancy cannot be escalated"
            );
        }

        report.setReviewedBy(
                manager
        );

        report.setReviewedAt(
                LocalDateTime.now()
        );

        /*
         * Schema chưa có ESCALATED.
         *
         * INVESTIGATING +
         * BUSINESS_MANAGER
         * = đã chuyển Business Manager xử lý.
         */
        report.setStatus(
                DiscrepancyStatus.INVESTIGATING
        );

        report.setResponsibleParty(
                "BUSINESS_MANAGER"
        );

        report.setResolutionNote(
                request.getReason()
        );

        report.setUpdatedAt(
                LocalDateTime.now()
        );

        DiscrepancyReports saved =
                discrepancyReportRepository.save(
                        report
                );

        return buildResponse(saved);
    }


    // =====================================================
    // BUILD RESPONSE
    // =====================================================

    private DiscrepancyReportResponse buildResponse(
            DiscrepancyReports report
    ) {

        List<DiscrepancyItemResponse> items =
                List.of();

        if (report.getReferenceType()
                == DiscrepancyReferenceType.GOODS_RECEIPT) {

            items =
                    getProblemInspections(
                            report.getReferenceId()
                    )
                            .stream()

                            .map(
                                    discrepancyReportMapper
                                            ::toItemResponse
                            )

                            .toList();
        }

        Users reviewedBy =
                report.getReviewedBy();

        Users resolvedBy =
                report.getResolvedBy();

        return DiscrepancyReportResponse
                .builder()

                .id(
                        report.getId()
                )

                .reportCode(
                        report.getReportCode()
                )

                .discrepancyType(
                        report.getDiscrepancyType()
                                .name()
                )

                .status(
                        report.getStatus()
                                .name()
                )

                .description(
                        report.getDescription()
                )

                .goodsReceiptId(
                        report.getReferenceId()
                )

                .warehouseId(
                        report.getWarehouse()
                                .getId()
                )

                .responsibleParty(
                        report.getResponsibleParty()
                )

                .resolutionAction(
                        report.getResolutionAction()
                                != null
                                ? report
                                .getResolutionAction()
                                .name()
                                : null
                )

                .resolutionNote(
                        report.getResolutionNote()
                )

                .reviewedBy(
                        reviewedBy != null
                                ? reviewedBy.getId()
                                : null
                )

                .reviewedByName(
                        reviewedBy != null
                                ? reviewedBy.getName()
                                : null
                )

                .reviewedAt(
                        report.getReviewedAt()
                )

                .resolvedBy(
                        resolvedBy != null
                                ? resolvedBy.getId()
                                : null
                )

                .resolvedByName(
                        resolvedBy != null
                                ? resolvedBy.getName()
                                : null
                )

                .resolvedAt(
                        report.getResolvedAt()
                )

                .createdAt(
                        report.getCreatedAt()
                )

                .items(
                        items
                )

                .build();
    }


    // =====================================================
    // LẤY CÁC SẢN PHẨM CÓ CHÊNH LỆCH
    // =====================================================

    private List<ReceiptInspections>
    getProblemInspections(
            Long receiptId
    ) {

        return receiptInspectionRepository
                .findByGoodsReceiptId(
                        receiptId
                )

                .stream()

                .filter(inspection ->
                        !"MATCHED".equals(
                                inspection
                                        .getInspectedResult()
                                        .name()
                        )
                )

                .toList();
    }


    // =====================================================
    // GET REPORT
    // =====================================================

    private DiscrepancyReports getReport(
            Long discrepancyId
    ) {

        return discrepancyReportRepository
                .findById(discrepancyId)

                .orElseThrow(() ->
                        new NotFound(
                                "Discrepancy report not found: "
                                        + discrepancyId
                        )
                );
    }
}