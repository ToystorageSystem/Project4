package com.toystorage.backend.enums.inventories;

public enum StockCountStatus {

    /*
     * Hệ thống đã tạo kế hoạch kiểm kê định kỳ.
     */
    PLANNED,

    /*
     * Nhân viên đang thực hiện kiểm đếm lần đầu.
     */
    COUNTING,

    /*
     * Nhân viên hoặc Store Manager đang sửa
     * kết quả kiểm đếm bị nhập sai.
     */
    CORRECTING,

    /*
     * Có chênh lệch và đang thực hiện đếm lại.
     */
    RECOUNTING,

    /*
     * Nhân viên đã gửi kết quả,
     * đang chờ Store Manager xác nhận.
     */
    PENDING_CONFIRMATION,

    /*
     * Store Manager đã xác nhận.
     * Tồn kho thực tế đã được cập nhật.
     */
    COMPLETED,

    /*
     * Kế hoạch kiểm kê bị hủy trước khi hoàn tất.
     */
    CANCELLED
}