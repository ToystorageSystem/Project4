package com.toystorage.backend.dto.response.stores;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Product detail screen (by id or by barcode). */
public record StoreProductDetail(
        Long productId,
        String productCode,
        String barcode,
        String name,
        String imageUrl,
        BigDecimal sellingPrice,
        String baseUnit,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        String brandCountry,
        ShelfInfo shelf,
        int shelfQuantity,
        int backroomQuantity,
        int totalQuantity,
        int minShelfQuantity,
        boolean lowStock,
        LocalDateTime stockUpdatedAt) {
}