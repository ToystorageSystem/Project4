package com.toystorage.backend.dto.response.stores;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoreReturnInspectionItemResponse {

    private Long itemId;

    private Long productId;
    private String productName;

    private Integer requestedQuantity;
    private Integer issuedQuantity;
    private Integer receivedQuantity;

    private Integer approvedQuantity;
    private Integer rejectedQuantity;

    private String conditionStatus;

    private String note;

    private Long fromLocationId;
    private String fromLocationName;
}