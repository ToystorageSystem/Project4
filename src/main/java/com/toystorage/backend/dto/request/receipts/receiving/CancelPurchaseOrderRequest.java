package com.toystorage.backend.dto.request.receipts.receiving;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelPurchaseOrderRequest {

    @NotBlank(message = "Cancel reason is required")
    private String reason;

    // Với ORDERED, người dùng phải xác nhận nhà cung cấp đồng ý hủy.
    private Boolean supplierAgreed;
}