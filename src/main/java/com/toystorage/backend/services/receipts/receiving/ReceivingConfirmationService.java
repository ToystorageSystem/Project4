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
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptRepository;
import com.toystorage.backend.repository.receipts.receiving.ReceiptInspectionRepository;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.services.warehouses.putaway.PutawayTaskService;
import com.toystorage.backend.services.warehouses.location.WarehouseLocationService;
import com.toystorage.backend.dto.request.receipts.receiving.RequestReinspectionRequest;
import com.toystorage.backend.services.inventories.discrepancy.ReceivingDiscrepancyService;
import com.toystorage.backend.entity.receipts.ReceivingReinspectionRequest;
import com.toystorage.backend.entity.warehouses.WarehouseTaskClaim;

import com.toystorage.backend.enums.warehouses.WarehouseTaskType;

import com.toystorage.backend.repository.receipts.receiving.ReceivingReinspectionRequestRepository;

import com.toystorage.backend.services.warehouses.taskclaim.WarehouseTaskClaimService;
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

    private final WarehouseLocationService warehouseLocationService;

    private final ReceivingConfirmationMapper
            mapper;

    private final ReceivingReinspectionRequestRepository
            reinspectionRequestRepository;

    private final WarehouseTaskClaimService
            warehouseTaskClaimService;

    private final ReceivingDiscrepancyService
            receivingDiscrepancyService;


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
                getReceipt(receiptId);

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

        receivingDiscrepancyService.assertCanConfirm(receiptId);
        /*
         * =============================================
         * WAREHOUSE
         * =============================================
         */

        WarehouseLocations receivingLocation =
                warehouseLocationService
                        .getReceivingLocation(
                                receipt
                                        .getWarehouse()
                                        .getId()
                        );

        /*
         * =============================================
         * INVENTORY
         * =============================================
         */

        receivingInventoryService
                .updateReceivingInventory(
                        receipt,
                        items,
                        manager,
                        receivingLocation
                );

        /*
         * =============================================
         * PUTAWAY
         * =============================================
         */

        putawayTaskService
                .createFromGoodsReceipt(
                        receipt,
                        items,
                        manager,
                        receivingLocation
                );

        /*
         * =============================================
         * RECEIPT
         * =============================================
         */

        receipt.setInspectionConfirmedBy(
                manager
        );

        receipt.setInspectionConfirmedAt(
                LocalDateTime.now()
        );

        receipt.setStatus(
                GoodsReceiptStatus.COMPLETED
        );

        receipt.setUpdatedAt(
                LocalDateTime.now()
        );

        goodsReceiptRepository.save(
                receipt
        );

        /*
         * Build response directly after confirmation.
         * getInspectionResult() also supports COMPLETED
         * for read-only history viewing.
         */
        return buildResponse(receipt);
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
                getReceipt(receiptId);

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