package com.toystorage.backend.mapper.suppliers;

import com.toystorage.backend.dto.response.suppliers.SupplierProductResponse;
import com.toystorage.backend.entity.suppliers.SupplierProducts;
import org.springframework.stereotype.Component;

@Component
public class SupplierProductMapper {

    public SupplierProductResponse toResponse(SupplierProducts entity) {

        if (entity == null) {
            return null;
        }

        return SupplierProductResponse.builder()
                .id(entity.getId())
                .linkCode(entity.getSupplierProductsCode())

                .productId(
                        entity.getProduct() != null
                                ? entity.getProduct().getId()
                                : null
                )

                .productCode(
                        entity.getProduct() != null
                                ? entity.getProduct().getProductsCode()
                                : null
                )

                .productName(
                        entity.getProduct() != null
                                ? entity.getProduct().getName()
                                : null
                )

                .productStatus(
                        entity.getProduct() != null
                                && entity.getProduct().getStatus() != null
                                ? entity.getProduct().getStatus().name()
                                : null
                )

                .supplierId(
                        entity.getSupplier() != null
                                ? entity.getSupplier().getId()
                                : null
                )

                .supplierCode(
                        entity.getSupplier() != null
                                ? entity.getSupplier().getSuppliersCode()
                                : null
                )

                .supplierName(
                        entity.getSupplier() != null
                                ? entity.getSupplier().getName()
                                : null
                )

                .supplierStatus(
                        entity.getSupplier() != null
                                && entity.getSupplier().getStatus() != null
                                ? entity.getSupplier().getStatus().name()
                                : null
                )

                .supplierProductCode(
                        entity.getSupplierProductCode()
                )

                .purchasePrice(
                        entity.getPurchasePrice()
                )

                .leadTimeDays(
                        entity.getLeadTimeDays()
                )

                .minimumOrderQuantity(
                        entity.getMinimumOrderQuantity()
                )

                .defaultSupplier(
                        Boolean.TRUE.equals(entity.getIsDefault())
                )

                .status(
                        entity.getStatus() != null
                                ? entity.getStatus().name()
                                : null
                )

                .build();
    }
}