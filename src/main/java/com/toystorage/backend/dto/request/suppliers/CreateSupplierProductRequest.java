package com.toystorage.backend.dto.request.suppliers;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateSupplierProductRequest {

    @NotNull(message = "Product id is required")
    @Positive(message = "Product id must be greater than 0")
    private Long productId;

    @NotNull(message = "Supplier id is required")
    @Positive(message = "Supplier id must be greater than 0")
    private Long supplierId;

    @Size(
            max = 100,
            message = "Supplier product code must not exceed 100 characters"
    )
    private String supplierProductCode;

    @NotNull(message = "Purchase price is required")
    @DecimalMin(
            value = "0.01",
            message = "Purchase price must be greater than 0"
    )
    private BigDecimal purchasePrice;

    @NotNull(message = "Lead time days is required")
    @Min(
            value = 0,
            message = "Lead time days must be greater than or equal to 0"
    )
    private Integer leadTimeDays;

    @NotNull(message = "Minimum order quantity is required")
    @Min(
            value = 1,
            message = "Minimum order quantity must be at least 1"
    )
    private Integer minimumOrderQuantity;

    private Boolean defaultSupplier = false;
}