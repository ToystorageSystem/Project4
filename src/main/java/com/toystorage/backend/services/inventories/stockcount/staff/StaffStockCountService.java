package com.toystorage.backend.services.inventories.stockcount.staff;

import com.toystorage.backend.dto.request.inventories.stockcount.StaffStockCountItemRequest;

import com.toystorage.backend.dto.response.inventories.stockcount.StaffStockCountDetailResponse;
import com.toystorage.backend.dto.response.inventories.stockcount.StaffStockCountListResponse;



import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffStockCountService {

    private final StaffStockCountQueryService
            queryService;

    private final StaffStockCountExecutionService
            executionService;

    private final StaffStockCountSubmissionService
            submissionService;



    public List<StaffStockCountListResponse>
    getTasks() {

        return queryService
                .getAvailableAndMine();
    }


    public StaffStockCountDetailResponse getDetail(
            Long stockCountId
    ) {

        return queryService
                .getDetail(
                        stockCountId
                );
    }


    public StaffStockCountDetailResponse start(
            Long stockCountId
    ) {

        return executionService
                .start(
                        stockCountId
                );
    }


    public StaffStockCountDetailResponse countItem(
            Long stockCountId,
            StaffStockCountItemRequest request
    ) {

        return executionService
                .countItem(
                        stockCountId,
                        request
                );
    }


    public StaffStockCountDetailResponse submit(
            Long stockCountId
    ) {

        return submissionService
                .submit(
                        stockCountId
                );
    }
}