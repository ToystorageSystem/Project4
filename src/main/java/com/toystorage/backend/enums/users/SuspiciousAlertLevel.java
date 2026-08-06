package com.toystorage.backend.enums.users;

public enum SuspiciousAlertLevel {

    /*
     * Chỉ cần theo dõi.
     */
    LOW,

    /*
     * Có dấu hiệu bất thường,
     * cần Admin kiểm tra.
     */
    MEDIUM,

    /*
     * Có nguy cơ ảnh hưởng đến
     * dữ liệu hoặc vận hành.
     */
    HIGH,

    /*
     * Mức độ nghiêm trọng,
     * cần xử lý ngay.
     */
    CRITICAL
}