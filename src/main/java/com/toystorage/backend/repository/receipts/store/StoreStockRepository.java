package com.toystorage.backend.repository.receipts.store;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.toystorage.backend.entity.stores.StoreShelves;
import com.toystorage.backend.entity.stores.StoreStocks;
import com.toystorage.backend.enums.stores.ShelfStatus;
import com.toystorage.backend.repository.stores.ShelfUsage;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.toystorage.backend.enums.products.ProductStatus;





public interface StoreStockRepository extends JpaRepository<StoreStocks, Long> {

    long countByStoreId(Long storeId);

    /** stock rows of the products on ONE page (1 query, shelf included) */
    @EntityGraph(attributePaths = {"shelf"})
    List<StoreStocks> findByStoreIdAndProductIdIn(Long storeId, Collection<Long> productIds);

    /** stock row of one product (detail screen) */
    @EntityGraph(attributePaths = {"shelf"})
    Optional<StoreStocks> findByStoreIdAndProductId(Long storeId, Long productId);




        /** product count + shelf quantity of every shelf in the store (1 query for the shelf list) */
    @Query("""
            select new com.toystorage.backend.repository.stores.ShelfUsage(
                   s.shelf.id, count(s), sum(s.shelfQuantity))
            from StoreStocks s
            where s.storeId = :storeId and s.shelf is not null
            group by s.shelf.id
            """)
    List<ShelfUsage> sumByShelf(@Param("storeId") Long storeId);

    /** stock rows on one shelf, with product loaded (no N+1) */
    @EntityGraph(attributePaths = {"product", "shelf"})
    List<StoreStocks> findByStoreIdAndShelf_IdOrderByProduct_NameAsc(Long storeId, Long shelfId);

    /** shelf at/below minimum AND backroom still has goods to refill */
    @Query("""
            select s from StoreStocks s
            join fetch s.product p
            join fetch s.shelf sh
            where s.storeId = :storeId
              and p.status = :status
              and s.shelfQuantity <= s.minShelfQuantity
              and s.backroomQuantity > 0
            order by sh.name, p.name
            """)
    List<StoreStocks> findLowShelf(@Param("storeId") Long storeId,
                                   @Param("status") ProductStatus status);


}