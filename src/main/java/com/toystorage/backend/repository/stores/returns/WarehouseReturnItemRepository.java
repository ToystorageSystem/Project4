package com.toystorage.backend.repository.stores.returns;

import com.toystorage.backend.entity.stores.StoreReturnItems;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WarehouseReturnItemRepository
        extends JpaRepository<StoreReturnItems, Long> {

    List<StoreReturnItems>
    findByStoreReturnId(
            Long returnId
    );


    @Query("""
        select i
        from StoreReturnItems i
        join i.product p
        where i.storeReturn.id = :returnId
          and p.barcode = :barcode
    """)
    Optional<StoreReturnItems>
    findByReturnIdAndProductBarcode(
            @Param("returnId")
            Long returnId,

            @Param("barcode")
            String barcode
    );
}