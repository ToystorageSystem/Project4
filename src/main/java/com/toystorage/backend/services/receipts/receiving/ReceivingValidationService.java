package com.toystorage.backend.services.receipts.receiving;

import com.toystorage.backend.dto.request.receipts.receiving.ReceiptInspectionBatchRequest;
import com.toystorage.backend.dto.request.receipts.receiving.ReceiptInspectionItemRequest;

import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptRepository;
import com.toystorage.backend.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReceivingValidationService {

    private final GoodsReceiptRepository
            goodsReceiptRepository;

    private final UserRepository
            userRepository;


    // =====================================================
    // CURRENT USER
    // =====================================================

    public Users getCurrentUser() {

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
    // GET RECEIPT
    // =====================================================

    public GoodsReceipts getReceipt(
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
    // SAME WAREHOUSE
    // =====================================================

    public void validateSameWarehouse(
            Users user,
            GoodsReceipts receipt
    ) {

        if (user.getWarehouse() == null) {

            throw new Forbidden(
                    "User is not assigned to a warehouse"
            );
        }

        if (receipt.getWarehouse() == null) {

            throw new BadRequest(
                    "Goods receipt has no warehouse"
            );
        }

        if (!user.getWarehouse()
                .getId()
                .equals(
                        receipt.getWarehouse()
                                .getId()
                )) {

            throw new Forbidden(
                    "You cannot receive goods "
                            + "from another warehouse"
            );
        }
    }


    // =====================================================
    // CAN VIEW RECEIVING
    // =====================================================

    public void validateCanViewReceiving(
            GoodsReceipts receipt
    ) {

        if (receipt.getStatus()
                != GoodsReceiptStatus.CONFIRMED

                && receipt.getStatus()
                != GoodsReceiptStatus.RECEIVING) {

            throw new BadRequest(
                    "Goods receipt is not available for receiving"
            );
        }
    }


    // =====================================================
    // CAN START
    // =====================================================

    public void validateCanStart(
            GoodsReceipts receipt
    ) {

        if (receipt.getStatus()
                != GoodsReceiptStatus.CONFIRMED) {

            throw new BadRequest(
                    "Only CONFIRMED goods receipt "
                            + "can start receiving"
            );
        }
    }


    // =====================================================
    // CAN INSPECT
    // =====================================================

    public void validateCanInspect(
            GoodsReceipts receipt
    ) {

        if (receipt.getStatus()
                != GoodsReceiptStatus.RECEIVING) {

            throw new BadRequest(
                    "Goods receipt must be RECEIVING "
                            + "before inspection"
            );
        }
    }


    // =====================================================
    // DUPLICATE PRODUCTS IN BATCH
    // =====================================================

    public void validateNoDuplicateProducts(
            ReceiptInspectionBatchRequest request
    ) {

        Set<Long> productIds =
                new HashSet<>();

        for (ReceiptInspectionItemRequest item
                : request.getItems()) {

            if (!productIds.add(
                    item.getProductId()
            )) {

                throw new BadRequest(
                        "Duplicate product id in request: "
                                + item.getProductId()
                );
            }
        }
    }


    // =====================================================
    // PRODUCT BELONGS TO RECEIPT
    // =====================================================

    public GoodsReceiptItems getReceiptItem(
            List<GoodsReceiptItems> items,
            Long productId
    ) {

        return items.stream()

                .filter(item ->
                        item.getProduct()
                                .getId()
                                .equals(productId)
                )

                .findFirst()

                .orElseThrow(() ->
                        new BadRequest(
                                "Product "
                                        + productId
                                        + " does not belong "
                                        + "to goods receipt"
                        )
                );
    }


    // =====================================================
    // QUANTITY VALIDATION
    // =====================================================

    public void validateQuantities(
            ReceiptInspectionItemRequest request
    ) {

        if (request.getActualQuantity() == null
                || request.getActualQuantity() < 0) {

            throw new BadRequest(
                    "Actual quantity cannot be negative"
            );
        }

        int damaged =
                request.getDamagedQuantity() != null
                        ? request.getDamagedQuantity()
                        : 0;

        if (damaged < 0) {

            throw new BadRequest(
                    "Damaged quantity cannot be negative"
            );
        }

        if (damaged
                > request.getActualQuantity()) {

            throw new BadRequest(
                    "Damaged quantity cannot exceed "
                            + "actual quantity for product "
                            + request.getProductId()
            );
        }
    }
}