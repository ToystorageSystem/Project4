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

import com.toystorage.backend.repository.packages.StaffPackageItemRepository;
import com.toystorage.backend.repository.packages.StaffPackageTransferItemRepository;

import com.toystorage.backend.repository.transfers.StockTransferItemRepository;
import com.toystorage.backend.repository.transfers.StockTransferRepository;

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
         * Staff chỉ gửi kết quả đóng hàng.
         *
         * Đây KHÔNG phải xác nhận xuất kho.
         */
        transfer.setStatus(
                TransferStatus.PACKED
        );


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


        /*
         * Không chỉnh inventory.
         * Không chuyển SHIPPED.
         * Không CHECKED package.
         *
         * Warehouse Manager xử lý bước sau.
         */
        return buildResponse(
                transfer
        );
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