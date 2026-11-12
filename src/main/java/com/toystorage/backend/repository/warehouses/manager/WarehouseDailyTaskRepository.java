package com.toystorage.backend.repository.warehouses.manager;

import com.toystorage.backend.entity.warehouses.WarehouseDailyTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WarehouseDailyTaskRepository extends JpaRepository<WarehouseDailyTask, Long> {

    @Query("""
        SELECT t FROM WarehouseDailyTask t
        JOIN FETCH t.assignedTo JOIN FETCH t.assignedBy
        WHERE t.warehouse.id = :warehouseId AND t.taskDate = :date
        ORDER BY t.assignedTo.name ASC, t.id ASC
        """)
    List<WarehouseDailyTask> findForWarehouseDay(@Param("warehouseId") Long warehouseId, @Param("date") LocalDate date);

    @Query("""
        SELECT t FROM WarehouseDailyTask t
        JOIN FETCH t.assignedTo JOIN FETCH t.assignedBy
        WHERE t.assignedTo.id = :staffId AND t.taskDate = :date
        ORDER BY t.id ASC
        """)
    List<WarehouseDailyTask> findForStaffDay(@Param("staffId") Long staffId, @Param("date") LocalDate date);

    @Query("""
        SELECT t FROM WarehouseDailyTask t
        JOIN FETCH t.assignedTo JOIN FETCH t.assignedBy
        WHERE t.warehouse.id = :warehouseId AND t.taskDate BETWEEN :from AND :to
        """)
    List<WarehouseDailyTask> findForWarehouseRange(@Param("warehouseId") Long warehouseId,
                                                   @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT t FROM WarehouseDailyTask t JOIN FETCH t.assignedTo JOIN FETCH t.assignedBy WHERE t.id = :id")
    Optional<WarehouseDailyTask> findDetail(@Param("id") Long id);

    long countByTaskDate(LocalDate taskDate);
}
