
package com.toystorage.backend.dto.response.businessstaff;

import com.toystorage.backend.enums.suppliers.SupplierResponseStatus;
import com.toystorage.backend.enums.suppliers.SupplierItemResponseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SupplierFeedbackResponse(
        Long purchaseOrderId,
        String orderCode,
        SupplierResponseStatus supplierResponseStatus,
        String supplierResponseNote,
        LocalDateTime supplierRespondedAt,
        LocalDate requestedDeliveryDate,
        LocalDate supplierProposedDeliveryDate,
        LocalDate agreedDeliveryDate,
        List<ItemResponse> items
) {
    public record ItemResponse(
            Long purchaseOrderItemId,
            Long productId,
            SupplierItemResponseStatus supplierResponseStatus,
            Integer confirmedQuantity,
            BigDecimal originalUnitPrice,
            BigDecimal confirmedUnitPrice,
            String supplierResponseNote
    ) {}
}
