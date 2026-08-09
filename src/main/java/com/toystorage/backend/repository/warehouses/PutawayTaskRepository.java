package com.toystorage.backend.repository.warehouses;

import com.toystorage.backend.entity.warehouses.PutawayTasks;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PutawayTaskRepository
        extends JpaRepository<PutawayTasks, Long> {

    boolean existsByGoodsReceiptId(Long goodsReceiptId);
}