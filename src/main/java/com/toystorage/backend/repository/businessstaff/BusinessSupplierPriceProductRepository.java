
package com.toystorage.backend.repository.businessstaff;

import com.toystorage.backend.entity.suppliers.SupplierProducts;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessSupplierPriceProductRepository
        extends JpaRepository<SupplierProducts, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sp
            FROM SupplierProducts sp
            WHERE sp.supplier.id = :supplierId
              AND sp.product.id = :productId
            """)
    Optional<SupplierProducts> findLockedBySupplierAndProduct(
            @Param("supplierId") Long supplierId,
            @Param("productId") Long productId
    );
}
