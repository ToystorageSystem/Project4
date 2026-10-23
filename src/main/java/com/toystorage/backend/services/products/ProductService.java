package com.toystorage.backend.services.products;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toystorage.backend.dto.request.products.CreateProductRequest;
import com.toystorage.backend.dto.request.products.RejectProductRequest;
import com.toystorage.backend.dto.request.products.UpdateProductRequest;
import com.toystorage.backend.dto.response.products.ProductChangeRequestResponse;
import com.toystorage.backend.dto.response.products.ProductPageResponse;
import com.toystorage.backend.dto.response.products.ProductResponse;
import com.toystorage.backend.entity.products.Brands;
import com.toystorage.backend.entity.products.Categories;
import com.toystorage.backend.entity.products.ProductChangeRequests;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.products.ProductChangeRequestStatus;
import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.mapper.products.ProductMapper;
import com.toystorage.backend.repository.products.ProductChangeRequestRepository;
import com.toystorage.backend.repository.products.ProductRepository;
import com.toystorage.backend.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductChangeRequestRepository changeRequestRepository;
    private final UserRepository userRepository;
    private final ProductValidationService validationService;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;

    /**
     * Business Staff tạo sản phẩm mới và gửi yêu cầu phê duyệt.
     */
    @Transactional
    public ProductChangeRequestResponse createProduct(
            CreateProductRequest request
    ) {
        validationService.validateCreateRequest(request);

        Categories category =
                validationService.getActiveCategory(
                        request.getCategoryId()
                );

        Brands brand =
                validationService.getActiveBrand(
                        request.getBrandId()
                );

        Users currentUser = getCurrentUser();

        Products product = productMapper.toEntity(
                request,
                category,
                brand,
                currentUser
        );

        product = productRepository.save(product);

        ProductChangeRequests changeRequest =
                ProductChangeRequests.builder()
                        .productChangeRequestsCode(
                                generateRequestCode()
                        )
                        .product(product)
                        .createdBy(currentUser)
                        .oldValue(null)
                        .newValue(
                                toJson(
                                        productMapper.toResponse(
                                                product
                                        )
                                )
                        )
                        .requestReason(
                                normalizeNullable(
                                        request.getRequestReason()
                                )
                        )
                        .status(
                                ProductChangeRequestStatus.PENDING
                        )
                        .build();

        changeRequest =
                changeRequestRepository.save(changeRequest);

        return productMapper.toChangeRequestResponse(
                changeRequest
        );
    }

    /**
     * Business Staff gửi yêu cầu chỉnh sửa sản phẩm.
     *
     * Dữ liệu trong bảng products chưa bị thay đổi.
     * Dữ liệu cũ và mới được lưu vào product_change_requests
     * để Business Manager kiểm tra và phê duyệt.
     */
    @Transactional
    public ProductChangeRequestResponse requestProductUpdate(
            Long productId,
            UpdateProductRequest request
    ) {
        Products product =
                validationService.getProduct(productId);

        validationService.validateProductCanBeChanged(
                product
        );

        validationService.validateUpdateRequest(
                productId,
                request
        );

        Categories category =
                validationService.getActiveCategory(
                        request.getCategoryId()
                );

        Brands brand =
                validationService.getActiveBrand(
                        request.getBrandId()
                );

        Users currentUser = getCurrentUser();

        ProductResponse oldProduct =
                productMapper.toResponse(product);

        ProductResponse proposedProduct =
                productMapper.toProposedResponse(
                        product,
                        request,
                        category,
                        brand
                );

        ProductChangeRequests changeRequest =
                ProductChangeRequests.builder()
                        .productChangeRequestsCode(
                                generateRequestCode()
                        )
                        .product(product)
                        .createdBy(currentUser)
                        .oldValue(
                                toJson(oldProduct)
                        )
                        .newValue(
                                toJson(proposedProduct)
                        )
                        .requestReason(
                                normalizeNullable(
                                        request.getRequestReason()
                                )
                        )
                        .status(
                                ProductChangeRequestStatus.PENDING
                        )
                        .build();

        changeRequest =
                changeRequestRepository.save(
                        changeRequest
                );

        return productMapper.toChangeRequestResponse(
                changeRequest
        );
    }

    /**
     * Xem danh sách sản phẩm theo trạng thái.
     * Nếu status null thì lấy toàn bộ.
     */
    @Transactional(readOnly = true)
    public ProductPageResponse getProducts(
            ProductStatus status,
            int page,
            int size
    ) {
        validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        Page<Products> productPage;

        if (status == null) {
            productPage =
                    productRepository.findAll(pageable);
        } else {
            productPage =
                    productRepository.findByStatus(
                            status,
                            pageable
                    );
        }

        return productMapper.toPageResponse(productPage);
    }

    /**
     * Xem chi tiết một sản phẩm.
     */
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        Products product =
                validationService.getProduct(productId);

        return productMapper.toResponse(product);
    }

    /**
     * Business Manager xem các yêu cầu đang chờ duyệt.
     */
    @Transactional(readOnly = true)
    public Page<ProductChangeRequestResponse>
    getPendingRequests(
            int page,
            int size
    ) {
        validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size);

        return changeRequestRepository
                .findByStatusOrderByCreatedAtDesc(
                        ProductChangeRequestStatus.PENDING,
                        pageable
                )
                .map(
                        productMapper
                                ::toChangeRequestResponse
                );
    }

    /**
     * Xem chi tiết một yêu cầu đang chờ duyệt.
     */
    @Transactional(readOnly = true)
    public ProductChangeRequestResponse getPendingRequest(
            Long requestId
    ) {
        ProductChangeRequests changeRequest =
                validationService.getPendingRequest(
                        requestId
                );

        return productMapper.toChangeRequestResponse(
                changeRequest
        );
    }

    /**
     * Business Manager phê duyệt yêu cầu tạo mới
     * hoặc chỉnh sửa sản phẩm.
     */
    @Transactional
    public ProductChangeRequestResponse approveCreateRequest(
            Long requestId
    ) {
        ProductChangeRequests changeRequest =
                validationService.getPendingRequest(
                        requestId
                );

        Products product = changeRequest.getProduct();
        Users currentUser = getCurrentUser();

        /*
         * oldValue null nghĩa là yêu cầu tạo mới.
         */
        if (changeRequest.getOldValue() == null) {
            validationService.validatePendingCreateProduct(
                    product
            );

            product.setStatus(ProductStatus.ACTIVE);
        } else {
            /*
             * oldValue có dữ liệu nghĩa là yêu cầu chỉnh sửa.
             */
            ProductResponse proposedProduct =
                    fromJson(
                            changeRequest.getNewValue()
                    );

            applyApprovedUpdate(
                    product,
                    proposedProduct
            );
        }

        product.setApprovedBy(currentUser);
        product.setRejectionReason(null);

        changeRequest.setStatus(
                ProductChangeRequestStatus.APPROVED
        );
        changeRequest.setApprovedBy(currentUser);
        changeRequest.setApprovedAt(
                LocalDateTime.now()
        );
        changeRequest.setRejectionReason(null);

        productRepository.saveAndFlush(product);
        changeRequestRepository.save(changeRequest);

        return productMapper.toChangeRequestResponse(
                changeRequest
        );
    }

    /**
     * Business Manager từ chối yêu cầu tạo mới
     * hoặc yêu cầu chỉnh sửa sản phẩm.
     */
    @Transactional
    public ProductChangeRequestResponse rejectCreateRequest(
            Long requestId,
            RejectProductRequest request
    ) {
        ProductChangeRequests changeRequest =
                validationService.getPendingRequest(requestId);

        Products product = changeRequest.getProduct();
        Users currentUser = getCurrentUser();

        String rejectionReason =
                request.getRejectionReason().trim();

        /*
         * oldValue null nghĩa là yêu cầu tạo sản phẩm mới.
         * Khi từ chối, sản phẩm chuyển sang REJECTED.
         */
        if (changeRequest.getOldValue() == null) {
            validationService.validatePendingCreateProduct(product);

            product.setStatus(ProductStatus.REJECTED);
            product.setApprovedBy(currentUser);
            product.setRejectionReason(rejectionReason);

            productRepository.save(product);
        }

        /*
         * Nếu oldValue khác null thì đây là yêu cầu chỉnh sửa.
         * Không thay đổi dữ liệu thật trong bảng products.
         */
        changeRequest.setStatus(
                ProductChangeRequestStatus.REJECTED
        );
        changeRequest.setApprovedBy(currentUser);
        changeRequest.setApprovedAt(LocalDateTime.now());
        changeRequest.setRejectionReason(rejectionReason);

        changeRequestRepository.save(changeRequest);

        return productMapper.toChangeRequestResponse(
                changeRequest
        );
    }

    /**
     * Business Staff ẩn sản phẩm đang hoạt động.
     *
     * Không xóa sản phẩm khỏi cơ sở dữ liệu,
     * chỉ chuyển trạng thái từ ACTIVE sang INACTIVE.
     */
    @Transactional
    public ProductResponse deactivateProduct(Long productId) {
        Products product =
                validationService.getProduct(productId);

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequest(
                    "Chỉ có thể ẩn sản phẩm đang hoạt động"
            );
        }

        product.setStatus(ProductStatus.INACTIVE);

        product = productRepository.saveAndFlush(product);

        return productMapper.toResponse(product);
    }

    /**
     * Business Staff khôi phục sản phẩm đã ẩn.
     *
     * Chuyển trạng thái từ INACTIVE sang ACTIVE.
     */
    @Transactional
    public ProductResponse activateProduct(Long productId) {
        Products product =
                validationService.getProduct(productId);

        if (product.getStatus() != ProductStatus.INACTIVE) {
            throw new BadRequest(
                    "Chỉ có thể khôi phục sản phẩm đã bị ẩn"
            );
        }

        product.setStatus(ProductStatus.ACTIVE);

        product = productRepository.saveAndFlush(product);

        return productMapper.toResponse(product);
    }

    private ProductResponse fromJson(String json) {
        try {
            return objectMapper.readValue(
                    json,
                    ProductResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new BadRequest(
                    "Dữ liệu thay đổi sản phẩm không hợp lệ"
            );
        }
    }

    private void applyApprovedUpdate(
            Products product,
            ProductResponse proposedProduct
    ) {
        String productCode =
                proposedProduct.getProductCode()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        String barcode =
                proposedProduct.getBarcode().trim();

        if (productRepository
                .existsByProductsCodeIgnoreCaseAndIdNot(
                        productCode,
                        product.getId()
                )) {
            throw new BadRequest(
                    "Mã sản phẩm đã tồn tại"
            );
        }

        if (productRepository.existsByBarcodeAndIdNot(
                barcode,
                product.getId()
        )) {
            throw new BadRequest(
                    "Barcode đã tồn tại"
            );
        }

        Categories category =
                validationService.getActiveCategory(
                        proposedProduct.getCategoryId()
                );

        Brands brand =
                validationService.getActiveBrand(
                        proposedProduct.getBrandId()
                );

        ProductStatus proposedStatus =
                proposedProduct.getStatus();

        if (proposedStatus != ProductStatus.ACTIVE
                && proposedStatus != ProductStatus.INACTIVE) {
            throw new BadRequest(
                    "Trạng thái đề nghị của sản phẩm không hợp lệ"
            );
        }

        product.setProductsCode(productCode);
        product.setBarcode(barcode);
        product.setImageUrl(
                normalizeNullable(
                        proposedProduct.getImageUrl()
                )
        );
        product.setName(
                proposedProduct.getName().trim()
        );
        product.setCategory(category);
        product.setBrand(brand);
        product.setBaseUnit(
                proposedProduct.getBaseUnit()
                        .trim()
                        .toUpperCase(Locale.ROOT)
        );
        product.setPurchasePrice(
                proposedProduct.getPurchasePrice()
        );
        product.setSellingPrice(
                proposedProduct.getSellingPrice()
        );
        product.setStatus(proposedStatus);
    }

    private Users getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                        authentication.getPrincipal()
                )) {
            throw new Unauthorized(
                    "Người dùng chưa đăng nhập"
            );
        }

        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new NotFound(
                                "Không tìm thấy tài khoản đang đăng nhập"
                        )
                );
    }

    private String generateRequestCode() {
        String requestCode;

        do {
            requestCode =
                    "PCR-"
                            + UUID.randomUUID()
                            .toString()
                            .toUpperCase();
        } while (
                changeRequestRepository
                        .existsByProductChangeRequestsCode(
                                requestCode
                        )
        );

        return requestCode;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BadRequest(
                    "Không thể lưu dữ liệu yêu cầu sản phẩm"
            );
        }
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private void validatePageParameters(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new BadRequest(
                    "Số trang không được nhỏ hơn 0"
            );
        }

        if (size < 1 || size > 100) {
            throw new BadRequest(
                    "Kích thước trang phải từ 1 đến 100"
            );
        }
    }
}