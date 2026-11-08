
package com.toystorage.backend.dto.response.businessstaff;

import com.toystorage.backend.enums.suppliers.SupplierItemResponseStatus;
import java.math.BigDecimal;
import java.util.List;

public record SupplierPriceComparisonResponse(
        Long purchaseOrderId,
        String orderCode,
        Long supplierId,
        String supplierName,
        List<ItemResponse> items
) {

    public record ItemResponse(
            Long purchaseOrderItemId,
            Long productId,
            String productName,
            SupplierItemResponseStatus supplierResponseStatus,
            Integer orderedQuantity,
            Integer receivedQuantity,
            Integer confirmedQuantity,
            BigDecimal originalUnitPrice,
            BigDecimal confirmedUnitPrice,
            BigDecimal differencePerUnit,
            BigDecimal differencePercent,
            BigDecimal estimatedOrderDifference,
            String priceChange,
            String priceDecision
    ) {
    }
}
