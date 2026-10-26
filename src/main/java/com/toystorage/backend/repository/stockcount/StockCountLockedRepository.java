package com.toystorage.backend.repository.stockcount;

import com.toystorage.backend.entity.inventories.StockCounts;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StockCountLockedRepository extends JpaRepository<StockCounts, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from StockCounts c where c.id = :id")
    Optional<StockCounts> lock(@Param("id") Long id);
    List<StockCounts> findByWarehouseIdOrderByIdDesc(Long warehouseId);
    boolean existsByWarehouseIdAndScheduledDateAndCountTypeIn(Long warehouseId, java.time.LocalDate date, java.util.Collection<com.toystorage.backend.enums.inventories.StockCountType> types);
    Optional<StockCounts> findByCountCode(String countCode);
}
