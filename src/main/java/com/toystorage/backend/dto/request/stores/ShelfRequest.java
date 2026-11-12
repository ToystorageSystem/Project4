package com.toystorage.backend.dto.request.stores;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /api/store/shelves and PUT /api/store/shelves/{id} */
public record ShelfRequest(
        @NotBlank(message = "Shelf name is required")
        @Size(max = 100, message = "Shelf name must be at most 100 characters") String name,
        @Size(max = 255, message = "Description must be at most 255 characters") String description) {
}
