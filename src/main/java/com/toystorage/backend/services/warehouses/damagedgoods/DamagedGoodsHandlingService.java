package com.toystorage.backend.services.warehouses.damagedgoods;

import com.toystorage.backend.dto.request.warehouses.damagedgoods.HandleDamagedGoodsRequest;
import com.toystorage.backend.dto.response.warehouses.damagedgoods.DamagedGoodsReportResponse;
import com.toystorage.backend.entity.warehouses.DamagedGoodsItems;
import com.toystorage.backend.entity.warehouses.DamagedGoodsReports;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.warehouses.DamagedGoodsItemStatus;
import com.toystorage.backend.enums.warehouses.DamagedGoodsStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.mapper.warehouses.damagedgoods.DamagedGoodsMapper;
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

    private final DamagedGoodsValidationService
            validationService;

    private final DamagedInventoryService
            inventoryService;

    private final DamagedGoodsMapper
            damagedGoodsMapper;

    @Transactional(readOnly = true)
    public List<DamagedGoodsReportResponse>
    getReports() {

        Users manager =
                validationService.getCurrentUser();

        Long warehouseId =
                validationService.getWarehouseId(manager);

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
                .map(this::buildResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DamagedGoodsReportResponse getReport(
            Long reportId
    ) {

        DamagedGoodsReports report =
                validationService.getReport(reportId);

        Users manager =
                validationService.getCurrentUser();

        validationService.validateWarehouse(
                manager,
                report
        );

        return buildResponse(report);
    }

    @Transactional
    public DamagedGoodsReportResponse startInspection(
            Long reportId
    ) {

        DamagedGoodsReports report =
                validationService.getReport(reportId);

        Users manager =
                validationService.getCurrentUser();

        validationService.validateWarehouse(
                manager,
                report
        );

        if (report.getStatus()
                != DamagedGoodsStatus.REPORTED) {

            throw new BadRequest(
                    "Only REPORTED damaged goods can be inspected"
            );
        }

        report.setStatus(
                DamagedGoodsStatus.INSPECTING
        );

        report.setReviewedBy(manager);

        report.setReviewedAt(
                LocalDateTime.now()
        );

        report.setUpdatedAt(
                LocalDateTime.now()
        );

        return buildResponse(
                reportRepository.save(report)
        );
    }

    @Transactional
    public DamagedGoodsReportResponse handleItem(
            Long reportId,
            Long itemId,
            HandleDamagedGoodsRequest request
    ) {

        DamagedGoodsReports report =
                validationService.getReport(reportId);

        Users manager =
                validationService.getCurrentUser();

        validationService.validateWarehouse(
                manager,
                report
        );

        if (report.getStatus()
                != DamagedGoodsStatus.INSPECTING) {

            throw new BadRequest(
                    "Damaged goods report must be INSPECTING before handling items"
            );
        }

        DamagedGoodsItems item =
                itemRepository
                        .findById(itemId)
                        .orElseThrow(() ->
                                new NotFound(
                                        "Damaged goods item not found"
                                )
                        );

        if (item.getDamagedGoodsReport() == null
                || !item.getDamagedGoodsReport()
                .getId()
                .equals(reportId)) {

            throw new BadRequest(
                    "Item does not belong to report"
            );
        }

        if (request.getConfirmedQuantity() == null
                || request.getConfirmedQuantity() <= 0) {

            throw new BadRequest(
                    "Confirmed quantity must be greater than 0"
            );
        }

        if (request.getConfirmedQuantity()
                > item.getQuantity()) {

            throw new BadRequest(
                    "Confirmed quantity cannot exceed reported quantity"
            );
        }

        /*
         * Loại hàng hỏng khỏi available inventory trước khi xử lý.
         */
        inventoryService.removeFromAvailable(
                item,
                request.getConfirmedQuantity(),
                manager
        );

        item.setQuantity(
                request.getConfirmedQuantity()
        );

        item.setDisposition(
                request.getDisposition()
        );

        switch (request.getDisposition()) {

            case QUARANTINE -> {
                item.setStatus(
                        DamagedGoodsItemStatus.APPROVED
                );

                /*
                 * TODO:
                 * Gọi location-transfer service để chuyển vật lý
                 * sang location QUARANTINE.
                 */
            }

            case RETURN_TO_SUPPLIER -> {
                item.setStatus(
                        DamagedGoodsItemStatus.APPROVED
                );

                /*
                 * TODO:
                 * Tạo supplier-return workflow.
                 * Hàng vẫn không được đưa lại vào available inventory.
                 */
            }

            case DISPOSE -> {
                inventoryService.dispose(
                        item,
                        request.getConfirmedQuantity(),
                        manager
                );

                item.setStatus(
                        DamagedGoodsItemStatus.DISPOSED
                );
            }

            default -> item.setStatus(
                    DamagedGoodsItemStatus.APPROVED
            );
        }

        itemRepository.save(item);

        updateReportStatus(report);

        return buildResponse(
                validationService.getReport(reportId)
        );
    }

    private void updateReportStatus(
            DamagedGoodsReports report
    ) {

        List<DamagedGoodsItems> items =
                itemRepository
                        .findByDamagedGoodsReportId(
                                report.getId()
                        );

        boolean allFinished =
                !items.isEmpty()
                        && items.stream()
                        .allMatch(item ->
                                item.getStatus()
                                        == DamagedGoodsItemStatus.APPROVED
                                        ||
                                        item.getStatus()
                                                == DamagedGoodsItemStatus.DISPOSED
                        );

        if (allFinished) {

            report.setStatus(
                    DamagedGoodsStatus.APPROVED
            );

            report.setResolvedAt(
                    LocalDateTime.now()
            );
        }

        report.setUpdatedAt(
                LocalDateTime.now()
        );

        reportRepository.save(report);
    }

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