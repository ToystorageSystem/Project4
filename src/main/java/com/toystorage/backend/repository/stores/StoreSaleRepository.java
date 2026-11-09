package com.toystorage.backend.repository.stores;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.toystorage.backend.entity.stores.StoreSales;

public interface StoreSaleRepository extends JpaRepository<StoreSales, Long> {

    /** sale history of a store, newest first (+ seller name, no N+1) */
    @EntityGraph(attributePaths = {"createdBy"})
    Page<StoreSales> findByStoreIdOrderByCreatedAtDescIdDesc(Long storeId, Pageable pageable);

    /** sale detail with lines + products in 1 query; other store -> empty -> 404 */
    @EntityGraph(attributePaths = {"createdBy", "items", "items.product"})
    Optional<StoreSales> findByIdAndStoreId(Long id, Long storeId);

    /** totals in [from, to) – dashboard + sales summary */
    @Query("""
            select new com.toystorage.backend.repository.stores.SalesTotals(
                   count(s), sum(s.totalQuantity), sum(s.totalAmount))
            from StoreSales s
            where s.storeId = :storeId and s.createdAt >= :from and s.createdAt < :to
            """)
    SalesTotals sumBetween(@Param("storeId") Long storeId,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to);

    /** sale history in [from, to), newest first (service always passes both bounds) */
    @EntityGraph(attributePaths = {"createdBy"})
    @Query(value = """
            select s from StoreSales s
            where s.storeId = :storeId and s.createdAt >= :from and s.createdAt < :to
            order by s.createdAt desc, s.id desc
            """,
           countQuery = """
            select count(s) from StoreSales s
            where s.storeId = :storeId and s.createdAt >= :from and s.createdAt < :to
            """)
    Page<StoreSales> search(@Param("storeId") Long storeId,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to,
                            Pageable pageable);


}