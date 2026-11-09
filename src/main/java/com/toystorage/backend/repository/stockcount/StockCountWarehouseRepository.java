package com.toystorage.backend.repository.stockcount;

import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.warehouses.WarehouseType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockCountWarehouseRepository extends JpaRepository<Warehouses, Long> {
    List<Warehouses> findByType(WarehouseType type);
}
