package com.toystorage.backend.services.products;

import com.toystorage.backend.dto.request.products.CreateProductRequest;
import com.toystorage.backend.dto.request.products.UpdateProductRequest;
import com.toystorage.backend.entity.products.Brands;
import com.toystorage.backend.entity.products.Categories;
import com.toystorage.backend.entity.products.ProductChangeRequests;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.enums.products.ProductChangeRequestStatus;
import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.repository.products.BrandRepository;
import com.toystorage.backend.repository.products.CategoryRepository;
import com.toystorage.backend.repository.products.ProductChangeRequestRepository;
import com.toystorage.backend.repository.products.ProductRepository;
import com.toystorage.backend.repository.suppliers.SupplierProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductValidationService {

    private final ProductRepository productRepository;
    private final SupplierProductRepository supplierProductRepository;

    private final CategoryRepository categoryRepository;

    private final BrandRepository brandRepository;

    private final ProductChangeRequestRepository
            productChangeRequestRepository;

    public void validateCreateRequest(CreateProductRequest request) {
        validateCreateRequest(request, false);
    }

    // Chi luong Business co Supplier cu the moi duoc phep trung Barcode.
    public void validateCreateRequest(CreateProductRequest request, boolean allowSupplierBarcode) {
        String productCode = request.getProductCode();
        if (productCode != null && !productCode.isBlank()
                && productRepository.existsByProductsCodeIgnoreCase(productCode.trim())) {
            throw new BadRequest("Product code already exists");
        }
        if (!allowSupplierBarcode && productRepository.existsByBarcode(request.getBarcode().trim())) {
            throw new BadRequest("Barcode already exists");
        }
    }

    public void validateBarcodeForProduct(Long productId, String barcode) {
        Products product = getProduct(productId);
        if (product.getBarcode().equals(barcode)) return;
        var links = product.getOwningSupplier() != null
                ? java.util.List.of(product.getOwningSupplier().getId())
                : supplierProductRepository.findByProduct_IdOrderByIsDefaultDescSupplier_NameAsc(productId)
                        .stream().map(link -> link.getSupplier().getId()).distinct().toList();
        if (links.isEmpty()) {
            if (productRepository.existsByBarcodeAndIdNot(barcode, productId)) {
                throw new BadRequest("Barcode already exists");
            }
            return;
        }
        for (Long supplierId : links) {
            if (productRepository.existsLinkedBarcodeForSupplier(barcode, supplierId, productId)
                    || productRepository.existsByBarcodeAndOwningSupplier_IdAndIdNot(
                            barcode, supplierId, productId)) {
                throw new BadRequest("This supplier already has a product with this barcode");
            }
        }
    }

    public Categories getActiveCategory(Long categoryId) {
        return categoryRepository
                .findByIdAndStatus(
                        categoryId,
                        CommonStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Active category not found "
                                        + "with ID: "
                                        + categoryId
                        )
                );
    }

    public Brands getActiveBrand(Long brandId) {
        if (brandId == null) {
            return null;
        }

        return brandRepository
                .findByIdAndStatus(
                        brandId,
                        CommonStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Active brand not found "
                                        + "with ID: "
                                        + brandId
                        )
                );
    }

    public Products getProduct(Long productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() ->
                        new NotFound(
                                "Product not found with ID: "
                                        + productId
                        )
                );
    }

    public ProductChangeRequests getPendingRequest(
            Long requestId
    ) {
        return productChangeRequestRepository
                .findByIdAndStatus(
                        requestId,
                        ProductChangeRequestStatus.PENDING
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Pending approval request not found "
                                        + "with ID: "
                                        + requestId
                        )
                );
    }

    public void validatePendingCreateProduct(
            Products product
    ) {
        if (product.getStatus()
                != ProductStatus.PENDING_CREATE) {

            throw new BadRequest(
                    "Product is not pending approval"
            );
        }
    }
    public void validateUpdateRequest(
            Long productId,
            UpdateProductRequest request
    ) {
        String productCode = request.getProductCode()
                .trim()
                .toUpperCase(Locale.ROOT);

        String barcode = request.getBarcode().trim();

        if (!getProduct(productId).getProductsCode().equalsIgnoreCase(productCode)) {
            throw new BadRequest("Product code is generated automatically and cannot be changed");
        }

        // Barcode trung duoc phep chi khi khac nha cung cap.
        validateBarcodeForProduct(productId, barcode);
    }

    public void validateProductCanBeChanged(
            Products product
    ) {
        if (product.getStatus() != ProductStatus.ACTIVE
                && product.getStatus()
                != ProductStatus.INACTIVE) {

            throw new BadRequest(
                    "Only products that are "
                            + "active or hidden can be changed"
            );
        }

        boolean hasPendingRequest =
                productChangeRequestRepository
                        .existsByProduct_IdAndStatus(
                                product.getId(),
                                ProductChangeRequestStatus.PENDING
                        );

        if (hasPendingRequest) {
            throw new BadRequest(
                    "This product has a pending approval request"
            );
        }
    }
}