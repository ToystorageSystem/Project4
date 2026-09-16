package com.toystorage.backend.repository.products;

import com.toystorage.backend.entity.suppliers.SupplierProducts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface SupplierProductRepository
        extends JpaRepository<SupplierProducts, Long> {

    List<SupplierProducts>
    findByProductId(
            Long productId
    );
}