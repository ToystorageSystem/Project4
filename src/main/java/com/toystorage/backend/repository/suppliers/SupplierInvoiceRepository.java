package com.toystorage.backend.repository.suppliers;

import com.toystorage.backend.entity.suppliers.SupplierInvoices;
import com.toystorage.backend.enums.suppliers.SupplierInvoiceStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierInvoiceRepository
        extends JpaRepository<SupplierInvoices, Long> {

    /*
     * Dùng khi sinh mã hóa đơn nội bộ.
     */
    boolean existsByInvoiceCode(String invoiceCode);


    /*
     * Số hóa đơn không được trùng trong cùng một nhà cung cấp.
     *
     * Hai Supplier khác nhau vẫn có thể sử dụng cùng invoice number.
     */
    boolean existsBySupplier_IdAndInvoiceNumberIgnoreCase(
            Long supplierId,
            String invoiceNumber
    );


    /*
     * Dùng khi update:
     * kiểm tra trùng invoice number nhưng bỏ qua chính invoice đang sửa.
     */
    boolean existsBySupplier_IdAndInvoiceNumberIgnoreCaseAndIdNot(
            Long supplierId,
            String invoiceNumber,
            Long id
    );


    /*
     * Lấy detail invoice cùng các quan hệ cần thiết.
     *
     * EntityGraph giúp tránh LazyInitializationException khi mapper
     * cần đọc Supplier, Purchase Order và người upload.
     */
    @EntityGraph(
            attributePaths = {
                    "supplier",
                    "purchaseOrder",
                    "uploadedBy"
            }
    )
    @Query("""
            select invoice
            from SupplierInvoices invoice
            where invoice.id = :id
            """)
    Optional<SupplierInvoices> findDetailedById(
            @Param("id") Long id
    );


    /*
     * Tìm kiếm danh sách hóa đơn.
     *
     * keyword hỗ trợ:
     * - invoice code
     * - invoice number
     * - supplier name
     * - purchase order code
     *
     * status dùng để filter trạng thái thanh toán/xử lý.
     */
    @EntityGraph(
            attributePaths = {
                    "supplier",
                    "purchaseOrder",
                    "uploadedBy"
            }
    )
    @Query("""
            select invoice
            from SupplierInvoices invoice
            join invoice.supplier supplier
            join invoice.purchaseOrder purchaseOrder
            where (
                :keyword is null
                or lower(invoice.invoiceCode)
                    like lower(concat('%', :keyword, '%'))
                or lower(invoice.invoiceNumber)
                    like lower(concat('%', :keyword, '%'))
                or lower(supplier.name)
                    like lower(concat('%', :keyword, '%'))
                or lower(purchaseOrder.orderCode)
                    like lower(concat('%', :keyword, '%'))
            )
            and (
                :status is null
                or invoice.status = :status
            )
            order by invoice.uploadedAt desc
            """)
    List<SupplierInvoices> search(
            @Param("keyword") String keyword,
            @Param("status") SupplierInvoiceStatus status
    );
}