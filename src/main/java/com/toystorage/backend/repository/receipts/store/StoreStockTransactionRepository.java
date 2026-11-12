package com.toystorage.backend.repository.receipts.store;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.toystorage.backend.entity.stores.StoreStockTransactions;
import com.toystorage.backend.enums.stores.StockTransactionType;

public interface StoreStockTransactionRepository extends JpaRepository<StoreStockTransactions, Long> {

    /** history of a store; productId / type = null -> no filter; [from, to) always given */
    @EntityGraph(attributePaths = {"product", "createdBy"})
    @Query(value = """
            select t from StoreStockTransactions t
            where t.storeId = :storeId
              and (:productId is null or t.product.id = :productId)
              and (:type is null or t.type = :type)
              and t.createdAt >= :from and t.createdAt < :to
            order by t.createdAt desc, t.id desc
            """,
           countQuery = """
            select count(t) from StoreStockTransactions t
            where t.storeId = :storeId
              and (:productId is null or t.product.id = :productId)
              and (:type is null or t.type = :type)
              and t.createdAt >= :from and t.createdAt < :to
            """)
    Page<StoreStockTransactions> search(@Param("storeId") Long storeId,
                                        @Param("productId") Long productId,
                                        @Param("type") StockTransactionType type,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to,
                                        Pageable pageable);
}
