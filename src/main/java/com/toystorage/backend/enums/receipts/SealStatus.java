package com.toystorage.backend.enums.receipts;

public enum SealStatus {

    /*
     * Seal còn nguyên.
     */
    INTACT,

    /*
     * Seal bị rách, bị mở hoặc không còn nguyên trạng.
     */
    BROKEN,

    /*
     * Kiện hàng không có seal.
     */
    MISSING
}