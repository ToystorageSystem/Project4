package com.toystorage.backend.services.packages.packing;

import com.toystorage.backend.dto.response.packages.packing.StaffPackingSubmissionResponse;

import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.PackageTransferItem;
import com.toystorage.backend.entity.packages.Packages;

import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.transfers.StockTransferItems;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.transfers.TransferStatus;

import com.toystorage.backend.mapper.packages.packing.StaffPackingSubmissionMapper;

import com.toystorage.backend.repository.packages.packing.StaffPackageItemRepository;
import com.toystorage.backend.repository.packages.packing.StaffPackageTransferItemRepository;

import com.toystorage.backend.repository.transfers.picking.StockTransferItemRepository;
import com.toystorage.backend.repository.transfers.picking.StockTransferRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffPackingSubmissionService {

    private final StockTransferRepository
            stockTransferRepository;

    private final StockTransferItemRepository
            stockTransferItemRepository;

    private final StaffPackageTransferItemRepository
            packageTransferItemRepository;

    private final StaffPackageItemRepository
            packageItemRepository;

    private final StaffPackagePackingValidationService
            packingValidationService;

    private final StaffPackingSubmissionValidationService
            submissionValidationService;

    private final StaffPackingSubmissionMapper
            mapper;



    /** List the actual PACKING queue for the authenticated source warehouse. */
    @Transactional(readOnly = true)
    public List<StaffPackingSubmissionResponse> getPackingQueue() {
        Users staff = packingValidationService.getCurrentUser();
        if (staff.getWarehouse() == null) {
            throw new com.toystorage.backend.exceptions.Forbidden("User is not assigned to warehouse");
        }
        return stockTransferRepository
                .findByFromWarehouseIdAndStatusInOrderByCreatedAtDesc(
                        staff.getWarehouse().getId(), List.of(TransferStatus.PACKING))
                .stream()
                // already submitted to the manager -> no longer in the staff queue
                .filter(transfer -> transfer.getPackingCompletedAt() == null).map(transfer -> StaffPackingSubmissionResponse.builder()
                        .transferId(transfer.getId())
                        .transferCode(transfer.getTransferCode())
                        .status(transfer.getStatus().name())
                        .fromWarehouseId(transfer.getFromWarehouse().getId())
                        .fromWarehouseName(transfer.getFromWarehouse().getName())
                        .toWarehouseId(transfer.getToWarehouse() == null ? null : transfer.getToWarehouse().getId())
                        .toWarehouseName(transfer.getToWarehouse() == null ? null : transfer.getToWarehouse().getName())
                        .build()).toList();
    }

    // =====================================================
    // PREVIEW BEFORE SUBMIT
    // =====================================================

    @Transactional(readOnly = true)
    public StaffPackingSubmissionResponse preview(
            Long transferId
    ) {

        Users staff =
                packingValidationService
                        .getCurrentUser();


        StockTransfer transfer =
                packingValidationService
                        .getTransfer(
                                transferId
                        );


        submissionValidationService
                .validateWarehouse(
                        staff,
                        transfer
                );


        return buildResponse(
                transfer
        );
    }


    // =====================================================
    // SUBMIT PACKING RESULT
    // =====================================================

    @Transactional
    public StaffPackingSubmissionResponse submit(
            Long transferId
    ) {

        Users staff =
                packingValidationService
                        .getCurrentUser();


        StockTransfer transfer =
                packingValidationService
                        .getTransfer(
                                transferId
                        );


        submissionValidationService
                .validateWarehouse(
                        staff,
                        transfer
                );


        submissionValidationService
                .validateTransferStatus(
                        transfer
                );


        List<StockTransferItems> transferItems =
                stockTransferItemRepository
                        .findByStockTransferId(
                                transferId
                        );


        submissionValidationService
                .validateItemsPacked(
                        transferItems
                );


        List<PackageTransferItem> relations =
                packageTransferItemRepository
                        .findByStockTransferId(
                                transferId
                        );


        submissionValidationService
                .validatePackages(
                        relations
                );


        /*
         * Option B (2026-11-13): staff only SUBMITS the packing result.
         * The transfer stays PACKING; packing_completed_at marks it "waiting for manager review".
         * The Warehouse Manager confirms packing -> PACKED + manifest + delivery trip + driver.
         * No inventory change, no SHIPPED, packages not CHECKED here.
         */
        transfer.setPackingCompletedBy(
                staff
        );
        transfer.setPackingCompletedAt(
                LocalDateTime.now()
        );
        transfer.setUpdatedAt(
                LocalDateTime.now()
        );
        stockTransferRepository
                .save(
                        transfer
                );
        StaffPackingSubmissionResponse response =
                buildResponse(
                        transfer
                );

        return response;
    }


    // =====================================================
    // BUILD RESPONSE
    // =====================================================

    private StaffPackingSubmissionResponse buildResponse(
            StockTransfer transfer
    ) {

        List<StockTransferItems> transferItems =
                stockTransferItemRepository
                        .findByStockTransferId(
                                transfer.getId()
                        );


        int totalPicked =
                transferItems.stream()
                        .mapToInt(item ->
                                item.getPickedQuantity() == null
                                        ? 0
                                        : item.getPickedQuantity()
                        )
                        .sum();


        int totalPacked =
                transferItems.stream()
                        .mapToInt(item ->
                                item.getPackedQuantity() == null
                                        ? 0
                                        : item.getPackedQuantity()
                        )
                        .sum();


        List<Packages> packages =
                packageTransferItemRepository
                        .findByStockTransferId(
                                transfer.getId()
                        )

                        .stream()

                        .map(
                                PackageTransferItem::getPackageEntity
                        )

                        .toList();


        Map<Long, List<PackageItems>>
                packageItemsMap =
                packages.stream()
                        .collect(
                                Collectors.toMap(
                                        Packages::getId,
                                        packageEntity ->
                                                packageItemRepository
                                                        .findByPackageEntityId(
                                                                packageEntity.getId()
                                                        )
                                )
                        );


        return mapper.toResponse(
                transfer,
                totalPicked,
                totalPacked,
                packages,
                packageItemsMap
        );
    }
}