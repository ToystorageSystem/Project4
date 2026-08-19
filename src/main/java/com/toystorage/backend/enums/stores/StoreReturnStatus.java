package com.toystorage.backend.enums.stores;

public enum StoreReturnStatus {

    /*
     * Store đang tạo yêu cầu.
     */
    DRAFT,

    /*
     * Chờ Business hoặc Warehouse xác nhận yêu cầu trả hàng.
     */
    PENDING_APPROVAL,

    /*
     * Business hoặc Warehouse đã xác nhận.
     *
     * Tại thời điểm này Store trừ tồn theo nghiệp vụ trả hàng.
     */
    APPROVED,

    /*
     * Store đang đóng hàng trả.
     */
    PACKING,

    /*
     * Store đã bàn giao hàng cho vận chuyển.
     */
    ISSUED,

    /*
     * Hàng đang được vận chuyển về kho tổng.
     */
    SHIPPED,

    /*
     * Warehouse Staff đã nhận hàng vật lý
     * và đang kiểm số lượng, tình trạng sản phẩm.
     *
     * CHƯA cộng tồn kho.
     */
    INSPECTING,

    /*
     * Warehouse Staff đã kiểm xong
     * và gửi kết quả cho Warehouse Manager xác nhận.
     *
     * CHƯA cộng tồn kho.
     */
    PENDING_CONFIRMATION,

    /*
     * Warehouse Manager đã xác nhận kết quả kiểm hàng.
     *
     * Sau bước này hệ thống mới xử lý cộng
     * số lượng được chấp nhận vào tồn kho phù hợp.
     */
    RECEIVED,

    /*
     * Phiếu trả hàng bị hủy.
     */
    CANCELLED
}