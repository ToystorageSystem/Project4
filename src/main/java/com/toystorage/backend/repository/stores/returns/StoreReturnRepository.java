package com.toystorage.backend.repository.stores.returns;

import com.toystorage.backend.entity.stores.StoreReturns;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreReturnRepository
        extends JpaRepository<StoreReturns, Long> {

    List<StoreReturns>
    findByWarehouseIdOrderByCreatedAtDesc(
            Long warehouseId
    );
}