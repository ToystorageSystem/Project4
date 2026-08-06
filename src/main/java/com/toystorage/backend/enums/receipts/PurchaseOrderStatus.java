package com.toystorage.backend.enums.receipts;

public enum PurchaseOrderStatus {

    /*
     * Business Staff đang tạo đơn.
     */
    DRAFT,


    ORDERED,

    /*
     * Kho mới nhận được một phần hàng.
     */
    PARTIALLY_RECEIVED,

    /*
     * Kho đã nhận đủ toàn bộ hàng.
     */
    COMPLETED,

    /*
     * Đơn mua hàng đã bị hủy.
     */
    CANCELLED
}