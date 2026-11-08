package com.toystorage.backend.dto.response.shipments;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentManifestListItemResponse {

    // =====================================================
    // MANIFEST
    // =====================================================

    private Long manifestId;

    private String manifestCode;

    private String manifestStatus;


    // =====================================================
    // FROM WAREHOUSE / STORE
    // =====================================================

    private Long fromWarehouseId;

    private String fromWarehouseCode;

    private String fromWarehouseName;


    // =====================================================
    // TO WAREHOUSE / STORE
    // =====================================================

    private Long toWarehouseId;

    private String toWarehouseCode;

    private String toWarehouseName;


    // =====================================================
    // CREATED BY
    // =====================================================

    private Long createdById;

    private String createdByCode;

    private String createdByName;

    private LocalDateTime createdAt;


    // =====================================================
    // SUMMARY
    // =====================================================

    private Integer totalTransfers;

    private Integer totalPackages;


    // =====================================================
    // TRANSPORT
    // =====================================================

    private String transportStatus;
}