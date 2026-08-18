package com.toystorage.backend.repository.products;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.enums.products.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository
        extends JpaRepository<Products, Long> {

    boolean existsByProductsCodeIgnoreCase(
            String productsCode
    );

    boolean existsByBarcode(
            String barcode
    );

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
}