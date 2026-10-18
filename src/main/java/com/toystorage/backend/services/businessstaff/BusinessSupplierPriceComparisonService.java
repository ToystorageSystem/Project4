
package com.toystorage.backend.services.businessstaff;

import com.toystorage.backend.dto.response.businessstaff.SupplierPriceComparisonResponse;
import com.toystorage.backend.entity.receipts.PurchaseOrderItems;
import com.toystorage.backend.entity.receipts.PurchaseOrders;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.repository.businessstaff.BusinessPurchaseOrderRepository;
import com.toystorage.backend.repository.businessstaff.BusinessPurchaseOrderItemRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessSupplierPriceComparisonService {

    private final BusinessPurchaseOrderRepository orderRepository;

    private final BusinessPurchaseOrderItemRepository itemRepository;

    @Transactional(readOnly = true)
    public SupplierPriceComparisonResponse getPriceComparison(
            Long purchaseOrderId
    ) {

        PurchaseOrders order = orderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new NotFound(
                        "Purchase Order not found: " + purchaseOrderId
                ));

        List<PurchaseOrderItems> orderItems =
                itemRepository.findByPurchaseOrder_IdOrderByIdAsc(
                        purchaseOrderId
                );

        List<SupplierPriceComparisonResponse.ItemResponse> results =
                orderItems.stream()
                        .map(this::toItemResponse)
                        .toList();

        return new SupplierPriceComparisonResponse(
                order.getId(),
                order.getOrderCode(),
                order.getSupplier().getId(),
                order.getSupplier().getName(),
                results
        );
    }

    private SupplierPriceComparisonResponse.ItemResponse toItemResponse(
            PurchaseOrderItems item
    ) {

        BigDecimal originalPrice = item.getUnitPrice();

        BigDecimal confirmedPrice = item.getConfirmedUnitPrice();

        BigDecimal difference = null;

        BigDecimal differencePercent = null;

        BigDecimal estimatedOrderDifference = null;

        String priceChange = "NOT_QUOTED";

        if (confirmedPrice != null && originalPrice != null) {

            difference = confirmedPrice.subtract(originalPrice);

            int comparison = confirmedPrice.compareTo(originalPrice);

            if (comparison > 0) {
                priceChange = "INCREASED";
            } else if (comparison < 0) {
                priceChange = "DECREASED";
            } else {
                priceChange = "UNCHANGED";
            }

            if (originalPrice.compareTo(BigDecimal.ZERO) != 0) {
                differencePercent = difference
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                originalPrice,
                                2,
                                RoundingMode.HALF_UP
                        );
            }

            estimatedOrderDifference = difference.multiply(
                    BigDecimal.valueOf(item.getOrderedQuantity())
            );
        }

        return new SupplierPriceComparisonResponse.ItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getSupplierResponseStatus(),
                item.getOrderedQuantity(),
                item.getReceivedQuantity(),
                item.getConfirmedQuantity(),
                originalPrice,
                confirmedPrice,
                difference,
                differencePercent,
                estimatedOrderDifference,
                priceChange
        );
    }
}
