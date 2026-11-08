package com.toystorage.backend.repository.products;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.enums.products.ProductStatus;

@Repository
public interface ProductRepository extends JpaRepository<Products, Long> {

    // ===== Store module (read only) =====

    /** keyword / categoryId = null -> filter skipped. utf8mb4_unicode_ci -> case-insensitive LIKE. */
    @EntityGraph(attributePaths = {"category", "brand"})
    @Query(value = """
            SELECT p FROM Products p
            WHERE p.status = :status
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:keyword IS NULL
                   OR p.name LIKE CONCAT('%', :keyword, '%')
                   OR p.productsCode LIKE CONCAT('%', :keyword, '%'))
            ORDER BY p.name ASC, p.id ASC
            """,
           countQuery = """
            SELECT COUNT(p) FROM Products p
            WHERE p.status = :status
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:keyword IS NULL
                   OR p.name LIKE CONCAT('%', :keyword, '%')
                   OR p.productsCode LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<Products> searchActive(@Param("status") ProductStatus status,
                                @Param("keyword") String keyword,
                                @Param("categoryId") Long categoryId,
                                Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand"})
    Optional<Products> findByIdAndStatus(Long id, ProductStatus status);

    @EntityGraph(attributePaths = {"category", "brand"})
    Optional<Products> findByBarcodeAndStatus(String barcode, ProductStatus status);
}