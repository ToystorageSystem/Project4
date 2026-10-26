package com.toystorage.backend.dto.stores;

public record StoreDetailsResponse(Long warehouseId, String storeCode, String storeName,
                                   String storeManagerName, String openingHours, String storePhone) {}
