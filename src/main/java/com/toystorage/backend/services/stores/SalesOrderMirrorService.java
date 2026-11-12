package com.toystorage.backend.services.stores;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.toystorage.backend.entity.stores.SalesOrderItems;
import com.toystorage.backend.entity.stores.SalesOrders;
import com.toystorage.backend.entity.stores.StoreSaleItems;
import com.toystorage.backend.entity.stores.StoreSales;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.stores.PaymentMethod;
import com.toystorage.backend.enums.stores.SalesOrderStatus;
import com.toystorage.backend.repository.stores.SalesOrderItemMirrorRepository;
import com.toystorage.backend.repository.stores.SalesOrderMirrorRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * Store sales are recorded in store_sales (store module), but the team's analytics
 * (Business dashboard revenue, forecasts) read sales_orders. Every store sale is therefore
 * copied to one COMPLETED sales_orders row + its items, in the SAME transaction as the sale.
 * Decision 2026-11-13 (claude/system-flow-decisions.md).
 */
@Service
@RequiredArgsConstructor
public class SalesOrderMirrorService {

    private final SalesOrderMirrorRepository orderRepository;
    private final SalesOrderItemMirrorRepository itemRepository;
    private final EntityManager entityManager;

    /** Call after the StoreSales row is saved (needs sale id). Joins the caller's transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public SalesOrders mirror(StoreSales sale, Users seller) {
        Warehouses store = entityManager.getReference(Warehouses.class, sale.getStoreId());

        SalesOrders order = orderRepository.save(SalesOrders.builder()
                .salesOrdersCode("SO-" + sale.getCode())
                .store(store)
                .totalAmount(sale.getTotalAmount())
                .paymentMethod(PaymentMethod.CASH)          // simulated sale: no payment step
                .status(SalesOrderStatus.COMPLETED)
                .createdBy(seller)
                .createdAt(sale.getCreatedAt())
                .updatedAt(sale.getCreatedAt())
                .build());

        List<SalesOrderItems> items = new ArrayList<>();
        for (StoreSaleItems line : sale.getItems()) {
            items.add(SalesOrderItems.builder()
                    .salesOrderItemsCode("SOI-" + sale.getId() + "-" + line.getProduct().getId())
                    .salesOrder(order)
                    .product(line.getProduct())
                    .quantity(line.getQuantity())
                    .unitPrice(line.getUnitPrice())
                    .subtotal(line.getLineTotal())
                    .build());
        }
        itemRepository.saveAll(items);
        return order;
    }
}
