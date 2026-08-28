package com.toystorage.backend.services.inventories.discrepancy;

import com.toystorage.backend.dto.response.inventories.discrepancy.BusinessManagerDiscrepancyItemResponse;
import com.toystorage.backend.dto.response.inventories.discrepancy.BusinessManagerDiscrepancyResponse;
import com.toystorage.backend.entity.inventories.DiscrepancyItems;
import com.toystorage.backend.entity.inventories.DiscrepancyReports;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.PurchaseOrderItems;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import com.toystorage.backend.entity.receipts.StoreReceipts;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.transfers.StockTransferItems;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.inventories.DiscrepancyReferenceType;
import com.toystorage.backend.enums.inventories.DiscrepancyStatus;
import com.toystorage.backend.enums.inventories.DiscrepancyType;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.repository.inventories.discrepancy.DiscrepancyItemRepository;
import com.toystorage.backend.repository.inventories.discrepancy.DiscrepancyReportRepository;
import com.toystorage.backend.repository.receipts.PurchaseOrderItemRepository;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptItemRepository;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptRepository;
import com.toystorage.backend.repository.receipts.receiving.ReceiptInspectionRepository;
import com.toystorage.backend.repository.receipts.store.StoreReceiptRepository;
import com.toystorage.backend.repository.transfers.picking.StockTransferItemRepository;
import com.toystorage.backend.repository.transfers.picking.StockTransferRepository;
import com.toystorage.backend.dto.request.inventories.discrepancy.BusinessManagerResolveDiscrepancyItemRequest;
import com.toystorage.backend.dto.request.inventories.discrepancy.BusinessManagerResolveDiscrepancyRequest;

import com.toystorage.backend.entity.receipts.StoreReceiptItems;
import com.toystorage.backend.entity.users.ActivityLogs;

import com.toystorage.backend.enums.inventories.ResolutionAction;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.enums.receipts.StoreReceiptStatus;
import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;

