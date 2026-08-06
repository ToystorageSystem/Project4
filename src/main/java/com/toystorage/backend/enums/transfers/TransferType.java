package com.toystorage.backend.enums.transfers;

public enum TransferType {

    /*
     * Điều chuyển hàng thông thường:
     * Warehouse -> Store
     * Warehouse -> Warehouse
     * Store -> Store
     */
    NORMAL,

    /*
     * Store trả hàng về kho.
     */
    STORE_RETURN,

    /*
     * Trả lại số hàng Store nhận dư.
     */
    SURPLUS_RETURN,

    /*
     * Trả hàng hư hỏng về kho.
     */
    DAMAGED_RETURN,

    /*
     * Thu hồi sản phẩm theo yêu cầu của Business
     * hoặc nhà cung cấp.
     */
    RECALL
}