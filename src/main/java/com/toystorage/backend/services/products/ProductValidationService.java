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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductValidationService {

    private final ProductRepository productRepository;

    private final CategoryRepository categoryRepository;

    private final BrandRepository brandRepository;

    private final ProductChangeRequestRepository
            productChangeRequestRepository;

    public void validateCreateRequest(
            CreateProductRequest request
    ) {
        String productCode = request.getProductCode()
                .trim()
                .toUpperCase(Locale.ROOT);

        String barcode = request.getBarcode().trim();

        if (productRepository
                .existsByProductsCodeIgnoreCase(productCode)) {

            throw new BadRequest(
                    "Product code already exists"
            );
        }

        if (productRepository.existsByBarcode(barcode)) {
            throw new BadRequest(
                    "Barcode already exists"
            );
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

        if (productRepository
                .existsByProductsCodeIgnoreCaseAndIdNot(
                        productCode,
                        productId
                )) {

            throw new BadRequest(
                    "Product code already exists"
            );
        }

        if (productRepository.existsByBarcodeAndIdNot(
                barcode,
                productId
        )) {
            throw new BadRequest(
                    "Barcode already exists"
            );
        }
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