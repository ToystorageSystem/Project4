package com.toystorage.backend.enums.products;

public enum ProductChangeType {

    /**
     * Business Staff tạo sản phẩm mới.
     */
    CREATE,

    /**
     * Business Staff yêu cầu chỉnh sửa sản phẩm.
     */
    UPDATE,

    /**
     * Business Staff yêu cầu ẩn sản phẩm.
     */
    DEACTIVATE
}