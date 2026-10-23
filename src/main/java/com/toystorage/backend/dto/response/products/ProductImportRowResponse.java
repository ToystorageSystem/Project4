package com.toystorage.backend.dto.response.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportRowResponse {

    private int rowNumber;

    private String productCode;

    private String barcode;

    private String imageUrl;

    private String name;

    private Long categoryId;

    private Long brandId;

    private String baseUnit;

    private BigDecimal purchasePrice;

    private BigDecimal sellingPrice;

    private String requestReason;

    private boolean valid;

    @Builder.Default
    private List<String> errors = new ArrayList<>();
}