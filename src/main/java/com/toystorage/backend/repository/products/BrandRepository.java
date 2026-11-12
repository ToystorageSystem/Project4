package com.toystorage.backend.repository.products;

import com.toystorage.backend.entity.products.Brands;
import com.toystorage.backend.enums.products.CommonStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository
        extends JpaRepository<Brands, Long> {

    Optional<Brands> findByIdAndStatus(
            Long id,
            CommonStatus status
    );

    boolean existsByBrandsCodeIgnoreCase(String brandsCode);

    boolean existsByBrandsCodeIgnoreCaseAndIdNot(String brandsCode, Long brandId);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long brandId);

    List<Brands> findByStatusOrderByNameAsc(CommonStatus status);

    @Query("""
            SELECT b
            FROM Brands b
            WHERE (:status IS NULL OR b.status = :status)
              AND (
                    :keyword IS NULL
                    OR LOWER(b.brandsCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(b.country) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<Brands> search(
            @Param("keyword") String keyword,
            @Param("status") CommonStatus status,
            Pageable pageable
    );
}
