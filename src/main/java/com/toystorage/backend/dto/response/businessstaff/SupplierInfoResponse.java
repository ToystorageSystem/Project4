
package com.toystorage.backend.dto.response.businessstaff;

import com.toystorage.backend.enums.products.CommonStatus;

public record SupplierInfoResponse(
        Long id,
        String suppliersCode,
        String name,
        String contactPerson,
        String contactPosition,
        String phone,
        String email,
        String address,
        String taxCode,
        CommonStatus status
) {}
