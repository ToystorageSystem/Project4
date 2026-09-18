package com.toystorage.backend.services.receipts.receiving;

import com.toystorage.backend.dto.response.receipts.receiving.ReceivingMonitorResponse;
import com.toystorage.backend.dto.response.receipts.receiving.ReceivingProgressResponse;

import com.toystorage.backend.entity.inventories.DiscrepancyReports;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.inventories.DiscrepancyReferenceType;
import com.toystorage.backend.enums.inventories.DiscrepancyStatus;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.inventories.discrepancy.DiscrepancyReportRepository;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptRepository;
import com.toystorage.backend.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceivingMonitorService {

    private final GoodsReceiptRepository
            goodsReceiptRepository;

    private final DiscrepancyReportRepository
            discrepancyReportRepository;

    private final UserRepository
            userRepository;

    private final ReceivingMonitorQueryService
            monitorQueryService;

    private final ReceivingMonitorResponseBuilder
            responseBuilder;


    // =====================================================
    // RECEIVING
    // =====================================================

    @Transactional(readOnly = true)
    public Page<ReceivingMonitorResponse>
    getReceivingReceipts(
            String keyword,
            Pageable pageable
    ) {

        Users currentUser =
                getCurrentUser();

        validateUserWarehouse(
                currentUser
        );

        Long warehouseId =
                currentUser
                        .getWarehouse()
                        .getId();

        Page<GoodsReceipts> receipts =
                goodsReceiptRepository
                        .findByWarehouseIdAndStatus(
                                warehouseId,
                                GoodsReceiptStatus.RECEIVING,
                                pageable
                        );

        return receipts.map(
                responseBuilder::buildMonitorResponse
        );
    }


    // =====================================================
    // WAITING FOR WAREHOUSE MANAGER REVIEW
    // =====================================================

    @Transactional(readOnly = true)
    public Page<ReceivingMonitorResponse>
    getWaitingReviewReceipts(
            String keyword,
            Pageable pageable
    ) {

        Users currentUser =
                getCurrentUser();

        validateUserWarehouse(
                currentUser
        );

        Long warehouseId =
                currentUser
                        .getWarehouse()
                        .getId();

        String normalizedKeyword =
                normalizeKeyword(
                        keyword
                );


        /*
         * Lấy tất cả receipt đã INSPECTED.
         *
         * Không dùng custom JPQL để tránh lỗi parser.
         */
        List<GoodsReceipts> inspectedReceipts =
                goodsReceiptRepository
                        .findByWarehouseIdAndStatusOrderByCreatedAtDesc(
                                warehouseId,
                                GoodsReceiptStatus.INSPECTED
                        );


        /*
         * Filter:
         *
         * 1. Search keyword.
         * 2. Chỉ giữ receipt còn thuộc Waiting Review
         *    của Warehouse Manager.
         */
        List<GoodsReceipts> filtered =
                inspectedReceipts
                        .stream()

                        .filter(
                                receipt ->
                                        matchesKeyword(
                                                receipt,
                                                normalizedKeyword
                                        )
                        )

                        .filter(
                                this::shouldShowInWarehouseWaitingReview
                        )

                        .toList();


        /*
         * Manual pagination.
         */
        int start =
                Math.toIntExact(
                        pageable.getOffset()
                );

        if (start >= filtered.size()) {

            return new PageImpl<>(
                    List.of(),
                    pageable,
                    filtered.size()
            );
        }


        int end =
                Math.min(
                        start
                                + pageable.getPageSize(),
                        filtered.size()
                );


        List<ReceivingMonitorResponse> content =
                filtered
                        .subList(
                                start,
                                end
                        )
                        .stream()
                        .map(
                                responseBuilder::buildMonitorResponse
                        )
                        .toList();


        return new PageImpl<>(
                content,
                pageable,
                filtered.size()
        );
    }


    // =====================================================
    // WAITING REVIEW RULE
    // =====================================================

    private boolean shouldShowInWarehouseWaitingReview(
            GoodsReceipts receipt
    ) {

        List<DiscrepancyReports> reports =
                discrepancyReportRepository
                        .findByReferenceTypeAndReferenceId(
                                DiscrepancyReferenceType.GOODS_RECEIPT,
                                receipt.getId()
                        );


        /*
         * Không có discrepancy.
         *
         * Inspection sạch.
         *
         * Manager vẫn phải Confirm Receiving.
         */
        if (reports.isEmpty()) {

            return true;
        }


        /*
         * Chỉ cần còn ít nhất một report:
         *
         * OPEN / INVESTIGATING
         * +
         * WAREHOUSE_MANAGER
         *
         * thì receipt vẫn nằm Waiting Review.
         */
        return reports
                .stream()
                .anyMatch(
                        report -> {

                            boolean warehouseResponsible =
                                    "WAREHOUSE_MANAGER"
                                            .equalsIgnoreCase(
                                                    report.getResponsibleParty()
                                            );


                            boolean unresolved =
                                    report.getStatus()
                                            == DiscrepancyStatus.OPEN

                                            ||

                                            report.getStatus()
                                                    == DiscrepancyStatus.INVESTIGATING;


                            return warehouseResponsible
                                    && unresolved;
                        }
                );
    }


    // =====================================================
    // KEYWORD
    // =====================================================

    private boolean matchesKeyword(
            GoodsReceipts receipt,
            String keyword
    ) {

        if (
                keyword == null
                        ||
                        keyword.isBlank()
        ) {

            return true;
        }


        String receiptCode =
                receipt.getReceiptCode() != null
                        ? receipt
                        .getReceiptCode()
                        .toLowerCase()
                        : "";


        String staffName = "";

        if (
                receipt.getReceivedBy()
                        != null

                        &&

                        receipt.getReceivedBy()
                                .getName()
                                != null
        ) {

            staffName =
                    receipt
                            .getReceivedBy()
                            .getName()
                            .toLowerCase();
        }


        return receiptCode.contains(
                keyword
        )
                ||
                staffName.contains(
                        keyword
                );
    }


    // =====================================================
    // COMPLETED
    // =====================================================

    @Transactional(readOnly = true)
    public Page<ReceivingMonitorResponse>
    getCompletedReceipts(
            String keyword,
            Pageable pageable
    ) {

        Users currentUser =
                getCurrentUser();

        validateUserWarehouse(
                currentUser
        );

        Long warehouseId =
                currentUser
                        .getWarehouse()
                        .getId();


        Page<GoodsReceipts> receipts =
                goodsReceiptRepository
                        .findByWarehouseIdAndStatus(
                                warehouseId,
                                GoodsReceiptStatus.COMPLETED,
                                pageable
                        );


        return receipts.map(
                responseBuilder::buildMonitorResponse
        );
    }


    // =====================================================
    // PROGRESS
    // =====================================================

    @Transactional(readOnly = true)
    public ReceivingProgressResponse
    getProgress(
            Long receiptId
    ) {

        GoodsReceipts receipt =
                monitorQueryService
                        .getGoodsReceipt(
                                receiptId
                        );


        Users currentUser =
                getCurrentUser();


        validateSameWarehouse(
                currentUser,
                receipt
        );


        return responseBuilder
                .buildProgressResponse(
                        receipt
                );
    }


    // =====================================================
    // NORMALIZE KEYWORD
    // =====================================================

    private String normalizeKeyword(
            String keyword
    ) {

        if (keyword == null) {

            return "";
        }


        return keyword
                .trim()
                .toLowerCase();
    }


    // =====================================================
    // CURRENT USER
    // =====================================================

    private Users getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (
                authentication == null

                        ||

                        !authentication
                                .isAuthenticated()

                        ||

                        "anonymousUser"
                                .equals(
                                        authentication
                                                .getPrincipal()
                                )
        ) {

            throw new Unauthorized(
                    "User is not authenticated"
            );
        }


        String email =
                authentication
                        .getName();


        return userRepository
                .findByEmail(
                        email
                )
                .orElseThrow(
                        () ->
                                new NotFound(
                                        "Authenticated user not found: "
                                                + email
                                )
                );
    }


    // =====================================================
    // VALIDATE USER WAREHOUSE
    // =====================================================

    private void validateUserWarehouse(
            Users currentUser
    ) {

        if (
                currentUser
                        .getWarehouse()
                        == null
        ) {

            throw new Forbidden(
                    "User is not assigned to any warehouse"
            );
        }
    }


    // =====================================================
    // VALIDATE SAME WAREHOUSE
    // =====================================================

    private void validateSameWarehouse(
            Users currentUser,
            GoodsReceipts receipt
    ) {

        validateUserWarehouse(
                currentUser
        );


        if (
                receipt
                        .getWarehouse()
                        == null
        ) {

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


        if (
                !userWarehouseId
                        .equals(
                                receiptWarehouseId
                        )
        ) {

            throw new Forbidden(
                    "You cannot monitor receiving activity "
                            + "from another warehouse"
            );
        }
    }
}