package com.toystorage.backend.enums.receipts;

public enum StoreReceiptStatus {

    /*
     * Chưa bắt đầu nhận hàng.
     */
    PENDING,

    /*
     * Store đang kiểm nhận hàng.
     */
    RECEIVING,

    /*
     * Store đã kiểm số lượng và tình trạng hàng.
     */
    INSPECTED,

    /*
     * Store Manager đã xác nhận kết quả.
     */
    CONFIRMED,

    /*
     * Hoàn tất nhận hàng.
     * Tại bước này Store mới được tăng tồn kho.
     */
    COMPLETED,

    /*
     * Store từ chối nhận hàng.
     */
    REJECTED,

    /*
     * Phiếu nhận bị hủy.
     */
    CANCELLED
}