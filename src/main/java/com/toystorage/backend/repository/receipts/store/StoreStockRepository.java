package com.toystorage.backend.repository.stores;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.toystorage.backend.entity.stores.StoreStocks;

public interface StoreStockRepository extends JpaRepository<StoreStocks, Long> {

    long countByStoreId(Long storeId);

    /** stock rows of the products on ONE page (1 query, shelf included) */
    @EntityGraph(attributePaths = {"shelf"})
    List<StoreStocks> findByStoreIdAndProductIdIn(Long storeId, Collection<Long> productIds);

    /** stock row of one product (detail screen) */
    @EntityGraph(attributePaths = {"shelf"})
    Optional<StoreStocks> findByStoreIdAndProductId(Long storeId, Long productId);
}