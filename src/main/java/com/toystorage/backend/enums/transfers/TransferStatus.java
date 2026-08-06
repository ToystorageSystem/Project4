package com.toystorage.backend.enums.transfers;

public enum TransferStatus {

    /*
     * Business đang tạo phiếu.
     */
    DRAFT,

    /*
     * Store nhận đã gửi yêu cầu đến Store nguồn.
     * Chỉ dùng khi điều chuyển Store -> Store.
     */
    PENDING_SOURCE_CONFIRMATION,
    /*
     * Phiếu đã được tạo/xác nhận.
     * Tại bước nghiệp vụ này tồn kho nguồn bị trừ.
     */
    CREATED,

    /*
     * Nhân viên kho đang lấy hàng.
     */
    PICKING,

    /*
     * Kho đang đóng kiện.
     */
    PACKING,

    /*
     * Đã hoàn tất đóng hàng.
     */
    PACKED,

    /*
     * Hàng đã rời kho và đang vận chuyển.
     */
    SHIPPED,

    /*
     * Địa điểm nhận đang kiểm hàng.
     */
    RECEIVING,

    /*
     * Địa điểm nhận đã xác nhận hàng.
     */
    RECEIVED,

    /*
     * Hoàn tất toàn bộ phiếu điều chuyển.
     */
    COMPLETED,
    /*
     * Store nguồn từ chối yêu cầu.
     */
    REJECTED,

    /*
     * Phiếu bị hủy.
     *
     * Nếu hàng chưa giao thì hoàn tồn về kho nguồn.
     */
    CANCELLED
}