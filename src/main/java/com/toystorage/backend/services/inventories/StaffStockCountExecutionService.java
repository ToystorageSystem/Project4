package com.toystorage.backend.services.inventories;

import com.toystorage.backend.dto.request.inventories.StaffStockCountItemRequest;

import com.toystorage.backend.dto.response.inventories.StaffStockCountDetailResponse;

import com.toystorage.backend.entity.inventories.StockCountItems;
import com.toystorage.backend.entity.inventories.StockCounts;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.inventories.StockCountStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;

import com.toystorage.backend.mapper.inventories.StaffStockCountMapper;

import com.toystorage.backend.repository.inventories.StaffStockCountItemRepository;
import com.toystorage.backend.repository.inventories.StaffStockCountRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StaffStockCountExecutionService {

    private final StaffStockCountRepository
            stockCountRepository;

    private final StaffStockCountItemRepository
            stockCountItemRepository;

    private final StaffStockCountValidationService
            validationService;

    private final StaffStockCountMapper
            mapper;


    // =====================================================
    // CLAIM + START
    // =====================================================

    @Transactional
    public StaffStockCountDetailResponse start(
            Long stockCountId
    ) {

        Users staff =
                validationService.getCurrentUser();


        if (staff.getWarehouse() == null) {

            throw new Forbidden(
                    "User is not assigned to warehouse"
            );
        }


        LocalDateTime now =
                LocalDateTime.now();


        int updated =
                stockCountRepository
                        .claimAndStart(
                                stockCountId,
                                staff.getWarehouse().getId(),
                                staff,
                                StockCountStatus.PLANNED,
                                StockCountStatus.COUNTING,
                                now
                        );


        /*
         * update = 1:
         * mình claim thành công.
         */
        if (updated == 1) {

            StockCounts stockCount =
                    validationService
                            .getStockCount(
                                    stockCountId
                            );


            return buildResponse(
                    stockCount
            );
        }


        /*
         * update = 0:
         *
         * - task không tồn tại
         * - khác warehouse
         * - người khác đã claim
         * - task không còn PLANNED
         */

        StockCounts stockCount =
                validationService
                        .getStockCount(
                                stockCountId
                        );


        validationService.validateWarehouse(
                staff,
                stockCount
        );


        /*
         * Chính mình đã start trước đó.
         * Cho phép idempotent.
         */
        if (stockCount.getAssignedTo() != null
                && stockCount
                .getAssignedTo()
                .getId()
                .equals(staff.getId())

                &&

                stockCount.getStatus()
                        == StockCountStatus.COUNTING) {

            return buildResponse(
                    stockCount
            );
        }


        if (stockCount.getAssignedTo() != null
                && !stockCount
                .getAssignedTo()
                .getId()
                .equals(staff.getId())) {

            throw new Forbidden(
                    "Stock count has already been claimed by another staff"
            );
        }


        throw new BadRequest(
                "Stock count cannot be started"
        );
    }


    // =====================================================
    // COUNT / RECOUNT
    // =====================================================

    @Transactional
    public StaffStockCountDetailResponse countItem(
            Long stockCountId,
            StaffStockCountItemRequest request
    ) {

        Users staff =
                validationService.getCurrentUser();


        StockCounts stockCount =
                validationService
                        .getStockCount(
                                stockCountId
                        );


        validationService.validateWarehouse(
                staff,
                stockCount
        );


        validationService.validateOwner(
                staff,
                stockCount
        );


        validationService.validateEditable(
                stockCount
        );


        StockCountItems item =
                validationService
                        .getItem(
                                stockCountId,
                                request.getProductBarcode(),
                                request.getLocationCode()
                        );


        int quantity =
                request.getQuantity();


        if (quantity < 0) {

            throw new BadRequest(
                    "Count quantity cannot be negative"
            );
        }


        // =================================================
        // FIRST COUNT
        // =================================================

        if (stockCount.getStatus()
                == StockCountStatus.COUNTING) {

            item.setFirstCountQuantity(
                    quantity
            );


            item.setCountedBy(
                    staff
            );


            item.setDifferenceQuantity(
                    quantity
                            - item.getSystemQuantity()
            );
        }


        // =================================================
        // RECOUNT
        // =================================================

        else if (stockCount.getStatus()
                == StockCountStatus.RECOUNTING) {

            item.setSecondCountQuantity(
                    quantity
            );


            item.setRecountedBy(
                    staff
            );


            item.setDifferenceQuantity(
                    quantity
                            - item.getSystemQuantity()
            );
        }


        /*
         * Không set InventoryBalance.
         *
         * Không tạo InventoryAdjustment.
         *
         * Không set finalQuantity tại đây.
         *
         * Manager là người quyết định kết quả cuối.
         */
        stockCountItemRepository.save(
                item
        );


        return buildResponse(
                stockCount
        );
    }


    private StaffStockCountDetailResponse buildResponse(
            StockCounts stockCount
    ) {

        return mapper.toDetailResponse(
                stockCount,

                stockCountItemRepository
                        .findByStockCountIdOrderByIdAsc(
                                stockCount.getId()
                        )
        );
    }
}