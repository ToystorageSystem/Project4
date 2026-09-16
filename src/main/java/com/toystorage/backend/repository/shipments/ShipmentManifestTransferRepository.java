package com.toystorage.backend.repository.shipments;

import com.toystorage.backend.entity.shipments.ShipmentManifestTransfer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface ShipmentManifestTransferRepository
        extends JpaRepository<
        ShipmentManifestTransfer,
        Long
        > {


    Optional<ShipmentManifestTransfer>
    findByTransferId(
            Long transferId
    );


    @Query("""
            SELECT smt
            FROM ShipmentManifestTransfer smt
            JOIN smt.manifest manifest
            JOIN smt.transfer transfer
            WHERE transfer.fromWarehouse.id = :warehouseId
              AND (
                    :keyword = ''
                    OR LOWER(transfer.transferCode)
                        LIKE LOWER(
                            CONCAT('%', :keyword, '%')
                        )
                    OR LOWER(manifest.manifestCode)
                        LIKE LOWER(
                            CONCAT('%', :keyword, '%')
                        )
              )
            """)
    Page<ShipmentManifestTransfer>
    findShipmentHistory(
            @Param("warehouseId")
            Long warehouseId,

            @Param("keyword")
            String keyword,

            Pageable pageable
    );
}