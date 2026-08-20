package com.toystorage.backend.services.inventories;

import com.toystorage.backend.dto.response.inventories.StaffStockCountDetailResponse;

import com.toystorage.backend.entity.inventories.StockCountItems;
import com.toystorage.backend.entity.inventories.StockCounts;

import com.toystorage.backend.enums.warehouses.WarehouseTaskType;
import com.toystorage.backend.services.warehouses.WarehouseTaskClaimService;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.inventories.StockCountStatus;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.mapper.inventories.StaffStockCountMapper;

import com.toystorage.backend.repository.inventories.StaffStockCountItemRepository;
import com.toystorage.backend.repository.inventories.StaffStockCountRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffStockCountSubmissionService {

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

    @Transactional
    public StaffStockCountDetailResponse submit(
            Long stockCountId
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


        List<StockCountItems> items =
                stockCountItemRepository
                        .findByStockCountIdOrderByIdAsc(
                                stockCountId
                        );


        if (items.isEmpty()) {

            throw new BadRequest(
                    "Stock count contains no items"
            );
        }


        // =================================================
        // FIRST COUNT
        // =================================================

        if (stockCount.getStatus()
                == StockCountStatus.COUNTING) {

            boolean incomplete =
                    items.stream()
                            .anyMatch(item ->
                                    item.getFirstCountQuantity()
                                            == null
                            );


            if (incomplete) {

                throw new BadRequest(
                        "All items must be counted before submission"
                );
            }
        }


        // =================================================
        // SECOND COUNT
        // =================================================

        else if (stockCount.getStatus()
                == StockCountStatus.RECOUNTING) {

            boolean incomplete =
                    items.stream()
                            .anyMatch(item ->
                                    item.getSecondCountQuantity()
                                            == null
                            );


            if (incomplete) {

                throw new BadRequest(
                        "All items must be recounted before submission"
                );
            }
        }


        stockCount.setStatus(
                StockCountStatus.PENDING_CONFIRMATION
        );


        stockCount.setCompletedAt(
                LocalDateTime.now()
        );


        stockCountRepository.save(
                stockCount
        );

        taskClaimService.release(
                WarehouseTaskType.STOCK_COUNT,
                stockCountId,
                staff
        );
        /*
         * DỪNG TẠI ĐÂY.
         *
         * Warehouse Staff:
         *
         * không chỉnh tồn
         * không set COMPLETED
         * không approve
         * không tạo adjustment
         */


        return mapper.toDetailResponse(
                stockCount,
                items
        );
    }
}