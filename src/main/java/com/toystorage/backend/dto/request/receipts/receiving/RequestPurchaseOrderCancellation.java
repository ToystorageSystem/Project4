package com.toystorage.backend.dto.request.receipts.receiving;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestPurchaseOrderCancellation {

    @NotBlank(message = "Cancellation reason is required")
    @Size(min = 10, max = 1000, message = "Cancellation reason must contain 10 to 1000 characters")
    private String reason;

    // Nhân viên xác nhận đã trao đổi và NCC đồng ý hủy.
    @NotNull(message = "Supplier agreement confirmation is required")
    private Boolean supplierAgreed;
}
