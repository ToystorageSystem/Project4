package com.toystorage.backend.dto.request.suppliers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelSupplierInvoiceRequest {

    @NotBlank(message = "Cancel reason is required")
    @Size(
            max = 500,
            message = "Cancel reason must not exceed 500 characters"
    )
    private String reason;
}