package com.toystorage.backend.repository.warehouses.putaway;

import com.toystorage.backend.entity.warehouses.PutawayTasks;
import com.toystorage.backend.enums.warehouses.PutawayTaskStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PutawayTaskRepository
        extends JpaRepository<PutawayTasks, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from PutawayTasks e where e.id = :id")
    Optional<PutawayTasks> findByIdForUpdate(@Param("id") Long id);


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