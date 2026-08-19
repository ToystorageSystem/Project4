package com.toystorage.backend.services.receipts;

import com.toystorage.backend.dto.request.receipts.CreateGoodsReceiptItemRequest;
import com.toystorage.backend.dto.request.receipts.CreateGoodsReceiptRequest;
import com.toystorage.backend.dto.response.receipts.GoodsReceiptDetailResponse;
import com.toystorage.backend.dto.response.receipts.GoodsReceiptItemDetailResponse;
import com.toystorage.backend.dto.response.receipts.GoodsReceiptOptionResponse;
import com.toystorage.backend.dto.response.receipts.GoodsReceiptSummaryResponse;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.PurchaseOrderItems;
import com.toystorage.backend.entity.receipts.PurchaseOrders;
import com.toystorage.backend.entity.suppliers.Suppliers;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.enums.receipts.PurchaseOrderStatus;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.enums.warehouses.WarehouseType;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.repository.products.ProductRepository;
import com.toystorage.backend.repository.receipts.GoodsReceiptItemRepository;
import com.toystorage.backend.repository.receipts.GoodsReceiptRepository;
import com.toystorage.backend.repository.receipts.PurchaseOrderItemRepository;
import com.toystorage.backend.repository.receipts.PurchaseOrderRepository;
import com.toystorage.backend.repository.suppliers.SupplierRepository;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.warehouses.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class GoodsReceiptCreationService {

    private static final DateTimeFormatter CODE_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final GoodsReceiptRepository goodsReceiptRepository;

    private final GoodsReceiptItemRepository goodsReceiptItemRepository;

    private final PurchaseOrderRepository purchaseOrderRepository;

    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    private final SupplierRepository supplierRepository;

    private final WarehouseRepository warehouseRepository;

    private final ProductRepository productRepository;

    private final UserRepository userRepository;


    // =====================================================
    // BUSINESS STAFF CREATE GOODS RECEIPT
    // =====================================================

    @Transactional
    public GoodsReceiptDetailResponse create(
            CreateGoodsReceiptRequest request
    ) {

        // 1. Lấy Business Staff đang đăng nhập
        Users currentUser = getCurrentUser();


        // 2. Kiểm tra Supplier
        Suppliers supplier =
                getActiveSupplier(
                        request.getSupplierId()
                );


        // 3. Kiểm tra kho nhận hàng
        Warehouses warehouse =
                getActiveMainWarehouse(
                        request.getWarehouseId()
                );


        // 4. Kiểm tra danh sách sản phẩm
        validateItems(request.getItems());


        // 5. Load Product từ database
        Map<Long, Products> productsById =
                new HashMap<>();

        for (
                CreateGoodsReceiptItemRequest itemRequest
                : request.getItems()
        ) {

            Products product =
                    productRepository
                            .findById(
                                    itemRequest.getProductId()
                            )
                            .orElseThrow(() ->
                                    new NotFound(
                                            "Product not found with id: "
                                                    + itemRequest.getProductId()
                                    )
                            );


            // Task #5 chỉ cho dùng sản phẩm ACTIVE
            if (
                    product.getStatus()
                            != ProductStatus.ACTIVE
            ) {

                throw new BadRequest(
                        "Product "
                                + product.getId()
                                + " is not ACTIVE"
                );
            }


            productsById.put(
                    product.getId(),
                    product
            );
        }


        // =================================================
        // 6. TẠO PURCHASE ORDER NỀN
        // =================================================

        PurchaseOrders purchaseOrder =
                PurchaseOrders.builder()

                        .orderCode(
                                generatePurchaseOrderCode()
                        )

                        .supplier(supplier)

                        .warehouse(warehouse)

                        .status(
                                PurchaseOrderStatus.ORDERED
                        )

                        .createdBy(currentUser)

                        .note(
                                normalizeNote(
                                        request.getNote()
                                )
                        )

                        .build();


        purchaseOrder =
                purchaseOrderRepository.save(
                        purchaseOrder
                );


        // =================================================
        // 7. TẠO PURCHASE ORDER ITEMS
        // =================================================

        List<PurchaseOrderItems> purchaseOrderItems =
                new ArrayList<>();


        for (
                CreateGoodsReceiptItemRequest itemRequest
                : request.getItems()
        ) {

            Products product =
                    productsById.get(
                            itemRequest.getProductId()
                    );


            PurchaseOrderItems item =
                    PurchaseOrderItems.builder()

                            .purchaseOrder(
                                    purchaseOrder
                            )

                            .product(product)

                            .orderedQuantity(
                                    itemRequest
                                            .getExpectedQuantity()
                            )

                            .receivedQuantity(0)

                            .unitPrice(
                                    itemRequest
                                            .getUnitPrice()
                            )

                            .purchaseOrderItemsCode(
                                    generateCode("POI")
                            )

                            .build();


            purchaseOrderItems.add(item);
        }


        purchaseOrderItemRepository
                .saveAll(purchaseOrderItems);


        // =================================================
        // 8. TẠO GOODS RECEIPT
        // =================================================

        GoodsReceipts goodsReceipt =
                GoodsReceipts.builder()

                        .receiptCode(
                                generateReceiptCode()
                        )

                        .goodsReceiptsCode(
                                generateGoodsReceiptInternalCode()
                        )

                        .purchaseOrder(
                                purchaseOrder
                        )

                        .warehouse(warehouse)

                        // CREATED = Chờ xử lý
                        .status(
                                GoodsReceiptStatus.CREATED
                        )

                        .build();


        goodsReceipt =
                goodsReceiptRepository.save(
                        goodsReceipt
                );


        // =================================================
        // 9. TẠO GOODS RECEIPT ITEMS
        // =================================================

        List<GoodsReceiptItems> goodsReceiptItems =
                new ArrayList<>();


        for (
                CreateGoodsReceiptItemRequest itemRequest
                : request.getItems()
        ) {

            Products product =
                    productsById.get(
                            itemRequest.getProductId()
                    );


            GoodsReceiptItems item =
                    GoodsReceiptItems.builder()

                            .goodsReceipt(
                                    goodsReceipt
                            )

                            .product(product)

                            .expectedQuantity(
                                    itemRequest
                                            .getExpectedQuantity()
                            )

                            // Khi vừa tạo phiếu chưa nhận hàng
                            .actualQuantity(0)

                            .acceptedQuantity(0)

                            .damagedQuantity(0)

                            .surplusQuantity(0)

                            .shortageQuantity(0)

                            .goodsReceiptItemsCode(
                                    generateCode("GRI")
                            )

                            .build();


            goodsReceiptItems.add(item);
        }


        goodsReceiptItemRepository
                .saveAll(goodsReceiptItems);


        // =================================================
        // QUAN TRỌNG:
        // KHÔNG UPDATE INVENTORY Ở TASK NÀY
        // =================================================


        // 10. Trả response cho Frontend
        return buildDetailResponse(
                goodsReceipt,
                purchaseOrderItems,
                goodsReceiptItems
        );
    }


    // =====================================================
    // GET ACTIVE SUPPLIERS
    // =====================================================

    public List<GoodsReceiptOptionResponse> getActiveSuppliers() {

        return supplierRepository
                .findByStatusOrderByNameAsc(
                        CommonStatus.ACTIVE
                )
                .stream()
                .map(supplier ->
                        GoodsReceiptOptionResponse
                                .builder()
                                .id(supplier.getId())
                                .code(supplier.getSuppliersCode())
                                .name(supplier.getName())
                                .build()
                )
                .toList();
    }


    // =====================================================
    // GET ACTIVE MAIN WAREHOUSES
    // =====================================================

    public List<GoodsReceiptOptionResponse> getActiveMainWarehouses() {

        return warehouseRepository
                .findByTypeAndStatusOrderByNameAsc(
                        WarehouseType.MAIN_WAREHOUSE,
                        WarehouseStatus.ACTIVE
                )
                .stream()
                .map(warehouse ->
                        GoodsReceiptOptionResponse
                                .builder()
                                .id(warehouse.getId())
                                .code(warehouse.getWarehousesCode())
                                .name(warehouse.getName())
                                .build()
                )
                .toList();
    }


    // =====================================================
    // GET RECEIPTS OF CURRENT WAREHOUSE
    // =====================================================

    @Transactional(readOnly = true)
    public List<GoodsReceiptSummaryResponse> getCurrentWarehouseReceipts(
            GoodsReceiptStatus status
    ) {

        Users currentUser = getCurrentUser();

        if (currentUser.getWarehouse() == null) {
            throw new BadRequest(
                    "Current user is not assigned to a warehouse"
            );
        }


        Long warehouseId =
                currentUser.getWarehouse().getId();


        List<GoodsReceipts> receipts;


        if (status == null) {

            receipts =
                    goodsReceiptRepository
                            .findByWarehouseIdOrderByCreatedAtDesc(
                                    warehouseId
                            );

        } else {

            receipts =
                    goodsReceiptRepository
                            .findByWarehouseIdAndStatusOrderByCreatedAtDesc(
                                    warehouseId,
                                    status
                            );
        }


        return receipts
                .stream()
                .map(this::buildSummaryResponse)
                .toList();
    }


    // =====================================================
    // GET GOODS RECEIPT DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public GoodsReceiptDetailResponse getById(
            Long receiptId
    ) {

        GoodsReceipts receipt =
                goodsReceiptRepository
                        .findById(receiptId)
                        .orElseThrow(() ->
                                new NotFound(
                                        "Goods receipt not found with id: "
                                                + receiptId
                                )
                        );


        List<GoodsReceiptItems> receiptItems =
                goodsReceiptItemRepository
                        .findByGoodsReceiptId(
                                receipt.getId()
                        );


        List<PurchaseOrderItems> purchaseItems =
                purchaseOrderItemRepository
                        .findByPurchaseOrderId(
                                receipt
                                        .getPurchaseOrder()
                                        .getId()
                        );


        return buildDetailResponse(
                receipt,
                purchaseItems,
                receiptItems
        );
    }


    // =====================================================
    // BUILD DETAIL RESPONSE
    // =====================================================

    private GoodsReceiptDetailResponse buildDetailResponse(
            GoodsReceipts receipt,
            List<PurchaseOrderItems> purchaseOrderItems,
            List<GoodsReceiptItems> goodsReceiptItems
    ) {

        PurchaseOrders purchaseOrder =
                receipt.getPurchaseOrder();

        Suppliers supplier =
                purchaseOrder.getSupplier();

        Warehouses warehouse =
                receipt.getWarehouse();

        Users createdBy =
                purchaseOrder.getCreatedBy();


        /*
         * Map:
         *
         * productId
         *      ->
         * PurchaseOrderItem
         *
         * để lấy unitPrice nhanh hơn.
         */
        Map<Long, PurchaseOrderItems>
                purchaseItemByProduct =
                new HashMap<>();


        for (
                PurchaseOrderItems item
                : purchaseOrderItems
        ) {

            purchaseItemByProduct.put(
                    item.getProduct().getId(),
                    item
            );
        }


        BigDecimal estimatedTotal =
                BigDecimal.ZERO;


        List<GoodsReceiptItemDetailResponse>
                itemResponses =
                new ArrayList<>();


        for (
                GoodsReceiptItems receiptItem
                : goodsReceiptItems
        ) {

            Products product =
                    receiptItem.getProduct();


            PurchaseOrderItems purchaseItem =
                    purchaseItemByProduct.get(
                            product.getId()
                    );


            BigDecimal unitPrice =
                    purchaseItem != null
                            ? purchaseItem.getUnitPrice()
                            : BigDecimal.ZERO;


            BigDecimal subtotal =
                    unitPrice.multiply(
                            BigDecimal.valueOf(
                                    receiptItem
                                            .getExpectedQuantity()
                            )
                    );


            estimatedTotal =
                    estimatedTotal.add(
                            subtotal
                    );


            GoodsReceiptItemDetailResponse response =
                    GoodsReceiptItemDetailResponse
                            .builder()

                            .productId(
                                    product.getId()
                            )

                            .productCode(
                                    product.getProductsCode()
                            )

                            .productName(
                                    product.getName()
                            )

                            .expectedQuantity(
                                    receiptItem
                                            .getExpectedQuantity()
                            )

                            .unitPrice(
                                    unitPrice
                            )

                            .subtotal(
                                    subtotal
                            )

                            .build();


            itemResponses.add(response);
        }


        return GoodsReceiptDetailResponse
                .builder()

                .id(
                        receipt.getId()
                )

                .receiptCode(
                        receipt.getReceiptCode()
                )

                .status(
                        receipt.getStatus().name()
                )

                .purchaseOrderId(
                        purchaseOrder.getId()
                )

                .purchaseOrderCode(
                        purchaseOrder.getOrderCode()
                )

                .supplierId(
                        supplier.getId()
                )

                .supplierCode(
                        supplier.getSuppliersCode()
                )

                .supplierName(
                        supplier.getName()
                )

                .warehouseId(
                        warehouse.getId()
                )

                .warehouseCode(
                        warehouse.getWarehousesCode()
                )

                .warehouseName(
                        warehouse.getName()
                )

                .createdById(
                        createdBy.getId()
                )

                .createdByName(
                        createdBy.getName()
                )

                .note(
                        purchaseOrder.getNote()
                )

                .estimatedTotalAmount(
                        estimatedTotal
                )

                .createdAt(
                        receipt.getCreatedAt()
                )

                .items(
                        itemResponses
                )

                .build();
    }


    // =====================================================
    // BUILD SUMMARY RESPONSE
    // =====================================================

    private GoodsReceiptSummaryResponse buildSummaryResponse(
            GoodsReceipts receipt
    ) {

        List<GoodsReceiptItems> items =
                goodsReceiptItemRepository
                        .findByGoodsReceiptId(
                                receipt.getId()
                        );


        int totalProducts =
                items.size();


        int totalExpectedQuantity =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getExpectedQuantity
                        )
                        .sum();


        PurchaseOrders purchaseOrder =
                receipt.getPurchaseOrder();


        Suppliers supplier =
                purchaseOrder.getSupplier();


        Warehouses warehouse =
                receipt.getWarehouse();


        return GoodsReceiptSummaryResponse
                .builder()

                .id(
                        receipt.getId()
                )

                .receiptCode(
                        receipt.getReceiptCode()
                )

                .status(
                        receipt.getStatus().name()
                )

                .supplierId(
                        supplier.getId()
                )

                .supplierName(
                        supplier.getName()
                )

                .warehouseId(
                        warehouse.getId()
                )

                .warehouseName(
                        warehouse.getName()
                )

                .totalProducts(
                        totalProducts
                )

                .totalExpectedQuantity(
                        totalExpectedQuantity
                )

                .createdAt(
                        receipt.getCreatedAt()
                )

                .build();
    }


    // =====================================================
    // VALIDATE ITEMS
    // =====================================================

    private void validateItems(
            List<CreateGoodsReceiptItemRequest> items
    ) {

        if (
                items == null
                        || items.isEmpty()
        ) {

            throw new BadRequest(
                    "At least one product is required"
            );
        }


        Set<Long> productIds =
                new HashSet<>();


        for (
                CreateGoodsReceiptItemRequest item
                : items
        ) {

            if (
                    item == null
                            || item.getProductId() == null
            ) {

                throw new BadRequest(
                        "Product id is required"
                );
            }


            /*
             * add() trả false nếu ID đã tồn tại.
             */
            if (
                    !productIds.add(
                            item.getProductId()
                    )
            ) {

                throw new BadRequest(
                        "Duplicate product in goods receipt: "
                                + item.getProductId()
                );
            }
        }
    }


    // =====================================================
    // GET ACTIVE SUPPLIER
    // =====================================================

    private Suppliers getActiveSupplier(
            Long supplierId
    ) {

        Suppliers supplier =
                supplierRepository
                        .findById(supplierId)
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
                    "Supplier is not ACTIVE"
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
                        .findById(warehouseId)
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
                    "Goods receipt can only be created for MAIN_WAREHOUSE"
            );
        }


        if (
                warehouse.getStatus()
                        != WarehouseStatus.ACTIVE
        ) {

            throw new BadRequest(
                    "Warehouse is not ACTIVE"
            );
        }


        return warehouse;
    }


    // =====================================================
    // GET CURRENT LOGIN USER
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
    // GENERATE GOODS RECEIPT CODE
    // =====================================================

    private String generateReceiptCode() {

        String code;

        do {

            code =
                    "GR-"
                            + LocalDate.now()
                            .format(CODE_DATE)
                            + "-"
                            + randomToken(8);

        } while (
                goodsReceiptRepository
                        .existsByReceiptCode(code)
        );


        return code;
    }


    // =====================================================
    // GENERATE GOODS RECEIPT INTERNAL CODE
    // =====================================================

    private String generateGoodsReceiptInternalCode() {

        String code;

        do {

            code =
                    generateCode("GRC");

        } while (
                goodsReceiptRepository
                        .existsByGoodsReceiptsCode(
                                code
                        )
        );


        return code;
    }


    // =====================================================
    // GENERATE AUTO PURCHASE ORDER CODE
    // =====================================================

    private String generatePurchaseOrderCode() {

        String code;

        do {

            code =
                    "PO-AUTO-"
                            + LocalDate.now()
                            .format(CODE_DATE)
                            + "-"
                            + randomToken(8);

        } while (
                purchaseOrderRepository
                        .existsByOrderCode(code)
        );


        return code;
    }


    // =====================================================
    // GENERATE INTERNAL CODE
    // =====================================================

    private String generateCode(
            String prefix
    ) {

        return prefix
                + "-"
                + randomToken(12);
    }


    private String randomToken(
            int length
    ) {

        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, length)
                .toUpperCase();
    }


    // =====================================================
    // NORMALIZE NOTE
    // =====================================================

    private String normalizeNote(
            String note
    ) {

        if (note == null) {
            return null;
        }


        String normalized =
                note.trim();


        return normalized.isEmpty()
                ? null
                : normalized;
    }
}