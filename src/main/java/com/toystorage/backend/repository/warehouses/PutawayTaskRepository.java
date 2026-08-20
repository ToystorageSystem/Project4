package com.toystorage.backend.repository.warehouses;

import com.toystorage.backend.entity.warehouses.PutawayTasks;
import com.toystorage.backend.enums.warehouses.PutawayTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PutawayTaskRepository
        extends JpaRepository<PutawayTasks, Long> {

    boolean existsByGoodsReceiptId(Long goodsReceiptId);
    Optional<PutawayTasks>
    findByGoodsReceiptId(Long goodsReceiptId);

    List<PutawayTasks>
    findByWarehouseIdAndStatusInOrderByCreatedAtDesc(
            Long warehouseId,
            List<PutawayTaskStatus> statuses
    );
    List<PutawayTasks>
    findByAssignedToIdAndStatusInOrderByCreatedAtDesc(
            Long staffId,
            List<PutawayTaskStatus> statuses
    );
    
    Optional<PutawayTasks>
    findByIdAndAssignedToId(
            Long id,
            Long assignedToId
    );
}