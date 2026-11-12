package com.toystorage.backend.services.receipts.receiving;

import com.toystorage.backend.dto.response.receipts.receiving.ReceivingConfirmationResponse;
import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.mapper.receipts.receiving.ReceivingConfirmationMapper;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptItemRepository;
import com.toystorage.backend.repository.receipts.receiving.ReceiptInspectionRepository;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.services.warehouses.putaway.PutawayTaskService;
import com.toystorage.backend.services.warehouses.location.WarehouseLocationService;
import com.toystorage.backend.dto.request.receipts.receiving.RequestReinspectionRequest;
import com.toystorage.backend.services.inventories.discrepancy.ReceivingDiscrepancyService;
import com.toystorage.backend.entity.receipts.ReceivingReinspectionRequest;
import com.toystorage.backend.entity.warehouses.WarehouseTaskClaim;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptRepository;
import com.toystorage.backend.enums.warehouses.WarehouseTaskType;

import com.toystorage.backend.repository.receipts.receiving.ReceivingReinspectionRequestRepository;

import com.toystorage.backend.services.warehouses.taskclaim.WarehouseTaskClaimService;
import com.toystorage.backend.enums.inventories.InventoryReferenceType;
import com.toystorage.backend.enums.inventories.InventoryTransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceivingConfirmationService {

    private final GoodsReceiptRepository goodsReceiptRepository;

    private final GoodsReceiptItemRepository goodsReceiptItemRepository;

    private final ReceiptInspectionRepository receiptInspectionRepository;

    private final UserRepository userRepository;

    private final ReceivingInventoryService receivingInventoryService;

    private final PutawayTaskService putawayTaskService;

    private final PurchaseOrderReceivingService purchaseOrderReceivingService;

    private final WarehouseLocationService warehouseLocationService;

    private final ReceivingConfirmationMapper
            mapper;

    private final ReceivingReinspectionRequestRepository
            reinspectionRequestRepository;

    private final WarehouseTaskClaimService
            warehouseTaskClaimService;

    private final ReceivingDiscrepancyService
            receivingDiscrepancyService;

    private final com.toystorage.backend.services.inventories.damagedlot.DamagedStockService
            damagedStockService;

    private final com.toystorage.backend.services.receipts.incidents.ReceivingIncidentReportService
            receivingIncidentReportService;


    // =====================================================
    // VIEW RESULT
    // =====================================================

    @Transactional(readOnly = true)
    public ReceivingConfirmationResponse getInspectionResult(
            Long receiptId
    ) {

        GoodsReceipts receipt =
                getReceipt(receiptId);

        Users manager =
                getCurrentUser();

        validateSameWarehouse(
                manager,
                receipt
        );

        if (receipt.getStatus() != GoodsReceiptStatus.INSPECTED
                && receipt.getStatus() != GoodsReceiptStatus.COMPLETED) {
            throw new BadRequest("Inspection has not been completed yet");
        }

        return buildResponse(receipt);
    }


    // =====================================================
    // CONFIRM RECEIVING
    // =====================================================

    @Transactional
    public ReceivingConfirmationResponse confirmInspectionResult(
            Long receiptId
    ) {

        GoodsReceipts receipt =
                getReceiptForUpdate(receiptId);

        Users manager =
                getCurrentUser();

        validateSameWarehouse(
                manager,
                receipt
        );

        /*
         * Staff phải finish inspection.
         */
        if (receipt.getStatus()
                != GoodsReceiptStatus.INSPECTED) {

            throw new BadRequest(
                    "Receiving result can only be confirmed "
                            + "after Warehouse Staff completes inspection"
            );
        }

        /*
         * Không confirm lần 2.
         */
        if (receipt.getInspectionConfirmedAt() != null) {

            throw new BadRequest(
                    "Receiving result has already been confirmed"
            );
        }

        List<GoodsReceiptItems> items =
                goodsReceiptItemRepository
                        .findByGoodsReceiptId(receiptId);

        if (items.isEmpty()) {

            throw new BadRequest(
                    "Goods receipt contains no items"
            );
        }

        WarehouseTaskClaim latestClaim =
                warehouseTaskClaimService
                        .getLatestClaim(
                                WarehouseTaskType.GOODS_RECEIVING,
                                receiptId
                        );


        if (latestClaim == null) {

            throw new BadRequest(
                    "Receiving task claim was not found"
            );
        }


        if (latestClaim.getReleasedAt() == null) {

            throw new BadRequest(
                    "Warehouse Staff has not finished "
                            + "the latest inspection attempt"
            );
        }


        long inspectionCount =
                receiptInspectionRepository
                        .countByTaskClaimId(
                                latestClaim.getId()
                        );


        if (inspectionCount != items.size()) {

            throw new BadRequest(
                    "Not all goods receipt items have been inspected "
                            + "in the latest inspection attempt"
            );
        }

        complete(receipt, items, latestClaim, manager);
        return buildResponse(receipt);
    }

    // =====================================================
    // AUTO COMPLETE AFTER STAFF INSPECTION
    // =====================================================

    /**
     * Nghiep vu moi: Warehouse Staff kiem xong la phieu nhap hoan thanh, khong can Manager duyet.
     *  - Hang dat (accepted) duoc cong ton tai khu nhan hang, sau do nguoi da kiem cat len ke (putaway).
     *  - Hang hong / loi KHONG cong ton kha dung: dua vao khu hang hong, ghi theo lo LOT-<ma phieu nhap>.
     *  - Hang thieu / hong / du duoc ghi vao bien ban su co de Manager xem, sua, in cho Business.
     */
    @Transactional
    public void completeAfterInspection(Long receiptId, Users staff) {
        GoodsReceipts receipt = getReceiptForUpdate(receiptId);
        if (receipt.getStatus() == GoodsReceiptStatus.COMPLETED) return;
        List<GoodsReceiptItems> items = goodsReceiptItemRepository.findByGoodsReceiptId(receiptId);
        if (items.isEmpty()) throw new BadRequest("Goods receipt contains no items");
        WarehouseTaskClaim latestClaim = warehouseTaskClaimService.getLatestClaim(WarehouseTaskType.GOODS_RECEIVING, receiptId);
        if (latestClaim == null) throw new BadRequest("Receiving task claim was not found");
        complete(receipt, items, latestClaim, staff);
    }

    private void complete(GoodsReceipts receipt, List<GoodsReceiptItems> items, WarehouseTaskClaim latestClaim, Users actor) {
        WarehouseLocations receivingLocation =
                warehouseLocationService.getReceivingLocation(receipt.getWarehouse().getId());

        // Hang dat: cong ton (khu nhan hang), sau putaway moi thanh ton kha dung.
        receivingInventoryService.updateReceivingInventory(receipt, items, actor, receivingLocation);

        // Hang hong / loi: vao khu hang hong, ma lo = ma don PO cua nha cung cap (khong co PO thi dung ma phieu nhap).
        // Khong cong ton kha dung.
        String poCode = receipt.getPurchaseOrder() != null ? receipt.getPurchaseOrder().getOrderCode() : null;
        String lotCode = poCode != null && !poCode.isBlank() ? poCode : receipt.getReceiptCode();
        for (GoodsReceiptItems item : items) {
            int damaged = item.getDamagedQuantity() == null ? 0 : item.getDamagedQuantity();
            if (damaged <= 0) continue;
            damagedStockService.record(receipt.getWarehouse(), item.getProduct(), damaged,
                    "GOODS_RECEIPT", receipt.getId(), receipt.getReceiptCode(), lotCode,
                    "Damaged / defective when receiving from supplier (PO " + (poCode == null ? "—" : poCode)
                            + ", receipt " + receipt.getReceiptCode() + ")", actor,
                    InventoryReferenceType.GOODS_RECEIPT, InventoryTransactionType.DAMAGED);
        }

        Users receivingStaff = latestClaim.getClaimedBy();
        if (receivingStaff == null) throw new BadRequest("Receiving staff was not found");
        // Nguoi da kiem hang thi cat hang len ke.
        putawayTaskService.createFromGoodsReceipt(receipt, items, receivingStaff, receivingLocation);

        // Bien ban su co (thieu / hong / du) de Manager in cho Business lam viec voi nha cung cap.
        receivingIncidentReportService.generateForCompletedReceipt(receipt, receivingStaff);

        LocalDateTime now = LocalDateTime.now();
        receipt.setInspectionConfirmedBy(actor);
        receipt.setInspectionConfirmedAt(now);
        // PO learns what arrived: received_quantity + PARTIALLY_RECEIVED / COMPLETED (2026-11-13)
        purchaseOrderReceivingService.applyCompletedReceipt(receipt, items);

        receipt.setStatus(GoodsReceiptStatus.COMPLETED);
        receipt.setUpdatedAt(now);
        goodsReceiptRepository.save(receipt);
    }


    // =====================================================
    // BUILD RESPONSE
    // =====================================================

    private ReceivingConfirmationResponse buildResponse(
            GoodsReceipts receipt
    ) {

        List<GoodsReceiptItems> items =
                goodsReceiptItemRepository
                        .findByGoodsReceiptId(
                                receipt.getId()
                        );

        WarehouseTaskClaim latestClaim =
                warehouseTaskClaimService
                        .getLatestClaim(
                                WarehouseTaskType.GOODS_RECEIVING,
                                receipt.getId()
                        );


        List<ReceiptInspections> inspections =
                latestClaim == null
                        ? List.of()
                        : receiptInspectionRepository
                        .findByTaskClaimId(
                                latestClaim.getId()
                        );


        return mapper.toResponse(
                receipt,
                items,
                inspections
        );
    }


    // =====================================================
    // RECEIPT
    // =====================================================

    private GoodsReceipts getReceiptForUpdate(Long receiptId) {
        return goodsReceiptRepository.findByIdForUpdate(receiptId)
                .orElseThrow(() -> new NotFound("Goods receipt not found: " + receiptId));
    }

    private GoodsReceipts getReceipt(
            Long receiptId
    ) {

        return goodsReceiptRepository
                .findById(receiptId)
                .orElseThrow(() ->
                        new NotFound(
                                "Goods receipt not found: "
                                        + receiptId
                        )
                );
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
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

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
    // WAREHOUSE SECURITY
    // =====================================================

    private void validateSameWarehouse(
            Users manager,
            GoodsReceipts receipt
    ) {

        if (manager.getWarehouse() == null) {

            throw new Forbidden(
                    "Manager is not assigned to a warehouse"
            );
        }

        if (receipt.getWarehouse() == null) {

            throw new BadRequest(
                    "Goods receipt is not assigned to a warehouse"
            );
        }

        if (!manager
                .getWarehouse()
                .getId()
                .equals(
                        receipt
                                .getWarehouse()
                                .getId()
                )) {

            throw new Forbidden(
                    "You cannot confirm receiving "
                            + "for another warehouse"
            );
        }
    }
    @Transactional
    public ReceivingConfirmationResponse requestReinspection(
            Long receiptId,
            RequestReinspectionRequest request
    ) {

        GoodsReceipts receipt =
                getReceiptForUpdate(receiptId);

        Users manager =
                getCurrentUser();

        validateSameWarehouse(
                manager,
                receipt
        );

        if (receipt.getStatus()
                != GoodsReceiptStatus.INSPECTED) {

            throw new BadRequest(
                    "Re-inspection can only be requested "
                            + "after inspection is completed"
            );
        }
        WarehouseTaskClaim latestClaim =
                warehouseTaskClaimService
                        .getLatestClaim(
                                WarehouseTaskType.GOODS_RECEIVING,
                                receiptId
                        );


        if (latestClaim == null) {

            throw new BadRequest(
                    "Previous inspection task was not found"
            );
        }


        if (latestClaim.getReleasedAt() == null) {

            throw new BadRequest(
                    "Current inspection has not been completed"
            );
        }


        ReceivingReinspectionRequest reinspectionRequest =
                ReceivingReinspectionRequest
                        .builder()

                        .goodsReceipt(
                                receipt
                        )

                        .requestedBy(
                                manager
                        )

                        .reason(
                                request
                                        .getReason()
                                        .trim()
                        )

                        .allowSameStaff(
                                request.isAllowSameStaff()
                        )

                        .requestedAt(
                                LocalDateTime.now()
                        )

                        .build();


        reinspectionRequestRepository.save(
                reinspectionRequest
        );
        /*
         * Tạo record ReceivingReinspectionRequest
         * để lưu:
         * manager
         * reason
         * requestedAt
         * allowSameStaff
         */
        receivingDiscrepancyService.markReinspectionRequested(
                receiptId,
                manager,
                request.getReason().trim()
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

        return buildResponse(
                receipt
        );
    }
}