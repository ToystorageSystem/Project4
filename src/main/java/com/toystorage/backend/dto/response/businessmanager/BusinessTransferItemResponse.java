
package com.toystorage.backend.dto.response.businessmanager;

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
public class BusinessTransferItemResponse {

    private Long id;

    private Long productId;

    private String productCode;

    private String productName;

    private Integer requestedQuantity;

    private Integer approvedQuantity;

    private Integer pickedQuantity;

    private Integer packedQuantity;

    private Integer shippedQuantity;

    private Integer receivedQuantity;

    private Integer shortageQuantity;

    private Integer surplusQuantity;
}
