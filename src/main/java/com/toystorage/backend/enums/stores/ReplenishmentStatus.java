package com.toystorage.backend.enums.stores;

public enum ReplenishmentStatus {

    /*
     * Yêu cầu đang chờ Business xem xét.
     */
    PENDING,

    /*
     * Yêu cầu đã được Business duyệt.
     */
    APPROVED,

    /*
     * Yêu cầu bị từ chối.
     */
    REJECTED,

    /*
     * Business đã tạo Stock Transfer
     * để điều hàng về Store.
     */
    TRANSFER_CREATED,

    /*
     * Store đã nhận đủ hàng.
     */
    COMPLETED,

    /*
     * Yêu cầu bị hủy.
     */
    CANCELLED
}