package com.toystorage.backend.repository.stores;

import org.springframework.data.jpa.repository.JpaRepository;

import com.toystorage.backend.entity.stores.StoreShelves;

public interface StoreShelfRepository extends JpaRepository<StoreShelves, Long> {

    long countByStoreId(Long storeId);
}