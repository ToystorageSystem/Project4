package com.toystorage.backend.dto.request.products;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateProductRequest {

    @NotBlank(message = "Product code is required")
    @Size(max = 50, message = "Product code cannot exceed 50 characters")
    private String productCode;

    @NotBlank(message = "Barcode is required")
    @Size(max = 100, message = "Barcode cannot exceed 100 characters")
    private String barcode;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name cannot exceed 200 characters")
    private String name;

    @NotNull(message = "Category is required")
    @Positive(message = "Category ID must be greater than 0")
    private Long categoryId;

    @Positive(message = "Brand ID must be greater than 0")
    private Long brandId;

    @NotBlank(message = "Base unit is required")
    @Size(max = 30, message = "Base unit cannot exceed 30 characters")
    private String baseUnit;

    @NotNull(message = "Purchase price is required")
    @DecimalMin(value = "0.01", message = "Purchase price must be greater than 0")
    private BigDecimal purchasePrice;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.01", message = "Selling price must be greater than 0")
    private BigDecimal sellingPrice;

    @Size(max = 500, message = "Request reason cannot exceed 500 characters")
    private String requestReason;
}