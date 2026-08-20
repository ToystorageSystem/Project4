package com.toystorage.backend.repository.inventories;

import com.toystorage.backend.entity.inventories.StockCountItems;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffStockCountItemRepository
        extends JpaRepository<StockCountItems, Long> {


    List<StockCountItems>
    findByStockCountIdOrderByIdAsc(
            Long stockCountId
    );


    @Query("""
        select i
        from StockCountItems i
        join i.product p
        join i.location l
        where i.stockCount.id = :stockCountId
          and p.barcode = :barcode
          and l.warehouseCode = :locationCode
    """)
    Optional<StockCountItems>
    findItemForCounting(
            @Param("stockCountId")
            Long stockCountId,

            @Param("barcode")
            String barcode,

            @Param("locationCode")
            String locationCode
    );
}