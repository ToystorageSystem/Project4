package com.toystorage.backend.services.stores.returns;

import com.toystorage.backend.dto.request.stores.returns.InspectReturnedItemRequest;
import com.toystorage.backend.dto.request.stores.returns.ReturnedPackageScanRequest;

import com.toystorage.backend.dto.response.stores.returns.ReturnedGoodsDetailResponse;
import com.toystorage.backend.dto.response.stores.returns.ReturnedPackageResponse;

import com.toystorage.backend.entity.inventories.DiscrepancyReports;
import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.entity.stores.StoreReturnItems;
import com.toystorage.backend.entity.stores.StoreReturns;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;

import com.toystorage.backend.enums.inventories.DiscrepancyReferenceType;
import com.toystorage.backend.enums.inventories.DiscrepancyStatus;
import com.toystorage.backend.enums.inventories.DiscrepancyType;

import com.toystorage.backend.enums.packages.PackageStatus;

import com.toystorage.backend.enums.stores.ReturnItemCondition;
import com.toystorage.backend.enums.stores.StoreReturnStatus;

import com.toystorage.backend.enums.warehouses.WarehouseTaskType;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.mapper.stores.returns.ReturnedGoodsInspectionMapper;

import com.toystorage.backend.repository.inventories.discrepancy.DiscrepancyReportRepository;

import com.toystorage.backend.repository.packages.packing.StaffPackageRepository;
import com.toystorage.backend.repository.packages.packing.StaffPackageTransferItemRepository;

import com.toystorage.backend.repository.stores.returns.WarehouseReturnItemRepository;
import com.toystorage.backend.repository.stores.returns.WarehouseReturnRepository;

