package com.toystorage.backend.dto.response.shipments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InTransitTransferResponse {

    private Long transferId;

    private String transferCode;

    private String transferStatus;

    private String transferType;

    private LocalDate expectedShipmentDate;

    private LocalDate expectedReceiptDate;

    private Integer totalProducts;

    private Integer totalRequestedQuantity;

    private Integer totalShippedQuantity;

    private Integer totalReceivedQuantity;

    @Builder.Default
    private List<InTransitTransferProductResponse> products =
            new ArrayList<>();
}