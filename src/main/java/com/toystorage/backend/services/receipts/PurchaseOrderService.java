package com.toystorage.backend.services.receipts;

import com.toystorage.backend.dto.request.receipts.CancelPurchaseOrderRequest;
import com.toystorage.backend.dto.request.receipts.CreatePurchaseOrderRequest;
import com.toystorage.backend.dto.request.receipts.PurchaseOrderItemRequest;
import com.toystorage.backend.dto.request.receipts.UpdatePurchaseOrderRequest;
import com.toystorage.backend.dto.response.receipts.PurchaseOrderDetailResponse;
import com.toystorage.backend.dto.response.receipts.PurchaseOrderOptionResponse;
import com.toystorage.backend.dto.response.receipts.PurchaseOrderProductOptionResponse;
import com.toystorage.backend.dto.response.receipts.PurchaseOrderSummaryResponse;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.receipts.PurchaseOrderItems;
import com.toystorage.backend.entity.receipts.PurchaseOrders;
import com.toystorage.backend.entity.suppliers.SupplierProducts;
import com.toystorage.backend.entity.suppliers.Suppliers;
import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.enums.receipts.PurchaseOrderStatus;
import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.enums.warehouses.WarehouseType;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.mapper.receipts.PurchaseOrderMapper;
import com.toystorage.backend.repository.receipts.PurchaseOrderItemRepository;
import com.toystorage.backend.repository.receipts.PurchaseOrderRepository;
import com.toystorage.backend.repository.suppliers.SupplierProductRepository;
import com.toystorage.backend.repository.suppliers.SupplierRepository;
import com.toystorage.backend.repository.users.ActivityLogRepository;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.warehouses.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;

    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    private final SupplierRepository supplierRepository;

    private final SupplierProductRepository supplierProductRepository;

    private final WarehouseRepository warehouseRepository;

    private final UserRepository userRepository;

    private final ActivityLogRepository activityLogRepository;

    private final PurchaseOrderMapper purchaseOrderMapper;


    // =====================================================
    // GET SUPPLIER OPTIONS
    // =====================================================

    @Transactional(readOnly = true)
    public List<PurchaseOrderOptionResponse> getSupplierOptions() {

        return supplierRepository
                .findByStatusOrderByNameAsc(
                        CommonStatus.ACTIVE
                )
                .stream()
                .map(
                        purchaseOrderMapper::toSupplierOption
                )
                .toList();
    }


    // =====================================================
    // GET MAIN WAREHOUSE OPTIONS
    // =====================================================

    @Transactional(readOnly = true)
    public List<PurchaseOrderOptionResponse> getMainWarehouseOptions() {

        return warehouseRepository
                .findByTypeAndStatusOrderByNameAsc(
                        WarehouseType.MAIN_WAREHOUSE,
                        WarehouseStatus.ACTIVE
                )
                .stream()
                .map(
                        purchaseOrderMapper::toWarehouseOption
                )
                .toList();
    }


    // =====================================================
    // GET PRODUCTS BY SUPPLIER
    // =====================================================

    @Transactional(readOnly = true)
    public List<PurchaseOrderProductOptionResponse>
    getProductsBySupplier(
            Long supplierId
    ) {

        Suppliers supplier =
                getActiveSupplier(
                        supplierId
                );


        return supplierProductRepository
                .findBySupplier_IdAndStatusOrderByProduct_NameAsc(
                        supplier.getId(),
                        CommonStatus.ACTIVE
                )
                .stream()

                /*
                 * Link supplier-product ACTIVE thôi chưa đủ.
                 *
                 * Product cũng phải ACTIVE.
                 */
                .filter(link ->
                        link.getProduct() != null
                                && link.getProduct().getStatus()
                                == ProductStatus.ACTIVE
                )

                .map(
                        purchaseOrderMapper::toProductOption
                )
                .toList();
    }


    // =====================================================
    // GET PURCHASE ORDER LIST
    // =====================================================

    @Transactional(readOnly = true)
    public List<PurchaseOrderSummaryResponse> getPurchaseOrders(
            String keyword,
            PurchaseOrderStatus status,
            Long supplierId,
            LocalDate createdFrom,
            LocalDate createdTo
    ) {

        if (
                createdFrom != null
                        && createdTo != null
                        && createdFrom.isAfter(createdTo)
        ) {

            throw new BadRequest(
                    "Created from date cannot be after created to date"
            );
        }


        String normalizedKeyword =
                normalizeKeyword(
                        keyword
                );


        LocalDateTime fromDateTime =
                createdFrom != null
                        ? createdFrom.atStartOfDay()
                        : null;


        LocalDateTime toDateTime =
                createdTo != null
                        ? createdTo.atTime(
                                LocalTime.MAX
                        )
                        : null;


        List<PurchaseOrders> orders =
                purchaseOrderRepository.search(
                        normalizedKeyword,
                        status,
                        supplierId,
                        fromDateTime,
                        toDateTime
                );


        if (orders.isEmpty()) {
            return List.of();
        }


        List<Long> orderIds =
                orders
                        .stream()
                        .map(PurchaseOrders::getId)
                        .toList();


        List<PurchaseOrderItems> allItems =
                purchaseOrderItemRepository
                        .findAllByPurchaseOrderIds(
                                orderIds
                        );


        Map<Long, List<PurchaseOrderItems>>
                itemsByOrder =
                new HashMap<>();


        for (PurchaseOrderItems item : allItems) {

            Long orderId =
                    item
                            .getPurchaseOrder()
                            .getId();


            itemsByOrder
                    .computeIfAbsent(
                            orderId,
                            key -> new ArrayList<>()
                    )
                    .add(item);
        }


        return orders
                .stream()
                .map(order ->
                        purchaseOrderMapper
                                .toSummaryResponse(
                                        order,
                                        itemsByOrder.getOrDefault(
                                                order.getId(),
                                                List.of()
                                        )
                                )
                )
                .toList();
    }


    // =====================================================
    // GET PURCHASE ORDER DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public PurchaseOrderDetailResponse getPurchaseOrderDetail(
            Long orderId
    ) {

        PurchaseOrders order =
                getPurchaseOrder(
                        orderId
                );


        List<PurchaseOrderItems> items =
                purchaseOrderItemRepository
                        .findByPurchaseOrder_IdOrderByIdAsc(
                                order.getId()
                        );


        return purchaseOrderMapper
                .toDetailResponse(
                        order,
                        items
                );
    }


    // =====================================================
    // CREATE PURCHASE ORDER
    // =====================================================

    @Transactional
    public PurchaseOrderDetailResponse create(
            CreatePurchaseOrderRequest request
    ) {

        Users currentUser =
                getCurrentUser();


        Suppliers supplier =
                getActiveSupplier(
                        request.getSupplierId()
                );


        Warehouses warehouse =
                getActiveMainWarehouse(
                        request.getWarehouseId()
                );


        PurchaseOrders order =
                PurchaseOrders.builder()

                        .orderCode(
                                generateOrderCode()
                        )

                        .supplier(
                                supplier
                        )

                        .warehouse(
                                warehouse
                        )

                        .status(
                                PurchaseOrderStatus.DRAFT
                        )

                        .createdBy(
                                currentUser
                        )

                        .expectedDeliveryDate(
                                request.getExpectedDeliveryDate()
                        )

                        .note(
                                normalizeText(
                                        request.getNote()
                                )
                        )

                        .build();


        order =
                purchaseOrderRepository.save(
                        order
                );


        List<PurchaseOrderItems> items =
                buildItems(
                        order,
                        supplier,
                        request.getItems()
                );


        items =
                purchaseOrderItemRepository
                        .saveAll(
                                items
                        );


        saveHistory(
                currentUser,
                ActivityAction.CREATE,
                order,
                null,
                snapshot(
                        order,
                        items
                )
        );


        return purchaseOrderMapper
                .toDetailResponse(
                        order,
                        items
                );
    }


    // =====================================================
    // UPDATE DRAFT PURCHASE ORDER
    // =====================================================

    @Transactional
    public PurchaseOrderDetailResponse update(
            Long orderId,
            UpdatePurchaseOrderRequest request
    ) {

        PurchaseOrders order =
                getPurchaseOrder(
                        orderId
                );


        /*
         * Issue #9:
         * Không được sửa danh sách sản phẩm
         * sau khi đơn chuyển sang ORDERED.
         */
        if (
                order.getStatus()
                        != PurchaseOrderStatus.DRAFT
        ) {

            throw new BadRequest(
                    "Only DRAFT purchase orders can be updated"
            );
        }


        Users currentUser =
                getCurrentUser();


        List<PurchaseOrderItems> oldItems =
                purchaseOrderItemRepository
                        .findByPurchaseOrder_IdOrderByIdAsc(
                                order.getId()
                        );


        String oldValue =
                snapshot(
                        order,
                        oldItems
                );


        Suppliers supplier =
                getActiveSupplier(
                        request.getSupplierId()
                );


        Warehouses warehouse =
                getActiveMainWarehouse(
                        request.getWarehouseId()
                );


        order.setSupplier(
                supplier
        );

        order.setWarehouse(
                warehouse
        );

        order.setExpectedDeliveryDate(
                request.getExpectedDeliveryDate()
        );

        order.setNote(
                normalizeText(
                        request.getNote()
                )
        );


        purchaseOrderRepository.save(
                order
        );


        /*
         * Xóa các item cũ của DRAFT.
         *
         * Sau đó tạo lại theo request mới.
         *
         * Không xóa Purchase Order.
         */
        purchaseOrderItemRepository
                .deleteByPurchaseOrder_Id(
                        order.getId()
                );


        /*
         * Flush DELETE trước khi INSERT lại.
         *
         * Tránh unique constraint:
         * purchase_order_id + product_id.
         */
        purchaseOrderItemRepository.flush();


        List<PurchaseOrderItems> newItems =
                buildItems(
                        order,
                        supplier,
                        request.getItems()
                );


        newItems =
                purchaseOrderItemRepository
                        .saveAll(
                                newItems
                        );


        saveHistory(
                currentUser,
                ActivityAction.UPDATE,
                order,
                oldValue,
                snapshot(
                        order,
                        newItems
                )
        );


        return purchaseOrderMapper
                .toDetailResponse(
                        order,
                        newItems
                );
    }


    // =====================================================
    // SUBMIT PURCHASE ORDER
    // DRAFT -> ORDERED
    // =====================================================

    @Transactional
    public PurchaseOrderDetailResponse submit(
            Long orderId
    ) {

        PurchaseOrders order =
                getPurchaseOrder(
                        orderId
                );


        if (
                order.getStatus()
                        != PurchaseOrderStatus.DRAFT
        ) {

            throw new BadRequest(
                    "Only DRAFT purchase orders can be submitted"
            );
        }


        List<PurchaseOrderItems> items =
                purchaseOrderItemRepository
                        .findByPurchaseOrder_IdOrderByIdAsc(
                                order.getId()
                        );


        if (items.isEmpty()) {

            throw new BadRequest(
                    "Purchase order must contain at least one product"
            );
        }


        /*
         * Trước khi gửi,
         * kiểm tra Supplier vẫn còn ACTIVE.
         */
        if (
                order.getSupplier() == null
                        || order.getSupplier().getStatus()
                        != CommonStatus.ACTIVE
        ) {

            throw new BadRequest(
                    "Supplier is no longer active"
            );
        }


        /*
         * Kho phải vẫn là kho tổng ACTIVE.
         */
        if (
                order.getWarehouse() == null
                        || order.getWarehouse().getType()
                        != WarehouseType.MAIN_WAREHOUSE
                        || order.getWarehouse().getStatus()
                        != WarehouseStatus.ACTIVE
        ) {

            throw new BadRequest(
                    "Receiving warehouse is not an active main warehouse"
            );
        }


        /*
         * Kiểm tra lại từng Product trước khi gửi.
         */
        validateItemsBeforeSubmit(
                order,
                items
        );


        Users currentUser =
                getCurrentUser();


        String oldValue =
                snapshot(
                        order,
                        items
                );


        order.setStatus(
                PurchaseOrderStatus.ORDERED
        );


        order =
                purchaseOrderRepository.save(
                        order
                );


        saveHistory(
                currentUser,
                ActivityAction.UPDATE,
                order,
                oldValue,
                snapshot(
                        order,
                        items
                )
        );


        return purchaseOrderMapper
                .toDetailResponse(
                        order,
                        items
                );
    }


    // =====================================================
    // CANCEL PURCHASE ORDER
    // =====================================================

    @Transactional
    public PurchaseOrderDetailResponse cancel(
            Long orderId,
            CancelPurchaseOrderRequest request
    ) {

        PurchaseOrders order =
                getPurchaseOrder(
                        orderId
                );


        /*
         * Issue:
         * Không được hủy đơn đã hoàn tất.
         */
        if (
                order.getStatus()
                        == PurchaseOrderStatus.COMPLETED
        ) {

            throw new BadRequest(
                    "Completed purchase order cannot be cancelled"
            );
        }


        if (
                order.getStatus()
                        == PurchaseOrderStatus.CANCELLED
        ) {

            throw new BadRequest(
                    "Purchase order is already cancelled"
            );
        }


        List<PurchaseOrderItems> items =
                purchaseOrderItemRepository
                        .findByPurchaseOrder_IdOrderByIdAsc(
                                order.getId()
                        );


        Users currentUser =
                getCurrentUser();


        String oldValue =
                snapshot(
                        order,
                        items
                );


        order.setCancelReason(
                normalizeText(
                        request.getReason()
                )
        );

        order.setStatus(
                PurchaseOrderStatus.CANCELLED
        );


        order =
                purchaseOrderRepository.save(
                        order
                );


        saveHistory(
                currentUser,
                ActivityAction.CANCEL,
                order,
                oldValue,
                snapshot(
                        order,
                        items
                )
        );


        return purchaseOrderMapper
                .toDetailResponse(
                        order,
                        items
                );
    }


    // =====================================================
    // BUILD PURCHASE ORDER ITEMS
    // =====================================================

    private List<PurchaseOrderItems> buildItems(
            PurchaseOrders order,
            Suppliers supplier,
            List<PurchaseOrderItemRequest> requests
    ) {

        if (
                requests == null
                        || requests.isEmpty()
        ) {

            throw new BadRequest(
                    "Purchase order must contain at least one product"
            );
        }


        List<PurchaseOrderItems> result =
                new ArrayList<>();


        Set<Long> productIds =
                new HashSet<>();


        for (PurchaseOrderItemRequest request : requests) {

            Long productId =
                    request.getProductId();


            /*
             * Không cho Product xuất hiện 2 lần
             * trong cùng Purchase Order.
             */
            if (!productIds.add(productId)) {

                throw new BadRequest(
                        "Duplicate product in purchase order: "
                                + productId
                );
            }


            /*
             * Quan trọng:
             *
             * Chỉ cho thêm Product đã liên kết ACTIVE
             * với Supplier đang chọn.
             */
            SupplierProducts supplierProduct =
                    supplierProductRepository
                            .findBySupplier_IdAndProduct_IdAndStatus(
                                    supplier.getId(),
                                    productId,
                                    CommonStatus.ACTIVE
                            )
                            .orElseThrow(() ->
                                    new BadRequest(
                                            "Product "
                                                    + productId
                                                    + " is not actively linked to supplier "
                                                    + supplier.getId()
                                    )
                            );


            Products product =
                    supplierProduct.getProduct();


            if (
                    product == null
                            || product.getStatus()
                            != ProductStatus.ACTIVE
            ) {

                throw new BadRequest(
                        "Product "
                                + productId
                                + " is not active"
                );
            }


            Integer quantity =
                    request.getOrderedQuantity();


            /*
             * supplier_products có MOQ.
             */
            Integer minimumOrderQuantity =
                    supplierProduct
                            .getMinimumOrderQuantity();


            if (
                    minimumOrderQuantity != null
                            && quantity
                            < minimumOrderQuantity
            ) {

                throw new BadRequest(
                        "Ordered quantity for product "
                                + productId
                                + " must be at least "
                                + minimumOrderQuantity
                );
            }


            BigDecimal unitPrice =
                    resolveUnitPrice(
                            request.getUnitPrice(),
                            supplierProduct.getPurchasePrice()
                    );


            PurchaseOrderItems item =
                    PurchaseOrderItems.builder()

                            .purchaseOrder(
                                    order
                            )

                            .product(
                                    product
                            )

                            .orderedQuantity(
                                    quantity
                            )

                            .receivedQuantity(
                                    0
                            )

                            .unitPrice(
                                    unitPrice
                            )

                            .purchaseOrderItemsCode(
                                    generateItemCode()
                            )

                            .build();


            result.add(
                    item
            );
        }


        return result;
    }


    // =====================================================
    // RESOLVE PURCHASE PRICE
    // =====================================================

    private BigDecimal resolveUnitPrice(
            BigDecimal requestedPrice,
            BigDecimal supplierPrice
    ) {

        if (
                supplierPrice == null
                        || supplierPrice.compareTo(
                        BigDecimal.ZERO
                ) <= 0
        ) {

            throw new BadRequest(
                    "Supplier purchase price is invalid"
            );
        }


        /*
         * FE không truyền giá
         * -> lấy giá mặc định từ supplier_products.
         */
        if (requestedPrice == null) {

            return supplierPrice;
        }


        /*
         * Nếu FE truyền đúng giá NCC
         * thì không cần quyền đặc biệt.
         */
        if (
                requestedPrice.compareTo(
                        supplierPrice
                ) == 0
        ) {

            return requestedPrice;
        }


        /*
         * Giá khác giá supplier_products
         * -> phải có quyền override.
         */
        if (!canOverridePurchasePrice()) {

            throw new Forbidden(
                    "You do not have permission to override purchase price"
            );
        }


        return requestedPrice;
    }


    // =====================================================
    // PRICE OVERRIDE PERMISSION
    // =====================================================

    private boolean canOverridePurchasePrice() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (
                authentication == null
                        || !authentication.isAuthenticated()
        ) {

            return false;
        }


        Set<String> authorities =
                authentication
                        .getAuthorities()
                        .stream()
                        .map(
                                GrantedAuthority::getAuthority
                        )
                        .collect(
                                java.util.stream.Collectors.toSet()
                        );


        return authorities.contains(
                "PURCHASE_ORDER_PRICE_OVERRIDE"
        )
                || authorities.contains(
                "ROLE_BUSINESS_MANAGER"
        )
                || authorities.contains(
                "ROLE_ADMIN"
        );
    }


    // =====================================================
    // VALIDATE ITEMS BEFORE SUBMIT
    // =====================================================

    private void validateItemsBeforeSubmit(
            PurchaseOrders order,
            List<PurchaseOrderItems> items
    ) {

        for (PurchaseOrderItems item : items) {

            if (
                    item.getProduct() == null
                            || item.getProduct().getStatus()
                            != ProductStatus.ACTIVE
            ) {

                throw new BadRequest(
                        "Purchase order contains inactive product"
                );
            }


            supplierProductRepository
                    .findBySupplier_IdAndProduct_IdAndStatus(
                            order.getSupplier().getId(),
                            item.getProduct().getId(),
                            CommonStatus.ACTIVE
                    )
                    .orElseThrow(() ->
                            new BadRequest(
                                    "Product "
                                            + item.getProduct().getId()
                                            + " is no longer linked to supplier "
                                            + order.getSupplier().getId()
                            )
                    );
        }
    }


    // =====================================================
    // GET PURCHASE ORDER ENTITY
    // =====================================================

    private PurchaseOrders getPurchaseOrder(
            Long orderId
    ) {

        return purchaseOrderRepository
                .findDetailedById(
                        orderId
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Purchase order not found with id: "
                                        + orderId
                        )
                );
    }


    // =====================================================
    // GET ACTIVE SUPPLIER
    // =====================================================

    private Suppliers getActiveSupplier(
            Long supplierId
    ) {

        Suppliers supplier =
                supplierRepository
                        .findById(
                                supplierId
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Supplier not found with id: "
                                                + supplierId
                                )
                        );


        if (
                supplier.getStatus()
                        != CommonStatus.ACTIVE
        ) {

            throw new BadRequest(
                    "Supplier is not active"
            );
        }


        return supplier;
    }


    // =====================================================
    // GET ACTIVE MAIN WAREHOUSE
    // =====================================================

    private Warehouses getActiveMainWarehouse(
            Long warehouseId
    ) {

        Warehouses warehouse =
                warehouseRepository
                        .findById(
                                warehouseId
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Warehouse not found with id: "
                                                + warehouseId
                                )
                        );


        if (
                warehouse.getType()
                        != WarehouseType.MAIN_WAREHOUSE
        ) {

            throw new BadRequest(
                    "Purchase order can only be received by MAIN_WAREHOUSE"
            );
        }


        if (
                warehouse.getStatus()
                        != WarehouseStatus.ACTIVE
        ) {

            throw new BadRequest(
                    "Warehouse is not active"
            );
        }


        return warehouse;
    }


    // =====================================================
    // CURRENT LOGIN USER
    // =====================================================

    private Users getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (
                authentication == null
                        || !authentication.isAuthenticated()
                        || "anonymousUser".equals(
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
    // SAVE ACTIVITY LOG
    // =====================================================

    private void saveHistory(
            Users user,
            ActivityAction action,
            PurchaseOrders order,
            String oldValue,
            String newValue
    ) {

        ActivityLogs activityLog =
                ActivityLogs.builder()

                        .user(
                                user
                        )

                        .action(
                                action
                        )

                        .entityType(
                                ActivityEntityType.PURCHASE_ORDER
                        )

                        .entityId(
                                order.getId()
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


    // =====================================================
    // SNAPSHOT FOR AUDIT
    // =====================================================

    private String snapshot(
            PurchaseOrders order,
            List<PurchaseOrderItems> items
    ) {

        StringBuilder builder =
                new StringBuilder();


        builder.append(
                "orderCode="
        ).append(
                order.getOrderCode()
        );


        builder.append(
                ", supplierId="
        ).append(
                order.getSupplier() != null
                        ? order.getSupplier().getId()
                        : null
        );


        builder.append(
                ", warehouseId="
        ).append(
                order.getWarehouse() != null
                        ? order.getWarehouse().getId()
                        : null
        );


        builder.append(
                ", status="
        ).append(
                order.getStatus()
        );


        builder.append(
                ", expectedDeliveryDate="
        ).append(
                order.getExpectedDeliveryDate()
        );


        builder.append(
                ", note="
        ).append(
                order.getNote()
        );


        builder.append(
                ", rejectionReason="
        ).append(
                order.getRejectionReason()
        );


        builder.append(
                ", cancelReason="
        ).append(
                order.getCancelReason()
        );


        builder.append(
                ", items=["
        );


        for (int i = 0; i < items.size(); i++) {

            PurchaseOrderItems item =
                    items.get(i);


            if (i > 0) {
                builder.append(", ");
            }


            builder.append(
                    "{productId="
            ).append(
                    item.getProduct() != null
                            ? item.getProduct().getId()
                            : null
            ).append(
                    ", orderedQuantity="
            ).append(
                    item.getOrderedQuantity()
            ).append(
                    ", receivedQuantity="
            ).append(
                    item.getReceivedQuantity()
            ).append(
                    ", unitPrice="
            ).append(
                    item.getUnitPrice()
            ).append(
                    "}"
            );
        }


        builder.append(
                "]"
        );


        return builder.toString();
    }


    // =====================================================
    // GENERATE PURCHASE ORDER CODE
    // =====================================================

    private String generateOrderCode() {

        DateTimeFormatter formatter =
                DateTimeFormatter.BASIC_ISO_DATE;


        for (int attempt = 0; attempt < 20; attempt++) {

            String code =
                    "PO-"
                            + LocalDate.now()
                            .format(formatter)

                            + "-"

                            + UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, 8)
                            .toUpperCase();


            if (
                    !purchaseOrderRepository
                            .existsByOrderCode(
                                    code
                            )
            ) {

                return code;
            }
        }


        throw new IllegalStateException(
                "Could not generate unique purchase order code"
        );
    }


    // =====================================================
    // GENERATE ITEM CODE
    // =====================================================

    private String generateItemCode() {

        for (int attempt = 0; attempt < 20; attempt++) {

            String code =
                    "POI-"
                            + UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, 12)
                            .toUpperCase();


            if (
                    !purchaseOrderItemRepository
                            .existsByPurchaseOrderItemsCode(
                                    code
                            )
            ) {

                return code;
            }
        }


        throw new IllegalStateException(
                "Could not generate unique purchase order item code"
        );
    }


    // =====================================================
    // NORMALIZE KEYWORD
    // =====================================================

    private String normalizeKeyword(
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
    // NORMALIZE TEXT
    // =====================================================

    private String normalizeText(
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
}