import com.toystorage.backend.services.warehouses.taskclaim.WarehouseTaskClaimService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ReturnedGoodsInspectionService {

    private final WarehouseReturnRepository
            returnRepository;

    private final WarehouseReturnItemRepository
            returnItemRepository;

    private final StaffPackageRepository
            packageRepository;

    private final StaffPackageTransferItemRepository
            packageTransferItemRepository;

    private final ReturnedGoodsInspectionValidationService
            validationService;

    private final ReturnedGoodsInspectionMapper
            mapper;

    private final WarehouseTaskClaimService
            taskClaimService;


    private final DiscrepancyReportRepository
            discrepancyReportRepository;

    private final StoreReturnInventoryService
            inventoryService;

    private final StoreReturnLocationService
            locationService;


    // =====================================================
    // START INSPECTION
    // =====================================================

    @Transactional
    public ReturnedGoodsDetailResponse startInspection(
            Long returnId
    ) {

        Users staff =
                validationService.getCurrentUser();

        StoreReturns storeReturn =
                validationService.getReturn(
                        returnId
                );

        validationService.validateWarehouse(
                staff,
                storeReturn
        );


        if (
                storeReturn.getStatus()
                        == StoreReturnStatus.INSPECTING
        ) {

            taskClaimService.validateOwner(
                    WarehouseTaskType.STORE_RETURN_RECEIVING,
                    returnId,
                    staff
            );

            return buildResponse(
                    storeReturn
            );
        }


        if (
                storeReturn.getStatus()
                        != StoreReturnStatus.SHIPPED
        ) {

            throw new BadRequest(
                    "Store return cannot start inspection from status "
                            + storeReturn.getStatus()
            );
        }


        taskClaimService.claim(
                WarehouseTaskType.STORE_RETURN_RECEIVING,
                returnId,
                staff
        );


        storeReturn.setStatus(
                StoreReturnStatus.INSPECTING
        );

        storeReturn.setUpdatedAt(
                LocalDateTime.now()
        );


        returnRepository.save(
                storeReturn
        );


        return buildResponse(
                storeReturn
        );
    }


    // =====================================================
    // SCAN PACKAGE
    // =====================================================

    @Transactional
    public ReturnedPackageResponse scanPackage(
            Long returnId,
            ReturnedPackageScanRequest request
    ) {

        Users staff =
                validationService.getCurrentUser();


        StoreReturns storeReturn =
                validationService.getReturn(
                        returnId
                );


        validationService.validateWarehouse(
                staff,
                storeReturn
        );


        taskClaimService.validateOwner(
                WarehouseTaskType.STORE_RETURN_RECEIVING,
                returnId,
                staff
        );


        validationService.validateEditable(
                storeReturn
        );


        Packages packageEntity =
                packageRepository
                        .findByPackagesCode(
                                request.getPackageBarcode()
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Package not found"
                                )
                        );


        if (
                storeReturn.getStockTransfer()
                        == null
        ) {

            throw new BadRequest(
                    "Store return is not linked to stock transfer"
            );
        }


        boolean belongs =
                packageTransferItemRepository
                        .existsByPackageEntityIdAndStockTransferId(
                                packageEntity.getId(),
                                storeReturn
                                        .getStockTransfer()
                                        .getId()
                        );


        if (!belongs) {

            throw new BadRequest(
                    "Package does not belong to this store return"
            );
        }


        if (
                packageEntity.getStatus()
                        == PackageStatus.SHIPPED
        ) {

            packageEntity.setStatus(
                    PackageStatus.RECEIVED
            );

            packageRepository.save(
                    packageEntity
            );
        }


        return ReturnedPackageResponse
                .builder()

                .packageId(
                        packageEntity.getId()
                )

                .packageCode(
                        packageEntity.getPackagesCode()
                )

                .sealNumber(
                        packageEntity.getSealNumber()
                )

                .status(
                        packageEntity
                                .getStatus()
                                .name()
                )

                .build();
    }


    // =====================================================
    // INSPECT ITEM
    // =====================================================

    @Transactional
    public ReturnedGoodsDetailResponse inspectItem(
            Long returnId,
            InspectReturnedItemRequest request
    ) {

        Users staff =
                validationService.getCurrentUser();


        StoreReturns storeReturn =
                validationService.getReturn(
                        returnId
                );


        validationService.validateWarehouse(
                staff,
                storeReturn
        );


        taskClaimService.validateOwner(
                WarehouseTaskType.STORE_RETURN_RECEIVING,
                returnId,
                staff
        );


        validationService.validateEditable(
                storeReturn
        );


        StoreReturnItems item =
                validationService
                        .getItemByBarcode(
                                returnId,
                                request.getProductBarcode()
                        );


        Integer receivedQuantity =
                request.getReceivedQuantity();


        if (
                receivedQuantity == null
                        ||
                        receivedQuantity < 0
        ) {

            throw new BadRequest(
                    "Received quantity must be greater than or equal to 0"
            );
        }


        /*
         * QUAN TRỌNG:
         *
         * Không giới hạn:
         *
         * receivedQuantity <= issuedQuantity
         *
         * vì thực tế Warehouse có thể nhận THỪA.
         *
         * Ví dụ:
         *
         * Store khai gửi 10
         * Warehouse đếm 12
         *
         * => received = 12
         * => tạo SURPLUS discrepancy.
         */
        item.setReceivedQuantity(
                receivedQuantity
        );


        item.setConditionStatus(
                request.getConditionStatus()
        );


        item.setNote(
                request.getNote()
        );


        item.setInspected(
                true
        );


        returnItemRepository.save(
                item
        );


        return buildResponse(
                storeReturn
        );
    }


    // =====================================================
    // COMPLETE RECEIVING
    //
    // Staff kiểm xong + xác nhận đã đưa hàng về location.
    //
    // KHÔNG cần Warehouse Manager confirm.
    // =====================================================

    @Transactional
    public ReturnedGoodsDetailResponse submitInspection(
            Long returnId
    ) {

        Users staff =
                validationService.getCurrentUser();


        StoreReturns storeReturn =
                validationService.getReturn(
                        returnId
                );


        validationService.validateWarehouse(
                staff,
                storeReturn
        );


        taskClaimService.validateOwner(
                WarehouseTaskType.STORE_RETURN_RECEIVING,
                returnId,
                staff
        );


        validationService.validateEditable(
                storeReturn
        );


        List<StoreReturnItems> items =
                returnItemRepository
                        .findByStoreReturnId(
                                returnId
                        );


        if (
                items.isEmpty()
        ) {

            throw new BadRequest(
                    "Store return contains no items"
            );
        }


        // =================================================
        // VALIDATE ALL ITEMS
        // =================================================

        for (
                StoreReturnItems item : items
        ) {

            if (
                    !Boolean.TRUE.equals(
                            item.getInspected()
                    )
            ) {

                throw new BadRequest(
                        "Product has not been inspected: "
                                + item.getProduct()
                                .getName()
                );
            }


            if (
                    item.getReceivedQuantity()
                            == null
            ) {

                throw new BadRequest(
                        "Received quantity is required for product "
                                + item.getProduct()
                                .getName()
                );
            }


            if (
                    item.getConditionStatus()
                            == null
            ) {

                throw new BadRequest(
                        "Condition is required for product "
                                + item.getProduct()
                                .getName()
                );
            }


            int expected =
                    getExpectedQuantity(
                            item
                    );

            int actual =
                    item.getReceivedQuantity();


            if (
                    actual != expected

                            &&

                            (
                                    item.getNote()
                                            == null

                                            ||

                                            item.getNote()
                                                    .isBlank()
                            )
            ) {

                throw new BadRequest(
                        "Note is required for discrepancy product "
                                + item.getProduct()
                                .getName()
                );
            }


            if (
                    item.getConditionStatus()
                            != ReturnItemCondition.NORMAL

                            &&

                            (
                                    item.getNote()
                                            == null

                                            ||

                                            item.getNote()
                                                    .isBlank()
                            )
            ) {

                throw new BadRequest(
                        "Note is required for abnormal condition product "
                                + item.getProduct()
                                .getName()
                );
            }
        }


        // =================================================
        // DISCREPANCY + INVENTORY
        // =================================================

        for (
                StoreReturnItems item : items
        ) {

            syncDiscrepancy(
                    storeReturn,
                    item,
                    staff
            );


            moveToInventory(
                    storeReturn,
                    item,
                    staff
            );
        }


        // =================================================
        // COMPLETE RETURN
        // =================================================

        LocalDateTime now =
                LocalDateTime.now();


        storeReturn.setInspectedBy(
                staff
        );


        storeReturn.setInspectionSubmittedAt(
                now
        );


        storeReturn.setReceivedAt(
                now
        );


        storeReturn.setStatus(
                StoreReturnStatus.RECEIVED
        );


        storeReturn.setUpdatedAt(
                now
        );


        returnRepository.save(
                storeReturn
        );


        // =================================================
        // RELEASE CLAIM
        // =================================================

        taskClaimService.release(
                WarehouseTaskType.STORE_RETURN_RECEIVING,
                returnId,
                staff
        );


        return mapper.toDetailResponse(
                storeReturn,
                items
        );
    }


    // =====================================================
    // INVENTORY
    // =====================================================

    private void moveToInventory(
            StoreReturns storeReturn,
            StoreReturnItems item,
            Users staff
    ) {

        int quantity =
                item.getReceivedQuantity() != null
                        ? item.getReceivedQuantity()
                        : 0;


        if (
                quantity <= 0
        ) {

            return;
        }


        ReturnItemCondition condition =
                item.getConditionStatus();


        // =================================================
        // NORMAL
        // =================================================

        if (
                condition == ReturnItemCondition.NORMAL
        ) {

            WarehouseLocations location =
                    locationService
                            .getNormalLocation(
                                    storeReturn
                                            .getWarehouse()
                                            .getId()
                            );


            inventoryService
                    .addAvailableInventory(
                            storeReturn,
                            item,
                            location,
                            quantity,
                            staff
                    );


            item.setApprovedQuantity(
                    quantity
            );


            item.setRejectedQuantity(
                    0
            );


            returnItemRepository.save(
                    item
            );

            return;
        }


        // =================================================
        // DAMAGED / EXPIRED / QUARANTINE
        // =================================================

        WarehouseLocations quarantineLocation =
                locationService
                        .getQuarantineLocation(
                                storeReturn
                                        .getWarehouse()
                                        .getId()
                        );


        inventoryService
                .addQuarantineInventory(
                        storeReturn,
                        item,
                        quarantineLocation,
                        quantity,
                        staff
                );


        item.setApprovedQuantity(
                0
        );


        item.setRejectedQuantity(
                quantity
        );


        returnItemRepository.save(
                item
        );
    }


    // =====================================================
    // DISCREPANCY
    // =====================================================

    private void syncDiscrepancy(
            StoreReturns storeReturn,
            StoreReturnItems item,
            Users staff
    ) {

        int expected =
                getExpectedQuantity(
                        item
                );


        int actual =
                item.getReceivedQuantity() != null
                        ? item.getReceivedQuantity()
                        : 0;


        // =================================================
        // SHORTAGE
        // =================================================

        if (
                actual < expected
        ) {

            createDiscrepancyIfNotExists(
                    storeReturn,
                    item,
                    staff,
                    DiscrepancyType.SHORTAGE,

                    "Store return shortage. Expected: "
                            + expected
                            + ", received: "
                            + actual
                            + ", missing: "
                            + (
                            expected - actual
                    )
            );
        }


        // =================================================
        // SURPLUS
        // =================================================

        if (
                actual > expected
        ) {

            createDiscrepancyIfNotExists(
                    storeReturn,
                    item,
                    staff,
                    DiscrepancyType.SURPLUS,

                    "Store return surplus. Expected: "
                            + expected
                            + ", received: "
                            + actual
                            + ", surplus: "
                            + (
                            actual - expected
                    )
            );
        }


        // =================================================
        // DAMAGED
        // =================================================

        if (
                item.getConditionStatus()
                        == ReturnItemCondition.DAMAGED
        ) {

            createDiscrepancyIfNotExists(
                    storeReturn,
                    item,
                    staff,
                    DiscrepancyType.DAMAGED,

                    "Damaged product detected during Store Return inspection"
            );
        }


        // =================================================
        // EXPIRED
        //
        // Hiện project chưa có DiscrepancyType.EXPIRED.
        // =================================================

        if (
                item.getConditionStatus()
                        == ReturnItemCondition.EXPIRED
        ) {

            createDiscrepancyIfNotExists(
                    storeReturn,
                    item,
                    staff,
                    DiscrepancyType.DAMAGED,

                    "Expired product detected during Store Return inspection"
            );
        }
    }


    // =====================================================
    // CREATE DISCREPANCY
    // =====================================================

    private void createDiscrepancyIfNotExists(
            StoreReturns storeReturn,
            StoreReturnItems item,
            Users staff,
            DiscrepancyType type,
            String description
    ) {

        boolean exists =
                discrepancyReportRepository
                        .existsByReferenceTypeAndReferenceIdAndProductIdAndDiscrepancyTypeAndStatusIn(

                                DiscrepancyReferenceType.STORE_RETURN,

                                storeReturn.getId(),

                                item.getProduct()
                                        .getId(),

                                type,

                                List.of(
                                        DiscrepancyStatus.OPEN,
                                        DiscrepancyStatus.INVESTIGATING
                                )
                        );


        if (
                exists
        ) {

            return;
        }


        String code =
                generateCode(
                        "DR-SR"
                );


        DiscrepancyReports report =
                DiscrepancyReports
                        .builder()

                        .reportCode(
                                code
                        )

                        .discrepancyReportsCode(
                                code
                        )

                        .referenceType(
                                DiscrepancyReferenceType.STORE_RETURN
                        )

                        .referenceId(
                                storeReturn.getId()
                        )

                        .productId(
                                item.getProduct()
                                        .getId()
                        )

                        .warehouse(
                                storeReturn
                                        .getWarehouse()
                        )

                        .discrepancyType(
                                type
                        )

                        .status(
                                DiscrepancyStatus.OPEN
                        )

                        .reportedBy(
                                staff
                        )

                        /*
                         * Không block Store Return.
                         *
                         * Discrepancy sẽ được xử lý riêng.
                         */
                        .responsibleParty(
                                "WAREHOUSE"
                        )

                        .description(
                                description
                        )

                        .evidenceImageUrl(
                                item.getEvidenceImageUrl()
                        )

                        .build();


        discrepancyReportRepository.save(
                report
        );
    }


    // =====================================================
    // EXPECTED QUANTITY
    // =====================================================

    private int getExpectedQuantity(
            StoreReturnItems item
    ) {

        if (
                item.getIssuedQuantity()
                        != null
        ) {

            return item.getIssuedQuantity();
        }


        if (
                item.getApprovedQuantity()
                        != null
        ) {

            return item.getApprovedQuantity();
        }


        return item.getRequestedQuantity()
                != null
                ? item.getRequestedQuantity()
                : 0;
    }


    // =====================================================
    // CODE
    // =====================================================

    private String generateCode(
            String prefix
    ) {

        return prefix
                + "-"
                + UUID.randomUUID()
                .toString()
                .replace(
                        "-",
                        ""
                )
                .substring(
                        0,
                        10
                )
                .toUpperCase();
    }


    // =====================================================
    // RESPONSE
    // =====================================================

    private ReturnedGoodsDetailResponse buildResponse(
            StoreReturns storeReturn
    ) {

        return mapper.toDetailResponse(
                storeReturn,

                returnItemRepository
                        .findByStoreReturnId(
                                storeReturn.getId()
                        )
        );
    }
}