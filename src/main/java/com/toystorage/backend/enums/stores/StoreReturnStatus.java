package com.toystorage.backend.enums.stores;

public enum StoreReturnStatus {

    DRAFT,

    PENDING_APPROVAL,

    APPROVED,

    PACKING,

    ISSUED,

    /*
     * Store đã gửi hàng.
     *
     * Warehouse chưa bắt đầu kiểm.
     */
    SHIPPED,

    /*
     * Warehouse Staff đang:
     *
     * - kiểm số lượng
     * - kiểm tình trạng
     * - xử lý vị trí nhận hàng
     *
     * Chưa hoàn thành Store Return.
     */
    INSPECTING,

    /*
     * LEGACY STATUS.
     *
     * Luồng Store Return mới không còn sử dụng
     * bước Warehouse Manager confirmation.
     *
     * Giữ lại tạm thời để tương thích database
     * và dữ liệu cũ.
     */
    PENDING_CONFIRMATION,

    /*
     * Warehouse Staff đã:
     *
     * - kiểm hàng
     * - xác nhận số lượng thực tế
     * - xử lý location
     * - hoàn thành receiving
     *
     * Thiếu/thừa/hỏng nếu có được xử lý
     * bằng discrepancy riêng.
     */
    RECEIVED,

    CANCELLED
}