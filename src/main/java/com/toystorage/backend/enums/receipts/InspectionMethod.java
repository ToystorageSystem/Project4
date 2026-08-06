package com.toystorage.backend.enums.receipts;

public enum InspectionMethod {

    /*
     * Đếm và kiểm tra toàn bộ sản phẩm trong kiện.
     */
    FULL_COUNT,

    /*
     * Chỉ kiểm tra một phần sản phẩm theo mẫu.
     */
    SAMPLE_CHECK,

    /*
     * Chỉ kiểm tra seal và tình trạng bên ngoài.
     */
    SEAL_ONLY
}