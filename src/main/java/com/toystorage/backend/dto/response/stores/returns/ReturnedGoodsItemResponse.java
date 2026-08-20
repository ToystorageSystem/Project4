package com.toystorage.backend.dto.response.stores.returns;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReturnedGoodsItemResponse {

    private Long itemId;

    private Long productId;

    private String productCode;

    private String productName;

    private String barcode;

    private Integer expectedQuantity;

    private Integer receivedQuantity;

    private Integer differenceQuantity;

    private String conditionStatus;

    private String note;

    private String evidenceImageUrl;
}