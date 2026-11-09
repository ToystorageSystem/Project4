package com.toystorage.backend.dto.request.receipts.store;

import jakarta.validation.constraints.Size;

/** POST /manifests/{manifestCode}/confirm (Store Manager) – body optional */
public record ConfirmReceivingRequest(
        @Size(max = 1000, message = "Note is too long")
        String note) {
}