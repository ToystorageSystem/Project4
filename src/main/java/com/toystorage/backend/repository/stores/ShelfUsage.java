package com.toystorage.backend.repository.stores;

/** Aggregate per shelf: how many products and how many items are on it */
public record ShelfUsage(Long shelfId, Long productCount, Long shelfQuantity) {
}
