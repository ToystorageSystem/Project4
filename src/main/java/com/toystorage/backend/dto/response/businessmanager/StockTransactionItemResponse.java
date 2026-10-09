
package com.toystorage.backend.dto.response.businessmanager;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockTransactionItemResponse {

    // Thong tin san pham
    private Long productId;
    private String productCode;
    private String productName;

    // So luong cua phieu nhap
    private Integer expectedQuantity;
    private Integer actualQuantity;
    private Integer acceptedQuantity;
    private Integer damagedQuantity;

    // So luong cua phieu xuat
    private Integer requestedQuantity;
    private Integer pickedQuantity;
    private Integer approvedQuantity;
    private Integer packedQuantity;
    private Integer shippedQuantity;
    private Integer receivedQuantity;

    // Chenh lech so luong
    private Integer shortageQuantity;
    private Integer surplusQuantity;
}
