package com.toystorage.backend.repository.stores;

import com.toystorage.backend.entity.stores.StoreReturnItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreReturnItemRepository
        extends JpaRepository<StoreReturnItems, Long> {

    List<StoreReturnItems>
    findByStoreReturnId(
            Long storeReturnId
    );
}