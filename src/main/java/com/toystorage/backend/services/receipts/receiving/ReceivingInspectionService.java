package com.toystorage.backend.services.receipts.receiving;

import com.toystorage.backend.dto.request.receipts.receiving.ReceiptInspectionBatchRequest;
import com.toystorage.backend.dto.request.receipts.receiving.ReceiptInspectionItemRequest;

import com.toystorage.backend.dto.response.receipts.receiving.ReceivingDetailResponse;

import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import com.toystorage.backend.entity.receipts.ReceivingReinspectionRequest;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseTaskClaim;
import com.toystorage.backend.services.inventories.discrepancy.ReceivingDiscrepancyService;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.enums.receipts.InspectionResult;
import com.toystorage.backend.enums.warehouses.WarehouseTaskType;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptItemRepository;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptRepository;
import com.toystorage.backend.repository.receipts.receiving.ReceiptInspectionRepository;
import com.toystorage.backend.repository.receipts.receiving.ReceivingReinspectionRequestRepository;

import com.toystorage.backend.services.warehouses.taskclaim.WarehouseTaskClaimService;

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

    private final ReceivingReinspectionRequestRepository
            reinspectionRequestRepository;

    private final ReceivingValidationService
            validationService;

    private final ReceivingQueryService
            queryService;

    private final WarehouseTaskClaimService
            warehouseTaskClaimService;
    private final ReceivingDiscrepancyService
            receivingDiscrepancyService;

    // =====================================================
    // START RECEIVING
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


        /*
         * =================================================
         * RECEIVING STATUS
         * =================================================
         *
         * Có 2 trường hợp:
         *
         * 1. Staff đã Start trước đó.
         * 2. Manager vừa Request Re-inspection.
         */
        if (receipt.getStatus()
                == GoodsReceiptStatus.RECEIVING) {

            WarehouseTaskClaim activeClaim =
                    warehouseTaskClaimService
                            .getActiveClaim(
                                    WarehouseTaskType.GOODS_RECEIVING,
                                    receiptId
                            );


            /*
             * Có active claim:
             *
             * Nếu chính Staff đang giữ task
             * -> idempotent.
             *
             * Nếu Staff khác
             * -> validateOwner sẽ throw.
             */
            if (activeClaim != null) {

                warehouseTaskClaimService
                        .validateOwner(
                                WarehouseTaskType.GOODS_RECEIVING,
                                receiptId,
                                staff
                        );

                return queryService
                        .buildResponse(
                                receipt
                        );
            }


            /*
             * Không có active claim.
             *
             * Đây có thể là re-inspection.
             */
            WarehouseTaskClaim previousClaim =
                    warehouseTaskClaimService
                            .getLatestClaim(
                                    WarehouseTaskType.GOODS_RECEIVING,
                                    receiptId
                            );


            ReceivingReinspectionRequest
                    reinspectionRequest =
                    reinspectionRequestRepository
                            .findFirstByGoodsReceiptIdOrderByRequestedAtDesc(
                                    receiptId
                            )
                            .orElse(null);


            /*
             * Nếu đã từng có claim nhưng không có
             * ReinspectionRequest thì RECEIVING
             * đang ở trạng thái không hợp lệ.
             */
            if (previousClaim != null
                    && reinspectionRequest == null) {

                throw new BadRequest(
                        "Receiving task has no active claim"
                );
            }


            /*
             * Manager có thể không cho Staff cũ
             * thực hiện lại inspection.
             */
            if (
                    previousClaim != null
                            && reinspectionRequest != null
                            && !Boolean.TRUE.equals(
                            reinspectionRequest
                                    .getAllowSameStaff()
                    )
                            && previousClaim
                            .getClaimedBy()
                            .getId()
                            .equals(
                                    staff.getId()
                            )
            ) {

                throw new BadRequest(
                        "The previous Warehouse Staff "
                                + "is not allowed to perform "
                                + "this re-inspection"
                );
            }


            /*
             * Tạo attempt mới.
             *
             * Ví dụ:
             * attempt 1 -> Staff A
             * attempt 2 -> Staff B
             */
            warehouseTaskClaimService
                    .claim(
                            WarehouseTaskType.GOODS_RECEIVING,
                            receiptId,
                            staff
                    );


            receipt.setReceivedBy(
                    staff
            );

            receipt.setReceivedAt(
                    LocalDateTime.now()
            );

            receipt.setUpdatedAt(
                    LocalDateTime.now()
            );


            goodsReceiptRepository
                    .save(
                            receipt
                    );


            return queryService
                    .buildResponse(
                            receipt
                    );
        }


        /*
         * =================================================
         * FIRST RECEIVING
         * =================================================
         */

        validationService
                .validateCanStart(
                        receipt
                );


        /*
         * Staff claim task trước khi bắt đầu.
         */
        warehouseTaskClaimService
                .claim(
                        WarehouseTaskType.GOODS_RECEIVING,
                        receiptId,
                        staff
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


        goodsReceiptRepository
                .save(
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


        /*
         * Chỉ Staff đang claim task mới được
         * save inspection.
         */
        warehouseTaskClaimService
                .validateOwner(
                        WarehouseTaskType.GOODS_RECEIVING,
                        receiptId,
                        staff
                );


        WarehouseTaskClaim activeClaim =
                warehouseTaskClaimService
                        .getActiveClaim(
                                WarehouseTaskType.GOODS_RECEIVING,
                                receiptId
                        );


        if (activeClaim == null) {

            throw new BadRequest(
                    "Active receiving task claim was not found"
            );
        }


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


        for (
                ReceiptInspectionItemRequest itemRequest
                : request.getItems()
        ) {

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
                    staff,
                    activeClaim
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
            Users staff,
            WarehouseTaskClaim activeClaim
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
                        ? request.getDamagedQuantity()
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


        /*
         * GoodsReceiptItems giữ kết quả
         * của inspection hiện tại/latest.
         *
         * Inventory CHƯA được update.
         */
        updateReceiptItem(
                receiptItem,
                actual,
                accepted,
                damaged,
                shortage,
                surplus
        );


        /*
         * ReceiptInspections giữ history
         * theo từng task claim / attempt.
         */
        upsertInspection(
                receipt,
                receiptItem,
                request,
                result,
                staff,
                activeClaim
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
                .save(
                        item
                );
    }


    // =====================================================
    // UPSERT RECEIPT INSPECTION
    // =====================================================

    private void upsertInspection(
            GoodsReceipts receipt,
            GoodsReceiptItems receiptItem,
            ReceiptInspectionItemRequest request,
            InspectionResult result,
            Users staff,
            WarehouseTaskClaim activeClaim
    ) {

        /*
         * Quan trọng:
         *
         * Không tìm theo:
         *
         * receiptId + productId
         *
         * nữa.
         *
         * Phải tìm theo:
         *
         * taskClaimId + productId
         *
         * để mỗi attempt có inspection riêng.
         */
        ReceiptInspections inspection =
                receiptInspectionRepository
                        .findByTaskClaimIdAndProductId(
                                activeClaim.getId(),
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

                                        .taskClaim(
                                                activeClaim
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


        /*
         * Đảm bảo inspection luôn thuộc
         * đúng attempt hiện tại.
         */
        inspection.setTaskClaim(
                activeClaim
        );


        receiptInspectionRepository
                .save(
                        inspection
                );
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


        /*
         * Chỉ owner của attempt hiện tại
         * mới được Finish.
         */
        warehouseTaskClaimService
                .validateOwner(
                        WarehouseTaskType.GOODS_RECEIVING,
                        receiptId,
                        staff
                );


        WarehouseTaskClaim activeClaim =
                warehouseTaskClaimService
                        .getActiveClaim(
                                WarehouseTaskType.GOODS_RECEIVING,
                                receiptId
                        );


        if (activeClaim == null) {

            throw new BadRequest(
                    "Active receiving task claim was not found"
            );
        }


        long itemCount =
                goodsReceiptItemRepository
                        .countByGoodsReceiptId(
                                receiptId
                        );


        /*
         * Chỉ đếm inspection thuộc
         * attempt hiện tại.
         */
        long inspectionCount =
                receiptInspectionRepository
                        .countByTaskClaimId(
                                activeClaim.getId()
                        );


        if (itemCount == 0) {

            throw new BadRequest(
                    "Goods receipt contains no products"
            );
        }


        if (inspectionCount != itemCount) {

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
                .save(
                        receipt
                );

        receivingDiscrepancyService.syncFromFinishedInspection(
                receipt,
                activeClaim,
                staff
        );

        /*
         * Kết thúc attempt hiện tại.
         *
         * claimedAt  = Start
         * releasedAt = Finish
         */
        warehouseTaskClaimService
                .release(
                        WarehouseTaskType.GOODS_RECEIVING,
                        receiptId,
                        staff
                );


        /*
         * Không update Inventory.
         *
         * Manager Confirm Receiving
         * mới update Inventory và tạo Putaway.
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

        if (
                result ==
                        InspectionResult.MATCHED
        ) {

            return;
        }


        if (
                request.getNotes() == null
                        || request
                        .getNotes()
                        .isBlank()
        ) {

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
                .replace(
                        "-",
                        ""
                )
                .substring(
                        0,
                        10
                )
                .toUpperCase();
    }

}