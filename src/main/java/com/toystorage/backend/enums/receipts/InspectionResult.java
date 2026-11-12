package com.toystorage.backend.enums.receipts;

public enum InspectionResult {

    /*
     * Số lượng và tình trạng khớp với chứng từ.
     */
    MATCHED,

    /*
     * Số lượng thực tế ít hơn số lượng dự kiến.
     */
    SHORTAGE,

    /*
     * Số lượng thực tế nhiều hơn số lượng dự kiến.
     */
    SURPLUS,

    /*
     * Phát hiện hàng hư hỏng.
     */
    DAMAGED,

    /*
     * Có nhiều loại chênh lệch cùng lúc.
     *
     * Ví dụ:
     * vừa thiếu vừa có hàng lỗi.
     */
    PARTIAL
}