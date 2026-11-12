package com.toystorage.backend.dto.request.stores;

import com.toystorage.backend.enums.stores.ShelfStatus;

import jakarta.validation.constraints.NotNull;

/** PATCH /api/store/shelves/{id}/status  {"status": "ACTIVE" | "INACTIVE"} */
public record ShelfStatusRequest(@NotNull(message = "status is required") ShelfStatus status) {
}
