package com.toystorage.backend.dto.response.receipts.store;

/** One product line inside a package (F2). actual/damaged null = not counted yet */
public record PackageCheckLineResponse(
        Long productId,
        String productCode,
        String barcode,
        String name,
        int expectedQuantity,           // 0 = not in the package (surplus line)
        Integer actualQuantity,
        Integer damagedQuantity,
        String note) {
}