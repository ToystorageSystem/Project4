package com.toystorage.backend.dto.response.stores;

import com.toystorage.backend.enums.stores.ShelfStatus;

/** Store Manager shelf list: active and inactive shelves */
public record ShelfAdminItem(Long id, String code, String name, String description, ShelfStatus status,
                             long productCount, long shelfQuantity) {
}
