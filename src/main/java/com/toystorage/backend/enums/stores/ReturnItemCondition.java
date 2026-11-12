package com.toystorage.backend.enums.stores;

public enum ReturnItemCondition {

    /*
     * Hàng bình thường, còn có thể bán.
     */
    NORMAL,

    /*
     * Hàng bị lỗi hoặc hư hỏng.
     */
    DAMAGED,

    /*
     * Hàng hết hạn sử dụng.
     */
    EXPIRED,

    /*
     * Hàng cần được cách ly để kiểm tra thêm.
     */
    QUARANTINE
}