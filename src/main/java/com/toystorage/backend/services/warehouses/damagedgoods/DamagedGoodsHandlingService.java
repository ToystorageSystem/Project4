package com.toystorage.backend.services.warehouses.damagedgoods;

import com.toystorage.backend.dto.request.warehouses.damagedgoods.HandleDamagedGoodsRequest;
import com.toystorage.backend.dto.response.warehouses.damagedgoods.DamagedGoodsReportResponse;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.entity.warehouses.DamagedGoodsHistory;
import com.toystorage.backend.entity.warehouses.DamagedGoodsItems;
import com.toystorage.backend.entity.warehouses.DamagedGoodsReports;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;

import com.toystorage.backend.enums.warehouses.DamagedGoodsHistoryAction;
import com.toystorage.backend.enums.warehouses.DamagedGoodsItemStatus;
import com.toystorage.backend.enums.warehouses.DamagedGoodsStatus;
import com.toystorage.backend.enums.warehouses.DamageDisposition;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.mapper.warehouses.damagedgoods.DamagedGoodsMapper;

import com.toystorage.backend.repository.warehouses.damagedgoods.DamagedGoodsHistoryRepository;
import com.toystorage.backend.repository.warehouses.damagedgoods.DamagedGoodsItemRepository;
import com.toystorage.backend.repository.warehouses.damagedgoods.DamagedGoodsReportRepository;

