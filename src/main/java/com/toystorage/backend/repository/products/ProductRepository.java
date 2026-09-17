package com.toystorage.backend.repository.products;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.enums.products.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Products, Long> {

    boolean existsByCategory_IdAndStatus(
            Long categoryId,
            ProductStatus status
    );
}