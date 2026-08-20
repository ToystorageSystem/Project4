package com.toystorage.backend.services.receipts.receiving;

import com.toystorage.backend.dto.response.receipts.receiving.ReceivingConfirmationItemResponse;
import com.toystorage.backend.dto.response.receipts.receiving.ReceivingConfirmationResponse;
import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
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

        if (receipt.getStatus()
                != GoodsReceiptStatus.INSPECTED) {

            throw new BadRequest(
                    "Inspection has not been completed yet"
            );
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

        long inspectionCount =
                receiptInspectionRepository
                        .countByGoodsReceiptId(
                                receiptId
                        );

        /*
         * Đảm bảo tất cả sản phẩm đã được kiểm.
         */
        if (inspectionCount != items.size()) {

            throw new BadRequest(
                    "Not all goods receipt items have been inspected"
            );
        }

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
         * Không gọi getInspectionResult()
         * vì receipt lúc này đã COMPLETED.
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

        List<ReceiptInspections> inspections =
                receiptInspectionRepository
                        .findByGoodsReceiptId(
                                receipt.getId()
                        );

        List<ReceivingConfirmationItemResponse> responses =
                items.stream()
                        .map(item -> {

                            ReceiptInspections inspection =
                                    inspections.stream()
                                            .filter(i ->
                                                    i.getProduct()
                                                            .getId()
                                                            .equals(
                                                                    item.getProduct()
                                                                            .getId()
                                                            )
                                            )
                                            .findFirst()
                                            .orElse(null);

                            return ReceivingConfirmationItemResponse
                                    .builder()

                                    .productId(
                                            item.getProduct().getId()
                                    )

                                    .productName(
                                            item.getProduct().getName()
                                    )

                                    .expectedQuantity(
                                            item.getExpectedQuantity()
                                    )

                                    .actualQuantity(
                                            item.getActualQuantity()
                                    )

                                    .acceptedQuantity(
                                            item.getAcceptedQuantity()
                                    )

                                    .damagedQuantity(
                                            item.getDamagedQuantity()
                                    )

                                    .shortageQuantity(
                                            item.getShortageQuantity()
                                    )

                                    .surplusQuantity(
                                            item.getSurplusQuantity()
                                    )

                                    .inspectionResult(
                                            inspection != null
                                                    ? inspection
                                                    .getInspectedResult()
                                                    .name()
                                                    : null
                                    )

                                    .build();
                        })
                        .toList();

        int expected =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getExpectedQuantity
                        )
                        .sum();

        int actual =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getActualQuantity
                        )
                        .sum();

        int accepted =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getAcceptedQuantity
                        )
                        .sum();

        int damaged =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getDamagedQuantity
                        )
                        .sum();

        int shortage =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getShortageQuantity
                        )
                        .sum();

        int surplus =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getSurplusQuantity
                        )
                        .sum();

        Users staff =
                receipt.getReceivedBy();

        Users manager =
                receipt.getInspectionConfirmedBy();

        return ReceivingConfirmationResponse
                .builder()

                .receiptId(
                        receipt.getId()
                )

                .receiptCode(
                        receipt.getReceiptCode()
                )

                .status(
                        receipt.getStatus().name()
                )

                .staffId(
                        staff != null
                                ? staff.getId()
                                : null
                )

                .staffName(
                        staff != null
                                ? staff.getName()
                                : null
                )

                .totalExpectedQuantity(
                        expected
                )

                .totalActualQuantity(
                        actual
                )

                .totalAcceptedQuantity(
                        accepted
                )

                .totalDamagedQuantity(
                        damaged
                )

                .totalShortageQuantity(
                        shortage
                )

                .totalSurplusQuantity(
                        surplus
                )

                .items(
                        responses
                )

                /*
                 * CHÚ Ý:
                 * DTO của bạn là confirmedBy,
                 * KHÔNG phải inspectionConfirmedBy.
                 */
                .confirmedBy(
                        manager != null
                                ? manager.getId()
                                : null
                )

                .confirmedByName(
                        manager != null
                                ? manager.getName()
                                : null
                )

                .confirmedAt(
                        receipt.getInspectionConfirmedAt()
                )

                .build();
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
}