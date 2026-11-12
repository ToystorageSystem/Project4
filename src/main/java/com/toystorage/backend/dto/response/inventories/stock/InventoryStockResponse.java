package com.toystorage.backend.dto.response.inventories.stock;

import java.util.List;

/** Read-only inventory for a single product in the authenticated warehouse. */
public record InventoryStockResponse(
        Long productId,
        String productCode,
        String productName,
        String barcode,
        String unit,
        long onHand,
        long reserved,
        long available,
        List<LocationStock> locations
) {
    public record LocationStock(
            Long locationId,
            String locationCode,
            String locationName,
            String zone,
            String shelf,
            String locationType,
            long onHand,
            long reserved,
            long available
    ) {}
}
