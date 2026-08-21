package com.toystorage.backend.repository.suppliers;

import com.toystorage.backend.entity.suppliers.SupplierProducts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierProductRepository
        extends JpaRepository<SupplierProducts, Long> {

    /**
     * Kiểm tra Supplier và Product đã được liên kết hay chưa.
     */
    boolean existsBySupplier_IdAndProduct_Id(
            Long supplierId,
            Long productId
    );

    /**
     * Lấy danh sách nhà cung cấp của một sản phẩm.
     * Nhà cung cấp mặc định nằm trên cùng.
     */
    List<SupplierProducts>
    findByProduct_IdOrderByIsDefaultDescSupplier_NameAsc(
            Long productId
    );

    /**
     * Lấy danh sách sản phẩm của một nhà cung cấp.
     */
    List<SupplierProducts>
    findBySupplier_IdOrderByProduct_NameAsc(
            Long supplierId
    );

    /**
     * Tìm nhà cung cấp mặc định hiện tại của sản phẩm.
     */
    Optional<SupplierProducts>
    findByProduct_IdAndIsDefaultTrue(
            Long productId
    );
}