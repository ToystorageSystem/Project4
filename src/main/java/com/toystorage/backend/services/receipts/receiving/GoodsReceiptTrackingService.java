package com.toystorage.backend.services.receipts.receiving;

import com.toystorage.backend.dto.response.receipts.GoodsReceiptSummaryResponse;
import com.toystorage.backend.dto.response.receipts.GoodsReceiptTrackingPageResponse;
import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.PurchaseOrders;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptItemRepository;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoodsReceiptTrackingService {

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final GoodsReceiptItemRepository goodsReceiptItemRepository;

    // Chi doc: theo doi phieu nhap tu PO, khong thay doi ton kho.
    @Transactional(readOnly = true)
    public GoodsReceiptTrackingPageResponse search(
            String keyword,
            Long purchaseOrderId,
            Long warehouseId,
            GoodsReceiptStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer size
    ) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequest("From date must be before or equal to to date");
        }
        if (purchaseOrderId != null && purchaseOrderId <= 0) {
            throw new BadRequest("Invalid purchase order id");
        }
        if (warehouseId != null && warehouseId <= 0) {
            throw new BadRequest("Invalid warehouse id");
        }
        if (page != null && page < 0) {
            throw new BadRequest("Page must not be negative");
        }
        if (size != null && (size < 1 || size > 100)) {
            throw new BadRequest("Size must be between 1 and 100");
        }

        int safePage = page == null ? 0 : page;
        int safeSize = size == null ? 10 : size;
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        LocalDateTime start = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime endExclusive = toDate == null ? null : toDate.plusDays(1).atStartOfDay();

        Page<GoodsReceipts> result = goodsReceiptRepository.searchForBusinessStaff(
                normalizedKeyword, purchaseOrderId, warehouseId, status,
                start, endExclusive,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<GoodsReceiptSummaryResponse> items = result.getContent().stream()
                .map(this::toSummary)
                .toList();

        return GoodsReceiptTrackingPageResponse.builder()
                .items(items)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    private GoodsReceiptSummaryResponse toSummary(GoodsReceipts receipt) {
        PurchaseOrders order = receipt.getPurchaseOrder();
        List<GoodsReceiptItems> lines = goodsReceiptItemRepository.findByGoodsReceiptId(receipt.getId());
        int expectedQuantity = lines.stream()
                .mapToInt(line -> line.getExpectedQuantity() == null ? 0 : line.getExpectedQuantity())
                .sum();

        return GoodsReceiptSummaryResponse.builder()
                .id(receipt.getId())
                .receiptCode(receipt.getReceiptCode())
                .purchaseOrderId(order.getId())
                .purchaseOrderCode(order.getOrderCode())
                // Nguoi tao PO, khong phai nguoi tao phieu nhap.
                .purchaseOrderCreatedById(order.getCreatedBy().getId())
                .purchaseOrderCreatedByName(order.getCreatedBy().getName())
                .status(receipt.getStatus().name())
                .supplierId(order.getSupplier().getId())
                .supplierName(order.getSupplier().getName())
                .warehouseId(receipt.getWarehouse().getId())
                .warehouseName(receipt.getWarehouse().getName())
                .totalProducts(lines.size())
                .totalExpectedQuantity(expectedQuantity)
                .createdAt(receipt.getCreatedAt())
                .build();
    }
}
