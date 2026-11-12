package com.toystorage.backend.repository.stores;

import org.springframework.data.jpa.repository.JpaRepository;

import com.toystorage.backend.entity.stores.SalesOrderItems;

/** Writes the sales_order_items rows that mirror store_sale_items. */
public interface SalesOrderItemMirrorRepository extends JpaRepository<SalesOrderItems, Long> {
}
