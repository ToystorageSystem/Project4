package com.toystorage.backend.repository.inventories;


import com.toystorage.backend.entity.inventories.StockCountItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockCountItemRepository
        extends JpaRepository<StockCountItems, Long> {

    List<StockCountItems>
    findByStockCountId(
            Long stockCountId
    );

    boolean existsByStockCountIdAndProductIdAndLocationId(
            Long stockCountId,
            Long productId,
            Long locationId
    );
}