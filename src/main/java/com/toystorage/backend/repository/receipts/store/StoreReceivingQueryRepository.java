package com.toystorage.backend.repository.receipts.store;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.PackageTransferItem;
import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.shipments.ShipmentManifests;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.transfers.StockTransferItems;
import com.toystorage.backend.enums.deliveries.DeliveryStatus;

import jakarta.persistence.LockModeType;

/**
 * READ-ONLY queries on the team's delivery / manifest / package / transfer tables,
 * used by store receiving. Kept here so the team's repositories are not modified.
 * Extends the marker interface Repository -> no save/delete methods.
 */
public interface StoreReceivingQueryRepository extends Repository<ShipmentManifests, Long> {

    // ---------- deliveries / manifests of MY store ----------

    @Query("""
            SELECT d FROM Deliveries d
            JOIN FETCH d.manifest m
            JOIN FETCH d.fromWarehouse
            WHERE d.toWarehouse.id = :storeId
              AND d.deliveryStatus IN :statuses
            ORDER BY d.expectedDeliveryAt ASC, d.id ASC
            """)
    List<Deliveries> findStoreDeliveries(@Param("storeId") Long storeId,
                                         @Param("statuses") Collection<DeliveryStatus> statuses);

    /** newest first; normally exactly one delivery per manifest */
    @Query("""
            SELECT d FROM Deliveries d
            JOIN FETCH d.manifest m
            JOIN FETCH d.fromWarehouse
            WHERE m.manifestCode = :manifestCode
              AND d.toWarehouse.id = :storeId
            ORDER BY d.id DESC
            """)
    List<Deliveries> findStoreDeliveriesByManifestCode(@Param("storeId") Long storeId,
                                                       @Param("manifestCode") String manifestCode);

    /** delivery of MY store whose manifest contains the package (newest first) */
    @Query("""
            SELECT d FROM Deliveries d
            JOIN FETCH d.manifest m
            JOIN FETCH d.fromWarehouse
            WHERE d.toWarehouse.id = :storeId
              AND m.id IN (SELECT smp.manifest.id FROM ShipmentManifestPackage smp
                           WHERE smp.packageEntity.id = :packageId)
            ORDER BY d.id DESC
            """)
    List<Deliveries> findStoreDeliveriesByPackageId(@Param("storeId") Long storeId,
                                                    @Param("packageId") Long packageId);

    /** SELECT ... FOR UPDATE on the manifest row: serializes receiving writes per manifest */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM ShipmentManifests m WHERE m.id = :manifestId")
    Optional<ShipmentManifests> lockManifest(@Param("manifestId") Long manifestId);

    // ---------- packages / transfers of a manifest ----------

    @Query("""
            SELECT p FROM ShipmentManifestPackage smp
            JOIN smp.packageEntity p
            WHERE smp.manifest.id = :manifestId
            ORDER BY p.packagesCode
            """)
    List<Packages> findPackagesOfManifest(@Param("manifestId") Long manifestId);

    @Query("""
            SELECT t FROM ShipmentManifestTransfer smt
            JOIN smt.transfer t
            WHERE smt.manifest.id = :manifestId
            ORDER BY t.id
            """)
    List<StockTransfer> findTransfersOfManifest(@Param("manifestId") Long manifestId);

    // ---------- package details ----------

    @Query("SELECT p FROM Packages p WHERE p.packagesCode = :packageCode")
    Optional<Packages> findPackageByCode(@Param("packageCode") String packageCode);

    /** which transfer each package belongs to */
    @Query("""
            SELECT pti FROM PackageTransferItem pti
            JOIN FETCH pti.stockTransfer
            WHERE pti.packageEntity.id IN :packageIds
            """)
    List<PackageTransferItem> findPackageTransfers(@Param("packageIds") Collection<Long> packageIds);

    /** products expected inside the packages */
    @Query("""
            SELECT pi FROM PackageItems pi
            JOIN FETCH pi.product
            WHERE pi.packageEntity.id IN :packageIds
            """)
    List<PackageItems> findPackageItems(@Param("packageIds") Collection<Long> packageIds);

    /** transfer lines (shipped_quantity = expected quantity for the store) */
    @Query("""
            SELECT sti FROM StockTransferItems sti
            JOIN FETCH sti.product
            WHERE sti.stockTransfer.id IN :transferIds
            """)
    List<StockTransferItems> findTransferItems(@Param("transferIds") Collection<Long> transferIds);

    // ---------- products (F2) ----------

    /** products scanned in F2 (any status – a surplus item may not be ACTIVE) */
    @Query("SELECT p FROM Products p WHERE p.barcode IN :barcodes")
    List<Products> findProductsByBarcodes(@Param("barcodes") Collection<String> barcodes);
}