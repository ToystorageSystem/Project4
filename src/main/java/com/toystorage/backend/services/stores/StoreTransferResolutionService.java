package com.toystorage.backend.services.stores;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.receipts.StoreReceipts;
import com.toystorage.backend.entity.stores.StoreStockTransactions;
import com.toystorage.backend.entity.stores.StoreStocks;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.inventories.DiscrepancyReferenceType;
import com.toystorage.backend.enums.inventories.DiscrepancyStatus;
import com.toystorage.backend.enums.receipts.StoreReceiptStatus;
import com.toystorage.backend.enums.stores.StockTransactionType;
import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.repository.inventories.discrepancy.DiscrepancyReportRepository;
import com.toystorage.backend.repository.receipts.store.StoreReceiptRepository;
import com.toystorage.backend.repository.receipts.store.StoreStockRepository;
import com.toystorage.backend.repository.receipts.store.StoreStockTransactionRepository;
import com.toystorage.backend.repository.transfers.picking.StockTransferRepository;

import lombok.RequiredArgsConstructor;

/**
 * Store side of "Business resolves a store receiving discrepancy" (2026-11-13).
 * Called from BusinessManagerDiscrepancyService for STOCK_TRANSFER reports.
 */
@Service
@RequiredArgsConstructor
public class StoreTransferResolutionService {

    private final StoreStockRepository stockRepository;
    private final StoreStockTransactionRepository stockTransactionRepository;
    private final StoreInventorySyncService storeInventorySyncService;
    private final DiscrepancyReportRepository discrepancyReportRepository;
    private final StoreReceiptRepository storeReceiptRepository;
    private final StockTransferRepository stockTransferRepository;

    /**
     * Business changed the final received quantity of a product. If the store already added the
     * stock (receipt CONFIRMED), the backroom is corrected by the difference.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyFinalQuantityChange(StoreReceipts receipt, Products product, int delta, Users actor) {
        if (delta == 0 || receipt.getStatus() != StoreReceiptStatus.CONFIRMED) {
            return;
        }
        Long storeId = receipt.getStore().getId();
        StoreStocks stock = stockRepository.findByStoreIdAndProductId(storeId, product.getId())
                .orElseGet(() -> StoreStocks.newRow(storeId, product));
        try {
            stock.adjustBackroom(delta);
        } catch (IllegalStateException e) {
            throw new BadRequest("Store backroom does not have enough stock to reduce product "
                    + product.getProductsCode() + " by " + (-delta));
        }
        stockRepository.save(stock);
        storeInventorySyncService.sync(stock);

        stockTransactionRepository.save(StoreStockTransactions.builder()
                .code("STX-A" + receipt.getId() + "-" + product.getId() + "-"
                        + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .storeId(storeId)
                .product(product)
                .type(StockTransactionType.ADJUST)
                .shelfChange(0)
                .backroomChange(delta)
                .shelfAfter(stock.getShelfQuantity())
                .backroomAfter(stock.getBackroomQuantity())
                .storeReceipt(receipt)
                .note("Business resolved discrepancy of " + receipt.getStoreReceiptsCode())
                .createdBy(actor)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /** After a report is resolved: no open report left for the transfer -> transfer COMPLETED. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void completeTransferIfResolved(Long transferId) {
        boolean open = discrepancyReportRepository
                .findByReferenceTypeAndReferenceId(DiscrepancyReferenceType.STOCK_TRANSFER, transferId)
                .stream()
                .anyMatch(r -> r.getStatus() == DiscrepancyStatus.OPEN
                        || r.getStatus() == DiscrepancyStatus.INVESTIGATING);
        if (open) {
            return;
        }
        StockTransfer transfer = stockTransferRepository.findById(transferId).orElse(null);
        if (transfer == null || transfer.getStatus() != TransferStatus.RECEIVED) {
            return;
        }
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setUpdatedAt(LocalDateTime.now());

        List<StoreReceipts> receipts = storeReceiptRepository.findByStockTransfer_IdIn(List.of(transferId));
        for (StoreReceipts receipt : receipts) {
            if (receipt.getStatus() == StoreReceiptStatus.CONFIRMED) {
                receipt.setStatus(StoreReceiptStatus.COMPLETED);
            }
        }
    }
}
