package com.toystorage.backend.enums.deliveries;

public enum DeliveryStatus {

    /*
     * Chuyến vừa được tạo.
     */
    CREATED,

    /*
     * Hệ thống đã tự động phân công Delivery Staff.
     */
    ASSIGNED,

    /*
     * Delivery Staff đã đồng ý nhận chuyến.
     *
     * Chưa đồng nghĩa với đã nhận kiện hàng.
     */
    ACCEPTED,

    /*
     * Delivery Staff từ chối chuyến.
     */
    REJECTED,

    /*
     * Hàng đã sẵn sàng để bàn giao.
     */
    READY_TO_SHIP,

    /*
     * Delivery Staff đã nhận kiện
     * và bắt đầu vận chuyển.
     */
    IN_TRANSIT,

    /*
     * Đã đến điểm giao.
     */
    ARRIVED,

    /*
     * Hoàn tất giao hàng.
     */
    DELIVERED,

    /*
     * Giao hàng thất bại.
     */
    FAILED,

    /*
     * Chuyến bị hủy.
     */
    CANCELLED
}