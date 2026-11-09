package com.toystorage.backend.dto.request.stores;

/** shelfId = null -> remove product from its shelf */
public record AssignShelfRequest(Long shelfId) {
}