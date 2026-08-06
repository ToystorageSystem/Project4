package com.toystorage.backend.enums.users;

public enum SuspiciousAlertStatus {

    /*
     * Cảnh báo mới phát sinh.
     */
    NEW,

    /*
     * Admin đang kiểm tra.
     */
    INVESTIGATING,

    /*
     * Đã xử lý xong.
     */
    RESOLVED,

    /*
     * Cảnh báo không hợp lệ
     * hoặc phát hiện nhầm.
     */
    FALSE_POSITIVE
}