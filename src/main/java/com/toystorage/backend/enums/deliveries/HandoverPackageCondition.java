package com.toystorage.backend.enums.deliveries;

public enum HandoverPackageCondition {

    NORMAL,

    // Hư hỏng bên ngoài
    DAMAGED,

    // Seal sai
    SEAL_MISMATCH,

    // Seal rách / có dấu hiệu mở
    SEAL_BROKEN,

    // Kiện không được bàn giao
    MISSING
}