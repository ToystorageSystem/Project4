package com.toystorage.backend.services.stores.returns;


import com.toystorage.backend.dto.request.stores.returns.InspectStoreReturnItemRequest;
import com.toystorage.backend.dto.response.stores.returns.StoreReturnInspectionResponse;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.stores.StoreReturnItems;
import com.toystorage.backend.entity.stores.StoreReturns;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;

import com.toystorage.backend.enums.stores.ReturnItemCondition;
import com.toystorage.backend.enums.stores.StoreReturnStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.mapper.stores.returns.StoreReturnInspectionMapper;

import com.toystorage.backend.repository.stores.StoreReturnItemRepository;
import com.toystorage.backend.repository.stores.StoreReturnRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreReturnInspectionService {

    private final StoreReturnRepository
            storeReturnRepository;

    private final StoreReturnItemRepository
            storeReturnItemRepository;

    private final StoreReturnInspectionValidationService
            validationService;

    private final StoreReturnLocationService
            locationService;

    private final StoreReturnInventoryService
            inventoryService;

    private final StoreReturnInspectionMapper
            mapper;


    // =====================================================
    // LIST
    // =====================================================

    @Transactional(readOnly = true)
    public List<StoreReturnInspectionResponse>
    getStoreReturns() {

        Users manager =
                validationService.getCurrentUser();

        Long warehouseId =
                validationService
                        .getWarehouseId(manager);

        return storeReturnRepository
                .findByWarehouseIdOrderByCreatedAtDesc(
                        warehouseId
                )

                .stream()

                .map(this::buildResponse)

                .toList();
    }


    // =====================================================
    // DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public StoreReturnInspectionResponse getStoreReturn(
            Long returnId
    ) {

        StoreReturns storeReturn =
                validationService
                        .getReturn(returnId);

        Users manager =
                validationService
                        .getCurrentUser();

        validationService.validateWarehouse(
                manager,
                storeReturn
        );

        return buildResponse(
                storeReturn
        );
    }


    // =====================================================
    // INSPECT ONE ITEM
    // =====================================================

    @Transactional
    public StoreReturnInspectionResponse inspectItem(
            Long returnId,
            Long itemId,
            InspectStoreReturnItemRequest request
    ) {

        StoreReturns storeReturn =
                validationService
                        .getReturn(returnId);

        Users manager =
                validationService
                        .getCurrentUser();

        validationService.validateWarehouse(
                manager,
                storeReturn
        );

        validationService.validateCanReceive(
                storeReturn
        );

        StoreReturnItems item =
                storeReturnItemRepository
                        .findById(itemId)

                        .orElseThrow(() ->
                                new NotFound(
                                        "Store return item not found: "
                                                + itemId
                                )
                        );

        if (item.getStoreReturn() == null
                || !item.getStoreReturn()
                .getId()
                .equals(returnId)) {

            throw new BadRequest(
                    "Item does not belong to store return"
            );
        }


        if (request.getReceivedQuantity()
                > item.getIssuedQuantity()) {

            throw new BadRequest(
                    "Received quantity cannot exceed issued quantity"
            );
        }


        /*
         * Tránh xử lý lại cùng một item,
         * nếu project của bạn khởi tạo received/approved/rejected = 0.
         */
        if (item.getReceivedQuantity() > 0
                || item.getApprovedQuantity() > 0
                || item.getRejectedQuantity() > 0) {

            throw new BadRequest(
                    "Store return item has already been inspected"
            );
        }


        String condition =
                request.getConditionStatus()
                        .trim()
                        .toUpperCase();


        switch (condition) {

            // =============================================
            // NORMAL
            // =============================================

            case "NORMAL" -> {

                item.setReceivedQuantity(
                        request.getReceivedQuantity()
                );

                item.setApprovedQuantity(
                        request.getReceivedQuantity()
                );

                item.setRejectedQuantity(0);

                /*
                 * Dùng đúng enum của entity thực tế.
                 */
                item.setConditionStatus(
                        ReturnItemCondition.NORMAL
                );

                WarehouseLocations normalLocation =
                        locationService
                                .getNormalLocation(
                                        storeReturn
                                                .getWarehouse()
                                                .getId()
                                );

                /*
                 * Chỉ NORMAL mới tăng available.
                 */
                inventoryService
                        .addAvailableInventory(
                                storeReturn,
                                item,
                                normalLocation,
                                request.getReceivedQuantity(),
                                manager
                        );
            }


            // =============================================
            // QUARANTINE
            // =============================================

            case "QUARANTINE" -> {

                item.setReceivedQuantity(
                        request.getReceivedQuantity()
                );

                item.setApprovedQuantity(0);

                item.setRejectedQuantity(
                        request.getReceivedQuantity()
                );

                item.setConditionStatus(
                        ReturnItemCondition.QUARANTINE
                );

                WarehouseLocations quarantine =
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
                                quarantine,
                                request.getReceivedQuantity(),
                                manager
                        );
            }


            // =============================================
            // DAMAGED / EXPIRED
            // =============================================

            case "DAMAGED", "EXPIRED" -> {

                item.setReceivedQuantity(
                        request.getReceivedQuantity()
                );

                item.setApprovedQuantity(0);

                item.setRejectedQuantity(
                        request.getReceivedQuantity()
                );

                item.setConditionStatus(
                        "DAMAGED".equals(condition)
                                ?  ReturnItemCondition.DAMAGED
                                : ReturnItemCondition
                                .EXPIRED
                );

                WarehouseLocations quarantine =
                        locationService
                                .getQuarantineLocation(
                                        storeReturn
                                                .getWarehouse()
                                                .getId()
                                );

                /*
                 * Có tồn vật lý,
                 * nhưng available = 0.
                 */
                inventoryService
                        .addQuarantineInventory(
                                storeReturn,
                                item,
                                quarantine,
                                request.getReceivedQuantity(),
                                manager
                        );

                /*
                 * TODO:
                 * gọi DamagedGoods service để tạo report.
                 *
                 * Không xử lý dispose ở đây.
                 */
            }


            default ->
                    throw new BadRequest(
                            "Unsupported condition status: "
                                    + condition
                    );
        }


        item.setNote(
                request.getNote()
        );

        storeReturnItemRepository.save(
                item
        );


        /*
         * Nếu item nào cũng đã kiểm thì hoàn thành return.
         */
        completeReturnIfFinished(
                storeReturn
        );


        return buildResponse(
                storeReturn
        );
    }


    // =====================================================
    // COMPLETE
    // =====================================================

    private void completeReturnIfFinished(
            StoreReturns storeReturn
    ) {

        List<StoreReturnItems> items =
                storeReturnItemRepository
                        .findByStoreReturnId(
                                storeReturn.getId()
                        );

        boolean allInspected =
                !items.isEmpty()

                        && items.stream()
                        .allMatch(item ->
                                item.getReceivedQuantity() != null
                                        &&
                                        (
                                                item.getApprovedQuantity()
                                                        + item.getRejectedQuantity()
                                        )
                                                ==
                                                item.getReceivedQuantity()
                        );


        if (!allInspected) {
            return;
        }

        /*
         * Schema hiện tại dùng RECEIVED làm trạng thái
         * phù hợp nhất sau khi kho nhận xong.
         */
        storeReturn.setStatus(
                StoreReturnStatus.RECEIVED
        );

        storeReturn.setReceivedAt(
                LocalDateTime.now()
        );

        storeReturn.setUpdatedAt(
                LocalDateTime.now()
        );

        storeReturnRepository.save(
                storeReturn
        );
    }


    // =====================================================
    // RESPONSE
    // =====================================================

    private StoreReturnInspectionResponse buildResponse(
            StoreReturns storeReturn
    ) {

        List<StoreReturnItems> items =
                storeReturnItemRepository
                        .findByStoreReturnId(
                                storeReturn.getId()
                        );

        return mapper.toResponse(
                storeReturn,
                items
        );
    }
}