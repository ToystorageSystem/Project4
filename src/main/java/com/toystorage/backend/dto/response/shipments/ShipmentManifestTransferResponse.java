package com.toystorage.backend.dto.response.shipments;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentManifestTransferResponse {

    /*
     * Stock Transfer
     */
    private Long transferId;

    private String transferCode;

    private String transferStatus;

    private String transferType;


    /*
     * Expected dates
     */
    private LocalDate expectedShipmentDate;

    private LocalDate expectedReceiptDate;


    /*
     * Summary
     */
    private Integer totalProducts;

    private Integer totalRequestedQuantity;
}