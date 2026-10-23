package com.toystorage.backend.dto.response.inventories.discrepancy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessManagerDiscrepancyResponse {

    private Long id;

    private String reportCode;

    private String discrepancyType;

    private String status;

    /*
     * GOODS_RECEIPT hoặc STOCK_TRANSFER
     */
    private String referenceType;

    private Long referenceId;

    private String referenceCode;

    private String sourceStatus;

    /*
     * Nơi phát sinh discrepancy
     */
    private Long warehouseId;

    private String warehouseCode;

    private String warehouseName;

    private String description;

    private String evidenceImageUrl;

    private String responsibleParty;

    /*
     * Người báo cáo
     */
    private Long reportedBy;

    private String reportedByName;

    /*
     * Người đã review trước khi chuyển lên BM
     */
    private Long reviewedBy;

    private String reviewedByName;

    private LocalDateTime reviewedAt;

    /*
     * Kết quả xử lý
     */
    private String resolutionAction;

    private String resolutionNote;

    private Long resolvedBy;

    private String resolvedByName;

    private LocalDateTime resolvedAt;

    /*
     * Nếu là Goods Receipt
     */
    private Long purchaseOrderId;

    private String purchaseOrderCode;

    /*
     * Nếu là Stock Transfer
     */
    private Long fromWarehouseId;

    private String fromWarehouseCode;

    private String fromWarehouseName;

    private Long toWarehouseId;

    private String toWarehouseCode;

    private String toWarehouseName;

    /*
     * Xác nhận của Warehouse
     */
    private Long warehouseConfirmedBy;

    private String warehouseConfirmedByName;

    private LocalDateTime warehouseConfirmedAt;

    /*
     * Xác nhận của Store
     */
    private Long storeReceiptId;

    private String storeReceiptCode;

    private String storeReceiptStatus;

    private Long storeConfirmedBy;

    private String storeConfirmedByName;

    private LocalDateTime storeConfirmedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<BusinessManagerDiscrepancyItemResponse> items;
}