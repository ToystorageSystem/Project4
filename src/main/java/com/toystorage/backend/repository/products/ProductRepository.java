package com.toystorage.backend.repository.products;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.toystorage.backend.enums.products.ProductStatus;
import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.entity.products.Categories;
import com.toystorage.backend.entity.products.Brands;
import com.toystorage.backend.entity.products.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface ProductRepository extends JpaRepository<Products, Long> {


    // Methods restored for legacy business APIs without replacing existing repository logic.

    // =====================================================
        // INVENTORY REPORT - CATEGORY OPTIONS
        // =====================================================

        @Query("""
                select distinct c
                from Products p
                join p.category c
                where c.status = :status
                order by c.name asc
                """)
        List<Categories> findInventoryReportCategories(
                @Param("status")
                CommonStatus status
        );

    // =====================================================
        // INVENTORY REPORT - BRAND OPTIONS
        // =====================================================

        @Query("""
                select distinct b
                from Products p
                join p.brand b
                where b.status = :status
                order by b.name asc
                """)
        List<Brands> findInventoryReportBrands(
                @Param("status")
                CommonStatus status
        );

    // =====================================================
        // PRODUCT APPROVAL - DUPLICATE VALIDATION
        // =====================================================

        boolean existsByProductsCodeIgnoreCaseAndIdNot(
                String productsCode,
                Long id
        );

    boolean existsByBarcodeAndIdNot(
                String barcode,
                Long id
        );

    Page<Products> findByStatus(
                ProductStatus status,
                Pageable pageable
        );

    boolean existsByProductsCodeIgnoreCase(
                String productsCode
        );

    boolean existsByBarcode(
                String barcode
        );


    // Kiem tra mot trang thai
    boolean existsByCategory_IdAndStatus(
            Long categoryId,
            ProductStatus status
    );

    // Kiem tra nhieu trang thai
    boolean existsByCategory_IdAndStatusIn(
            Long categoryId,
            java.util.Collection<ProductStatus> statuses
    );
    @Query("""
        select p
        from Products p
        left join fetch p.category c
        left join fetch p.brand b
        where p.status = :status
          and (:keyword is null
               or lower(p.name) like lower(concat('%', :keyword, '%'))
               or lower(p.productsCode) like lower(concat('%', :keyword, '%'))
               or lower(p.barcode) like lower(concat('%', :keyword, '%')))
          and (:categoryId is null or c.id = :categoryId)
        """)
    Page<Products> searchActive(
            @Param("status") ProductStatus status,
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );
    Optional<Products> findByBarcodeAndStatus(
            String barcode,
            ProductStatus status
    );
    Optional<Products> findByIdAndStatus(
            Long id,
            ProductStatus status
    );
    
    // Tim kiem va loc san pham cho Business Operations.
    // Khong thay doi cac phuong thuc hien tai cua role khac.
    @Query(
        value = """
            SELECT p FROM Products p
            LEFT JOIN p.category c
            WHERE (:status IS NULL OR p.status = :status)
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (
                  :keyword IS NULL
                  OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(p.productsCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(p.barcode) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """,
        countQuery = """
            SELECT COUNT(p) FROM Products p
            LEFT JOIN p.category c
            WHERE (:status IS NULL OR p.status = :status)
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (
                  :keyword IS NULL
                  OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(p.productsCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(p.barcode) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """
    )
    Page<Products> findByBusinessFilters(
            @Param("status") ProductStatus status,
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

}