package com.toystorage.backend.dto.request.receipts;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PurchaseOrderItemRequest {

    @NotNull(message = "Product id is required")
    @Positive(message = "Product id must be greater than 0")
    private Long productId;


    @NotNull(message = "Ordered quantity is required")
    @Min(
            value = 1,
            message = "Ordered quantity must be greater than 0"
    )
    private Integer orderedQuantity;


    /*
     * Nếu không gửi unitPrice:
     * backend tự lấy purchase_price từ supplier_products.
     *
     * Nếu gửi giá khác giá NCC:
     * Service sẽ kiểm tra quyền điều chỉnh giá.
     */
    @DecimalMin(
            value = "0.01",
            message = "Unit price must be greater than 0"
    )
    private BigDecimal unitPrice;
}