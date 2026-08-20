package com.toystorage.backend.services.receipts.receiving;

import com.toystorage.backend.dto.request.receipts.receiving.ReceiptInspectionBatchRequest;
import com.toystorage.backend.dto.request.receipts.receiving.ReceiptInspectionItemRequest;

import com.toystorage.backend.dto.response.receipts.receiving.ReceivingDetailResponse;

import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.enums.receipts.InspectionResult;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptItemRepository;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptRepository;
import com.toystorage.backend.repository.receipts.receiving.ReceiptInspectionRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceivingInspectionService {

    private final GoodsReceiptRepository
            goodsReceiptRepository;

    private final GoodsReceiptItemRepository
            goodsReceiptItemRepository;

    private final ReceiptInspectionRepository
            receiptInspectionRepository;

    private final ReceivingValidationService
            validationService;

    private final ReceivingQueryService
            queryService;


    // =====================================================
    // START RECEIVING
    // CONFIRMED -> RECEIVING
    // =====================================================

    @Transactional
    public ReceivingDetailResponse startReceiving(
            Long receiptId
    ) {

        GoodsReceipts receipt =
                validationService
                        .getReceipt(receiptId);

        Users staff =
                validationService
                        .getCurrentUser();

        validationService
                .validateSameWarehouse(
                        staff,
                        receipt
                );

        validationService
                .validateCanStart(
                        receipt
                );

        receipt.setReceivedBy(
                staff
        );

        receipt.setReceivedAt(
                LocalDateTime.now()
        );

        receipt.setStatus(
                GoodsReceiptStatus.RECEIVING
        );

        receipt.setUpdatedAt(
                LocalDateTime.now()
        );

        goodsReceiptRepository.save(
                receipt
        );

        return queryService
                .buildResponse(
                        receipt
                );
    }


    // =====================================================
    // SAVE ENTIRE INSPECTION LIST
    // =====================================================

    @Transactional
    public ReceivingDetailResponse saveBatchInspection(
            Long receiptId,
            ReceiptInspectionBatchRequest request
    ) {

        GoodsReceipts receipt =
                validationService
                        .getReceipt(receiptId);

        Users staff =
                validationService
                        .getCurrentUser();

        validationService
                .validateSameWarehouse(
                        staff,
                        receipt
                );

        validationService
                .validateCanInspect(
                        receipt
                );

        validationService
                .validateNoDuplicateProducts(
                        request
                );

        List<GoodsReceiptItems> receiptItems =
                goodsReceiptItemRepository
                        .findByGoodsReceiptId(
                                receiptId
                        );

        if (receiptItems.isEmpty()) {

            throw new BadRequest(
                    "Goods receipt contains no products"
            );
        }

        for (ReceiptInspectionItemRequest itemRequest
                : request.getItems()) {

            validationService
                    .validateQuantities(
                            itemRequest
                    );

            GoodsReceiptItems receiptItem =
                    validationService
                            .getReceiptItem(
                                    receiptItems,
                                    itemRequest
                                            .getProductId()
                            );

            saveInspectionItem(
                    receipt,
                    receiptItem,
                    itemRequest,
                    staff
            );
        }

        return queryService
                .buildResponse(
                        receipt
                );
    }


    // =====================================================
    // SAVE ONE PRODUCT
    // =====================================================

    private void saveInspectionItem(
            GoodsReceipts receipt,
            GoodsReceiptItems receiptItem,
            ReceiptInspectionItemRequest request,
            Users staff
    ) {

        int expected =
                receiptItem
                        .getExpectedQuantity();

        int actual =
                request
                        .getActualQuantity();

        int damaged =
                request.getDamagedQuantity()
                        != null
                        ? request
                        .getDamagedQuantity()
                        : 0;


        int shortage =
                Math.max(
                        expected - actual,
                        0
                );

        int surplus =
                Math.max(
                        actual - expected,
                        0
                );

        /*
         * Hàng hợp lệ =
         * actual - damaged.
         *
         * Chưa cộng tồn ở bước này.
         */
        int accepted =
                Math.max(
                        actual - damaged,
                        0
                );


        InspectionResult result =
                resolveInspectionResult(
                        expected,
                        actual,
                        damaged
                );


        validateDiscrepancyNote(
                result,
                request
        );


        updateReceiptItem(
                receiptItem,
                actual,
                accepted,
                damaged,
                shortage,
                surplus
        );


        upsertInspection(
                receipt,
                receiptItem,
                request,
                result,
                staff
        );
    }


    // =====================================================
    // UPDATE GOODS RECEIPT ITEM
    // =====================================================

    private void updateReceiptItem(
            GoodsReceiptItems item,
            int actual,
            int accepted,
            int damaged,
            int shortage,
            int surplus
    ) {

        item.setActualQuantity(
                actual
        );

        item.setAcceptedQuantity(
                accepted
        );

        item.setDamagedQuantity(
                damaged
        );

        item.setShortageQuantity(
                shortage
        );

        item.setSurplusQuantity(
                surplus
        );

        goodsReceiptItemRepository
                .save(item);
    }


    // =====================================================
    // UPSERT RECEIPT INSPECTION
    // =====================================================

    private void upsertInspection(
            GoodsReceipts receipt,
            GoodsReceiptItems receiptItem,
            ReceiptInspectionItemRequest request,
            InspectionResult result,
            Users staff
    ) {

        ReceiptInspections inspection =
                receiptInspectionRepository
                        .findByGoodsReceiptIdAndProductId(
                                receipt.getId(),
                                request.getProductId()
                        )

                        .orElseGet(() ->
                                ReceiptInspections
                                        .builder()

                                        .receiptInspectionsCode(
                                                generateInspectionCode()
                                        )

                                        .goodsReceipt(
                                                receipt
                                        )

                                        .product(
                                                receiptItem
                                                        .getProduct()
                                        )

                                        .build()
                        );


        inspection.setExpectedQuantity(
                receiptItem
                        .getExpectedQuantity()
        );

        inspection.setActualQuantity(
                request.getActualQuantity()
        );

        inspection.setInspectedResult(
                result
        );

        inspection.setPackageCode(
                request.getPackageCode()
        );

        inspection.setNotes(
                request.getNotes()
        );

        inspection.setInspectedBy(
                staff
        );

        inspection.setInspectedAt(
                LocalDateTime.now()
        );

        receiptInspectionRepository
                .save(inspection);
    }


    // =====================================================
    // FINISH INSPECTION
    // RECEIVING -> INSPECTED
    // =====================================================

    @Transactional
    public ReceivingDetailResponse finishInspection(
            Long receiptId
    ) {

        GoodsReceipts receipt =
                validationService
                        .getReceipt(receiptId);

        Users staff =
                validationService
                        .getCurrentUser();

        validationService
                .validateSameWarehouse(
                        staff,
                        receipt
                );

        validationService
                .validateCanInspect(
                        receipt
                );


        long itemCount =
                goodsReceiptItemRepository
                        .countByGoodsReceiptId(
                                receiptId
                        );

        long inspectionCount =
                receiptInspectionRepository
                        .countByGoodsReceiptId(
                                receiptId
                        );


        if (itemCount == 0) {

            throw new BadRequest(
                    "Goods receipt contains no products"
            );
        }


        if (inspectionCount
                != itemCount) {

            throw new BadRequest(
                    "Not all products have been inspected. "
                            + inspectionCount
                            + "/"
                            + itemCount
            );
        }


        receipt.setStatus(
                GoodsReceiptStatus.INSPECTED
        );

        receipt.setUpdatedAt(
                LocalDateTime.now()
        );

        goodsReceiptRepository
                .save(receipt);


        /*
         * Không update inventory.
         *
         * Warehouse Manager confirm
         * thì mới cộng tồn.
         */
        return queryService
                .buildResponse(
                        receipt
                );
    }


    // =====================================================
    // INSPECTION RESULT
    // =====================================================

    private InspectionResult resolveInspectionResult(
            int expected,
            int actual,
            int damaged
    ) {

        if (damaged > 0) {

            return InspectionResult.DAMAGED;
        }

        if (actual < expected) {

            return InspectionResult.SHORTAGE;
        }

        if (actual > expected) {

            return InspectionResult.SURPLUS;
        }

        return InspectionResult.MATCHED;
    }


    // =====================================================
    // DISCREPANCY NOTE
    // =====================================================

    private void validateDiscrepancyNote(
            InspectionResult result,
            ReceiptInspectionItemRequest request
    ) {

        if (result
                == InspectionResult.MATCHED) {

            return;
        }

        if (request.getNotes() == null
                || request.getNotes()
                .isBlank()) {

            throw new BadRequest(
                    "Discrepancy requires notes "
                            + "for product "
                            + request.getProductId()
            );
        }
    }


    // =====================================================
    // CODE
    // =====================================================

    private String generateInspectionCode() {

        return "RI-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }
}