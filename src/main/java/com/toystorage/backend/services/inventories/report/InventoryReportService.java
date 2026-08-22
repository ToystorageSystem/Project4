package com.toystorage.backend.services.inventories.report;

import com.toystorage.backend.config.security.CustomUserDetails;
import com.toystorage.backend.dto.response.inventories.report.InventoryProductLocationResponse;
import com.toystorage.backend.dto.response.inventories.report.InventoryReportItemResponse;
import com.toystorage.backend.dto.response.inventories.report.InventoryReportOptionResponse;
import com.toystorage.backend.dto.response.inventories.report.InventoryReportPageResponse;
import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.inventories.InventoryStockStatus;
import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.mapper.inventories.report.InventoryReportMapper;
import com.toystorage.backend.repository.inventories.InventoryBalanceRepository;
import com.toystorage.backend.repository.products.ProductRepository;
import com.toystorage.backend.repository.warehouses.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryReportService {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final int MAX_PAGE_SIZE = 100;


    private final InventoryBalanceRepository
            inventoryBalanceRepository;

    private final WarehouseRepository
            warehouseRepository;

    private final ProductRepository
            productRepository;

    private final InventoryReportMapper
            mapper;

    private final InventoryReportExcelService
            inventoryReportExcelService;


    // =====================================================
    // INVENTORY REPORT LIST
    // =====================================================

    @Transactional(readOnly = true)
    public InventoryReportPageResponse getInventoryReport(
            String keyword,
            Long warehouseId,
            Long categoryId,
            Long brandId,
            String stockStatus,
            Integer page,
            Integer size
    ) {

        Users currentUser =
                getCurrentUser();

        Long scopeWarehouseId =
                resolveScopeWarehouseId(
                        currentUser
                );

        String normalizedKeyword =
                normalizeKeyword(
                        keyword
                );

        String normalizedStockStatus =
                normalizeStockStatus(
                        stockStatus
                );

        int safePage =
                normalizePage(
                        page
                );

        int safeSize =
                normalizeSize(
                        size
                );


        validateRequestedWarehouse(
                warehouseId,
                scopeWarehouseId
        );


        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(
                                Sort.Direction.DESC,
                                "updatedAt"
                        )
                );


        Page<InventoryBalances> result =
                inventoryBalanceRepository
                        .searchInventoryReport(
                                normalizedKeyword,
                                warehouseId,
                                categoryId,
                                brandId,
                                normalizedStockStatus,
                                scopeWarehouseId,
                                pageable
                        );


        List<InventoryReportItemResponse> items =
                result
                        .getContent()
                        .stream()
                        .map(
                                mapper::toReportItem
                        )
                        .toList();


        return InventoryReportPageResponse
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
    // INVENTORY REPORT - EXCEL EXPORT
    // =====================================================

    /**
     * Xuất báo cáo tồn kho ra file Excel.
     *
     * Export sử dụng cùng:
     *
     * - keyword
     * - warehouse filter
     * - category filter
     * - brand filter
     * - stock status
     * - warehouse permission scope
     *
     * với API danh sách tồn kho.
     */
    @Transactional(readOnly = true)
    public byte[] exportInventoryReport(
            String keyword,
            Long warehouseId,
            Long categoryId,
            Long brandId,
            String stockStatus
    ) throws IOException {

        Users currentUser =
                getCurrentUser();


        Long scopeWarehouseId =
                resolveScopeWarehouseId(
                        currentUser
                );


        String normalizedKeyword =
                normalizeKeyword(
                        keyword
                );


        String normalizedStockStatus =
                normalizeStockStatus(
                        stockStatus
                );


        /*
         * Business Staff không được export
         * warehouse nằm ngoài phạm vi được phân công.
         */
        validateRequestedWarehouse(
                warehouseId,
                scopeWarehouseId
        );


        /*
         * Export toàn bộ dữ liệu thỏa điều kiện.
         *
         * Không phân trang vì file Excel cần chứa
         * toàn bộ kết quả sau search/filter.
         */
        Page<InventoryBalances> result =
                inventoryBalanceRepository
                        .searchInventoryReport(
                                normalizedKeyword,
                                warehouseId,
                                categoryId,
                                brandId,
                                normalizedStockStatus,
                                scopeWarehouseId,
                                Pageable.unpaged()
                        );


        List<InventoryReportItemResponse> items =
                result
                        .getContent()
                        .stream()

                        .map(
                                mapper::toReportItem
                        )

                        .toList();


        return inventoryReportExcelService
                .export(
                        items
                );
    }


    // =====================================================
    // PRODUCT INVENTORY BY LOCATIONS
    // =====================================================

    @Transactional(readOnly = true)
    public List<InventoryProductLocationResponse>
    getProductLocations(
            Long productId
    ) {

        if (productId == null) {

            throw new BadRequest(
                    "Product id is required"
            );
        }


        if (!productRepository.existsById(productId)) {

            throw new NotFound(
                    "Product not found with id: "
                            + productId
            );
        }


        Users currentUser =
                getCurrentUser();

        Long scopeWarehouseId =
                resolveScopeWarehouseId(
                        currentUser
                );


        return inventoryBalanceRepository
                .findProductLocations(
                        productId,
                        scopeWarehouseId
                )

                .stream()

                .map(
                        mapper::toProductLocation
                )

                .toList();
    }


    // =====================================================
    // WAREHOUSE / STORE OPTIONS
    // =====================================================

    @Transactional(readOnly = true)
    public List<InventoryReportOptionResponse>
    getWarehouseOptions() {

        Users currentUser =
                getCurrentUser();

        Long scopeWarehouseId =
                resolveScopeWarehouseId(
                        currentUser
                );


        /*
         * User được giới hạn tại một warehouse/store.
         */
        if (scopeWarehouseId != null) {

            Warehouses warehouse =
                    warehouseRepository
                            .findById(
                                    scopeWarehouseId
                            )
                            .orElseThrow(() ->
                                    new NotFound(
                                            "Warehouse not found with id: "
                                                    + scopeWarehouseId
                                    )
                            );


            if (warehouse.getStatus()
                    != WarehouseStatus.ACTIVE) {

                return List.of();
            }


            return List.of(
                    mapper.toWarehouseOption(
                            warehouse
                    )
            );
        }


        /*
         * Admin / Business Manager:
         * xem toàn bộ warehouse/store active.
         *
         * Business Staff có warehouse_id = null:
         * schema hiện tại chưa có multi-location assignment,
         * nên tạm thời không giới hạn warehouse.
         */
        return warehouseRepository
                .findByStatusOrderByNameAsc(
                        WarehouseStatus.ACTIVE
                )

                .stream()

                .map(
                        mapper::toWarehouseOption
                )

                .toList();
    }


    // =====================================================
    // CATEGORY OPTIONS
    // =====================================================

    @Transactional(readOnly = true)
    public List<InventoryReportOptionResponse>
    getCategoryOptions() {

        /*
         * Gọi để bảo đảm request
         * có user đăng nhập hợp lệ.
         */
        getCurrentUser();


        return productRepository
                .findInventoryReportCategories(
                        CommonStatus.ACTIVE
                )

                .stream()

                .map(
                        mapper::toCategoryOption
                )

                .toList();
    }


    // =====================================================
    // BRAND OPTIONS
    // =====================================================

    @Transactional(readOnly = true)
    public List<InventoryReportOptionResponse>
    getBrandOptions() {

        /*
         * Gọi để bảo đảm request
         * có user đăng nhập hợp lệ.
         */
        getCurrentUser();


        return productRepository
                .findInventoryReportBrands(
                        CommonStatus.ACTIVE
                )

                .stream()

                .map(
                        mapper::toBrandOption
                )

                .toList();
    }


    // =====================================================
    // CURRENT USER
    // =====================================================

    private Users getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new Unauthorized(
                    "User is not authenticated"
            );
        }


        Object principal =
                authentication.getPrincipal();


        if (!(principal instanceof CustomUserDetails userDetails)) {

            throw new Unauthorized(
                    "Invalid authenticated user"
            );
        }


        return userDetails.getUser();
    }


    // =====================================================
    // WAREHOUSE SCOPE
    // =====================================================

    /**
     * Xác định warehouse mà user được phép xem.
     *
     * Admin:
     * - xem toàn bộ.
     *
     * Business Manager:
     * - xem toàn bộ.
     *
     * Business Staff:
     * - nếu users.warehouse_id có giá trị
     *   thì chỉ được xem warehouse đó.
     *
     * - nếu users.warehouse_id = null
     *   thì schema hiện tại chưa có bảng
     *   multi-assignment để giới hạn nhiều warehouse/store.
     */
    private Long resolveScopeWarehouseId(
            Users currentUser
    ) {

        if (hasAuthority(
                "ROLE_ADMIN"
        )) {

            return null;
        }


        if (hasAuthority(
                "ROLE_BUSINESS_MANAGER"
        )) {

            return null;
        }


        if (hasAuthority(
                "ROLE_BUSINESS_STAFF"
        )) {

            if (currentUser.getWarehouse()
                    != null) {

                return currentUser
                        .getWarehouse()
                        .getId();
            }


            /*
             * Database hiện tại chỉ có users.warehouse_id
             * và chưa có bảng phân Business Staff
             * cho nhiều warehouse/store.
             *
             * Vì các Business Staff hiện tại có thể
             * warehouse_id = null nên tạm thời null
             * đại diện cho không giới hạn warehouse.
             */
            return null;
        }


        /*
         * Nếu sau này permission INVENTORY_REPORT_VIEW
         * được cấp cho role khác, ta vẫn giới hạn
         * theo warehouse của user nếu có.
         */
        if (currentUser.getWarehouse()
                != null) {

            return currentUser
                    .getWarehouse()
                    .getId();
        }


        return null;
    }


    // =====================================================
    // AUTHORITY CHECK
    // =====================================================

    private boolean hasAuthority(
            String authority
    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (authentication == null) {

            return false;
        }


        return authentication
                .getAuthorities()
                .stream()

                .map(
                        GrantedAuthority::getAuthority
                )

                .anyMatch(
                        authority::equals
                );
    }


    // =====================================================
    // VALIDATE WAREHOUSE FILTER
    // =====================================================

    private void validateRequestedWarehouse(
            Long requestedWarehouseId,
            Long scopeWarehouseId
    ) {

        if (requestedWarehouseId == null
                || scopeWarehouseId == null) {

            return;
        }


        if (!requestedWarehouseId.equals(
                scopeWarehouseId
        )) {

            throw new BadRequest(
                    "You are not allowed to view inventory "
                            + "for warehouse id: "
                            + requestedWarehouseId
            );
        }
    }


    // =====================================================
    // NORMALIZE STOCK STATUS
    // =====================================================

    private String normalizeStockStatus(
            String stockStatus
    ) {

        if (stockStatus == null
                || stockStatus.isBlank()) {

            return null;
        }


        String normalized =
                stockStatus
                        .trim()
                        .toUpperCase();


        try {

            return InventoryStockStatus
                    .valueOf(
                            normalized
                    )
                    .name();

        } catch (IllegalArgumentException ex) {

            throw new BadRequest(
                    "Invalid stock status. "
                            + "Allowed values: "
                            + "IN_STOCK, LOW_STOCK, OUT_OF_STOCK"
            );
        }
    }


    // =====================================================
    // NORMALIZE KEYWORD
    // =====================================================

    private String normalizeKeyword(
            String keyword
    ) {

        if (keyword == null
                || keyword.isBlank()) {

            return null;
        }


        return keyword.trim();
    }


    // =====================================================
    // PAGINATION
    // =====================================================

    private int normalizePage(
            Integer page
    ) {

        if (page == null) {

            return 0;
        }


        if (page < 0) {

            throw new BadRequest(
                    "Page must be greater than or equal to 0"
            );
        }


        return page;
    }


    private int normalizeSize(
            Integer size
    ) {

        if (size == null) {

            return DEFAULT_PAGE_SIZE;
        }


        if (size <= 0) {

            throw new BadRequest(
                    "Size must be greater than 0"
            );
        }


        if (size > MAX_PAGE_SIZE) {

            throw new BadRequest(
                    "Size must not exceed "
                            + MAX_PAGE_SIZE
            );
        }


        return size;
    }
}