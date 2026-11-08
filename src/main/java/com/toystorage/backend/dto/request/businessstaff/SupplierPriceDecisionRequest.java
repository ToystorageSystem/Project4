
package com.toystorage.backend.dto.request.businessstaff;

import com.toystorage.backend.enums.suppliers.SupplierPriceDecisionAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SupplierPriceDecisionRequest(

        @NotNull(message = "Price decision is required")
        SupplierPriceDecisionAction decision,

        @NotBlank(message = "Decision reason is required")
        String reason

) {
}
