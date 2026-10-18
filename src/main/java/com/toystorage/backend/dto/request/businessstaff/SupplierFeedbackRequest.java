
package com.toystorage.backend.dto.request.businessstaff;

import com.toystorage.backend.enums.suppliers.SupplierResponseStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SupplierFeedbackRequest(

        @NotNull
        SupplierResponseStatus supplierResponseStatus,

        String supplierResponseNote,

        @Valid
        List<SupplierItemFeedbackRequest> items

) {}
