package com.toystorage.backend.enums.stores;

public enum SalesOrderStatus {

    /*
     * Đơn đang được tạo.
     * Chưa thay đổi tồn kho.
     */
    DRAFT,

    /*
     * Đang chờ khách thanh toán.
     * Chưa thay đổi tồn kho.
     */
    PENDING_PAYMENT,

    /*
     * Đơn bán đã hoàn tất.
     *
     * Tại thời điểm này Store bị trừ tồn kho.
     */
    COMPLETED,

    /*
     * Đơn bán bị hủy.
     *
     * Nếu tồn đã bị trừ trước đó thì cần hoàn tồn.
     */
    CANCELLED,

    /*
     * Đơn đã được hoàn tiền.
     *
     * Việc cộng lại tồn hay không còn phụ thuộc
     * khách có trả lại hàng hay không.
     */
    REFUNDED
}