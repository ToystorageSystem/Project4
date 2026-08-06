package com.toystorage.backend.enums.products;

public enum ProductChangeRequestStatus {

    /**
     * Waiting for Business Manager approval.
     */
    PENDING,

    /**
     * Request has been approved.
     */
    APPROVED,

    /**
     * Request has been rejected.
     */
    REJECTED
}