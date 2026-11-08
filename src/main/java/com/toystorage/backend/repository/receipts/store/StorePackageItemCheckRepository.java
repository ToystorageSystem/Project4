package com.toystorage.backend.repository.receipts.store;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.toystorage.backend.entity.receipts.StorePackageItemChecks;

public interface StorePackageItemCheckRepository extends JpaRepository<StorePackageItemChecks, Long> {

    /** F2 counts of one package */
    @EntityGraph(attributePaths = {"product"})
    List<StorePackageItemChecks> findByPackageEntity_Id(Long packageId);

    /** F2 counts of all packages of the manifest's receipts (finish inspection) */
    @EntityGraph(attributePaths = {"product", "packageEntity", "storeReceipt"})
    List<StorePackageItemChecks> findByStoreReceipt_IdIn(Collection<Long> receiptIds);

    /** F2 re-save = delete old counts of the package, then insert the new ones */
    @Modifying
    @Query("DELETE FROM StorePackageItemChecks c WHERE c.packageEntity.id = :packageId")
    int deleteByPackageId(@Param("packageId") Long packageId);
}