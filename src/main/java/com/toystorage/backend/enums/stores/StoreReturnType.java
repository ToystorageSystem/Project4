package com.toystorage.backend.enums.stores;

public enum StoreReturnType {

    /*
     * Hàng bị lỗi hoặc hư hỏng.
     */
    DAMAGED,

    /*
     * Store nhận dư hàng.
     */
    SURPLUS,

    /*
     * Thu hồi sản phẩm từ toàn hệ thống.
     */
    RECALL,

    /*
     * Hàng bán chậm, Business điều phối về kho.
     */
    SLOW_MOVING,

    /*
     * Kho hoặc Business điều sai sản phẩm.
     */
    WRONG_TRANSFER,

    /*
     * Store đóng cửa hoặc chuyển địa điểm.
     */
    STORE_CLOSURE,

    /*
     * Các lý do khác.
     */
    OTHER
}