package com.toystorage.backend.services.inventories.stockcount.staff;

import com.toystorage.backend.dto.request.inventories.stockcount.StaffStockCountItemRequest;

import com.toystorage.backend.dto.response.inventories.stockcount.StaffStockCountDetailResponse;

import com.toystorage.backend.entity.inventories.StockCountItems;
import com.toystorage.backend.entity.inventories.StockCounts;

import com.toystorage.backend.enums.warehouses.WarehouseTaskType;
import com.toystorage.backend.services.warehouses.taskclaim.WarehouseTaskClaimService;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.inventories.StockCountStatus;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.mapper.inventories.stockcount.StaffStockCountMapper;

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

    private final WarehouseTaskClaimService
            taskClaimService;

    // =====================================================
    // CLAIM + START
    // =====================================================

    @Transactional
    public StaffStockCountDetailResponse start(
            Long stockCountId
    ) {

        Users staff =
                validationService.getCurrentUser();


        StockCounts stockCount =
                validationService.getStockCount(
                        stockCountId
                );


        validationService.validateWarehouse(
                staff,
                stockCount
        );


        /*
         * Chính owner gọi Start lại.
         */
        if (stockCount.getStatus()
                == StockCountStatus.COUNTING) {

            taskClaimService.validateOwner(
                    WarehouseTaskType.STOCK_COUNT,
                    stockCountId,
                    staff
            );

            return buildResponse(
                    stockCount
            );
        }


        if (stockCount.getStatus()
                != StockCountStatus.PLANNED) {

            throw new BadRequest(
                    "Only PLANNED stock count can be started"
            );
        }


        /*
         * Atomic claim qua table chung.
         */
        taskClaimService.claim(
                WarehouseTaskType.STOCK_COUNT,
                stockCountId,
                staff
        );


        stockCount.setStatus(
                StockCountStatus.COUNTING
        );


        stockCount.setStartedAt(
                LocalDateTime.now()
        );


        /*
         * assignedTo có thể giữ để audit.
         *
         * Không dùng nó để lock nữa.
         */
        stockCount.setAssignedTo(
                staff
        );


        stockCountRepository.save(
                stockCount
        );


        return buildResponse(
                stockCount
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


        taskClaimService.validateOwner(
                WarehouseTaskType.STOCK_COUNT,
                stockCountId,
                staff
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