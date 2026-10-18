
package com.toystorage.backend.dto.response.businessstaff;

import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.enums.products.ProductStatus;
import java.math.BigDecimal;

public record SupplierProductResponse(
        Long supplierProductId,
        Long productId,
        String productsCode,
        String barcode,
        String productName,
        String baseUnit,
        BigDecimal purchasePrice,
        Integer minimumOrderQuantity,
        Integer leadTimeDays,
        CommonStatus supplierProductStatus,
        ProductStatus productStatus
) {}
