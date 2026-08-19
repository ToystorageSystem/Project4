package com.toystorage.backend.dto.response.stores;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReturnedGoodsListResponse {

    private Long returnId;

    private String returnCode;

    private String status;

    private Long storeId;

    private String storeName;

    private Long warehouseId;

    private String warehouseName;

    private Integer totalExpectedQuantity;

    private Integer totalReceivedQuantity;
}