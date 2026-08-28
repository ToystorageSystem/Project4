package com.toystorage.backend.dto.response.inventories.discrepancy;

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
public class BusinessManagerDiscrepancyItemResponse {

    private Long productId;

    private String productCode;

    private String productName;

    private String barcode;

    private Integer expectedQuantity;

    private Integer actualQuantity;

    private Integer differenceQuantity;

    /*
     * GOODS RECEIPT
     */
    private Integer orderedQuantity;

    private Integer acceptedQuantity;

    /*
     * STOCK TRANSFER
     */
    private Integer shippedQuantity;

    private Integer receivedQuantity;

    /*
     * Số lượng cuối cùng hiện đang được
     * chứng từ nguồn ghi nhận.
     *
     * Goods Receipt -> acceptedQuantity
     * Stock Transfer -> receivedQuantity
     */
    private Integer finalQuantity;

    /*
     * Receiving inspection
     */
    private String inspectionResult;

    private String notes;

    private String evidenceImageUrl;
}