import com.toystorage.backend.repository.receipts.store.StoreReceiptItemRepository;
import com.toystorage.backend.repository.users.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BusinessManagerDiscrepancyService {

    private static final String BUSINESS_MANAGER =
            "BUSINESS_MANAGER";

    private final DiscrepancyReportRepository
            discrepancyReportRepository;

    private final DiscrepancyItemRepository
            discrepancyItemRepository;

    private final GoodsReceiptRepository
            goodsReceiptRepository;

    private final GoodsReceiptItemRepository
            goodsReceiptItemRepository;

    private final ReceiptInspectionRepository
            receiptInspectionRepository;

    private final PurchaseOrderItemRepository
            purchaseOrderItemRepository;

    private final StockTransferRepository
            stockTransferRepository;

    private final StockTransferItemRepository
            stockTransferItemRepository;

    private final StoreReceiptRepository
            storeReceiptRepository;
    private final StoreReceiptItemRepository
        storeReceiptItemRepository;

    private final DiscrepancyValidationService
            validationService;

    private final ActivityLogRepository
            activityLogRepository;


    // =====================================================
    // LIST REPORTS ESCALATED TO BUSINESS MANAGER
    // =====================================================

    @Transactional(readOnly = true)
    public List<BusinessManagerDiscrepancyResponse> getReports(
            String referenceType,
            String discrepancyType,
            String status
    ) {

        DiscrepancyReferenceType referenceTypeFilter =
                parseEnum(
                        referenceType,
                        DiscrepancyReferenceType.class,
                        "referenceType"
                );

        DiscrepancyType discrepancyTypeFilter =
                parseEnum(
                        discrepancyType,
                        DiscrepancyType.class,
                        "discrepancyType"
                );

        DiscrepancyStatus statusFilter =
                parseEnum(
                        status,
                        DiscrepancyStatus.class,
                        "status"
                );

        return discrepancyReportRepository
                .findByResponsiblePartyOrderByCreatedAtDesc(
                        BUSINESS_MANAGER
                )
                .stream()

                /*
                 * Scope của Issue #20:
                 * - nhập hàng
                 * - điều chuyển
                 */
                .filter(this::isSupportedReferenceType)

                .filter(report ->
                        referenceTypeFilter == null
                                || report.getReferenceType()
                                == referenceTypeFilter
                )

                .filter(report ->
                        discrepancyTypeFilter == null
                                || report.getDiscrepancyType()
                                == discrepancyTypeFilter
                )

                .filter(report ->
                        statusFilter == null
                                || report.getStatus()
                                == statusFilter
                )

                .map(this::buildResponse)
                .toList();
    }


    // =====================================================
    // DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public BusinessManagerDiscrepancyResponse getDetail(
            Long discrepancyId
    ) {

        DiscrepancyReports report =
                getBusinessManagerReport(
                        discrepancyId
                );

        return buildResponse(
                report
        );
    }

    // =====================================================
    // RESOLVE - BUSINESS MANAGER
    // =====================================================

    @Transactional
    public BusinessManagerDiscrepancyResponse resolve(
            Long discrepancyId,
            BusinessManagerResolveDiscrepancyRequest request
    ) {

        DiscrepancyReports report =
                getBusinessManagerReport(
                        discrepancyId
                );


        /*
         * Chỉ report đã được Warehouse/Store
         * chuyển lên Business Manager mới được xử lý.
         */
        if (report.getStatus()
                != DiscrepancyStatus.INVESTIGATING) {

            throw new BadRequest(
                    "Only INVESTIGATING discrepancy "
                            + "can be resolved by Business Manager"
            );
        }


        /*
         * RECOUNT không phải kết luận cuối cùng.
         *
         * Nếu cần kiểm lại thì phải trả về
         * Warehouse/Store để kiểm trước.
         */
        if (request.getResolutionAction()
                == ResolutionAction.RECOUNT) {

            throw new BadRequest(
                    "RECOUNT is not a final resolution action"
            );
        }


        Users manager =
                validationService
                        .getCurrentUser();


        List<DiscrepancyItems> discrepancyItems =
                discrepancyItemRepository
                        .findByDiscrepancyReportId(
                                report.getId()
                        );


        if (discrepancyItems.isEmpty()) {

            throw new BadRequest(
                    "Discrepancy report contains no items"
            );
        }


        /*
         * productId -> finalQuantity
         */
        Map<Long, Integer> finalQuantities =
                validateAndMapFinalQuantities(
                        discrepancyItems,
                        request.getItems()
                );


        /*
         * Snapshot trước khi thay đổi.
         */
        String oldSnapshot =
                buildResolutionSnapshot(
                        report
                );


        /*
         * =============================================
         * UPDATE SOURCE DOCUMENT
         * =============================================
         */

        if (report.getReferenceType()
                == DiscrepancyReferenceType.GOODS_RECEIPT) {

            applyGoodsReceiptFinalQuantities(
                    report,
                    discrepancyItems,
                    finalQuantities
            );

        } else {

            applyStockTransferFinalQuantities(
                    report,
                    discrepancyItems,
                    finalQuantities
            );
        }


        /*
         * =============================================
         * RESOLVE REPORT
         * =============================================
         */

        String previousNote =
                report.getResolutionNote();


        report.setResolutionAction(
                request.getResolutionAction()
        );


        /*
         * Giữ lại lý do Warehouse Manager
         * đã ghi lúc escalate.
         *
         * Không overwrite làm mất lịch sử.
         */
        report.setResolutionNote(
                mergeResolutionNotes(
                        previousNote,
                        request.getResolutionNote()
                )
        );


        report.setResolvedBy(
                manager
        );


        report.setResolvedAt(
                LocalDateTime.now()
        );


        report.setStatus(
                DiscrepancyStatus.RESOLVED
        );


        report.setUpdatedAt(
                LocalDateTime.now()
        );


        DiscrepancyReports saved =
                discrepancyReportRepository
                        .save(report);


        /*
         * Snapshot sau xử lý.
         */
        String newSnapshot =
                buildResolutionSnapshot(
                        saved
                );


        /*
         * Audit trail.
         */
        saveResolutionHistory(
                manager,
                saved,
                oldSnapshot,
                newSnapshot
        );


        return buildResponse(
                saved
        );
    }
    // =====================================================
    // BUILD COMMON RESPONSE
    // =====================================================

    private BusinessManagerDiscrepancyResponse buildResponse(
            DiscrepancyReports report
    ) {

        Users reportedBy =
                report.getReportedBy();

        Users reviewedBy =
                report.getReviewedBy();

        Users resolvedBy =
                report.getResolvedBy();

        BusinessManagerDiscrepancyResponse
                .BusinessManagerDiscrepancyResponseBuilder builder =
                BusinessManagerDiscrepancyResponse
                        .builder()

                        .id(report.getId())

                        .reportCode(
                                report.getReportCode()
                        )

                        .discrepancyType(
                                report.getDiscrepancyType()
                                        .name()
                        )

                        .status(
                                report.getStatus()
                                        .name()
                        )

                        .referenceType(
                                report.getReferenceType()
                                        .name()
                        )

                        .referenceId(
                                report.getReferenceId()
                        )

                        .warehouseId(
                                report.getWarehouse()
                                        .getId()
                        )

                        .warehouseCode(
                                report.getWarehouse()
                                        .getWarehousesCode()
                        )

                        .warehouseName(
                                report.getWarehouse()
                                        .getName()
                        )

                        .description(
                                report.getDescription()
                        )

                        .evidenceImageUrl(
                                report.getEvidenceImageUrl()
                        )

                        .responsibleParty(
                                report.getResponsibleParty()
                        )

                        .reportedBy(
                                reportedBy != null
                                        ? reportedBy.getId()
                                        : null
                        )

                        .reportedByName(
                                reportedBy != null
                                        ? reportedBy.getName()
                                        : null
                        )

                        .reviewedBy(
                                reviewedBy != null
                                        ? reviewedBy.getId()
                                        : null
                        )

                        .reviewedByName(
                                reviewedBy != null
                                        ? reviewedBy.getName()
                                        : null
                        )

                        .reviewedAt(
                                report.getReviewedAt()
                        )

                        .resolutionAction(
                                report.getResolutionAction() != null
                                        ? report.getResolutionAction()
                                        .name()
                                        : null
                        )

                        .resolutionNote(
                                report.getResolutionNote()
                        )

                        .resolvedBy(
                                resolvedBy != null
                                        ? resolvedBy.getId()
                                        : null
                        )

                        .resolvedByName(
                                resolvedBy != null
                                        ? resolvedBy.getName()
                                        : null
                        )

                        .resolvedAt(
                                report.getResolvedAt()
                        )

                        .createdAt(
                                report.getCreatedAt()
                        )

                        .updatedAt(
                                report.getUpdatedAt()
                        );


        if (report.getReferenceType()
                == DiscrepancyReferenceType.GOODS_RECEIPT) {

            populateGoodsReceipt(
                    report,
                    builder
            );

        } else if (report.getReferenceType()
                == DiscrepancyReferenceType.STOCK_TRANSFER) {

            populateStockTransfer(
                    report,
                    builder
            );

        } else {

            throw new BadRequest(
                    "Task #20 only supports "
                            + "GOODS_RECEIPT and STOCK_TRANSFER discrepancies"
            );
        }

        return builder.build();
    }


    // =====================================================
    // GOODS RECEIPT DETAIL
    // =====================================================

    private void populateGoodsReceipt(
            DiscrepancyReports report,
            BusinessManagerDiscrepancyResponse
                    .BusinessManagerDiscrepancyResponseBuilder builder
    ) {

        GoodsReceipts receipt =
                goodsReceiptRepository
                        .findById(
                                report.getReferenceId()
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Goods receipt not found: "
                                                + report.getReferenceId()
                                )
                        );


        Map<Long, GoodsReceiptItems> receiptItems =
                new HashMap<>();

        goodsReceiptItemRepository
                .findByGoodsReceiptId(
                        receipt.getId()
                )
                .forEach(item ->
                        receiptItems.put(
                                item.getProduct()
                                        .getId(),
                                item
                        )
                );


        Map<Long, PurchaseOrderItems> orderItems =
                new HashMap<>();

        purchaseOrderItemRepository
                .findByPurchaseOrder_IdOrderByIdAsc(
                        receipt.getPurchaseOrder()
                                .getId()
                )
                .forEach(item ->
                        orderItems.put(
                                item.getProduct()
                                        .getId(),
                                item
                        )
                );


        Map<Long, ReceiptInspections> inspections =
                new HashMap<>();

        receiptInspectionRepository
                .findByGoodsReceiptId(
                        receipt.getId()
                )
                .forEach(inspection ->
                        inspections.put(
                                inspection.getProduct()
                                        .getId(),
                                inspection
                        )
                );


        List<BusinessManagerDiscrepancyItemResponse> items =
                discrepancyItemRepository
                        .findByDiscrepancyReportId(
                                report.getId()
                        )
                        .stream()
                        .map(item ->
                                buildGoodsReceiptItem(
                                        item,
                                        receiptItems,
                                        orderItems,
                                        inspections
                                )
                        )
                        .toList();


        /*
         * Phiếu nhập hiện có:
         * - confirmedBy
         * - inspectionConfirmedBy
         *
         * Nếu Warehouse Manager đã xác nhận inspection
         * thì ưu tiên người đó.
         */
        Users warehouseConfirmer =
                receipt.getInspectionConfirmedBy()
                        != null
                        ? receipt.getInspectionConfirmedBy()
                        : receipt.getConfirmedBy();


        builder
                .referenceCode(
                        receipt.getReceiptCode()
                )

                .sourceStatus(
                        receipt.getStatus()
                                .name()
                )

                .purchaseOrderId(
                        receipt.getPurchaseOrder()
                                .getId()
                )

                .purchaseOrderCode(
                        receipt.getPurchaseOrder()
                                .getOrderCode()
                )

                .warehouseConfirmedBy(
                        warehouseConfirmer != null
                                ? warehouseConfirmer.getId()
                                : null
                )

                .warehouseConfirmedByName(
                        warehouseConfirmer != null
                                ? warehouseConfirmer.getName()
                                : null
                )

                .warehouseConfirmedAt(
                        receipt.getInspectionConfirmedAt()
                )

                .items(items);
    }


    private BusinessManagerDiscrepancyItemResponse
    buildGoodsReceiptItem(
            DiscrepancyItems discrepancyItem,
            Map<Long, GoodsReceiptItems> receiptItems,
            Map<Long, PurchaseOrderItems> orderItems,
            Map<Long, ReceiptInspections> inspections
    ) {

        Products product =
                discrepancyItem.getProduct();

        Long productId =
                product.getId();

        GoodsReceiptItems receiptItem =
                receiptItems.get(
                        productId
                );

        PurchaseOrderItems orderItem =
                orderItems.get(
                        productId
                );

        ReceiptInspections inspection =
                inspections.get(
                        productId
                );


        return BusinessManagerDiscrepancyItemResponse
                .builder()

                .productId(
                        productId
                )

                .productCode(
                        product.getProductsCode()
                )

                .productName(
                        product.getName()
                )

                .barcode(
                        product.getBarcode()
                )

                .expectedQuantity(
                        discrepancyItem
                                .getExpectedQuantity()
                )

                .actualQuantity(
                        discrepancyItem
                                .getActualQuantity()
                )

                .differenceQuantity(
                        discrepancyItem
                                .getDifferenceQuantity()
                )

                .orderedQuantity(
                        orderItem != null
                                ? orderItem.getOrderedQuantity()
                                : null
                )

                .acceptedQuantity(
                        receiptItem != null
                                ? receiptItem.getAcceptedQuantity()
                                : null
                )

                .finalQuantity(
                        receiptItem != null
                                ? receiptItem.getAcceptedQuantity()
                                : null
                )

                .inspectionResult(
                        inspection != null
                                ? inspection.getInspectedResult()
                                .name()
                                : null
                )

                .notes(
                        inspection != null
                                ? inspection.getNotes()
                                : null
                )

                .evidenceImageUrl(
                        inspection != null
                                ? inspection.getEvidenceImage()
                                : null
                )

                .build();
    }


    // =====================================================
    // STOCK TRANSFER DETAIL
    // =====================================================

    private void populateStockTransfer(
            DiscrepancyReports report,
            BusinessManagerDiscrepancyResponse
                    .BusinessManagerDiscrepancyResponseBuilder builder
    ) {

        StockTransfer transfer =
                stockTransferRepository
                        .findDetailedById(
                                report.getReferenceId()
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Stock transfer not found: "
                                                + report.getReferenceId()
                                )
                        );


        Map<Long, StockTransferItems> transferItems =
                new HashMap<>();

        stockTransferItemRepository
                .findByStockTransferId(
                        transfer.getId()
                )
                .forEach(item ->
                        transferItems.put(
                                item.getProduct()
                                        .getId(),
                                item
                        )
                );


        List<BusinessManagerDiscrepancyItemResponse> items =
                discrepancyItemRepository
                        .findByDiscrepancyReportId(
                                report.getId()
                        )
                        .stream()
                        .map(item ->
                                buildStockTransferItem(
                                        item,
                                        transferItems
                                )
                        )
                        .toList();


        StoreReceipts storeReceipt =
                storeReceiptRepository
                        .findFirstByStockTransferIdOrderByCreatedAtDesc(
                                transfer.getId()
                        )
                        .orElse(null);


        Users warehouseConfirmer =
                transfer.getConfirmedBy();

        Users storeConfirmer =
                storeReceipt != null
                        ? storeReceipt.getConfirmedBy()
                        : null;


        builder
                .referenceCode(
                        transfer.getTransferCode()
                )

                .sourceStatus(
                        transfer.getStatus()
                                .name()
                )

                .fromWarehouseId(
                        transfer.getFromWarehouse()
                                .getId()
                )

                .fromWarehouseCode(
                        transfer.getFromWarehouse()
                                .getWarehousesCode()
                )

                .fromWarehouseName(
                        transfer.getFromWarehouse()
                                .getName()
                )

                .toWarehouseId(
                        transfer.getToWarehouse()
                                .getId()
                )

                .toWarehouseCode(
                        transfer.getToWarehouse()
                                .getWarehousesCode()
                )

                .toWarehouseName(
                        transfer.getToWarehouse()
                                .getName()
                )

                .warehouseConfirmedBy(
                        warehouseConfirmer != null
                                ? warehouseConfirmer.getId()
                                : null
                )

                .warehouseConfirmedByName(
                        warehouseConfirmer != null
                                ? warehouseConfirmer.getName()
                                : null
                )

                .warehouseConfirmedAt(
                        transfer.getConfirmedAt()
                )

                .storeReceiptId(
                        storeReceipt != null
                                ? storeReceipt.getId()
                                : null
                )

                .storeReceiptCode(
                        storeReceipt != null
                                ? storeReceipt.getStoreReceiptsCode()
                                : null
                )

                .storeReceiptStatus(
                        storeReceipt != null
                                ? storeReceipt.getStatus()
                                .name()
                                : null
                )

                .storeConfirmedBy(
                        storeConfirmer != null
                                ? storeConfirmer.getId()
                                : null
                )

                .storeConfirmedByName(
                        storeConfirmer != null
                                ? storeConfirmer.getName()
                                : null
                )

                .storeConfirmedAt(
                        storeReceipt != null
                                ? storeReceipt.getReceivedAt()
                                : null
                )

                .items(items);
    }


    private BusinessManagerDiscrepancyItemResponse
    buildStockTransferItem(
            DiscrepancyItems discrepancyItem,
            Map<Long, StockTransferItems> transferItems
    ) {

        Products product =
                discrepancyItem.getProduct();

        Long productId =
                product.getId();

        StockTransferItems transferItem =
                transferItems.get(
                        productId
                );


        return BusinessManagerDiscrepancyItemResponse
                .builder()

                .productId(
                        productId
                )

                .productCode(
                        product.getProductsCode()
                )

                .productName(
                        product.getName()
                )

                .barcode(
                        product.getBarcode()
                )

                .expectedQuantity(
                        discrepancyItem
                                .getExpectedQuantity()
                )

                .actualQuantity(
                        discrepancyItem
                                .getActualQuantity()
                )

                .differenceQuantity(
                        discrepancyItem
                                .getDifferenceQuantity()
                )

                .shippedQuantity(
                        transferItem != null
                                ? transferItem.getShippedQuantity()
                                : null
                )

                .receivedQuantity(
                        transferItem != null
                                ? transferItem.getReceivedQuantity()
                                : null
                )

                .finalQuantity(
                        transferItem != null
                                ? transferItem.getReceivedQuantity()
                                : null
                )

                .build();
    }

    // =====================================================
    // RESOLUTION HELPERS
    // =====================================================

    private Map<Long, Integer> validateAndMapFinalQuantities(
            List<DiscrepancyItems> discrepancyItems,
            List<BusinessManagerResolveDiscrepancyItemRequest> requestItems
    ) {

        Map<Long, Integer> finalQuantities =
                new HashMap<>();


        /*
         * Không cho gửi duplicate product.
         */
        for (BusinessManagerResolveDiscrepancyItemRequest item
                : requestItems) {

            if (finalQuantities.putIfAbsent(
                    item.getProductId(),
                    item.getFinalQuantity()
            ) != null) {

                throw new BadRequest(
                        "Duplicate productId in resolve request: "
                                + item.getProductId()
                );
            }
        }


        /*
         * Tất cả sản phẩm trong discrepancy
         * phải có finalQuantity.
         */
        for (DiscrepancyItems discrepancyItem
                : discrepancyItems) {

            Long productId =
                    discrepancyItem
                            .getProduct()
                            .getId();


            if (!finalQuantities.containsKey(
                    productId
            )) {

                throw new BadRequest(
                        "Missing final quantity for product: "
                                + productId
                );
            }
        }


        /*
         * Không được gửi thêm product
         * không thuộc discrepancy.
         */
        if (finalQuantities.size()
                != discrepancyItems.size()) {

            throw new BadRequest(
                    "Resolve request contains product "
                            + "not belonging to discrepancy report"
            );
        }


        return finalQuantities;
    }
        // =====================================================
    // GOODS RECEIPT - APPLY FINAL QUANTITY
    // =====================================================

    private void applyGoodsReceiptFinalQuantities(
            DiscrepancyReports report,
            List<DiscrepancyItems> discrepancyItems,
            Map<Long, Integer> finalQuantities
    ) {

        GoodsReceipts receipt =
                goodsReceiptRepository
                        .findById(
                                report.getReferenceId()
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Goods receipt not found: "
                                                + report.getReferenceId()
                                )
                        );


        /*
         * Business Manager phải xử lý discrepancy
         * trước khi Warehouse Manager complete receipt.
         *
         * INSPECTED:
         * Warehouse Staff đã kiểm hàng
         * nhưng inventory chưa được cộng.
         */
        if (receipt.getStatus()
                != GoodsReceiptStatus.INSPECTED) {

            throw new BadRequest(
                    "Goods receipt discrepancy must be resolved "
                            + "before receipt completion"
            );
        }


        for (DiscrepancyItems discrepancyItem
                : discrepancyItems) {

            Long productId =
                    discrepancyItem
                            .getProduct()
                            .getId();


            GoodsReceiptItems receiptItem =
                    goodsReceiptItemRepository
                            .findByGoodsReceiptIdAndProductId(
                                    receipt.getId(),
                                    productId
                            )
                            .orElseThrow(() ->
                                    new NotFound(
                                            "Goods receipt item not found "
                                                    + "for product: "
                                                    + productId
                                    )
                            );


            int finalQuantity =
                    finalQuantities.get(
                            productId
                    );


            int actualQuantity =
                    receiptItem.getActualQuantity() == null
                            ? 0
                            : receiptItem.getActualQuantity();


            /*
             * Số lượng cuối cùng được chấp nhận
             * không thể vượt số lượng thực tế
             * Warehouse đã kiểm nhận.
             */
            if (finalQuantity
                    > actualQuantity) {

                throw new BadRequest(
                        "Final accepted quantity cannot exceed "
                                + "actual quantity for product: "
                                + productId
                );
            }


            /*
             * acceptedQuantity là số lượng
             * sẽ được cộng vào inventory
             * khi Goods Receipt được COMPLETED.
             */
            receiptItem.setAcceptedQuantity(
                    finalQuantity
            );


            goodsReceiptItemRepository
                    .save(
                            receiptItem
                    );
        }
    }


    // =====================================================
    // STOCK TRANSFER - APPLY FINAL QUANTITY
    // =====================================================

    private void applyStockTransferFinalQuantities(
            DiscrepancyReports report,
            List<DiscrepancyItems> discrepancyItems,
            Map<Long, Integer> finalQuantities
    ) {

        StockTransfer transfer =
                stockTransferRepository
                        .findDetailedById(
                                report.getReferenceId()
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Stock transfer not found: "
                                                + report.getReferenceId()
                                )
                        );


        /*
         * Transfer đã COMPLETED hoặc CANCELLED
         * thì không được thay đổi lại
         * số lượng nhận.
         */
        if (transfer.getStatus()
                == TransferStatus.COMPLETED

                ||

                transfer.getStatus()
                        == TransferStatus.CANCELLED) {

            throw new BadRequest(
                    "Completed or cancelled stock transfer "
                            + "cannot be resolved by changing receiving quantity"
            );
        }


        /*
         * Store phải có phiếu nhận trước
         * khi Business Manager xử lý.
         */
        StoreReceipts storeReceipt =
                storeReceiptRepository
                        .findFirstByStockTransferIdOrderByCreatedAtDesc(
                                transfer.getId()
                        )
                        .orElseThrow(() ->
                                new BadRequest(
                                        "Store receipt must be created "
                                                + "and inspected before "
                                                + "Business Manager resolution"
                                )
                        );


        /*
         * Store phải kiểm hàng xong trước.
         *
         * INSPECTED:
         * đã kiểm nhưng chưa manager confirm.
         *
         * CONFIRMED:
         * manager đã xác nhận nhưng inventory
         * chưa hoàn tất.
         */
        if (storeReceipt.getStatus()
                != StoreReceiptStatus.INSPECTED

                &&

                storeReceipt.getStatus()
                        != StoreReceiptStatus.CONFIRMED) {

            throw new BadRequest(
                    "Store receipt must be INSPECTED or CONFIRMED "
                            + "before Business Manager resolution"
            );
        }


        Map<Long, StockTransferItems> transferItems =
                new HashMap<>();


        stockTransferItemRepository
                .findByStockTransferId(
                        transfer.getId()
                )
                .forEach(item ->
                        transferItems.put(
                                item.getProduct()
                                        .getId(),
                                item
                        )
                );


        for (DiscrepancyItems discrepancyItem
                : discrepancyItems) {

            Long productId =
                    discrepancyItem
                            .getProduct()
                            .getId();


            int finalQuantity =
                    finalQuantities.get(
                            productId
                    );


            StockTransferItems transferItem =
                    transferItems.get(
                            productId
                    );


            if (transferItem == null) {

                throw new NotFound(
                        "Stock transfer item not found "
                                + "for product: "
                                + productId
                );
            }


            StoreReceiptItems storeReceiptItem =
                    storeReceiptItemRepository
                            .findByStoreReceiptIdAndProductId(
                                    storeReceipt.getId(),
                                    productId
                            )
                            .orElseThrow(() ->
                                    new NotFound(
                                            "Store receipt item not found "
                                                    + "for product: "
                                                    + productId
                                    )
                            );


            int damagedQuantity =
                    storeReceiptItem
                            .getDamagedQuantity() == null
                            ? 0
                            : storeReceiptItem
                            .getDamagedQuantity();


            /*
             * damagedQuantity là một phần
             * của số lượng thực tế nhận.
             */
            if (finalQuantity
                    < damagedQuantity) {

                throw new BadRequest(
                        "Final received quantity cannot be less "
                                + "than damaged quantity for product: "
                                + productId
                );
            }


            /*
             * Đồng bộ Stock Transfer.
             */
            transferItem.setReceivedQuantity(
                    finalQuantity
            );


            transferItem
                    .recalculateDiscrepancy();


            stockTransferItemRepository
                    .save(
                            transferItem
                    );


            /*
             * Đồng bộ Store Receipt.
             */
            storeReceiptItem.setActualQuantity(
                    finalQuantity
            );


            storeReceiptItem
                    .calculateDifference();


            storeReceiptItemRepository
                    .save(
                            storeReceiptItem
                    );
        }
    }


    // =====================================================
    // RESOLUTION NOTE
    // =====================================================

    private String mergeResolutionNotes(
            String previousNote,
            String managerConclusion
    ) {

        if (previousNote == null
                || previousNote.isBlank()) {

            return managerConclusion;
        }


        return "Escalation reason: "
                + previousNote
                + "\nBusiness Manager conclusion: "
                + managerConclusion;
    }


    // =====================================================
    // AUDIT SNAPSHOT
    // =====================================================

    private String buildResolutionSnapshot(
            DiscrepancyReports report
    ) {

        StringBuilder builder =
                new StringBuilder();


        builder.append(
                "reportId="
        ).append(
                report.getId()
        );


        builder.append(
                ", status="
        ).append(
                report.getStatus()
        );


        builder.append(
                ", resolutionAction="
        ).append(
                report.getResolutionAction()
        );


        builder.append(
                ", resolutionNote="
        ).append(
                report.getResolutionNote()
        );


        builder.append(
                ", finalQuantities=["
        );


        List<DiscrepancyItems> items =
                discrepancyItemRepository
                        .findByDiscrepancyReportId(
                                report.getId()
                        );


        for (int i = 0;
             i < items.size();
             i++) {

            DiscrepancyItems discrepancyItem =
                    items.get(i);


            Long productId =
                    discrepancyItem
                            .getProduct()
                            .getId();


            Integer finalQuantity =
                    null;


            /*
             * GOODS RECEIPT
             */
            if (report.getReferenceType()
                    == DiscrepancyReferenceType.GOODS_RECEIPT) {


                finalQuantity =
                        goodsReceiptItemRepository
                                .findByGoodsReceiptIdAndProductId(
                                        report.getReferenceId(),
                                        productId
                                )
                                .map(
                                        GoodsReceiptItems
                                                ::getAcceptedQuantity
                                )
                                .orElse(
                                        null
                                );


            /*
             * STOCK TRANSFER
             */
            } else if (report.getReferenceType()
                    == DiscrepancyReferenceType.STOCK_TRANSFER) {


                finalQuantity =
                        stockTransferItemRepository
                                .findByStockTransferId(
                                        report.getReferenceId()
                                )
                                .stream()

                                .filter(item ->
                                        productId.equals(
                                                item.getProduct()
                                                        .getId()
                                        )
                                )

                                .findFirst()

                                .map(
                                        StockTransferItems
                                                ::getReceivedQuantity
                                )

                                .orElse(
                                        null
                                );
            }


            builder.append(
                    "{productId="
            ).append(
                    productId
            ).append(
                    ", finalQuantity="
            ).append(
                    finalQuantity
            ).append(
                    "}"
            );


            if (i
                    < items.size() - 1) {

                builder.append(
                        ", "
                );
            }
        }


        builder.append(
                "]"
        );


        return builder.toString();
    }


    // =====================================================
    // ACTIVITY LOG
    // =====================================================

    private void saveResolutionHistory(
        Users manager,
        DiscrepancyReports report,
        String oldValue,
        String newValue
) {

    ActivityEntityType entityType;

    /*
     * activity_logs.entity_type trong DB là ENUM.
     *
     * Không tạo thêm loại mới.
     * Audit resolution được gắn vào chứng từ nguồn
     * mà discrepancy đang xử lý.
     */
    if (report.getReferenceType()
            == DiscrepancyReferenceType.GOODS_RECEIPT) {

        entityType =
                ActivityEntityType.GOODS_RECEIPT;

    } else {

        entityType =
                ActivityEntityType.STOCK_TRANSFER;
    }


    ActivityLogs log =
            ActivityLogs.builder()

                    .user(
                            manager
                    )

                    .action(
                            ActivityAction.UPDATE
                    )

                    .entityType(
                            entityType
                    )

                    /*
                     * entityId phải đi cùng entityType.
                     *
                     * GOODS_RECEIPT -> goods_receipts.id
                     * STOCK_TRANSFER -> stock_transfers.id
                     */
                    .entityId(
                            report.getReferenceId()
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
    // =====================================================
    // VALIDATION
    // =====================================================

    private DiscrepancyReports getBusinessManagerReport(
            Long discrepancyId
    ) {

        DiscrepancyReports report =
                discrepancyReportRepository
                        .findById(
                                discrepancyId
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Discrepancy report not found: "
                                                + discrepancyId
                                )
                        );


        if (!BUSINESS_MANAGER.equals(
                report.getResponsibleParty()
        )) {

            throw new BadRequest(
                    "Discrepancy has not been escalated "
                            + "to Business Manager"
            );
        }


        if (!isSupportedReferenceType(
                report
        )) {

            throw new BadRequest(
                    "Task #20 only supports "
                            + "GOODS_RECEIPT and STOCK_TRANSFER discrepancies"
            );
        }


        return report;
    }


    private boolean isSupportedReferenceType(
            DiscrepancyReports report
    ) {

        return report.getReferenceType()
                == DiscrepancyReferenceType.GOODS_RECEIPT

                ||

                report.getReferenceType()
                        == DiscrepancyReferenceType.STOCK_TRANSFER;
    }


    private <E extends Enum<E>> E parseEnum(
            String rawValue,
            Class<E> enumType,
            String fieldName
    ) {

        if (rawValue == null
                || rawValue.isBlank()) {

            return null;
        }


        try {

            return Enum.valueOf(
                    enumType,
                    rawValue
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (IllegalArgumentException ex) {

            throw new BadRequest(
                    "Invalid "
                            + fieldName
                            + ": "
                            + rawValue
            );
        }
    }
}