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

    boolean existsByCategory_IdAndStatus(
                Long categoryId,
                ProductStatus status
        );
}