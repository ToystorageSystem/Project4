package com.toystorage.backend.repository.suppliers;

import com.toystorage.backend.entity.suppliers.Suppliers;
import com.toystorage.backend.enums.products.CommonStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository
        extends JpaRepository<Suppliers, Long> {

    boolean existsByEmailIgnoreCase(
            String email
    );

    boolean existsByEmailIgnoreCaseAndIdNot(
            String email,
            Long supplierId
    );

    boolean existsByTaxCodeIgnoreCase(
            String taxCode
    );

    boolean existsByTaxCodeIgnoreCaseAndIdNot(
            String taxCode,
            Long supplierId
    );

    @Query("""
            SELECT supplier
            FROM Suppliers supplier
            WHERE (:status IS NULL OR supplier.status = :status)
              AND (
                    :keyword IS NULL
                    OR LOWER(supplier.suppliersCode)
                        LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(supplier.name)
                        LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR supplier.phone
                        LIKE CONCAT('%', :keyword, '%')
              )
            """)
    Page<Suppliers> search(
            @Param("keyword")
            String keyword,

            @Param("status")
            CommonStatus status,

            Pageable pageable
    );
}