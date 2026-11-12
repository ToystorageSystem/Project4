package com.toystorage.backend.repository.stores;

import org.springframework.data.jpa.repository.JpaRepository;

import com.toystorage.backend.entity.stores.SalesOrders;

/** Writes the team's sales_orders rows that mirror store_sales (read by the analytics dashboards). */
public interface SalesOrderMirrorRepository extends JpaRepository<SalesOrders, Long> {
}
