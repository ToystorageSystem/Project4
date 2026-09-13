package com.toystorage.backend.services.receipts.receiving;

import com.toystorage.backend.dto.response.receipts.receiving.ReceivingMonitorResponse;
import com.toystorage.backend.dto.response.receipts.receiving.ReceivingProgressResponse;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.enums.receipts.InspectionResult;
import com.toystorage.backend.enums.warehouses.WarehouseTaskType;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptRepository;
import com.toystorage.backend.repository.users.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceivingMonitorService {

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final UserRepository userRepository;
    private final ReceivingMonitorQueryService monitorQueryService;
    private final ReceivingMonitorResponseBuilder responseBuilder;

    @Transactional(readOnly = true)
    public Page<ReceivingMonitorResponse> getReceivingReceipts(
            String keyword,
            Pageable pageable
    ) {

        Users currentUser = getCurrentUser();

        validateUserWarehouse(currentUser);

        Long warehouseId =
                currentUser.getWarehouse().getId();

        return goodsReceiptRepository
                .findReceivingMonitorPage(
                        warehouseId,
                        GoodsReceiptStatus.RECEIVING,
                        WarehouseTaskType.GOODS_RECEIVING,
                        InspectionResult.MATCHED,
                        normalizeKeyword(keyword),
                        pageable
                );
    }


    @Transactional(readOnly = true)
    public Page<ReceivingMonitorResponse> getWaitingReviewReceipts(
            String keyword,
            Pageable pageable
    ) {

        Users currentUser = getCurrentUser();

        validateUserWarehouse(currentUser);

        Long warehouseId =
                currentUser.getWarehouse().getId();

        return goodsReceiptRepository
                .findReceivingMonitorPage(
                        warehouseId,
                        GoodsReceiptStatus.INSPECTED,
                        WarehouseTaskType.GOODS_RECEIVING,
                        InspectionResult.MATCHED,
                        normalizeKeyword(keyword),
                        pageable
                );
    }


    @Transactional(readOnly = true)
    public Page<ReceivingMonitorResponse> getCompletedReceipts(
            String keyword,
            Pageable pageable
    ) {

        Users currentUser = getCurrentUser();

        validateUserWarehouse(currentUser);

        Long warehouseId =
                currentUser.getWarehouse().getId();

        return goodsReceiptRepository
                .findReceivingMonitorPage(
                        warehouseId,
                        GoodsReceiptStatus.COMPLETED,
                        WarehouseTaskType.GOODS_RECEIVING,
                        InspectionResult.MATCHED,
                        normalizeKeyword(keyword),
                        pageable
                );
    }

    @Transactional(readOnly = true)
    public ReceivingProgressResponse getProgress(
            Long receiptId
    ) {

        GoodsReceipts receipt =
                monitorQueryService
                        .getGoodsReceipt(receiptId);

        Users currentUser =
                getCurrentUser();

        validateSameWarehouse(
                currentUser,
                receipt
        );

        return responseBuilder
                .buildProgressResponse(receipt);
    }
    private String normalizeKeyword(String keyword) {

        if (keyword == null) {
            return "";
        }

        return keyword.trim().toLowerCase();
    }
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

        String email =
                authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new NotFound(
                                "Authenticated user not found: "
                                        + email
                        )
                );
    }

    private void validateUserWarehouse(
            Users currentUser
    ) {

        if (currentUser.getWarehouse() == null) {

            throw new Forbidden(
                    "User is not assigned to any warehouse"
            );
        }
    }

    private void validateSameWarehouse(
            Users currentUser,
            GoodsReceipts receipt
    ) {

        validateUserWarehouse(currentUser);

        if (receipt.getWarehouse() == null) {

            throw new BadRequest(
                    "Goods receipt is not assigned to a warehouse"
            );
        }

        Long userWarehouseId =
                currentUser
                        .getWarehouse()
                        .getId();

        Long receiptWarehouseId =
                receipt
                        .getWarehouse()
                        .getId();

        if (!userWarehouseId.equals(receiptWarehouseId)) {

            throw new Forbidden(
                    "You cannot monitor receiving activity "
                            + "from another warehouse"
            );
        }
    }
}
