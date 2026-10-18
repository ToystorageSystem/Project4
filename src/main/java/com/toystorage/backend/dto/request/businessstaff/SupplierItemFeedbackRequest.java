
package com.toystorage.backend.dto.request.businessstaff;

import com.toystorage.backend.enums.suppliers.SupplierItemResponseStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SupplierItemFeedbackRequest(

        @NotNull
        @Positive
        Long purchaseOrderItemId,

        @NotNull
        SupplierItemResponseStatus supplierResponseStatus,

        @Min(0)
        Integer confirmedQuantity,

        @DecimalMin(
            value = "0.01",
            message = "Gia xac nhan phai lon hon 0"
        )
        BigDecimal confirmedUnitPrice,

        String supplierResponseNote

) {}
