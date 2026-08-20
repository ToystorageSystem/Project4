package com.toystorage.backend.repository.inventories;

import com.toystorage.backend.entity.inventories.StockCounts;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.inventories.StockCountStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StaffStockCountRepository
        extends JpaRepository<StockCounts, Long> {


    // =====================================================
    // AVAILABLE TASKS + MY TASKS
    // =====================================================

    @Query("""
        select s
        from StockCounts s
        where s.warehouse.id = :warehouseId
          and (
                s.assignedTo is null
                or s.assignedTo.id = :staffId
          )
          and s.status not in (
                :completedStatus,
                :cancelledStatus
          )
        order by s.scheduledDate asc, s.id asc
    """)
    List<StockCounts> findAvailableAndMine(
            @Param("warehouseId")
            Long warehouseId,

            @Param("staffId")
            Long staffId,

            @Param("completedStatus")
            StockCountStatus completedStatus,

            @Param("cancelledStatus")
            StockCountStatus cancelledStatus
    );


    // =====================================================
    // ATOMIC CLAIM + START
    //
    // Hai người cùng bấm:
    //
    // Staff A -> update = 1
    // Staff B -> update = 0
    //
    // Chỉ một người thắng.
    // =====================================================

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
        update StockCounts s
           set s.assignedTo = :staff,
               s.status = :newStatus,
               s.startedAt = :startedAt
         where s.id = :stockCountId
           and s.warehouse.id = :warehouseId
           and s.assignedTo is null
           and s.status = :currentStatus
    """)
    int claimAndStart(
            @Param("stockCountId")
            Long stockCountId,

            @Param("warehouseId")
            Long warehouseId,

            @Param("staff")
            Users staff,

            @Param("currentStatus")
            StockCountStatus currentStatus,

            @Param("newStatus")
            StockCountStatus newStatus,

            @Param("startedAt")
            LocalDateTime startedAt
    );
}