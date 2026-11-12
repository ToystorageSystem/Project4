package com.toystorage.backend.dto.request.products;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProductWithSupplierRequest {
    @Valid
    @NotNull(message = "Product details are required")
    private CreateProductRequest product;

    // Supplier cu: truyen ID; Supplier moi: nhap ten (ID de null).
    @Positive(message = "Supplier ID must be greater than 0")
    private Long supplierId;

    @Size(max = 200, message = "Supplier name cannot exceed 200 characters")
    private String supplierName;

    @Pattern(regexp = "^$|^\\+?[0-9]{9,15}$", message = "Phone number must contain 9 to 15 digits")
    private String supplierPhone;

    @Size(max = 100, message = "Supplier product code cannot exceed 100 characters")
    private String supplierProductCode;

    @NotNull(message = "Minimum order quantity is required")
    @Min(value = 1, message = "Minimum order quantity must be at least 1")
    private Integer minimumOrderQuantity;

    // Giao dien khong nhap Lead Time: gia tri mac dinh la 0.
    @Min(value = 0, message = "Lead time days cannot be negative")
    private Integer leadTimeDays = 0;
}
