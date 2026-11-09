package com.toystorage.backend.enums.receipts;

public enum PackageCondition {

    /*
     * Kiện hàng bình thường.
     */
    GOOD,

    /*
     * Kiện bị móp nhưng chưa ảnh hưởng sản phẩm.
     */
    DENTED,

    /*
     * Kiện bị rách.
     */
    TORN,

    /*
     * Kiện bị ướt.
     */
    WET,

    /*
     * Kiện hư hỏng nghiêm trọng.
     */
    DAMAGED
}