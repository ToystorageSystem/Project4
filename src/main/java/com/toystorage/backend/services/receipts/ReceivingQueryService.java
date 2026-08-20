package com.toystorage.backend.services.receipts;

import com.toystorage.backend.dto.response.receipts.ReceivingDetailResponse;
import com.toystorage.backend.dto.response.receipts.ReceivingListResponse;

import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;

import com.toystorage.backend.exceptions.Forbidden;

import com.toystorage.backend.mapper.receipts.ReceivingInspectionMapper;

import com.toystorage.backend.repository.receipts.GoodsReceiptItemRepository;
import com.toystorage.backend.repository.receipts.GoodsReceiptRepository;
import com.toystorage.backend.repository.receipts.ReceiptInspectionRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceivingQueryService {

    private final GoodsReceiptRepository
            goodsReceiptRepository;

    private final GoodsReceiptItemRepository
            goodsReceiptItemRepository;

    private final ReceiptInspectionRepository
            receiptInspectionRepository;

    private final ReceivingInspectionMapper
            receivingInspectionMapper;

    private final ReceivingValidationService
            validationService;


    // =====================================================
    // LIST RECEIVING
    // =====================================================

    @Transactional(readOnly = true)
    public List<ReceivingListResponse>
    getReceivingList() {

        Users staff =
                validationService
                        .getCurrentUser();

        if (staff.getWarehouse() == null) {

            throw new Forbidden(
                    "Warehouse Staff is not assigned "
                            + "to a warehouse"
            );
        }

        List<GoodsReceipts> receipts =
                goodsReceiptRepository
                        .findByWarehouseIdAndStatusInOrderByCreatedAtDesc(
                                staff.getWarehouse()
                                        .getId(),

                                List.of(
                                        GoodsReceiptStatus.CONFIRMED,
                                        GoodsReceiptStatus.RECEIVING
                                )
                        );

        return receipts.stream()
                .map(this::toListResponse)
                .toList();
    }


    // =====================================================
    // RECEIVING DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public ReceivingDetailResponse
    getReceivingDetail(
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
                .validateCanViewReceiving(
                        receipt
                );

        return buildResponse(
                receipt
        );
    }


    // =====================================================
    // BUILD DETAIL
    // =====================================================

    public ReceivingDetailResponse buildResponse(
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

        return receivingInspectionMapper
                .toDetailResponse(
                        receipt,
                        items,
                        inspections
                );
    }


    // =====================================================
    // LIST RESPONSE
    // =====================================================

    private ReceivingListResponse toListResponse(
            GoodsReceipts receipt
    ) {

        List<GoodsReceiptItems> items =
                goodsReceiptItemRepository
                        .findByGoodsReceiptId(
                                receipt.getId()
                        );

        int totalExpected =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems
                                        ::getExpectedQuantity
                        )
                        .sum();

        return ReceivingListResponse
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

                .purchaseOrderId(
                        receipt.getPurchaseOrder()
                                .getId()
                )

                .purchaseOrderCode(
                        receipt.getPurchaseOrder()
                                .getOrderCode()
                )

                .supplierId(
                        receipt.getPurchaseOrder()
                                .getSupplier()
                                .getId()
                )

                .supplierName(
                        receipt.getPurchaseOrder()
                                .getSupplier()
                                .getName()
                )

                .totalItems(
                        items.size()
                )

                .totalExpectedQuantity(
                        totalExpected
                )

                .build();
    }
}