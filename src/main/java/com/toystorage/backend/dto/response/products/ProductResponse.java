package com.toystorage.backend.dto.response.products;

import com.toystorage.backend.enums.products.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String productCode;
    private String barcode;
    private String imageUrl;
    private String name;

    private Long categoryId;
    private String categoryCode;
    private String categoryName;

    private Long brandId;
    private String brandCode;
    private String brandName;

    private String baseUnit;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private ProductStatus status;

    private Long createdById;
    private String createdByName;

    private Long approvedById;
    private String approvedByName;

    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}