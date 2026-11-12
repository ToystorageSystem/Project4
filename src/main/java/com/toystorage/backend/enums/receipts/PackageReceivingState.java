package com.toystorage.backend.enums.receipts;

/**
 * State of one package at the store (computed, not a DB column).
 */
public enum PackageReceivingState {
    /** no package_receiving_checks row */
    NOT_RECEIVED,
    /** received in F1 (inspection_method = SEAL_ONLY) – products not counted yet */
    RECEIVED,
    /** products counted in F2 (inspection_method = FULL_COUNT) */
    CHECKED
}