package com.toystorage.backend.enums.transfers;

public enum TransferReasonCode {

    /*
     * Điều phối lại tồn kho giữa các địa điểm.
     */
    REBALANCE,

    /*
     * Hàng bán chậm tại Store.
     */
    SLOW_MOVING,

    /*
     * Hàng bị lỗi hoặc hư hỏng.
     */
    DAMAGED,

    /*
     * Store nhận dư hàng.
     */
    SURPLUS,

    /*
     * Thu hồi sản phẩm.
     */
    RECALL,

    /*
     * Store đóng cửa hoặc chuyển địa điểm.
     */
    STORE_CLOSURE,

    /*
     * Phiếu điều chuyển trước đó bị sai.
     */
    WRONG_TRANSFER,

    /*
     * Lý do khác.
     * Khi chọn OTHER cần nhập reasonNote.
     */
    OTHER
}