package com.toystorage.backend.dto.response.shipments;

import lombok.*;

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