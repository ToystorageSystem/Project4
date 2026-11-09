package com.toystorage.backend.services.stores.returns;

import com.toystorage.backend.dto.response.stores.returns.ReturnedGoodsDetailResponse;
import com.toystorage.backend.dto.response.stores.returns.ReturnedGoodsListResponse;

import com.toystorage.backend.entity.stores.StoreReturnItems;
import com.toystorage.backend.entity.stores.StoreReturns;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.stores.StoreReturnStatus;

import com.toystorage.backend.mapper.stores.returns.ReturnedGoodsInspectionMapper;

import com.toystorage.backend.repository.stores.returns.WarehouseReturnItemRepository;
import com.toystorage.backend.repository.stores.returns.WarehouseReturnRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReturnedGoodsQueryService {

    private final WarehouseReturnRepository
            returnRepository;

    private final WarehouseReturnItemRepository
            returnItemRepository;

    private final ReturnedGoodsInspectionValidationService
            validationService;

    private final ReturnedGoodsInspectionMapper
            mapper;


    @Transactional(readOnly = true)
    public List<ReturnedGoodsListResponse>
    getWaitingReturns() {

        Users staff =
                validationService.getCurrentUser();


        if (staff.getWarehouse() == null) {

            return List.of();
        }


        List<StoreReturns> returns =
                returnRepository
                        .findByWarehouseIdAndStatusInOrderByUpdatedAtDesc(
                                staff.getWarehouse().getId(),

                                List.of(
                                        StoreReturnStatus.SHIPPED,
                                        StoreReturnStatus.INSPECTING,
                                        StoreReturnStatus.PENDING_CONFIRMATION
                                )
                        );


        return returns.stream()
                .map(storeReturn -> {

                    List<StoreReturnItems> items =
                            returnItemRepository
                                    .findByStoreReturnId(
                                            storeReturn.getId()
                                    );


                    return mapper.toListResponse(
                            storeReturn,
                            items
                    );
                })
                .toList();
    }


    @Transactional(readOnly = true)
    public ReturnedGoodsDetailResponse getDetail(
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


        return mapper.toDetailResponse(
                storeReturn,
                returnItemRepository
                        .findByStoreReturnId(
                                returnId
                        )
        );
    }
}