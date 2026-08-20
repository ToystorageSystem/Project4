package com.toystorage.backend.services.inventories;

import com.toystorage.backend.dto.response.inventories.StaffStockCountDetailResponse;
import com.toystorage.backend.dto.response.inventories.StaffStockCountListResponse;

import com.toystorage.backend.entity.inventories.StockCountItems;
import com.toystorage.backend.entity.inventories.StockCounts;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.inventories.StockCountStatus;

import com.toystorage.backend.mapper.inventories.StaffStockCountMapper;

import com.toystorage.backend.repository.inventories.StaffStockCountItemRepository;
import com.toystorage.backend.repository.inventories.StaffStockCountRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffStockCountQueryService {

    private final StaffStockCountRepository
            stockCountRepository;

    private final StaffStockCountItemRepository
            stockCountItemRepository;

    private final StaffStockCountValidationService
            validationService;

    private final StaffStockCountMapper
            mapper;


    @Transactional(readOnly = true)
    public List<StaffStockCountListResponse>
    getAvailableAndMine() {

        Users staff =
                validationService.getCurrentUser();


        if (staff.getWarehouse() == null) {
            return List.of();
        }


        List<StockCounts> stockCounts =
                stockCountRepository
                        .findByWarehouseIdAndStatusInOrderByScheduledDateAsc(
                                staff.getWarehouse().getId(),
                                List.of(
                                        StockCountStatus.PLANNED,
                                        StockCountStatus.COUNTING,
                                        StockCountStatus.RECOUNTING,
                                        StockCountStatus.PENDING_CONFIRMATION
                                )
                        );


        return stockCounts.stream()

                /*
                 * PLANNED:
                 * chưa ai bắt đầu -> hiện cho mọi Staff.
                 *
                 * Các task đang xử lý:
                 * có thể vẫn hiện để theo dõi,
                 * nhưng quyền WRITE sẽ do claim service chặn.
                 */

                .map(stockCount -> {

                    List<StockCountItems> items =
                            stockCountItemRepository
                                    .findByStockCountIdOrderByIdAsc(
                                            stockCount.getId()
                                    );


                    return mapper.toListResponse(
                            stockCount,
                            items
                    );
                })

                .toList();
    }


    @Transactional(readOnly = true)
    public StaffStockCountDetailResponse getDetail(
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


        return mapper.toDetailResponse(
                stockCount,

                stockCountItemRepository
                        .findByStockCountIdOrderByIdAsc(
                                stockCountId
                        )
        );
    }
}