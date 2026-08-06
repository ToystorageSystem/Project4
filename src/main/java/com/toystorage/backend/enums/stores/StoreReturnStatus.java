package com.toystorage.backend.enums.stores;

public enum StoreReturnStatus {

    /*
     * Store đang tạo yêu cầu.
     */
    DRAFT,

    /*
     * Chờ Business hoặc Warehouse xác nhận.
     */
    PENDING_APPROVAL,

    /*
     * Business hoặc Warehouse đã xác nhận.
     *
     * Tại thời điểm này Store trừ tồn ngay.
     */
    APPROVED,

    /*
     * Store đang đóng hàng.
     */
    PACKING,

    /*
     * Store đã bàn giao hàng.
     */
    ISSUED,

    /*
     * Hàng đang được vận chuyển về kho.
     */
    SHIPPED,

    /*
     * Kho đã nhận và kiểm hàng hoàn tất.
     *
     * Tại thời điểm này kho được cộng tồn.
     */
    RECEIVED,

    /*
     * Phiếu trả hàng bị hủy.
     */
    CANCELLED
}