package com.toystorage.backend.repository.receipts;

import com.toystorage.backend.entity.receipts.PurchaseOrderItems;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PurchaseOrderItemRepository
        extends JpaRepository<PurchaseOrderItems, Long> {

    boolean existsByPurchaseOrderItemsCode(
            String purchaseOrderItemsCode
    );


    boolean existsByPurchaseOrder_Id(
            Long purchaseOrderId
    );


    @EntityGraph(attributePaths = "product")
    List<PurchaseOrderItems>
    findByPurchaseOrder_IdOrderByIdAsc(
            Long purchaseOrderId
    );


    @EntityGraph(attributePaths = "product")
    @Query("""
            select item
            from PurchaseOrderItems item
            where item.purchaseOrder.id in :purchaseOrderIds
            order by item.purchaseOrder.id asc, item.id asc
            """)
    List<PurchaseOrderItems> findAllByPurchaseOrderIds(
            @Param("purchaseOrderIds")
            Collection<Long> purchaseOrderIds
    );


    void deleteByPurchaseOrder_Id(
            Long purchaseOrderId
    );
}