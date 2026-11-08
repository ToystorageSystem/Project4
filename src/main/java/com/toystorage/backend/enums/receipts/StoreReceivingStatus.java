package com.toystorage.backend.enums.receipts;

/**
 * Receiving progress of a whole manifest at the store (computed, not a DB column).
 */
public enum StoreReceivingStatus {
    /** no store receipt yet */
    NOT_STARTED,
    /** scanning packages (F1) */
    RECEIVING,
    /** "complete receiving" done (store_receipts.received_at set) – driver may leave; F2 in progress */
    PACKAGES_RECEIVED,
    /** finish inspection done (receipts INSPECTED) – waiting for Store Manager */
    INSPECTED,
    /** confirmed by Store Manager, stock added */
    COMPLETED
}