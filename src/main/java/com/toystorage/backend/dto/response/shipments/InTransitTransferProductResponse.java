package com.toystorage.backend.dto.response.shipments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InTransitTransferProductResponse {

    private Long productId;

    private String productCode;

    private String barcode;

    private String productName;

    private String baseUnit;

    private Integer requestedQuantity;

    private Integer approvedQuantity;

    private Integer packedQuantity;

    private Integer shippedQuantity;

    private Integer receivedQuantity;
}