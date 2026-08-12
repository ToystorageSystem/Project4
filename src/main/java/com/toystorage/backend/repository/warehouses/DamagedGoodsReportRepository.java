package com.toystorage.backend.repository.warehouses;


import com.toystorage.backend.entity.warehouses.DamagedGoodsReports;
import com.toystorage.backend.enums.warehouses.DamagedGoodsStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DamagedGoodsReportRepository
        extends JpaRepository<DamagedGoodsReports, Long> {

    List<DamagedGoodsReports>
    findByWarehouseIdAndStatusInOrderByCreatedAtDesc(
            Long warehouseId,
            List<DamagedGoodsStatus> statuses
    );
}