import com.toystorage.backend.services.inventories.damagedgoods.DamagedInventoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class DamagedGoodsHandlingService {

    private final DamagedGoodsReportRepository
            reportRepository;

    private final DamagedGoodsItemRepository
            itemRepository;

    private final DamagedGoodsHistoryRepository
            historyRepository;

    private final DamagedGoodsValidationService
            validationService;

    private final DamagedInventoryService
            inventoryService;

    private final DamagedGoodsMapper
            damagedGoodsMapper;


    // =====================================================
    // LIST
    // =====================================================

    @Transactional(readOnly = true)
    public List<DamagedGoodsReportResponse> getReports() {

        Users manager =
                validationService.getCurrentUser();


        Long warehouseId =
                validationService.getWarehouseId(
                        manager
                );


        return reportRepository
                .findByWarehouseIdAndStatusInOrderByCreatedAtDesc(
                        warehouseId,
                        List.of(
                                DamagedGoodsStatus.REPORTED,
                                DamagedGoodsStatus.INSPECTING,
                                DamagedGoodsStatus.APPROVED
                        )
                )

                .stream()

                .map(
                        this::buildResponse
                )

                .toList();
    }


    // =====================================================
    // DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public DamagedGoodsReportResponse getReport(
            Long reportId
    ) {

        DamagedGoodsReports report =
                validationService
                        .getReport(
                                reportId
                        );


        Users manager =
                validationService
                        .getCurrentUser();


        validationService
                .validateWarehouse(
                        manager,
                        report
                );


        return buildResponse(
                report
        );
    }


    // =====================================================
    // START INSPECTION
    // =====================================================

    @Transactional
    public DamagedGoodsReportResponse startInspection(
            Long reportId
    ) {

        DamagedGoodsReports report =
                validationService
                        .getReport(
                                reportId
                        );


        Users manager =
                validationService
                        .getCurrentUser();


        validationService
                .validateWarehouse(
                        manager,
                        report
                );


        if (
                report.getStatus()
                        != DamagedGoodsStatus.REPORTED
        ) {

            throw new BadRequest(
                    "Only REPORTED damaged goods can be inspected"
            );
        }


        LocalDateTime now =
                LocalDateTime.now();


        report.setStatus(
                DamagedGoodsStatus.INSPECTING
        );


        report.setReviewedBy(
                manager
        );


        report.setReviewedAt(
                now
        );


        report.setUpdatedAt(
                now
        );


        reportRepository.save(
                report
        );


        saveHistory(
                report,
                null,
                DamagedGoodsHistoryAction
                        .INSPECTION_STARTED,
                null,
                "Warehouse Manager started inspection",
                manager
        );


        return buildResponse(
                report
        );
    }


    // =====================================================
    // HANDLE ITEM
    // =====================================================

    @Transactional
    public DamagedGoodsReportResponse handleItem(
            Long reportId,
            Long itemId,
            HandleDamagedGoodsRequest request
    ) {

        DamagedGoodsReports report =
                validationService
                        .getReport(
                                reportId
                        );


        Users manager =
                validationService
                        .getCurrentUser();


        validationService
                .validateWarehouse(
                        manager,
                        report
                );


        // =================================================
        // REPORT STATUS
        // =================================================

        if (
                report.getStatus()
                        != DamagedGoodsStatus.INSPECTING
        ) {

            throw new BadRequest(
                    "Damaged goods report must be INSPECTING before handling items"
            );
        }


        // =================================================
        // GET ITEM
        // =================================================

        DamagedGoodsItems item =
                itemRepository
                        .findById(
                                itemId
                        )

                        .orElseThrow(
                                () ->
                                        new NotFound(
                                                "Damaged goods item not found"
                                        )
                        );


        // =================================================
        // VALIDATE REPORT ITEM
        // =================================================

        if (
                item.getDamagedGoodsReport()
                        == null

                        || !item
                        .getDamagedGoodsReport()
                        .getId()
                        .equals(
                                reportId
                        )
        ) {

            throw new BadRequest(
                    "Item does not belong to report"
            );
        }


        // =================================================
        // PREVENT DOUBLE PROCESS
        // =================================================

        if (
                item.getStatus()
                        == DamagedGoodsItemStatus.QUARANTINED

                        || item.getStatus()
                        == DamagedGoodsItemStatus
                        .RETURNED_TO_SUPPLIER

                        || item.getStatus()
                        == DamagedGoodsItemStatus.DISPOSED
        ) {

            throw new BadRequest(
                    "Damaged goods item has already been handled"
            );
        }


        // =================================================
        // CONFIRMED QUANTITY
        // =================================================

        Integer confirmedQuantity =
                request.getConfirmedQuantity();


        if (
                confirmedQuantity == null
                        || confirmedQuantity <= 0
        ) {

            throw new BadRequest(
                    "Confirmed quantity must be greater than 0"
            );
        }


        if (
                confirmedQuantity
                        > item.getQuantity()
        ) {

            throw new BadRequest(
                    "Confirmed quantity cannot exceed reported quantity"
            );
        }


        // =================================================
        // DISPOSITION
        // =================================================

        DamageDisposition disposition =
                request.getDisposition();


        if (disposition == null) {

            throw new BadRequest(
                    "Disposition is required"
            );
        }


        if (
                disposition
                        != DamageDisposition.QUARANTINE

                        && disposition
                        != DamageDisposition.RETURN_TO_SUPPLIER

                        && disposition
                        != DamageDisposition.DISPOSE
        ) {

            throw new BadRequest(
                    "Unsupported damaged goods disposition: "
                            + disposition
            );
        }


        // =================================================
        // SAVE CONFIRMATION
        // =================================================

        item.setConfirmedQuantity(
                confirmedQuantity
        );


        item.setDisposition(
                disposition
        );


        item.setStatus(
                DamagedGoodsItemStatus.INSPECTING
        );


        itemRepository.save(
                item
        );


        saveHistory(
                report,
                item,
                DamagedGoodsHistoryAction
                        .QUANTITY_CONFIRMED,
                confirmedQuantity,
                request.getResolutionNote(),
                manager
        );


        // =================================================
        // ALWAYS MOVE TO QUARANTINE FIRST
        // =================================================

        WarehouseLocations quarantineLocation =
                inventoryService
                        .moveToQuarantine(
                                item,
                                confirmedQuantity,
                                manager
                        );


        item.setLocation(
                quarantineLocation
        );


        itemRepository.save(
                item
        );


        saveHistory(
                report,
                item,
                DamagedGoodsHistoryAction
                        .MOVED_TO_QUARANTINE,
                confirmedQuantity,
                "Damaged goods moved to quarantine",
                manager
        );


        // =================================================
        // FINAL DISPOSITION
        // =================================================

        switch (
                disposition
        ) {

            // =============================================
            // QUARANTINE
            // =============================================

            case QUARANTINE -> {

                item.setStatus(
                        DamagedGoodsItemStatus
                                .QUARANTINED
                );


                itemRepository.save(
                        item
                );
            }


            // =============================================
            // RETURN TO SUPPLIER
            // =============================================

            case RETURN_TO_SUPPLIER -> {

                inventoryService
                        .returnToSupplier(
                                item,
                                confirmedQuantity,
                                manager
                        );


                item.setStatus(
                        DamagedGoodsItemStatus
                                .RETURNED_TO_SUPPLIER
                );


                itemRepository.save(
                        item
                );


                saveHistory(
                        report,
                        item,
                        DamagedGoodsHistoryAction
                                .RETURNED_TO_SUPPLIER,
                        confirmedQuantity,
                        request.getResolutionNote(),
                        manager
                );
            }


            // =============================================
            // DISPOSE
            // =============================================

            case DISPOSE -> {

                inventoryService
                        .dispose(
                                item,
                                confirmedQuantity,
                                manager
                        );


                item.setStatus(
                        DamagedGoodsItemStatus.DISPOSED
                );


                itemRepository.save(
                        item
                );


                saveHistory(
                        report,
                        item,
                        DamagedGoodsHistoryAction
                                .DISPOSED,
                        confirmedQuantity,
                        request.getResolutionNote(),
                        manager
                );
            }


            default ->
                    throw new BadRequest(
                            "Unsupported damaged goods disposition"
                    );
        }


        // =================================================
        // UPDATE REPORT
        // =================================================

        updateReportStatus(
                report,
                manager
        );


        return buildResponse(
                validationService
                        .getReport(
                                reportId
                        )
        );
    }


    // =====================================================
    // UPDATE REPORT STATUS
    // =====================================================

    private void updateReportStatus(
            DamagedGoodsReports report,
            Users manager
    ) {

        List<DamagedGoodsItems> items =
                itemRepository
                        .findByDamagedGoodsReportId(
                                report.getId()
                        );


        boolean allFinished =
                !items.isEmpty()

                        && items
                        .stream()

                        .allMatch(
                                item ->

                                        item.getStatus()
                                                == DamagedGoodsItemStatus
                                                .QUARANTINED

                                                ||

                                                item.getStatus()
                                                        == DamagedGoodsItemStatus
                                                        .RETURNED_TO_SUPPLIER

                                                ||

                                                item.getStatus()
                                                        == DamagedGoodsItemStatus
                                                        .DISPOSED
                        );


        if (!allFinished) {

            report.setUpdatedAt(
                    LocalDateTime.now()
            );


            reportRepository.save(
                    report
            );


            return;
        }


        report.setStatus(
                DamagedGoodsStatus.RESOLVED
        );


        report.setResolvedAt(
                LocalDateTime.now()
        );


        report.setUpdatedAt(
                LocalDateTime.now()
        );


        reportRepository.save(
                report
        );


        saveHistory(
                report,
                null,
                DamagedGoodsHistoryAction.RESOLVED,
                null,
                "Damaged goods report resolved",
                manager
        );
    }


    // =====================================================
    // HISTORY
    // =====================================================

    private void saveHistory(
            DamagedGoodsReports report,
            DamagedGoodsItems item,
            DamagedGoodsHistoryAction action,
            Integer quantity,
            String note,
            Users user
    ) {

        DamagedGoodsHistory history =
                DamagedGoodsHistory
                        .builder()

                        .report(
                                report
                        )

                        .item(
                                item
                        )

                        .action(
                                action
                        )

                        .quantity(
                                quantity
                        )

                        .note(
                                note
                        )

                        .performedBy(
                                user
                        )

                        .createdAt(
                                LocalDateTime.now()
                        )

                        .build();


        historyRepository.save(
                history
        );
    }


    // =====================================================
    // RESPONSE
    // =====================================================

    private DamagedGoodsReportResponse buildResponse(
            DamagedGoodsReports report
    ) {

        List<DamagedGoodsItems> items =
                itemRepository
                        .findByDamagedGoodsReportId(
                                report.getId()
                        );


        return damagedGoodsMapper
                .toReportResponse(
                        report,
                        items
                );
    }
}