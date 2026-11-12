package com.toystorage.backend.dto.response.receipts.receiving;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingProductProgressResponse {

    private Long productId;

    private String productCode;

    private String productName;

    private String barcode;

    private Integer expectedQuantity;

    private Integer actualQuantity;

    private Integer acceptedQuantity;

    private Integer damagedQuantity;

    private Integer shortageQuantity;

    private Integer surplusQuantity;

    /*
     * true  = Staff đã kiểm product này.
     * false = Chưa kiểm.
     */
    private Boolean inspected;

    /*
     * MATCHED
     * SHORTAGE
     * SURPLUS
     * DAMAGED
     * PARTIAL
     *
     * null nếu chưa kiểm.
     */
    private String inspectionResult;

    private String packageCode;

    private String evidenceImage;

    private String notes;

    private Long inspectedById;

    private String inspectedByName;

    private LocalDateTime inspectedAt;
}