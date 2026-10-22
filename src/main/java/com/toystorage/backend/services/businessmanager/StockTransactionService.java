
package com.toystorage.backend.services.businessmanager;

import com.toystorage.backend.dto.response.businessmanager.StockTransactionResponse;
import com.toystorage.backend.dto.response.businessmanager.StockTransactionDetailResponse;
import com.toystorage.backend.dto.response.businessmanager.StockTransactionItemResponse;

import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.transfers.StockTransferItems;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;

import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.enums.transfers.TransferStatus;

import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptRepository;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptItemRepository;
import com.toystorage.backend.repository.transfers.picking.StockTransferRepository;
import com.toystorage.backend.repository.transfers.picking.StockTransferItemRepository;

import com.toystorage.backend.exceptions.NotFound;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockTransactionService {

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final StockTransferRepository stockTransferRepository;

    // Issue #21 - Repositories chi tiet san pham
    private final GoodsReceiptItemRepository goodsReceiptItemRepository;
    private final StockTransferItemRepository stockTransferItemRepository;

    // ==========================================
    // 1. DANH SACH PHIEU NHAP
    // ==========================================

    public Page<StockTransactionResponse> getImportTransactions(
            String keyword,
            GoodsReceiptStatus status,
            Long warehouseId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    ) {
        validateDates(fromDate, toDate);

        return goodsReceiptRepository.searchForBusinessManager(
                normalizeKeyword(keyword),
                status,
                warehouseId,
                fromDate,
                toDate,
                pageable
        ).map(this::mapImport);
    }

    // ==========================================
    // 2. DANH SACH PHIEU XUAT
    // ==========================================

    public Page<StockTransactionResponse> getExportTransactions(
            String keyword,
            TransferStatus status,
            Long warehouseId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    ) {
        validateDates(fromDate, toDate);

        return stockTransferRepository.searchForBusinessManager(
                normalizeKeyword(keyword),
                status,
                warehouseId,
                fromDate,
                toDate,
                pageable
        ).map(this::mapExport);
    }

    // ==========================================
    // 3. CHI TIET CU - GIU DE TUONG THICH
    // ==========================================

    public StockTransactionResponse getImportTransaction(Long id) {
        GoodsReceipts receipt = goodsReceiptRepository.findById(id)
                .orElseThrow(() ->
                        new NotFound("Goods receipt not found: " + id)
                );

        return mapImport(receipt);
    }

    public StockTransactionResponse getExportTransaction(Long id) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() ->
                        new NotFound("Stock transfer not found: " + id)
                );

        return mapExport(transfer);
    }

    // ==========================================
    // 4. CHI TIET PHIEU NHAP - ISSUE #21
    // ==========================================

    public StockTransactionDetailResponse getImportTransactionDetail(
            Long id
    ) {
        GoodsReceipts receipt = goodsReceiptRepository.findById(id)
                .orElseThrow(() ->
                        new NotFound("Goods receipt not found: " + id)
                );

        // Lay thong tin chung cua phieu
        StockTransactionResponse summary = mapImport(receipt);

        // Lay cac dong san pham kem thong tin Products
        List<StockTransactionItemResponse> items =
                goodsReceiptItemRepository
                        .findDetailsByGoodsReceiptId(id)
                        .stream()
                        .map(this::mapImportItem)
                        .toList();

        return buildDetailResponse(summary, items);
    }

    // ==========================================
    // 5. CHI TIET PHIEU XUAT - ISSUE #21
    // ==========================================

    public StockTransactionDetailResponse getExportTransactionDetail(
            Long id
    ) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() ->
                        new NotFound("Stock transfer not found: " + id)
                );

        // Lay thong tin chung cua phieu
        StockTransactionResponse summary = mapExport(transfer);

        // Lay cac dong san pham kem thong tin Products
        List<StockTransactionItemResponse> items =
                stockTransferItemRepository
                        .findDetailsByStockTransferId(id)
                        .stream()
                        .map(this::mapExportItem)
                        .toList();

        return buildDetailResponse(summary, items);
    }

    // ==========================================
    // 6. MAP THONG TIN CHUNG PHIEU NHAP
    // ==========================================

    private StockTransactionResponse mapImport(
            GoodsReceipts receipt
    ) {
        Warehouses warehouse = receipt.getWarehouse();
        Users operator = receipt.getReceivedBy();

        return StockTransactionResponse.builder()
                .id(receipt.getId())
                .type("IMPORT")
                .transactionCode(receipt.getReceiptCode())
                .status(receipt.getStatus() == null
                        ? null : receipt.getStatus().name())
                .warehouseId(warehouse == null
                        ? null : warehouse.getId())
                .warehouseName(warehouse == null
                        ? null : warehouse.getName())
                .performedById(operator == null
                        ? null : operator.getId())
                .performedByName(operator == null
                        ? null : operator.getName())
                .createdAt(receipt.getCreatedAt())
                .build();
    }

    // ==========================================
    // 7. MAP THONG TIN CHUNG PHIEU XUAT
    // ==========================================

    private StockTransactionResponse mapExport(
            StockTransfer transfer
    ) {
        Warehouses source = transfer.getFromWarehouse();
        Warehouses destination = transfer.getToWarehouse();
        Users operator = transfer.getCreatedBy();

        return StockTransactionResponse.builder()
                .id(transfer.getId())
                .type("EXPORT")
                .transactionCode(transfer.getTransferCode())
                .status(transfer.getStatus() == null
                        ? null : transfer.getStatus().name())
                .warehouseId(source == null
                        ? null : source.getId())
                .warehouseName(source == null
                        ? null : source.getName())
                .destinationWarehouseId(destination == null
                        ? null : destination.getId())
                .destinationWarehouseName(destination == null
                        ? null : destination.getName())
                .performedById(operator == null
                        ? null : operator.getId())
                .performedByName(operator == null
                        ? null : operator.getName())
                .createdAt(transfer.getCreatedAt())
                .build();
    }

    // ==========================================
    // 8. MAP SAN PHAM TRONG PHIEU NHAP
    // ==========================================

    private StockTransactionItemResponse mapImportItem(
            GoodsReceiptItems item
    ) {
        Products product = item.getProduct();

        return StockTransactionItemResponse.builder()
                .productId(product.getId())
                .productCode(product.getProductsCode())
                .productName(product.getName())
                .expectedQuantity(item.getExpectedQuantity())
                .actualQuantity(item.getActualQuantity())
                .acceptedQuantity(item.getAcceptedQuantity())
                .damagedQuantity(item.getDamagedQuantity())
                .shortageQuantity(item.getShortageQuantity())
                .surplusQuantity(item.getSurplusQuantity())
                .build();
    }

    // ==========================================
    // 9. MAP SAN PHAM TRONG PHIEU XUAT
    // ==========================================

    private StockTransactionItemResponse mapExportItem(
            StockTransferItems item
    ) {
        Products product = item.getProduct();

        return StockTransactionItemResponse.builder()
                .productId(product.getId())
                .productCode(product.getProductsCode())
                .productName(product.getName())
                .requestedQuantity(item.getRequestedQuantity())
                .approvedQuantity(item.getApprovedQuantity())
                .pickedQuantity(item.getPickedQuantity())
                .packedQuantity(item.getPackedQuantity())
                .shippedQuantity(item.getShippedQuantity())
                .receivedQuantity(item.getReceivedQuantity())
                .shortageQuantity(item.getShortageQuantity())
                .surplusQuantity(item.getSurplusQuantity())
                .build();
    }

    // ==========================================
    // 10. TAO RESPONSE CHI TIET
    // ==========================================

    private StockTransactionDetailResponse buildDetailResponse(
            StockTransactionResponse summary,
            List<StockTransactionItemResponse> items
    ) {
        return StockTransactionDetailResponse.builder()
                .id(summary.getId())
                .type(summary.getType())
                .transactionCode(summary.getTransactionCode())
                .status(summary.getStatus())
                .warehouseId(summary.getWarehouseId())
                .warehouseName(summary.getWarehouseName())
                .destinationWarehouseId(
                        summary.getDestinationWarehouseId()
                )
                .destinationWarehouseName(
                        summary.getDestinationWarehouseName()
                )
                .performedById(summary.getPerformedById())
                .performedByName(summary.getPerformedByName())
                .createdAt(summary.getCreatedAt())
                .items(items)
                .build();
    }

    // ==========================================
    // 11. CAC HAM HO TRO
    // ==========================================

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private void validateDates(
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        if (fromDate != null && toDate != null
                && !fromDate.isBefore(toDate)) {
            throw new IllegalArgumentException(
                    "fromDate must be earlier than toDate"
            );
        }
    }
}
