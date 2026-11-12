package com.toystorage.backend.repository.products;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.toystorage.backend.enums.products.ProductStatus;
import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.entity.products.Categories;
import com.toystorage.backend.entity.products.Brands;
import com.toystorage.backend.entity.products.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.Collection;



@Repository
public interface ProductRepository extends JpaRepository<Products, Long> {

    // Khoa dong Product khi tao lien ket, tranh hai request gan Supplier dong thoi.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Products p where p.id = :id")
    Optional<Products> findByIdForSupplierLinkUpdate(@Param("id") Long id);



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

    // Kiem tra trong ca lien ket NCC legacy (chua co products.supplier_id).
    @Query("""
            select case when count(sp) > 0 then true else false end
            from SupplierProducts sp
            where sp.supplier.id = :supplierId
              and sp.product.barcode = :barcode
              and (:exceptProductId is null or sp.product.id <> :exceptProductId)
            """)
    boolean existsLinkedBarcodeForSupplier(
            @Param("barcode") String barcode,
            @Param("supplierId") Long supplierId,
            @Param("exceptProductId") Long exceptProductId);

    boolean existsByBarcodeAndOwningSupplier_IdAndIdNot(
            String barcode, Long supplierId, Long id);
    boolean existsByBarcodeAndOwningSupplierIsNull(String barcode);

    java.util.List<Products> findAllByBarcodeAndStatus(
            String barcode, ProductStatus status);


    // Kiem tra mot trang thai
    boolean existsByCategory_IdAndStatus(
            Long categoryId,
            ProductStatus status
    );

    // Kiem tra nhieu trang thai
    boolean existsByBrand_IdAndStatusIn(
            Long brandId,
            java.util.Collection<ProductStatus> statuses
    );

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


        List<Products> findByIdInAndStatus(Collection<Long> ids, ProductStatus status);

    // Tim kiem san pham theo tu khoa, danh muc va trang thai.
    @Query("""
            SELECT p
            FROM Products p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (
                    :keyword IS NULL
                    OR LOWER(p.name) LIKE CONCAT('%', :keyword, '%')
                    OR LOWER(p.productsCode) LIKE CONCAT('%', :keyword, '%')
                    OR LOWER(p.barcode) LIKE CONCAT('%', :keyword, '%')
              )
            """)
    Page<Products> searchProducts(
            @Param("status") ProductStatus status,
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );
}