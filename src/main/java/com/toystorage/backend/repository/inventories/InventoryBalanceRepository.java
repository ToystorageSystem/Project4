package com.toystorage.backend.repository.inventories;

import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.enums.products.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryBalanceRepository
        extends JpaRepository<InventoryBalances, Long> {

    Optional<InventoryBalances>
    findByWarehouseIdAndLocationIdAndProductId(
            Long warehouseId,
            Long locationId,
            Long productId
    );

    List<InventoryBalances>
    findByWarehouseId(
            Long warehouseId
    );

    Optional<InventoryBalances>
    findFirstByWarehouseIdAndProductId(
            Long warehouseId,
            Long productId
    );

    @Query("""
            select b
            from InventoryBalances b
            join b.location l
            where b.warehouse.id = :warehouseId
              and b.product.id = :productId
              and l.warehouseCode = :locationCode
            """)
    Optional<InventoryBalances>
    findPickingBalance(
            @Param("warehouseId")
            Long warehouseId,

            @Param("productId")
            Long productId,

            @Param("locationCode")
            String locationCode
    );


    // =====================================================
    // STOCK TRANSFER - TOTAL AVAILABLE QUANTITY
    // =====================================================

    @Query("""
            select coalesce(sum(b.availableQuantity), 0)
            from InventoryBalances b
            where b.warehouse.id = :warehouseId
              and b.product.id = :productId
            """)
    Long sumAvailableQuantity(
            @Param("warehouseId")
            Long warehouseId,

            @Param("productId")
            Long productId
    );


    // =====================================================
    // STOCK TRANSFER - AVAILABLE PRODUCTS AT SOURCE
    // =====================================================

    @Query("""
            select b.product, coalesce(sum(b.availableQuantity), 0)
            from InventoryBalances b
            where b.warehouse.id = :warehouseId
              and b.product.status = :productStatus
            group by b.product
            having sum(b.availableQuantity) > 0
            order by b.product.name asc
            """)
    List<Object[]> findAvailableProductsByWarehouseId(
            @Param("warehouseId")
            Long warehouseId,

            @Param("productStatus")
            ProductStatus productStatus
    );


    // =====================================================
    // STOCK TRANSFER - LOCK INVENTORY BEFORE SUBMIT
    // =====================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from InventoryBalances b
            where b.warehouse.id = :warehouseId
              and b.product.id = :productId
            order by b.id asc
            """)
    List<InventoryBalances> findByWarehouseIdAndProductIdForUpdate(
            @Param("warehouseId")
            Long warehouseId,

            @Param("productId")
            Long productId
    );


    // =====================================================
    // STOCK TRANSFER - LOCK EXACT RESERVED BALANCE
    // =====================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from InventoryBalances b
            where b.warehouse.id = :warehouseId
              and b.location.id = :locationId
              and b.product.id = :productId
            """)
    Optional<InventoryBalances> findExactBalanceForUpdate(
            @Param("warehouseId")
            Long warehouseId,

            @Param("locationId")
            Long locationId,

            @Param("productId")
            Long productId
    );


    // =====================================================
    // INVENTORY REPORT - SEARCH / FILTER / PAGINATION
    // =====================================================

    /**
     * Tra cứu báo cáo tồn kho.
     *
     * Hỗ trợ:
     *
     * - tìm theo mã sản phẩm
     * - tìm theo tên sản phẩm
     * - tìm theo barcode
     * - lọc warehouse / store
     * - lọc category
     * - lọc brand
     * - lọc trạng thái tồn
     * - giới hạn warehouse theo quyền của user
     *
     * stockStatus:
     *
     * OUT_OF_STOCK
     *      available = 0
     *
     * LOW_STOCK
     *      available > 0
     *      và available <= minimumStockLevel
     *
     * IN_STOCK
     *      available > minimumStockLevel
     */
    @Query(
            value = """
                    select b
                    from InventoryBalances b
                    join fetch b.product p
                    join fetch p.category c
                    left join fetch p.brand br
                    join fetch b.warehouse w
                    join fetch b.location l
                    where
                        (
                            :scopeWarehouseId is null
                            or w.id = :scopeWarehouseId
                        )
                      and (
                            :keyword is null
                            or trim(:keyword) = ''
                            or lower(p.productsCode)
                                like lower(concat('%', :keyword, '%'))
                            or lower(p.name)
                                like lower(concat('%', :keyword, '%'))
                            or lower(p.barcode)
                                like lower(concat('%', :keyword, '%'))
                        )
                      and (
                            :warehouseId is null
                            or w.id = :warehouseId
                        )
                      and (
                            :categoryId is null
                            or c.id = :categoryId
                        )
                      and (
                            :brandId is null
                            or br.id = :brandId
                        )
                      and (
                            :stockStatus is null
                            or trim(:stockStatus) = ''
                            or (
                                :stockStatus = 'OUT_OF_STOCK'
                                and b.availableQuantity = 0
                            )
                            or (
                                :stockStatus = 'LOW_STOCK'
                                and b.availableQuantity > 0
                                and b.availableQuantity
                                    <= b.minimumStockLevel
                            )
                            or (
                                :stockStatus = 'IN_STOCK'
                                and b.availableQuantity
                                    > b.minimumStockLevel
                            )
                        )
                    """,
            countQuery = """
                    select count(b)
                    from InventoryBalances b
                    join b.product p
                    join p.category c
                    left join p.brand br
                    join b.warehouse w
                    join b.location l
                    where
                        (
                            :scopeWarehouseId is null
                            or w.id = :scopeWarehouseId
                        )
                      and (
                            :keyword is null
                            or trim(:keyword) = ''
                            or lower(p.productsCode)
                                like lower(concat('%', :keyword, '%'))
                            or lower(p.name)
                                like lower(concat('%', :keyword, '%'))
                            or lower(p.barcode)
                                like lower(concat('%', :keyword, '%'))
                        )
                      and (
                            :warehouseId is null
                            or w.id = :warehouseId
                        )
                      and (
                            :categoryId is null
                            or c.id = :categoryId
                        )
                      and (
                            :brandId is null
                            or br.id = :brandId
                        )
                      and (
                            :stockStatus is null
                            or trim(:stockStatus) = ''
                            or (
                                :stockStatus = 'OUT_OF_STOCK'
                                and b.availableQuantity = 0
                            )
                            or (
                                :stockStatus = 'LOW_STOCK'
                                and b.availableQuantity > 0
                                and b.availableQuantity
                                    <= b.minimumStockLevel
                            )
                            or (
                                :stockStatus = 'IN_STOCK'
                                and b.availableQuantity
                                    > b.minimumStockLevel
                            )
                        )
                    """
    )
    Page<InventoryBalances> searchInventoryReport(
            @Param("keyword")
            String keyword,

            @Param("warehouseId")
            Long warehouseId,

            @Param("categoryId")
            Long categoryId,

            @Param("brandId")
            Long brandId,

            @Param("stockStatus")
            String stockStatus,

            @Param("scopeWarehouseId")
            Long scopeWarehouseId,

            Pageable pageable
    );


    // =====================================================
    // INVENTORY REPORT - PRODUCT BY LOCATIONS
    // =====================================================

    /**
     * Xem tồn của một sản phẩm tại tất cả vị trí
     * mà người dùng được phép truy cập.
     */
    @Query("""
            select b
            from InventoryBalances b
            join fetch b.product p
            join fetch p.category c
            left join fetch p.brand br
            join fetch b.warehouse w
            join fetch b.location l
            where p.id = :productId
              and (
                    :scopeWarehouseId is null
                    or w.id = :scopeWarehouseId
                  )
            order by w.name asc, l.name asc
            """)
    List<InventoryBalances> findProductLocations(
            @Param("productId")
            Long productId,

            @Param("scopeWarehouseId")
            Long scopeWarehouseId
    );
}