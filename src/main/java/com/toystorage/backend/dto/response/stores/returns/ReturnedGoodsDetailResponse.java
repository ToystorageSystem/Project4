package com.toystorage.backend.dto.response.stores.returns;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReturnedGoodsDetailResponse {

    private Long returnId;

    private String returnCode;

    private String status;

    private String returnType;

    private String reason;

    private Long storeId;

    private String storeName;

    private Long warehouseId;

    private String warehouseName;

    private Integer totalExpectedQuantity;

    private Integer totalReceivedQuantity;

    private Integer totalDifferenceQuantity;

    private String inspectedByName;

    private LocalDateTime inspectionSubmittedAt;

    private List<ReturnedGoodsItemResponse> items;
}