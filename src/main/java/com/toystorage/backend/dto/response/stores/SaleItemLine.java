package com.toystorage.backend.dto.response.stores;

import java.math.BigDecimal;

public record SaleItemLine(Long productId, String productCode, String barcode, String name,
                           int quantity, BigDecimal unitPrice, BigDecimal lineTotal,
                           int fromShelf, int fromBackroom) {
}