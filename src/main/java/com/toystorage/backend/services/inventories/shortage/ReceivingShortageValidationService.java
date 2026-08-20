package com.toystorage.backend.services.inventories.shortage;

import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptItemRepository;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptRepository;
import com.toystorage.backend.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReceivingShortageValidationService {

    private final UserRepository userRepository;

    private final GoodsReceiptRepository
            goodsReceiptRepository;

    private final GoodsReceiptItemRepository
            goodsReceiptItemRepository;


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


    public GoodsReceiptItems getReceiptItem(
            Long receiptId,
            Long productId
    ) {

        return goodsReceiptItemRepository
                .findByGoodsReceiptIdAndProductId(
                        receiptId,
                        productId
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Product does not belong "
                                        + "to goods receipt"
                        )
                );
    }


    public void validateSameWarehouse(
            Users staff,
            GoodsReceipts receipt
    ) {

        if (staff.getWarehouse() == null) {

            throw new Forbidden(
                    "User is not assigned to warehouse"
            );
        }

        if (!staff.getWarehouse()
                .getId()
                .equals(
                        receipt.getWarehouse()
                                .getId()
                )) {

            throw new Forbidden(
                    "You cannot report shortage "
                            + "for another warehouse"
            );
        }
    }


    public void validateShortage(
            GoodsReceiptItems item
    ) {

        int expected =
                item.getExpectedQuantity();

        int actual =
                item.getActualQuantity();

        if (actual >= expected) {

            throw new BadRequest(
                    "Shortage report can only be created "
                            + "when actual quantity is less "
                            + "than expected quantity"
            );
        }
    }
}