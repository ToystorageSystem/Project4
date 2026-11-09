package com.toystorage.backend.repository.warehouses.damagedgoods;

import com.toystorage.backend.entity.warehouses.DamagedGoodsItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DamagedGoodsItemRepository
        extends JpaRepository<DamagedGoodsItems, Long> {

    List<DamagedGoodsItems>
    findByDamagedGoodsReportId(Long reportId);

}