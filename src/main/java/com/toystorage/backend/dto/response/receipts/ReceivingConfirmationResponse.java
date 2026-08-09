package com.toystorage.backend.dto.response.receipts;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingConfirmationResponse {

    private Long receiptId;
    private String receiptCode;
    private String status;

    private Long staffId;
    private String staffName;

    private Integer totalExpectedQuantity;
    private Integer totalActualQuantity;
    private Integer totalAcceptedQuantity;
    private Integer totalDamagedQuantity;
    private Integer totalShortageQuantity;
    private Integer totalSurplusQuantity;

    private List<ReceivingConfirmationItemResponse> items;

    private Long confirmedBy;
    private String confirmedByName;
    private LocalDateTime confirmedAt;
}