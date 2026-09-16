package com.toystorage.backend.repository.warehouses.damagedgoods;

import com.toystorage.backend.entity.warehouses.DamagedGoodsHistory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface DamagedGoodsHistoryRepository
        extends JpaRepository<DamagedGoodsHistory, Long> {

    List<DamagedGoodsHistory>
    findByReportIdOrderByCreatedAtAsc(
            Long reportId
    );
}