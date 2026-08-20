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


        return stockCountRepository
                .findAvailableAndMine(
                        staff.getWarehouse().getId(),
                        staff.getId(),
                        StockCountStatus.COMPLETED,
                        StockCountStatus.CANCELLED
                )

                .stream()

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


        /*
         * Detail:
         *
         * - chưa ai nhận -> được xem để quyết định claim
         * - mình nhận    -> được xem
         * - người khác nhận -> không được xem thao tác
         */
        if (stockCount.getAssignedTo() != null
                && !stockCount
                .getAssignedTo()
                .getId()
                .equals(staff.getId())) {

            throw new Forbidden(
                    "Stock count is being handled by another staff"
            );
        }


        return mapper.toDetailResponse(
                stockCount,

                stockCountItemRepository
                        .findByStockCountIdOrderByIdAsc(
                                stockCountId
                        )
        );
    }
}