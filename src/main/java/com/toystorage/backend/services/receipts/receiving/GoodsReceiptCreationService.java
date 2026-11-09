package com.toystorage.backend.services.receipts.receiving;

import com.toystorage.backend.dto.request.receipts.receiving.CreateGoodsReceiptItemRequest;
import com.toystorage.backend.dto.request.receipts.receiving.CreateGoodsReceiptRequest;
import com.toystorage.backend.dto.response.receipts.receiving.GoodsReceiptDetailResponse;
import com.toystorage.backend.dto.response.receipts.receiving.GoodsReceiptItemDetailResponse;
import com.toystorage.backend.dto.response.receipts.receiving.GoodsReceiptOptionResponse;
import com.toystorage.backend.dto.response.receipts.receiving.GoodsReceiptSummaryResponse;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.PurchaseOrderItems;
import com.toystorage.backend.entity.receipts.PurchaseOrders;
import com.toystorage.backend.entity.suppliers.Suppliers;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.enums.receipts.PurchaseOrderStatus;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.enums.warehouses.WarehouseType;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.repository.products.ProductRepository;
import com.toystorage.backend.repository.receipts.business.PurchaseOrderItemRepository;
import com.toystorage.backend.repository.receipts.business.PurchaseOrderRepository;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptItemRepository;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptRepository;
import com.toystorage.backend.repository.suppliers.SupplierRepository;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.warehouses.business.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    public GoodsReceiptDetailResponse create(CreateGoodsReceiptRequest request) {
        // Xác thực phiên và vai trò thực hiện tại Controller.
        getCurrentUser();

        // Nguồn dữ liệu duy nhất cho phiếu nhập là Purchase Order đã gửi.
        PurchaseOrders purchaseOrder = purchaseOrderRepository
                .findForCancellationUpdate(request.getPurchaseOrderId())
                .orElseThrow(() -> new NotFound(
                        "Purchase order not found with id: " + request.getPurchaseOrderId()
                ));

        if (purchaseOrder.getStatus() != PurchaseOrderStatus.ORDERED
                && purchaseOrder.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new BadRequest(
                    "Goods receipt can only be created from ORDERED or PARTIALLY_RECEIVED purchase orders"
            );
        }

        // Giữ mối liên kết NCC + kho giống PO, không lấy từ request.
        getActiveSupplier(purchaseOrder.getSupplier().getId());
        Warehouses warehouse = getActiveMainWarehouse(purchaseOrder.getWarehouse().getId());

        // Không tạo hai phiếu nhận cùng lúc cho một PO.
        if (goodsReceiptRepository.existsByPurchaseOrder_IdAndStatusIn(
                purchaseOrder.getId(),
                List.of(
                        GoodsReceiptStatus.CREATED,
                        GoodsReceiptStatus.RECEIVING,
                        GoodsReceiptStatus.INSPECTED,
                        GoodsReceiptStatus.CONFIRMED
                ))) {
            throw new BadRequest(
                    "This purchase order already has a goods receipt awaiting completion"
            );
        }

        validateItems(request.getItems());
        List<PurchaseOrderItems> purchaseOrderItems = purchaseOrderItemRepository
                .findByPurchaseOrder_IdOrderByIdAsc(purchaseOrder.getId());

        if (purchaseOrderItems.isEmpty()) {
            throw new BadRequest("Purchase order contains no products");
        }

        Map<Long, PurchaseOrderItems> purchaseItemByProduct = new HashMap<>();
        for (PurchaseOrderItems item : purchaseOrderItems) {
            purchaseItemByProduct.put(item.getProduct().getId(), item);
        }

        // Chỉ nhận hàng đã có trong PO. Không vượt quá số lượng chưa nhận.
        for (CreateGoodsReceiptItemRequest requested : request.getItems()) {
            PurchaseOrderItems orderedItem = purchaseItemByProduct.get(requested.getProductId());
            if (orderedItem == null) {
                throw new BadRequest(
                        "Product " + requested.getProductId() + " does not belong to the purchase order"
                );
            }
            int received = orderedItem.getReceivedQuantity() == null
                    ? 0 : orderedItem.getReceivedQuantity();
            int remaining = orderedItem.getOrderedQuantity() - received;
            if (remaining < 1 || requested.getExpectedQuantity() > remaining) {
                throw new BadRequest(
                        "Expected quantity for product " + requested.getProductId()
                                + " must be between 1 and " + Math.max(remaining, 0)
                );
            }
        }

        GoodsReceipts receipt = GoodsReceipts.builder()
                .receiptCode(generateReceiptCode())
                .goodsReceiptsCode(generateGoodsReceiptInternalCode())
                .purchaseOrder(purchaseOrder)
                .warehouse(warehouse)
                .status(GoodsReceiptStatus.CREATED)
                .build();
        receipt = goodsReceiptRepository.save(receipt);

        List<GoodsReceiptItems> receiptItems = new ArrayList<>();
        for (CreateGoodsReceiptItemRequest requested : request.getItems()) {
            PurchaseOrderItems orderedItem = purchaseItemByProduct.get(requested.getProductId());
            receiptItems.add(GoodsReceiptItems.builder()
                    .goodsReceipt(receipt)
                    .product(orderedItem.getProduct())
                    .expectedQuantity(requested.getExpectedQuantity())
                    .actualQuantity(0)
                    .acceptedQuantity(0)
                    .damagedQuantity(0)
                    .surplusQuantity(0)
                    .shortageQuantity(0)
                    .goodsReceiptItemsCode(generateCode("GRI"))
                    .build());
        }
        receiptItems = goodsReceiptItemRepository.saveAll(receiptItems);

        // Không thay đổi tồn kho hoặc purchase_order_items.received_quantity lúc tạo.
        // Warehouse Manager sẽ hoàn tất việc nhận sau khi kiểm hàng.
        return buildDetailResponse(receipt, purchaseOrderItems, receiptItems);
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