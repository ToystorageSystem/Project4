package com.toystorage.backend.repository.stores;

import com.toystorage.backend.entity.stores.StoreReturns;
import com.toystorage.backend.enums.stores.StoreReturnStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WarehouseReturnRepository
        extends JpaRepository<StoreReturns, Long> {

    List<StoreReturns>
    findByWarehouseIdAndStatusInOrderByUpdatedAtDesc(
            Long warehouseId,
            Collection<StoreReturnStatus> statuses
    );
}