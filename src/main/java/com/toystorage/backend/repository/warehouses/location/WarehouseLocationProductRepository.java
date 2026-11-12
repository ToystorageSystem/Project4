package com.toystorage.backend.repository.warehouses.location;

import com.toystorage.backend.entity.warehouses.WarehouseLocationProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WarehouseLocationProductRepository extends JpaRepository<WarehouseLocationProduct, Long> {

    @Query("""
        SELECT lp FROM WarehouseLocationProduct lp JOIN FETCH lp.product JOIN FETCH lp.location l
        WHERE l.warehouse.id = :warehouseId
        ORDER BY l.warehouseCode ASC, lp.product.name ASC
        """)
    List<WarehouseLocationProduct> findForWarehouse(@Param("warehouseId") Long warehouseId);

    @Query("""
        SELECT lp FROM WarehouseLocationProduct lp JOIN FETCH lp.product
        WHERE lp.location.id = :locationId ORDER BY lp.product.name ASC
        """)
    List<WarehouseLocationProduct> findForLocation(@Param("locationId") Long locationId);

    @Modifying
    @Query("DELETE FROM WarehouseLocationProduct lp WHERE lp.location.id = :locationId")
    void deleteForLocation(@Param("locationId") Long locationId);
}
