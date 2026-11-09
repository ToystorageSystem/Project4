package com.toystorage.backend.dto.response.stores;

public record ShelfSummary(Long id, String code, String name, String description,
                           long productCount, long shelfQuantity) {
}