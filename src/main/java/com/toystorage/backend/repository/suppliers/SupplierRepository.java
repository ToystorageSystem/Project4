package com.toystorage.backend.repository.suppliers;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.toystorage.backend.entity.suppliers.Suppliers;
import com.toystorage.backend.enums.products.CommonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.util.List;

@Repository
public interface SupplierRepository
        extends JpaRepository<Suppliers, Long> {

    List<Suppliers> findByStatusOrderByNameAsc(
            CommonStatus status
    );

    java.util.Optional<Suppliers> findBySuppliersCodeIgnoreCase(String suppliersCode);

    List<Suppliers> findByNameIgnoreCase(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Suppliers s where s.id = :id")
    Optional<Suppliers> findByIdForProductCreation(@Param("id") Long id);


    // Methods restored for legacy business APIs without replacing existing repository logic.

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
}