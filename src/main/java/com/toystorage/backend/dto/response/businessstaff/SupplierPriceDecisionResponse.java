
package com.toystorage.backend.dto.response.businessstaff;

import com.toystorage.backend.enums.suppliers.SupplierPriceDecisionAction;
import java.math.BigDecimal;

public record SupplierPriceDecisionResponse(
        Long purchaseOrderId,
        Long purchaseOrderItemId,
        Long supplierId,
        Long productId,
        SupplierPriceDecisionAction decision,
        String reason,
        BigDecimal previousUnitPrice,
        BigDecimal quotedUnitPrice,
        BigDecimal resultingUnitPrice,
        BigDecimal previousSupplierPrice,
        BigDecimal resultingSupplierPrice,
        Long decidedByUserId
) {
}
