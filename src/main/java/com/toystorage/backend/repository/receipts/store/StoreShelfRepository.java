package com.toystorage.backend.repository.receipts.store;

import org.springframework.data.jpa.repository.JpaRepository;

import com.toystorage.backend.entity.stores.StoreShelves;
import com.toystorage.backend.enums.stores.ShelfStatus;

import java.util.List;
import java.util.Optional;

public interface StoreShelfRepository extends JpaRepository<StoreShelves, Long> {

    long countByStoreId(Long storeId);

            /** shelves of a store, e.g. status = ACTIVE, sorted by name */
    List<StoreShelves> findByStoreIdAndStatusOrderByNameAsc(Long storeId, ShelfStatus status);

    /** a shelf only if it belongs to this store (other store -> empty -> 404) */
    Optional<StoreShelves> findByIdAndStoreId(Long id, Long storeId);
}

