package com.toystorage.backend.enums.suppliers;

public enum SupplierInvoiceStatus {

    /*
     * Business Staff vừa nhập thông tin
     * hoặc tải file hóa đơn lên.
     */
    DRAFT,

    /*
     * Hóa đơn đang chờ kiểm tra và đối chiếu.
     */
    PENDING_VERIFICATION,

    /*
     * Hóa đơn đã được kiểm tra và dữ liệu hợp lệ.
     */
    VERIFIED,

    /*
     * Hóa đơn đã thanh toán một phần.
     */
    PARTIALLY_PAID,

    /*
     * Hóa đơn đã thanh toán đầy đủ.
     */
    PAID,

    /*
     * Hóa đơn đã quá hạn thanh toán.
     */

    OVERDUE,

    /*
     * Hóa đơn đã bị hủy hoặc nhập nhầm.
     */
    CANCELLED
}