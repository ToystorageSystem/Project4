package com.toystorage.backend.enums.products;

public enum ProductStatus {

    /**
     * Giữ lại để tương thích dữ liệu cũ trong DB.
     */
    PENDING,

    /**
     * Sản phẩm mới đang chờ Business Manager duyệt.
     */
    PENDING_CREATE,

    /**
     * Sản phẩm cũ đã được chỉnh sửa và đang chờ duyệt lại.
     */
    PENDING_UPDATE,

    ACTIVE,

    INACTIVE,

    DISCONTINUED,

    REJECTED
}