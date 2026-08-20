package com.toystorage.backend.services.stores;

import com.toystorage.backend.dto.request.stores.InspectReturnedItemRequest;
import com.toystorage.backend.dto.request.stores.ReturnedPackageScanRequest;

import com.toystorage.backend.dto.response.stores.ReturnedGoodsDetailResponse;
import com.toystorage.backend.dto.response.stores.ReturnedPackageResponse;
import com.toystorage.backend.enums.warehouses.WarehouseTaskType;
import com.toystorage.backend.services.warehouses.WarehouseTaskClaimService;

import com.toystorage.backend.entity.packages.Packages;

import com.toystorage.backend.entity.stores.StoreReturnItems;
import com.toystorage.backend.entity.stores.StoreReturns;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.packages.PackageStatus;
import com.toystorage.backend.enums.stores.StoreReturnStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.mapper.stores.ReturnedGoodsInspectionMapper;

import com.toystorage.backend.repository.packages.StaffPackageRepository;
import com.toystorage.backend.repository.packages.StaffPackageTransferItemRepository;

import com.toystorage.backend.repository.stores.WarehouseReturnItemRepository;
import com.toystorage.backend.repository.stores.WarehouseReturnRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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


    // =====================================================
    // START
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


        /*
         * Nếu đang INSPECTING thì chỉ owner
         * được gọi Start lại.
         */
        if (storeReturn.getStatus()
                == StoreReturnStatus.INSPECTING) {

            taskClaimService.validateOwner(
                    WarehouseTaskType.STORE_RETURN_RECEIVING,
                    returnId,
                    staff
            );

            return buildResponse(
                    storeReturn
            );
        }


        if (storeReturn.getStatus()
                != StoreReturnStatus.SHIPPED) {

            throw new BadRequest(
                    "Store return cannot start inspection from status "
                            + storeReturn.getStatus()
            );
        }


        /*
         * Staff đầu tiên claim được quyền kiểm.
         */
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
    // SCAN RETURN PACKAGE
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


        /*
         * Store Return liên kết StockTransfer.
         * Package cũng liên kết StockTransfer.
         * Hai bên phải cùng transfer.
         */
        if (storeReturn.getStockTransfer() == null) {

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


        /*
         * Package đã tới kho vật lý.
         * Không có inventory change tại đây.
         */
        if (packageEntity.getStatus()
                == PackageStatus.SHIPPED) {

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
    // INSPECT PRODUCT
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


        item.setReceivedQuantity(
                request.getReceivedQuantity()
        );


        item.setConditionStatus(
                request.getConditionStatus()
        );


        item.setNote(
                request.getNote()
        );


        /*
         * received - approved:
         *
         * < 0 = thiếu
         * > 0 = thừa
         * = 0 = đúng
         *
         * Không update tồn kho.
         */
        returnItemRepository.save(
                item
        );


        return buildResponse(
                storeReturn
        );
    }


    // =====================================================
    // SUBMIT TO MANAGER
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


        if (items.isEmpty()) {

            throw new BadRequest(
                    "Store return contains no items"
            );
        }


        for (StoreReturnItems item : items) {

            if (item.getReceivedQuantity() == null) {

                throw new BadRequest(
                        "Received quantity is required for product "
                                + item.getProduct().getName()
                );
            }


            if (item.getConditionStatus() == null) {

                throw new BadRequest(
                        "Condition is required for product "
                                + item.getProduct().getName()
                );
            }


            int expected =
                    item.getApprovedQuantity();

            int actual =
                    item.getReceivedQuantity();


            /*
             * Có chênh lệch thì bắt buộc ghi chú.
             */
            if (actual != expected
                    && (
                    item.getNote() == null
                            || item.getNote().isBlank()
            )) {

                throw new BadRequest(
                        "Note is required for discrepancy product "
                                + item.getProduct().getName()
                );
            }


            /*
             * Damaged / expired / quarantine
             * cũng bắt buộc ghi chú.
             */
            if (!"NORMAL".equals(
                    item.getConditionStatus().name()
            )
                    && (
                    item.getNote() == null
                            || item.getNote().isBlank()
            )) {

                throw new BadRequest(
                        "Note is required for abnormal condition product "
                                + item.getProduct().getName()
                );
            }
        }


        storeReturn.setInspectedBy(
                staff
        );

        storeReturn.setInspectionSubmittedAt(
                LocalDateTime.now()
        );

        storeReturn.setStatus(
                StoreReturnStatus.PENDING_CONFIRMATION
        );

        storeReturn.setUpdatedAt(
                LocalDateTime.now()
        );


        returnRepository.save(
                storeReturn
        );

        taskClaimService.release(
                WarehouseTaskType.STORE_RETURN_RECEIVING,
                returnId,
                staff
        );

        /*
         * KHÔNG:
         *
         * - update InventoryBalances
         * - tạo InventoryTransactions
         * - chuyển RECEIVED
         *
         * Manager làm bước xác nhận.
         */
        return mapper.toDetailResponse(
                storeReturn,
                items
        );
    }


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