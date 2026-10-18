
package com.toystorage.backend.services.businessstaff;

import com.toystorage.backend.dto.response.businessstaff.SupplierInfoResponse;
import com.toystorage.backend.dto.response.businessstaff.SupplierProductResponse;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.suppliers.SupplierProducts;
import com.toystorage.backend.entity.suppliers.Suppliers;
import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.repository.products.SupplierProductRepository;
import com.toystorage.backend.repository.suppliers.BusinessSupplierRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessSupplierReadService {

    private final BusinessSupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;

    public List<SupplierInfoResponse> getSuppliers() {
        return supplierRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toSupplierInfo)
                .toList();
    }

    public SupplierInfoResponse getSupplier(Long supplierId) {
        return toSupplierInfo(findSupplier(supplierId));
    }

    public List<SupplierProductResponse> getSupplierProducts(
            Long supplierId
    ) {
        findSupplier(supplierId);

        return supplierProductRepository
                .findBySupplier_IdAndStatusOrderByIdAsc(
                        supplierId,
                        CommonStatus.ACTIVE
                )
                .stream()
                .map(this::toSupplierProduct)
                .toList();
    }

    private Suppliers findSupplier(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() ->
                        new NotFound(
                                "Khong tim thay nha cung cap: " + supplierId
                        )
                );
    }

    private SupplierInfoResponse toSupplierInfo(Suppliers supplier) {
        return new SupplierInfoResponse(
                supplier.getId(),
                supplier.getSuppliersCode(),
                supplier.getName(),
                supplier.getContactPerson(),
                supplier.getContactPosition(),
                supplier.getPhone(),
                supplier.getEmail(),
                supplier.getAddress(),
                supplier.getTaxCode(),
                supplier.getStatus()
        );
    }

    private SupplierProductResponse toSupplierProduct(
            SupplierProducts link
    ) {
        Products product = link.getProduct();

        return new SupplierProductResponse(
                link.getId(),
                product.getId(),
                product.getProductsCode(),
                product.getBarcode(),
                product.getName(),
                product.getBaseUnit(),
                link.getPurchasePrice(),
                link.getMinimumOrderQuantity(),
                link.getLeadTimeDays(),
                link.getStatus(),
                product.getStatus()
        );
    }
}
