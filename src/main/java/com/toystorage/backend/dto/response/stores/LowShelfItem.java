package com.toystorage.backend.dto.response.stores;

/** product to refill: shelf <= min and backroom > 0 */
public record LowShelfItem(Long productId, String productCode, String barcode, String name,
                           ShelfInfo shelf, int shelfQuantity, int backroomQuantity,
                           int minShelfQuantity) {
}