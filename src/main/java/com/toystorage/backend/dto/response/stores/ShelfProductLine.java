package com.toystorage.backend.dto.response.stores;

public record ShelfProductLine(Long productId, String productCode, String barcode, String name,
                               int shelfQuantity, int backroomQuantity, int minShelfQuantity,
                               boolean lowStock) {
}