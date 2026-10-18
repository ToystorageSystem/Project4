
package com.toystorage.backend.repository.products;

import com.toystorage.backend.entity.suppliers.SupplierProducts;
import com.toystorage.backend.enums.products.CommonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SupplierProductRepository
        extends JpaRepository<SupplierProducts, Long> {

    List<SupplierProducts> findByProductId(
            Long productId
    );

    @EntityGraph(attributePaths = "product")
    List<SupplierProducts> findBySupplier_IdAndStatusOrderByIdAsc(
            Long supplierId,
            CommonStatus status
    );
}
