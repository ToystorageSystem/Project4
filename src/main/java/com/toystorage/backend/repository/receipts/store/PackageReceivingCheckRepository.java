package com.toystorage.backend.repository.receipts.store;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.toystorage.backend.entity.receipts.PackageReceivingChecks;

public interface PackageReceivingCheckRepository extends JpaRepository<PackageReceivingChecks, Long> {

    /** F1 results of the receipts of one (or many) manifests */
    @EntityGraph(attributePaths = {"packageEntity"})
    List<PackageReceivingChecks> findByStoreReceipt_IdIn(Collection<Long> receiptIds);
}