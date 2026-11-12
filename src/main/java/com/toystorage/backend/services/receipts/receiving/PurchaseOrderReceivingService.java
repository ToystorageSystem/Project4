package com.toystorage.backend.services.receipts.receiving;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.PurchaseOrderItems;
import com.toystorage.backend.entity.receipts.PurchaseOrders;
import com.toystorage.backend.enums.receipts.PurchaseOrderStatus;
import com.toystorage.backend.repository.receipts.business.PurchaseOrderItemRepository;

import lombok.RequiredArgsConstructor;

/**
 * When a goods receipt is COMPLETED the purchase order learns what arrived (2026-11-13):
 * purchase_order_items.received_quantity += accepted quantity, then the PO becomes
 * COMPLETED (everything received) or PARTIALLY_RECEIVED.
 * Without this a PO stayed ORDERED and the same quantity could be received again.
 */
@Service
@RequiredArgsConstructor
public class PurchaseOrderReceivingService {

    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void applyCompletedReceipt(GoodsReceipts receipt, List<GoodsReceiptItems> receiptItems) {
        PurchaseOrders order = receipt.getPurchaseOrder();
        if (order == null) {
            return;
        }
        Map<Long, Integer> acceptedByProduct = receiptItems.stream()
                .collect(Collectors.toMap(i -> i.getProduct().getId(),
                        i -> i.getAcceptedQuantity() == null ? 0 : i.getAcceptedQuantity(),
                        Integer::sum));

        List<PurchaseOrderItems> orderItems = purchaseOrderItemRepository.findByPurchaseOrderId(order.getId());
        boolean allReceived = true;
        for (PurchaseOrderItems item : orderItems) {
            int received = item.getReceivedQuantity() == null ? 0 : item.getReceivedQuantity();
            received += acceptedByProduct.getOrDefault(item.getProduct().getId(), 0);
            item.setReceivedQuantity(received);
            int ordered = item.getOrderedQuantity() == null ? 0 : item.getOrderedQuantity();
            if (received < ordered) {
                allReceived = false;
            }
        }
        purchaseOrderItemRepository.saveAll(orderItems);

        order.setStatus(allReceived ? PurchaseOrderStatus.COMPLETED : PurchaseOrderStatus.PARTIALLY_RECEIVED);
        order.setUpdatedAt(LocalDateTime.now());
    }
}
