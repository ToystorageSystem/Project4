package com.toystorage.backend.repository.receipts;

import com.toystorage.backend.entity.receipts.PurchaseOrders;
import com.toystorage.backend.enums.receipts.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrders, Long> {

    boolean existsByOrderCode(String orderCode);


    @EntityGraph(
            attributePaths = {
                    "supplier",
                    "warehouse",
                    "createdBy",
                    "approvedBy"
            }
    )
    @Query("""
            select po
            from PurchaseOrders po
            where po.id = :id
            """)
    Optional<PurchaseOrders> findDetailedById(
            @Param("id") Long id
    );


    @EntityGraph(
            attributePaths = {
                    "supplier",
                    "warehouse",
                    "createdBy",
                    "approvedBy"
            }
    )
    @Query("""
            select po
            from PurchaseOrders po
            join po.supplier supplier
            where (
                :keyword is null
                or lower(po.orderCode) like lower(concat('%', :keyword, '%'))
                or lower(supplier.name) like lower(concat('%', :keyword, '%'))
            )
            and (:status is null or po.status = :status)
            and (:supplierId is null or supplier.id = :supplierId)
            and (:createdFrom is null or po.createdAt >= :createdFrom)
            and (:createdTo is null or po.createdAt <= :createdTo)
            order by po.createdAt desc
            """)
    List<PurchaseOrders> search(
            @Param("keyword") String keyword,
            @Param("status") PurchaseOrderStatus status,
            @Param("supplierId") Long supplierId,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo
    );
        /*
     * Lấy các Purchase Order có thể gắn với hóa đơn
     * của một Supplier.
     *
     * Service sẽ truyền vào các trạng thái:
     * ORDERED, PARTIALLY_RECEIVED, COMPLETED.
     *
     * DRAFT và CANCELLED không được sử dụng
     * để tạo Supplier Invoice.
     */
    @EntityGraph(
            attributePaths = {
                    "supplier",
                    "warehouse"
            }
    )
    @Query("""
            select po
            from PurchaseOrders po
            where po.supplier.id = :supplierId
            and po.status in :statuses
            order by po.createdAt desc
            """)
    List<PurchaseOrders> findInvoiceEligibleBySupplierId(
            @Param("supplierId") Long supplierId,
            @Param("statuses")
            Collection<PurchaseOrderStatus> statuses
    );


    /*
     * Kiểm tra Purchase Order được Business chọn
     * thực sự thuộc Supplier đã chọn và có trạng thái
     * hợp lệ để liên kết với hóa đơn.
     */
    @EntityGraph(
            attributePaths = {
                    "supplier",
                    "warehouse"
            }
    )
    @Query("""
            select po
            from PurchaseOrders po
            where po.id = :purchaseOrderId
            and po.supplier.id = :supplierId
            and po.status in :statuses
            """)
    Optional<PurchaseOrders> findInvoiceEligibleById(
            @Param("purchaseOrderId") Long purchaseOrderId,
            @Param("supplierId") Long supplierId,
            @Param("statuses")
            Collection<PurchaseOrderStatus> statuses
    );
}