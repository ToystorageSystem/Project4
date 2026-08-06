package com.toystorage.backend.enums.shipments;

public enum ShipmentManifestStatus {

    /*
     * Hệ thống đã tạo bảng kê sau khi địa điểm xuất
     * hoàn tất đóng kiện.
     */
    CREATED,

    /*
     * Bảng kê đã được in tại Kho hoặc Store xuất hàng.
     */
    PRINTED,

    /*
     * Bảng kê đã được sử dụng để bàn giao hàng.
     */
    ISSUED,

    /*
     * Bảng kê đã bị hủy trước khi hàng được xuất.
     */
    CANCELLED
}