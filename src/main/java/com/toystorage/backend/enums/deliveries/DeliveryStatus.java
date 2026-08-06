package com.toystorage.backend.enums.deliveries;

public enum DeliveryStatus {

    /*
     * Chuyến giao hàng đã được tạo.
     */
    CREATED,

    /*
     * Đã phân công người hoặc bộ phận vận chuyển.
     */
    ASSIGNED,

    /*
     * Hàng đã sẵn sàng xuất kho.
     */
    READY_TO_SHIP,

    /*
     * Hàng đã rời kho và đang vận chuyển.
     */
    IN_TRANSIT,

    /*
     * Hàng đã đến địa điểm nhận nhưng chưa bàn giao hoàn tất.
     */
    ARRIVED,

    /*
     * Hàng đã được bàn giao thành công.
     */
    DELIVERED,

    /*
     * Giao hàng thất bại.
     */
    FAILED,

    /*
     * Chuyến giao hàng đã bị hủy.
     */
    CANCELLED
}