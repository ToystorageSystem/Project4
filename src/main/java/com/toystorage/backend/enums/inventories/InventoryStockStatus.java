package com.toystorage.backend.enums.inventories;

public enum InventoryStockStatus {

    /**
     * Tồn khả dụng lớn hơn mức tồn tối thiểu.
     */
    IN_STOCK,

    /**
     * Tồn khả dụng vẫn còn nhưng nhỏ hơn
     * hoặc bằng mức tồn tối thiểu.
     */
    LOW_STOCK,

    /**
     * Tồn khả dụng bằng 0.
     */
    OUT_OF_STOCK
}