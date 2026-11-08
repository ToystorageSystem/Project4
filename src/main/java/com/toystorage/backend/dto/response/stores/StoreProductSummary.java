package com.toystorage.backend.dto.response.stores;

import java.math.BigDecimal;

/** One row in the store product list. */
public record StoreProductSummary(
        Long productId,
        String productCode,
        String barcode,
        String name,
        String imageUrl,
        BigDecimal sellingPrice,
        String baseUnit,
        String categoryName,
        String brandName,
        ShelfInfo shelf,
        int shelfQuantity,
        int totalQuantity) {
}