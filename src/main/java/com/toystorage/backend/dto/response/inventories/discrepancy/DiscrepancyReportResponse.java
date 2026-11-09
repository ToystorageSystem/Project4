package com.toystorage.backend.dto.response.inventories.discrepancy;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscrepancyReportResponse {

    private Long id;
    private String reportCode;

    /*
     * GOODS_RECEIPT = receiving from supplier
     * STORE_RETURN  = store returns to warehouse
     */
    private String referenceType;
    private Long referenceId;

    /*
     * Backward compatibility for existing Receiving frontend/API.
     * For STORE_RETURN this is null.
     */
    private Long goodsReceiptId;

    private String discrepancyType;
    private String severity;
    private Boolean blocking;
    private String status;
    private String description;

    private Long productId;
    private Long warehouseId;

    private String responsibleParty;
    private String resolutionAction;
    private String resolutionNote;

    private Long reviewedBy;
    private String reviewedByName;
    private LocalDateTime reviewedAt;

    private Long resolvedBy;
    private String resolvedByName;
    private LocalDateTime resolvedAt;

    private LocalDateTime createdAt;

    private List<DiscrepancyItemResponse> items;
}
