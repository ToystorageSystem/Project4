package com.toystorage.backend.dto.response.inventories;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscrepancyItemResponse {

    private Long productId;

    private String productName;

    private Integer expectedQuantity;

    private Integer actualQuantity;

    private Integer differenceQuantity;

    private String inspectionResult;

    private String notes;
}