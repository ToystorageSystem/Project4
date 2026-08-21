package com.toystorage.backend.repository.suppliers;

import com.toystorage.backend.entity.suppliers.SupplierProducts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.toystorage.backend.enums.products.CommonStatus;

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
    * Tìm đúng liên kết ACTIVE giữa Supplier và Product.
    * Purchase Order dùng method này để chặn sản phẩm
    * không thuộc nhà cung cấp đã chọn.
    */
   Optional<SupplierProducts>
   findBySupplier_IdAndProduct_IdAndStatus(
           Long supplierId,
           Long productId,
           CommonStatus status
   );


   /**
    * Lấy các sản phẩm ACTIVE đã liên kết với Supplier.
    */
   List<SupplierProducts>
   findBySupplier_IdAndStatusOrderByProduct_NameAsc(
           Long supplierId,
           CommonStatus status
   );

    /**
     * Tìm nhà cung cấp mặc định hiện tại của sản phẩm.
     */
    Optional<SupplierProducts>
    findByProduct_IdAndIsDefaultTrue(
            Long productId
    );
}