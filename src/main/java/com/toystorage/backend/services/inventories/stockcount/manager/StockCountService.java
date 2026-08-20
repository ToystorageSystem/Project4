package com.toystorage.backend.services.inventories.stockcount.manager;

import com.toystorage.backend.dto.request.inventories.stockcount.StockCountRequest;
import com.toystorage.backend.dto.response.inventories.stockcount.StockCountItemResponse;
import com.toystorage.backend.dto.response.inventories.stockcount.StockCountResponse;

import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.inventories.StockCountItems;
import com.toystorage.backend.entity.inventories.StockCounts;

import com.toystorage.backend.enums.inventories.StockCountStatus;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.mapper.inventories.stockcount.StockCountMapper;

import com.toystorage.backend.repository.inventories.InventoryBalanceRepository;
import com.toystorage.backend.repository.inventories.StockCountItemRepository;
import com.toystorage.backend.repository.inventories.StockCountRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockCountService {

    private final StockCountRepository
            stockCountRepository;

    private final StockCountItemRepository
            stockCountItemRepository;

    private final InventoryBalanceRepository
            inventoryBalanceRepository;

    private final StockCountValidationService
            validationService;

    private final StockCountMapper
            mapper;


    // =====================================================
    // XEM KẾ HOẠCH
    // =====================================================

    @Transactional(readOnly = true)
    public StockCountResponse getStockCount(
            Long stockCountId
    ) {

        StockCounts stockCount =
                validationService
                        .getStockCount(stockCountId);

        Users user =
                validationService
                        .getCurrentUser();

        validationService.validateWarehouse(
                user,
                stockCount
        );

        return buildResponse(
                stockCount
        );
    }


    // =====================================================
    // BẮT ĐẦU KIỂM KÊ
    // Snapshot tồn hiện tại
    // =====================================================

    @Transactional
    public StockCountResponse start(
            Long stockCountId
    ) {

        StockCounts stockCount =
                validationService
                        .getStockCount(stockCountId);

        Users manager =
                validationService
                        .getCurrentUser();

        validationService.validateWarehouse(
                manager,
                stockCount
        );

        if (stockCount.getStatus()
                != StockCountStatus.PLANNED) {

            throw new BadRequest(
                    "Only PLANNED stock count can be started"
            );
        }


        /*
         * Lấy toàn bộ tồn kho hiện tại.
         */
        List<InventoryBalances> balances =
                inventoryBalanceRepository
                        .findByWarehouseId(
                                stockCount
                                        .getWarehouse()
                                        .getId()
                        );


        if (balances.isEmpty()) {

            throw new BadRequest(
                    "Warehouse has no inventory"
            );
        }


        for (InventoryBalances balance
                : balances) {

            boolean exists =
                    stockCountItemRepository
                            .existsByStockCountIdAndProductIdAndLocationId(
                                    stockCountId,
                                    balance
                                            .getProduct()
                                            .getId(),
                                    balance
                                            .getLocation()
                                            .getId()
                            );

            if (exists) {
                continue;
            }


            StockCountItems item =
                    new StockCountItems();

            item.setStockCountItemsCode(
                    generateCode()
            );

            item.setStockCount(
                    stockCount
            );

            item.setProduct(
                    balance.getProduct()
            );

            item.setLocation(
                    balance.getLocation()
            );


            /*
             * QUAN TRỌNG:
             *
             * Snapshot tồn hệ thống tại thời điểm
             * bắt đầu kiểm kê.
             */
            item.setSystemQuantity(
                    balance.getQuantity()
            );


            stockCountItemRepository
                    .save(item);
        }


        stockCount.setStatus(
                StockCountStatus.COUNTING
        );

        stockCount.setStartedAt(
                LocalDateTime.now()
        );

        stockCount.setUpdatedAt(
                LocalDateTime.now()
        );

        stockCountRepository.save(
                stockCount
        );


        return buildResponse(
                stockCount
        );
    }


    // =====================================================
    // STAFF NHẬP SỐ LƯỢNG THỰC TẾ
    // =====================================================

    @Transactional
    public StockCountItemResponse countProduct(
            Long stockCountId,
            Long itemId,
            StockCountRequest request
    ) {

        StockCounts stockCount =
                validationService
                        .getStockCount(stockCountId);

        Users staff =
                validationService
                        .getCurrentUser();

        validationService.validateWarehouse(
                staff,
                stockCount
        );


        if (stockCount.getStatus()
                != StockCountStatus.COUNTING) {

            throw new BadRequest(
                    "Stock count is not COUNTING"
            );
        }


        StockCountItems item =
                validationService
                        .getItem(
                                stockCountId,
                                itemId
                        );


        if (item.getFirstCountQuantity()
                != null) {

            throw new BadRequest(
                    "Product has already been counted"
            );
        }


        int countedQuantity =
                request.getQuantity();

        int systemQuantity =
                item.getSystemQuantity();


        int difference =
                countedQuantity
                        - systemQuantity;


        item.setFirstCountQuantity(
                countedQuantity
        );

        item.setDifferenceQuantity(
                difference
        );

        item.setFinalQuantity(
                countedQuantity
        );

        item.setCountedBy(
                staff
        );


        return mapper.toItemResponse(
                stockCountItemRepository
                        .save(item)
        );
    }


    // =====================================================
    // STAFF HOÀN THÀNH KIỂM KÊ
    // =====================================================

    @Transactional
    public StockCountResponse finishCounting(
            Long stockCountId
    ) {

        StockCounts stockCount =
                validationService
                        .getStockCount(stockCountId);


        List<StockCountItems> items =
                stockCountItemRepository
                        .findByStockCountId(
                                stockCountId
                        );


        boolean allCounted =
                !items.isEmpty()
                        &&
                        items.stream()
                                .allMatch(item ->
                                        item.getFirstCountQuantity()
                                                != null
                                );


        if (!allCounted) {

            throw new BadRequest(
                    "Some products have not been counted"
            );
        }


        stockCount.setStatus(
                StockCountStatus.PENDING_CONFIRMATION
        );

        stockCount.setUpdatedAt(
                LocalDateTime.now()
        );


        stockCountRepository.save(
                stockCount
        );


        return buildResponse(
                stockCount
        );
    }


    // =====================================================
    // MANAGER XÁC NHẬN
    // =====================================================

    @Transactional
    public StockCountResponse confirm(
            Long stockCountId
    ) {

        StockCounts stockCount =
                validationService
                        .getStockCount(stockCountId);

        Users manager =
                validationService
                        .getCurrentUser();

        validationService.validateWarehouse(
                manager,
                stockCount
        );


        if (stockCount.getStatus()
                != StockCountStatus.PENDING_CONFIRMATION) {

            throw new BadRequest(
                    "Stock count must be PENDING_CONFIRMATION"
            );
        }


        stockCount.setConfirmedBy(
                manager
        );

        stockCount.setConfirmedAt(
                LocalDateTime.now()
        );

        stockCount.setCompletedAt(
                LocalDateTime.now()
        );

        stockCount.setStatus(
                StockCountStatus.COMPLETED
        );

        stockCount.setUpdatedAt(
                LocalDateTime.now()
        );


        stockCountRepository.save(
                stockCount
        );


        return buildResponse(
                stockCount
        );
    }


    // =====================================================
    // RESPONSE
    // =====================================================

    private StockCountResponse buildResponse(
            StockCounts stockCount
    ) {

        List<StockCountItems> items =
                stockCountItemRepository
                        .findByStockCountId(
                                stockCount.getId()
                        );


        return mapper.toResponse(
                stockCount,
                items
        );
    }


    private String generateCode() {

        return "SCI-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }
}