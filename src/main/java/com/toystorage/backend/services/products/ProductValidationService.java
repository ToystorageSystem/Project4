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
                    "Mã sản phẩm đã tồn tại"
            );
        }

        if (productRepository.existsByBarcode(barcode)) {
            throw new BadRequest(
                    "Barcode đã tồn tại"
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
                                "Không tìm thấy danh mục đang hoạt động "
                                        + "có ID: "
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
                                "Không tìm thấy thương hiệu đang hoạt động "
                                        + "có ID: "
                                        + brandId
                        )
                );
    }

    public Products getProduct(Long productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() ->
                        new NotFound(
                                "Không tìm thấy sản phẩm có ID: "
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
                                "Không tìm thấy yêu cầu đang chờ duyệt "
                                        + "có ID: "
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
                    "Sản phẩm không ở trạng thái chờ duyệt"
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
                    "Mã sản phẩm đã tồn tại"
            );
        }

        if (productRepository.existsByBarcodeAndIdNot(
                barcode,
                productId
        )) {
            throw new BadRequest(
                    "Barcode đã tồn tại"
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
                    "Chỉ có thể thay đổi sản phẩm "
                            + "đang hoạt động hoặc tạm ẩn"
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
                    "Sản phẩm đang có yêu cầu chờ duyệt"
            );
        }
    }
}