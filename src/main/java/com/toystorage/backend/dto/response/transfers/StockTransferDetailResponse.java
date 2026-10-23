package com.toystorage.backend.dto.response.transfers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransferDetailResponse {

    private Long id;

    private String transferCode;

    private String status;

    private String transferType;

    /*
     * Source warehouse/store
     */
    private Long fromWarehouseId;

    private String fromWarehouseCode;

    private String fromWarehouseName;

    private String fromWarehouseType;

    /*
     * Destination store
     */
    private Long toWarehouseId;

    private String toWarehouseCode;

    private String toWarehouseName;

    private String toWarehouseType;

    /*
     * Business information
     */
    private LocalDate expectedShipmentDate;

    private LocalDate expectedReceiptDate;

    private String reasonCode;

    private String reasonNote;

    private String notes;

    /*
     * Task #12
     */
    private String cancelReason;

    /*
     * Summary
     */
    private Integer totalProducts;

    private Integer totalRequestedQuantity;

    /*
     * Creator
     */
    private Long createdById;

    private String createdByCode;

    private String createdByName;

    /*
     * Confirmation
     */
    private Long confirmedById;

    private String confirmedByCode;

    private String confirmedByName;

    private LocalDateTime confirmedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<StockTransferItemResponse> items;
}