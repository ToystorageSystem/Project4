package com.toystorage.backend.services.products;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toystorage.backend.dto.request.products.RejectProductApprovalRequest;
import com.toystorage.backend.dto.response.products.ProductApprovalPageResponse;
import com.toystorage.backend.dto.response.products.ProductApprovalRequestResponse;
import com.toystorage.backend.entity.products.Brands;
import com.toystorage.backend.entity.products.Categories;
import com.toystorage.backend.entity.products.ProductChangeRequests;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.enums.products.ProductChangeRequestStatus;
import com.toystorage.backend.enums.products.ProductChangeType;
import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.mapper.products.ProductApprovalMapper;
import com.toystorage.backend.repository.products.BrandRepository;
import com.toystorage.backend.repository.products.CategoryRepository;
import com.toystorage.backend.repository.products.ProductChangeRequestRepository;
import com.toystorage.backend.repository.products.ProductRepository;
import com.toystorage.backend.repository.users.ActivityLogRepository;
import com.toystorage.backend.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductApprovalService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductChangeRequestRepository
            productChangeRequestRepository;

    private final ProductRepository
            productRepository;

    private final CategoryRepository
            categoryRepository;

    private final BrandRepository
            brandRepository;

    private final UserRepository
            userRepository;

    private final ActivityLogRepository
            activityLogRepository;

    private final ProductApprovalMapper
            mapper;

    private final ObjectMapper
            objectMapper;


    // =====================================================
    // MANAGER - PENDING REQUESTS
    // =====================================================

    @Transactional(readOnly = true)
    public ProductApprovalPageResponse getPendingRequests(
            Integer page,
            Integer size
    ) {

        Pageable pageable =
                createPageable(
                        page,
                        size
                );

        Page<ProductChangeRequests> result =
                productChangeRequestRepository
                        .findByStatusOrderByCreatedAtDesc(
                                ProductChangeRequestStatus.PENDING,
                                pageable
                        );

        return toPageResponse(
                result
        );
    }


    // =====================================================
    // MANAGER - REQUEST DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public ProductApprovalRequestResponse getRequestDetail(
            Long requestId
    ) {

        ProductChangeRequests changeRequest =
                getChangeRequest(
                        requestId
                );

        return mapper.toResponse(
                changeRequest
        );
    }


    // =====================================================
    // BUSINESS STAFF - MY REQUESTS
    // =====================================================

    @Transactional(readOnly = true)
    public ProductApprovalPageResponse getMyRequests(
            Integer page,
            Integer size
    ) {

        Users currentUser =
                getCurrentUser();

        Pageable pageable =
                createPageable(
                        page,
                        size
                );

        Page<ProductChangeRequests> result =
                productChangeRequestRepository
                        .findByCreatedBy_IdOrderByCreatedAtDesc(
                                currentUser.getId(),
                                pageable
                        );

        return toPageResponse(
                result
        );
    }


    // =====================================================
    // APPROVE
    // =====================================================

    @Transactional
    public ProductApprovalRequestResponse approve(
            Long requestId
    ) {

        Users manager =
                getCurrentUser();

        ProductChangeRequests changeRequest =
                getPendingChangeRequest(
                        requestId
                );

        Products product =
                changeRequest.getProduct();

        if (product == null) {

            throw new BadRequest(
                    "Product change request does not contain a product"
            );
        }


        ProductChangeType requestType =
                mapper.resolveRequestType(
                        changeRequest
                );


        String productBefore =
                serializeProductSnapshot(
                        product
                );


        switch (requestType) {

            case CREATE ->
                    approveCreate(
                            changeRequest,
                            product,
                            manager
                    );

            case UPDATE ->
                    approveUpdate(
                            changeRequest,
                            product,
                            manager
                    );

            case DEACTIVATE ->
                    approveDeactivate(
                            changeRequest,
                            product,
                            manager
                    );

            default ->
                    throw new BadRequest(
                            "Unsupported product change request type"
                    );
        }


        changeRequest.setStatus(
                ProductChangeRequestStatus.APPROVED
        );

        changeRequest.setApprovedBy(
                manager
        );

        changeRequest.setApprovedAt(
                LocalDateTime.now()
        );

        changeRequest.setRejectionReason(
                null
        );


        productRepository.save(
                product
        );

        productChangeRequestRepository.save(
                changeRequest
        );


        String productAfter =
                serializeProductSnapshot(
                        product
                );


        saveHistory(
                manager,
                ActivityAction.APPROVE,
                product,
                productBefore,
                productAfter
        );


        return mapper.toResponse(
                changeRequest
        );
    }


    // =====================================================
    // REJECT
    // =====================================================

    @Transactional
    public ProductApprovalRequestResponse reject(
            Long requestId,
            RejectProductApprovalRequest request
    ) {

        Users manager =
                getCurrentUser();

        ProductChangeRequests changeRequest =
                getPendingChangeRequest(
                        requestId
                );

        String reason =
                normalizeRejectionReason(
                        request
                );

        Products product =
                changeRequest.getProduct();

        if (product == null) {

            throw new BadRequest(
                    "Product change request does not contain a product"
            );
        }


        ProductChangeType requestType =
                mapper.resolveRequestType(
                        changeRequest
                );


        String productBefore =
                serializeProductSnapshot(
                        product
                );


        /*
         * Nếu reject CREATE:
         * sản phẩm PENDING_CREATE sẽ chuyển thành REJECTED.
         *
         * Nếu reject UPDATE / DEACTIVATE:
         * sản phẩm thực tế giữ nguyên.
         */
        if (requestType == ProductChangeType.CREATE) {

            product.setStatus(
                    ProductStatus.REJECTED
            );

            product.setApprovedBy(
                    manager
            );

            product.setRejectionReason(
                    reason
            );

            productRepository.save(
                    product
            );
        }


        changeRequest.setStatus(
                ProductChangeRequestStatus.REJECTED
        );

        changeRequest.setApprovedBy(
                manager
        );

        changeRequest.setApprovedAt(
                LocalDateTime.now()
        );

        changeRequest.setRejectionReason(
                reason
        );


        productChangeRequestRepository.save(
                changeRequest
        );


        String productAfter =
                serializeProductSnapshot(
                        product
                );


        saveHistory(
                manager,
                ActivityAction.REJECT,
                product,
                productBefore,
                productAfter
        );


        return mapper.toResponse(
                changeRequest
        );
    }


    // =====================================================
    // APPROVE CREATE
    // =====================================================

    private void approveCreate(
            ProductChangeRequests changeRequest,
            Products product,
            Users manager
    ) {

        if (
                product.getStatus()
                        != ProductStatus.PENDING_CREATE
        ) {

            throw new BadRequest(
                    "Only PENDING_CREATE product can be approved as new product"
            );
        }


        JsonNode newValue =
                parseJson(
                        changeRequest.getNewValue()
                );


        applyRequestedProductData(
                product,
                newValue
        );


        validateDuplicateProduct(
                product
        );


        product.setStatus(
                ProductStatus.ACTIVE
        );

        product.setApprovedBy(
                manager
        );

        product.setRejectionReason(
                null
        );
    }


    // =====================================================
    // APPROVE UPDATE
    // =====================================================

    private void approveUpdate(
            ProductChangeRequests changeRequest,
            Products product,
            Users manager
    ) {

        if (
                product.getStatus()
                        == ProductStatus.PENDING_CREATE
                        ||
                        product.getStatus()
                                == ProductStatus.REJECTED
        ) {

            throw new BadRequest(
                    "Product is not in a valid state for update approval"
            );
        }


        JsonNode newValue =
                parseJson(
                        changeRequest.getNewValue()
                );


        ProductStatus currentStatus =
                product.getStatus();


        applyRequestedProductData(
                product,
                newValue
        );


        validateDuplicateProduct(
                product
        );


        /*
         * UPDATE thông thường không được tự ý thay đổi status.
         *
         * Status chỉ thay đổi qua request DEACTIVATE.
         */
        product.setStatus(
                currentStatus
        );

        product.setApprovedBy(
                manager
        );

        product.setRejectionReason(
                null
        );
    }


    // =====================================================
    // APPROVE DEACTIVATE
    // =====================================================

    private void approveDeactivate(
            ProductChangeRequests changeRequest,
            Products product,
            Users manager
    ) {

        if (
                product.getStatus()
                        != ProductStatus.ACTIVE
        ) {

            throw new BadRequest(
                    "Only ACTIVE product can be deactivated"
            );
        }


        /*
         * Không apply toàn bộ JSON để tránh việc một request
         * ẩn sản phẩm vô tình thay đổi các field khác.
         */
        product.setStatus(
                ProductStatus.INACTIVE
        );

        product.setApprovedBy(
                manager
        );

        product.setRejectionReason(
                null
        );
    }


    // =====================================================
    // APPLY REQUESTED PRODUCT DATA
    // =====================================================

    private void applyRequestedProductData(
            Products product,
            JsonNode newValue
    ) {

        if (
                newValue == null
                        || newValue.isNull()
        ) {

            throw new BadRequest(
                    "Product change request does not contain valid new value"
            );
        }


        if (newValue.hasNonNull("productCode")) {

            product.setProductsCode(
                    newValue
                            .get("productCode")
                            .asText()
                            .trim()
            );
        }


        if (newValue.hasNonNull("barcode")) {

            product.setBarcode(
                    newValue
                            .get("barcode")
                            .asText()
                            .trim()
            );
        }


        if (newValue.has("imageUrl")) {

            JsonNode imageUrl =
                    newValue.get("imageUrl");

            product.setImageUrl(
                    imageUrl == null
                            || imageUrl.isNull()
                            ? null
                            : imageUrl.asText()
            );
        }


        if (newValue.hasNonNull("name")) {

            product.setName(
                    newValue
                            .get("name")
                            .asText()
                            .trim()
            );
        }


        if (newValue.hasNonNull("categoryId")) {

            Long categoryId =
                    newValue
                            .get("categoryId")
                            .asLong();

            Categories category =
                    categoryRepository
                            .findByIdAndStatus(
                                    categoryId,
                                    CommonStatus.ACTIVE
                            )
                            .orElseThrow(() ->
                                    new BadRequest(
                                            "Active category not found with id: "
                                                    + categoryId
                                    )
                            );

            product.setCategory(
                    category
            );
        }


        if (newValue.has("brandId")) {

            JsonNode brandIdNode =
                    newValue.get(
                            "brandId"
                    );

            if (
                    brandIdNode == null
                            || brandIdNode.isNull()
            ) {

                product.setBrand(
                        null
                );

            } else {

                Long brandId =
                        brandIdNode.asLong();

                Brands brand =
                        brandRepository
                                .findByIdAndStatus(
                                        brandId,
                                        CommonStatus.ACTIVE
                                )
                                .orElseThrow(() ->
                                        new BadRequest(
                                                "Active brand not found with id: "
                                                        + brandId
                                        )
                                );

                product.setBrand(
                        brand
                );
            }
        }


        if (newValue.hasNonNull("baseUnit")) {

            product.setBaseUnit(
                    newValue
                            .get("baseUnit")
                            .asText()
                            .trim()
            );
        }


        if (newValue.hasNonNull("purchasePrice")) {

            BigDecimal purchasePrice =
                    newValue
                            .get("purchasePrice")
                            .decimalValue();

            if (
                    purchasePrice.compareTo(
                            BigDecimal.ZERO
                    ) < 0
            ) {

                throw new BadRequest(
                        "Purchase price must not be negative"
                );
            }

            product.setPurchasePrice(
                    purchasePrice
            );
        }


        if (newValue.hasNonNull("sellingPrice")) {

            BigDecimal sellingPrice =
                    newValue
                            .get("sellingPrice")
                            .decimalValue();

            if (
                    sellingPrice.compareTo(
                            BigDecimal.ZERO
                    ) <= 0
            ) {

                throw new BadRequest(
                        "Selling price must be greater than 0"
                );
            }

            product.setSellingPrice(
                    sellingPrice
            );
        }
    }


    // =====================================================
    // DUPLICATE VALIDATION
    // =====================================================

    private void validateDuplicateProduct(
            Products product
    ) {

        if (
                product.getProductsCode()
                        == null
                        ||
                        product.getProductsCode()
                                .isBlank()
        ) {

            throw new BadRequest(
                    "Product code is required"
            );
        }


        if (
                product.getBarcode()
                        == null
                        ||
                        product.getBarcode()
                                .isBlank()
        ) {

            throw new BadRequest(
                    "Barcode is required"
            );
        }


        boolean duplicatedCode =
                productRepository
                        .existsByProductsCodeIgnoreCaseAndIdNot(
                                product.getProductsCode(),
                                product.getId()
                        );

        if (duplicatedCode) {

            throw new BadRequest(
                    "Product code already exists: "
                            + product.getProductsCode()
            );
        }


        boolean duplicatedBarcode =
                productRepository
                        .existsByBarcodeAndIdNot(
                                product.getBarcode(),
                                product.getId()
                        );

        if (duplicatedBarcode) {

            throw new BadRequest(
                    "Barcode already exists: "
                            + product.getBarcode()
            );
        }
    }


    // =====================================================
    // GET REQUEST
    // =====================================================

    private ProductChangeRequests getChangeRequest(
            Long requestId
    ) {

        if (requestId == null) {

            throw new BadRequest(
                    "Product change request id is required"
            );
        }


        return productChangeRequestRepository
                .findById(
                        requestId
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Product change request not found with id: "
                                        + requestId
                        )
                );
    }


    private ProductChangeRequests getPendingChangeRequest(
            Long requestId
    ) {

        ProductChangeRequests changeRequest =
                getChangeRequest(
                        requestId
                );


        if (
                changeRequest.getStatus()
                        != ProductChangeRequestStatus.PENDING
        ) {

            throw new BadRequest(
                    "Only PENDING product change request can be processed"
            );
        }


        return changeRequest;
    }


    // =====================================================
    // CURRENT USER
    // =====================================================

    private Users getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (
                authentication == null
                        ||
                        !authentication.isAuthenticated()
                        ||
                        "anonymousUser".equals(
                                authentication.getPrincipal()
                        )
        ) {

            throw new Unauthorized(
                    "User is not authenticated"
            );
        }


        return userRepository
                .findByEmail(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Authenticated user not found"
                        )
                );
    }


    // =====================================================
    // REJECTION REASON
    // =====================================================

    private String normalizeRejectionReason(
            RejectProductApprovalRequest request
    ) {

        if (
                request == null
                        ||
                        request.getReason() == null
                        ||
                        request.getReason().isBlank()
        ) {

            throw new BadRequest(
                    "Rejection reason is required"
            );
        }


        String reason =
                request
                        .getReason()
                        .trim();


        if (reason.length() > 500) {

            throw new BadRequest(
                    "Rejection reason must not exceed 500 characters"
            );
        }


        return reason;
    }


    // =====================================================
    // PAGE
    // =====================================================

    private Pageable createPageable(
            Integer page,
            Integer size
    ) {

        int safePage =
                page == null
                        || page < 0
                        ? 0
                        : page;

        int safeSize =
                size == null
                        || size <= 0
                        ? DEFAULT_PAGE_SIZE
                        : Math.min(
                                size,
                                MAX_PAGE_SIZE
                        );


        return PageRequest.of(
                safePage,
                safeSize
        );
    }


    private ProductApprovalPageResponse toPageResponse(
            Page<ProductChangeRequests> result
    ) {

        List<ProductApprovalRequestResponse> items =
                result
                        .getContent()
                        .stream()
                        .map(
                                mapper::toResponse
                        )
                        .toList();


        return ProductApprovalPageResponse
                .builder()
                .items(
                        items
                )
                .page(
                        result.getNumber()
                )
                .size(
                        result.getSize()
                )
                .totalElements(
                        result.getTotalElements()
                )
                .totalPages(
                        result.getTotalPages()
                )
                .first(
                        result.isFirst()
                )
                .last(
                        result.isLast()
                )
                .build();
    }


    // =====================================================
    // JSON
    // =====================================================

    private JsonNode parseJson(
            String value
    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            return null;
        }


        try {

            return objectMapper.readTree(
                    value
            );

        } catch (JsonProcessingException exception) {

            throw new BadRequest(
                    "Invalid product change request JSON"
            );
        }
    }


    // =====================================================
    // PRODUCT SNAPSHOT
    // =====================================================

    private String serializeProductSnapshot(
            Products product
    ) {

        Map<String, Object> snapshot =
                new LinkedHashMap<>();


        snapshot.put(
                "id",
                product.getId()
        );

        snapshot.put(
                "productCode",
                product.getProductsCode()
        );

        snapshot.put(
                "barcode",
                product.getBarcode()
        );

        snapshot.put(
                "imageUrl",
                product.getImageUrl()
        );

        snapshot.put(
                "name",
                product.getName()
        );

        snapshot.put(
                "categoryId",
                product.getCategory() != null
                        ? product
                                .getCategory()
                                .getId()
                        : null
        );

        snapshot.put(
                "brandId",
                product.getBrand() != null
                        ? product
                                .getBrand()
                                .getId()
                        : null
        );

        snapshot.put(
                "baseUnit",
                product.getBaseUnit()
        );

        snapshot.put(
                "purchasePrice",
                product.getPurchasePrice()
        );

        snapshot.put(
                "sellingPrice",
                product.getSellingPrice()
        );

        snapshot.put(
                "status",
                product.getStatus() != null
                        ? product
                                .getStatus()
                                .name()
                        : null
        );

        snapshot.put(
                "approvedById",
                product.getApprovedBy() != null
                        ? product
                                .getApprovedBy()
                                .getId()
                        : null
        );

        snapshot.put(
                "rejectionReason",
                product.getRejectionReason()
        );


        try {

            return objectMapper
                    .writeValueAsString(
                            snapshot
                    );

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Cannot serialize product snapshot",
                    exception
            );
        }
    }


    // =====================================================
    // ACTIVITY LOG
    // =====================================================

    private void saveHistory(
            Users manager,
            ActivityAction action,
            Products product,
            String oldValue,
            String newValue
    ) {

        ActivityLogs log =
                ActivityLogs.builder()
                        .user(
                                manager
                        )
                        .action(
                                action
                        )
                        .entityType(
                                ActivityEntityType.PRODUCT
                        )
                        .entityId(
                                product.getId()
                        )
                        .oldValue(
                                oldValue
                        )
                        .newValue(
                                newValue
                        )
                        .build();


        activityLogRepository.save(
                log
        );
    }
}