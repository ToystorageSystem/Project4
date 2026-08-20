package com.toystorage.backend.services.receipts.receiving;

import com.toystorage.backend.dto.response.receipts.receiving.ReceivingIssueResponse;
import com.toystorage.backend.dto.response.receipts.receiving.ReceivingMonitorResponse;
import com.toystorage.backend.dto.response.receipts.receiving.ReceivingProgressResponse;
import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.enums.receipts.InspectionResult;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.mapper.receipts.receiving.ReceivingMonitorMapper;
import com.toystorage.backend.repository.receipts.GoodsReceiptItemRepository;
import com.toystorage.backend.repository.receipts.GoodsReceiptRepository;
import com.toystorage.backend.repository.receipts.ReceiptInspectionRepository;
import com.toystorage.backend.repository.users.UserRepository;
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

    private final GoodsReceiptItemRepository goodsReceiptItemRepository;

    private final ReceiptInspectionRepository receiptInspectionRepository;

    private final UserRepository userRepository;

    private final ReceivingMonitorMapper receivingMonitorMapper;


    // =====================================================
    // DANH SÁCH PHIẾU ĐANG KIỂM NHẬN
    // =====================================================

    @Transactional(readOnly = true)
    public List<ReceivingMonitorResponse>
    getReceivingReceipts() {

        Users manager =
                getCurrentUser();

        if (manager.getWarehouse() == null) {
            throw new Forbidden(
                    "Manager is not assigned to any warehouse"
            );
        }

        Long warehouseId =
                manager.getWarehouse().getId();

        List<GoodsReceipts> receipts =
                goodsReceiptRepository
                        .findByWarehouseIdAndStatus(
                                warehouseId,
                                GoodsReceiptStatus.RECEIVING
                        );

        return receipts
                .stream()
                .map(this::buildMonitorResponse)
                .toList();
    }


    // =====================================================
    // CHI TIẾT TIẾN ĐỘ
    // =====================================================

    @Transactional(readOnly = true)
    public ReceivingProgressResponse getProgress(
            Long receiptId
    ) {

        GoodsReceipts receipt =
                getGoodsReceipt(receiptId);

        Users manager =
                getCurrentUser();

        validateSameWarehouse(
                manager,
                receipt
        );

        List<GoodsReceiptItems> items =
                goodsReceiptItemRepository
                        .findByGoodsReceiptId(receiptId);

        List<ReceiptInspections> inspections =
                receiptInspectionRepository
                        .findByGoodsReceiptId(receiptId);

        int totalProducts =
                items.size();

        int inspectedProducts =
                inspections.size();

        int remainingProducts =
                Math.max(
                        totalProducts - inspectedProducts,
                        0
                );

        int totalExpectedQuantity =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getExpectedQuantity
                        )
                        .sum();

        int totalActualQuantity =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getActualQuantity
                        )
                        .sum();

        int totalDamagedQuantity =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getDamagedQuantity
                        )
                        .sum();

        int totalShortageQuantity =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getShortageQuantity
                        )
                        .sum();

        int totalSurplusQuantity =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getSurplusQuantity
                        )
                        .sum();

        double progress =
                totalProducts == 0
                        ? 0
                        : ((double) inspectedProducts
                        / totalProducts) * 100;

        List<ReceivingIssueResponse> issues =
                inspections.stream()

                        .filter(inspection ->
                                inspection.getInspectedResult()
                                        != InspectionResult.MATCHED
                        )

                        .map(
                                receivingMonitorMapper
                                        ::toIssueResponse
                        )

                        .toList();

        Users staff =
                receipt.getReceivedBy();

        return ReceivingProgressResponse.builder()

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

                .totalProducts(
                        totalProducts
                )

                .inspectedProducts(
                        inspectedProducts
                )

                .remainingProducts(
                        remainingProducts
                )

                .totalExpectedQuantity(
                        totalExpectedQuantity
                )

                .totalActualQuantity(
                        totalActualQuantity
                )

                .totalDamagedQuantity(
                        totalDamagedQuantity
                )

                .totalShortageQuantity(
                        totalShortageQuantity
                )

                .totalSurplusQuantity(
                        totalSurplusQuantity
                )

                .progressPercent(
                        Math.round(progress * 100.0)
                                / 100.0
                )

                .issues(issues)

                .build();
    }


    // =====================================================
    // BUILD MONITOR RESPONSE
    // =====================================================

    private ReceivingMonitorResponse buildMonitorResponse(
            GoodsReceipts receipt
    ) {

        long totalProducts =
                goodsReceiptItemRepository
                        .countByGoodsReceiptId(
                                receipt.getId()
                        );

        long inspectedProducts =
                receiptInspectionRepository
                        .countByGoodsReceiptId(
                                receipt.getId()
                        );

        long remainingProducts =
                Math.max(
                        totalProducts - inspectedProducts,
                        0
                );

        List<ReceiptInspections> inspections =
                receiptInspectionRepository
                        .findByGoodsReceiptId(
                                receipt.getId()
                        );

        long issueProducts =
                inspections.stream()
                        .filter(i ->
                                i.getInspectedResult()
                                        != InspectionResult.MATCHED
                        )
                        .count();

        double progress =
                totalProducts == 0
                        ? 0
                        : ((double) inspectedProducts
                        / totalProducts) * 100;

        Users staff =
                receipt.getReceivedBy();

        return ReceivingMonitorResponse.builder()

                .receiptId(
                        receipt.getId()
                )

                .receiptCode(
                        receipt.getReceiptCode()
                )

                .status(
                        receipt.getStatus().name()
                )

                .warehouseId(
                        receipt.getWarehouse().getId()
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

                .totalProducts(
                        (int) totalProducts
                )

                .inspectedProducts(
                        (int) inspectedProducts
                )

                .remainingProducts(
                        (int) remainingProducts
                )

                .issueProducts(
                        (int) issueProducts
                )

                .progressPercent(
                        Math.round(progress * 100.0)
                                / 100.0
                )

                .receivingStartedAt(
                        receipt.getReceivedAt()
                )

                .build();
    }


    // =====================================================
    // GET RECEIPT
    // =====================================================

    private GoodsReceipts getGoodsReceipt(
            Long receiptId
    ) {

        return goodsReceiptRepository
                .findById(receiptId)
                .orElseThrow(() ->
                        new NotFound(
                                "Goods receipt not found with id: "
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
    // WAREHOUSE VALIDATION
    // =====================================================

    private void validateSameWarehouse(
            Users manager,
            GoodsReceipts receipt
    ) {

        if (manager.getWarehouse() == null) {
            throw new Forbidden(
                    "Manager is not assigned to any warehouse"
            );
        }

        if (receipt.getWarehouse() == null) {
            throw new BadRequest(
                    "Goods receipt is not assigned to a warehouse"
            );
        }

        if (!manager.getWarehouse()
                .getId()
                .equals(
                        receipt.getWarehouse().getId()
                )) {

            throw new Forbidden(
                    "You cannot monitor receiving activity from another warehouse"
            );
        }
    }
}