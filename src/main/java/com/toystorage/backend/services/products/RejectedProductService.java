package com.toystorage.backend.services.products;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toystorage.backend.dto.request.products.ResubmitRejectedProductRequest;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RejectedProductService {

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
            productApprovalMapper;

    private final ObjectMapper
            objectMapper;


    // =====================================================
    // REJECTED LIST
    // =====================================================

    @Transactional(readOnly = true)
    public ProductApprovalPageResponse getRejectedProducts(
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
                        .findLatestRequestsByStatus(
                                ProductChangeRequestStatus.REJECTED,
                                pageable
                        );

        return toPageResponse(
                result
        );
    }


    // =====================================================
    // REJECTED DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public ProductApprovalRequestResponse getRejectedProductDetail(
            Long requestId
    ) {

        ProductChangeRequests changeRequest =
                getRejectedChangeRequest(
                        requestId
                );

        return productApprovalMapper.toResponse(
                changeRequest
        );
    }


    // =====================================================
    // RESUBMIT
    // =====================================================

    @Transactional
    public ProductApprovalRequestResponse resubmit(
            Long requestId,
            ResubmitRejectedProductRequest request
    ) {

        if (request == null) {

            throw new BadRequest(
                    "Resubmit request is required"
            );
        }

        Users currentUser =
                getCurrentUser();

        ProductChangeRequests rejectedRequest =
                getRejectedChangeRequest(
                        requestId
                );

        Products product =
                rejectedRequest.getProduct();

        if (product == null) {

            throw new BadRequest(
                    "Rejected request does not contain a product"
            );
        }


        // -------------------------------------------------
        // Không cho gửi lại rejection cũ nếu đã có request mới.
        // -------------------------------------------------

        ProductChangeRequests latestRequest =
                productChangeRequestRepository
                        .findTopByProduct_IdOrderByCreatedAtDescIdDesc(
                                product.getId()
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Product change request not found"
                                )
                        );

        if (!latestRequest.getId().equals(
                rejectedRequest.getId()
        )) {

            throw new BadRequest(
                    "This rejected request is no longer the latest request of the product"
            );
        }


        // -------------------------------------------------
        // Không cho tồn tại 2 request PENDING cùng lúc.
        // -------------------------------------------------

        boolean hasPendingRequest =
                productChangeRequestRepository
                        .existsByProduct_IdAndStatus(
                                product.getId(),
                                ProductChangeRequestStatus.PENDING
                        );

        if (hasPendingRequest) {

            throw new BadRequest(
                    "Product already has a pending approval request"
            );
        }


        ProductChangeType requestType =
                productApprovalMapper
                        .resolveRequestType(
                                rejectedRequest
                        );


        /*
         * Task #4 chỉ yêu cầu xử lý sản phẩm bị từ chối
         * thuộc luồng CREATE hoặc UPDATE.
         */
        if (
                requestType
                        != ProductChangeType.CREATE
                        &&
                        requestType
                                != ProductChangeType.UPDATE
        ) {

            throw new BadRequest(
                    "Only rejected CREATE or UPDATE requests can be resubmitted"
            );
        }


        // -------------------------------------------------
        // Validate Category
        // -------------------------------------------------

        Categories category =
                categoryRepository
                        .findByIdAndStatus(
                                request.getCategoryId(),
                                CommonStatus.ACTIVE
                        )
                        .orElseThrow(() ->
                                new BadRequest(
                                        "Active category not found with id: "
                                                + request.getCategoryId()
                                )
                        );


        // -------------------------------------------------
        // Validate Brand
        // -------------------------------------------------

        Brands brand = null;

        if (request.getBrandId() != null) {

            brand =
                    brandRepository
                            .findByIdAndStatus(
                                    request.getBrandId(),
                                    CommonStatus.ACTIVE
                            )
                            .orElseThrow(() ->
                                    new BadRequest(
                                            "Active brand not found with id: "
                                                    + request.getBrandId()
                                    )
                            );
        }


        // -------------------------------------------------
        // Normalize data
        // -------------------------------------------------

        String productCode =
                request
                        .getProductCode()
                        .trim();

        String barcode =
                request
                        .getBarcode()
                        .trim();

        String name =
                request
                        .getName()
                        .trim();

        String baseUnit =
                request
                        .getBaseUnit()
                        .trim();


        // -------------------------------------------------
        // Duplicate validation
        // -------------------------------------------------

        if (
                productRepository
                        .existsByProductsCodeIgnoreCaseAndIdNot(
                                productCode,
                                product.getId()
                        )
        ) {

            throw new BadRequest(
                    "Product code already exists: "
                            + productCode
            );
        }


        if (
                productRepository
                        .existsByBarcodeAndIdNot(
                                barcode,
                                product.getId()
                        )
        ) {

            throw new BadRequest(
                    "Barcode already exists: "
                            + barcode
            );
        }


        // -------------------------------------------------
        // Snapshot trước khi gửi lại.
        // -------------------------------------------------

        String productBefore =
                serializeProductSnapshot(
                        product
                );


        /*
         * newValue là dữ liệu Business Staff đã sửa.
         *
         * Business Manager sau này sẽ đọc dữ liệu này
         * để approve.
         */
        String proposedValue =
                serializeProposedProduct(
                        product,
                        request,
                        category,
                        brand
                );


        String oldValue;


        // =================================================
        // REJECTED CREATE
        // =================================================

        if (requestType == ProductChangeType.CREATE) {

            /*
             * CREATE đã bị từ chối:
             *
             * Product row đã tồn tại với status REJECTED.
             * Khi Staff sửa và gửi lại, cập nhật lại thông tin
             * draft trên chính product đó và chuyển về
             * PENDING_CREATE.
             */

            product.setProductsCode(
                    productCode
            );

            product.setBarcode(
                    barcode
            );

            product.setImageUrl(
                    normalizeNullable(
                            request.getImageUrl()
                    )
            );

            product.setName(
                    name
            );

            product.setCategory(
                    category
            );

            product.setBrand(
                    brand
            );

            product.setBaseUnit(
                    baseUnit
            );

            product.setPurchasePrice(
                    request.getPurchasePrice()
            );

            product.setSellingPrice(
                    request.getSellingPrice()
            );

            product.setStatus(
                    ProductStatus.PENDING_CREATE
            );

            product.setApprovedBy(
                    null
            );

            product.setRejectionReason(
                    null
            );


            /*
             * CREATE request không có oldValue.
             *
             * Giữ nguyên quy ước của luồng Product Approval:
             * oldValue == null => CREATE.
             */
            oldValue = null;

        } else {

            // =================================================
            // REJECTED UPDATE
            // =================================================

            /*
             * UPDATE:
             *
             * Không apply dữ liệu sửa trực tiếp vào product,
             * vì dữ liệu thật chỉ được thay đổi khi
             * Business Manager approve.
             *
             * Ta chỉ chuyển trạng thái product sang
             * PENDING_UPDATE.
             */

            oldValue =
                    productBefore;

            product.setStatus(
                    ProductStatus.PENDING_UPDATE
            );


        }


        productRepository.save(
                product
        );


        // -------------------------------------------------
        // Tạo request MỚI.
        //
        // Request REJECTED cũ vẫn giữ nguyên để làm lịch sử.
        // -------------------------------------------------

        ProductChangeRequests newRequest =
                ProductChangeRequests.builder()
                        .productChangeRequestsCode(
                                generateRequestCode()
                        )
                        .product(
                                product
                        )
                        .createdBy(
                                currentUser
                        )
                        .approvedBy(
                                null
                        )
                        .oldValue(
                                oldValue
                        )
                        .newValue(
                                proposedValue
                        )
                        .requestReason(
                                normalizeNullable(
                                        request.getRequestReason()
                                )
                        )
                        .rejectionReason(
                                null
                        )
                        .status(
                                ProductChangeRequestStatus.PENDING
                        )
                        .approvedAt(
                                null
                        )
                        .build();


        newRequest =
                productChangeRequestRepository.save(
                        newRequest
                );


        String productAfter =
                serializeProductSnapshot(
                        product
                );


        saveHistory(
                currentUser,
                product,
                productBefore,
                productAfter
        );


        return productApprovalMapper.toResponse(
                newRequest
        );
    }


    // =====================================================
    // GET REJECTED REQUEST
    // =====================================================

    private ProductChangeRequests getRejectedChangeRequest(
            Long requestId
    ) {

        if (requestId == null) {

            throw new BadRequest(
                    "Product change request id is required"
            );
        }


        ProductChangeRequests changeRequest =
                productChangeRequestRepository
                        .findById(
                                requestId
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Product change request not found with id: "
                                                + requestId
                                )
                        );


        if (
                changeRequest.getStatus()
                        != ProductChangeRequestStatus.REJECTED
        ) {

            throw new BadRequest(
                    "Only REJECTED product change request can be processed"
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
                                productApprovalMapper::toResponse
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
    // PROPOSED PRODUCT JSON
    // =====================================================

    private String serializeProposedProduct(
            Products product,
            ResubmitRejectedProductRequest request,
            Categories category,
            Brands brand
    ) {

        Map<String, Object> proposed =
                new LinkedHashMap<>();


        proposed.put(
                "id",
                product.getId()
        );

        proposed.put(
                "productCode",
                request
                        .getProductCode()
                        .trim()
        );

        proposed.put(
                "barcode",
                request
                        .getBarcode()
                        .trim()
        );

        proposed.put(
                "imageUrl",
                normalizeNullable(
                        request.getImageUrl()
                )
        );

        proposed.put(
                "name",
                request
                        .getName()
                        .trim()
        );

        proposed.put(
                "categoryId",
                category.getId()
        );

        proposed.put(
                "brandId",
                brand != null
                        ? brand.getId()
                        : null
        );

        proposed.put(
                "baseUnit",
                request
                        .getBaseUnit()
                        .trim()
        );

        proposed.put(
                "purchasePrice",
                request.getPurchasePrice()
        );

        proposed.put(
                "sellingPrice",
                request.getSellingPrice()
        );


        try {

            return objectMapper
                    .writeValueAsString(
                            proposed
                    );

        } catch (JsonProcessingException exception) {

            throw new BadRequest(
                    "Cannot serialize resubmitted product data"
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
    // REQUEST CODE
    // =====================================================

    private String generateRequestCode() {

        String requestCode;

        do {

            requestCode =
                    "PCR-"
                            + UUID.randomUUID()
                            .toString()
                            .toUpperCase();

        } while (
                productChangeRequestRepository
                        .existsByProductChangeRequestsCode(
                                requestCode
                        )
        );


        return requestCode;
    }


    // =====================================================
    // NORMALIZE
    // =====================================================

    private String normalizeNullable(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return null;
        }

        return value.trim();
    }


    // =====================================================
    // ACTIVITY LOG
    // =====================================================

    private void saveHistory(
            Users user,
            Products product,
            String oldValue,
            String newValue
    ) {

        ActivityLogs activityLog =
                ActivityLogs.builder()
                        .user(
                                user
                        )
                        .action(
                                ActivityAction.UPDATE
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
                activityLog
        );
    }
}