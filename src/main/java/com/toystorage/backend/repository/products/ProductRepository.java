package com.toystorage.backend.repository.products;

import com.toystorage.backend.entity.products.Brands;
import com.toystorage.backend.entity.products.Categories;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.enums.products.CommonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository
        extends JpaRepository<Products, Long> {

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
}