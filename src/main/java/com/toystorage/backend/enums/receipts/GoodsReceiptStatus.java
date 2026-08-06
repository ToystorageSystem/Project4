package com.toystorage.backend.enums.receipts;

public enum GoodsReceiptStatus {

    /*
     * Phiếu mới được tạo.
     * Chưa thay đổi tồn kho.
     */
    CREATED,

    /*
     * Warehouse Staff đang kiểm nhận hàng.
     * Chưa tăng tồn kho.
     */
    RECEIVING,

    /*
     * Đã kiểm tra số lượng và tình trạng hàng.
     * Chưa tăng tồn kho.
     */
    INSPECTED,

    /*
     * Warehouse Manager đã xác nhận kết quả kiểm hàng.
     */
    CONFIRMED,

    /*
     * Phiếu nhập hoàn tất.
     * Tại thời điểm này hệ thống mới tăng tồn kho.
     */
    COMPLETED,

    /*
     * Phiếu nhập bị hủy.
     */
    CANCELLED
}