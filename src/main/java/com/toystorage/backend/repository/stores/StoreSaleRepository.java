package com.toystorage.backend.repository.stores;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.toystorage.backend.entity.stores.StoreSales;

public interface StoreSaleRepository extends JpaRepository<StoreSales, Long> {

    /** sale history of a store, newest first (+ seller name, no N+1) */
    @EntityGraph(attributePaths = {"createdBy"})
    Page<StoreSales> findByStoreIdOrderByCreatedAtDescIdDesc(Long storeId, Pageable pageable);

    /** sale detail with lines + products in 1 query; other store -> empty -> 404 */
    @EntityGraph(attributePaths = {"createdBy", "items", "items.product"})
    Optional<StoreSales> findByIdAndStoreId(Long id, Long storeId);
}