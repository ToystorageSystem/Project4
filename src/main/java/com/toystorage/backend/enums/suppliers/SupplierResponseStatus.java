package com.toystorage.backend.enums.suppliers;

public enum SupplierResponseStatus {

    /*
     * Đã liên hệ nhưng đang chờ nhà cung cấp phản hồi.
     */
    PENDING_RESPONSE,

    /*
     * Nhà cung cấp có thể cung cấp toàn bộ đơn hàng.
     */
    AVAILABLE,

    /*
     * Nhà cung cấp chỉ có thể cung cấp một phần đơn hàng.
     */
    PARTIALLY_AVAILABLE,

    /*
     * Nhà cung cấp đang hết hàng.
     */
    OUT_OF_STOCK,

    /*
     * Một hoặc nhiều sản phẩm đã ngừng sản xuất.
     */
    DISCONTINUED,

    /*
     * Nhà cung cấp thông báo thay đổi giá.
     */
    PRICE_CHANGED,

    /*
     * Nhà cung cấp không thể giao đúng thời gian dự kiến.
     */
    DELAYED,

    /*
     * Nhà cung cấp từ chối đơn mua hàng.
     */
    DECLINED
}