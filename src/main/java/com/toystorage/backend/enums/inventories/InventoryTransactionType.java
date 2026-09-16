package com.toystorage.backend.enums.inventories;

public enum InventoryTransactionType {

    /**
     * Tăng tồn khi hoàn tất kiểm hàng nhập từ nhà cung cấp.
     */
    GOODS_RECEIPT,

    /**
     * Giảm tồn khi Business tạo phiếu điều chuyển.
     */
    TRANSFER_OUT,

    /**
     * Tăng tồn tại cửa hàng khi cửa hàng nhận hàng hoàn tất.
     */
    TRANSFER_IN,

    /**
     * Giảm tồn tại cửa hàng khi xác nhận trả hàng.
     */
    STORE_RETURN_OUT,

    /**
     * Tăng tồn tại kho khi kiểm hàng trả về hoàn tất.
     */
    STORE_RETURN_IN,

    /**
     * Chuyển hàng từ vị trí này sang vị trí khác.
     */
    LOCATION_TRANSFER,

    /**
     * Điều chỉnh tăng tồn kho.
     */
    ADJUSTMENT_INCREASE,

    /**
     * Điều chỉnh giảm tồn kho.
     */
    ADJUSTMENT_DECREASE,

    /**
     * Giảm tồn do hàng hư hỏng.
     */
    DAMAGED,

    DAMAGED_RETURN_TO_SUPPLIER,
    /**
     * Giảm tồn do hàng hết hạn.
     */
    EXPIRED,

    /**
     * Khôi phục tồn khi hủy phiếu điều chuyển.
     */
    TRANSFER_CANCELLED,

    /**
     * Giá trị khởi tạo ban đầu.
     */
    INITIAL
}
