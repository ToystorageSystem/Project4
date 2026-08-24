package com.toystorage.backend.dto.response.shipments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentManifestDetailResponse {

    /*
     * Manifest
     */
    private Long manifestId;

    private String manifestCode;

    private String manifestStatus;


    /*
     * From warehouse / store
     */
    private Long fromWarehouseId;

    private String fromWarehouseCode;

    private String fromWarehouseName;

    private String fromWarehouseAddress;


    /*
     * To warehouse / store
     */
    private Long toWarehouseId;

    private String toWarehouseCode;

    private String toWarehouseName;

    private String toWarehouseAddress;


    /*
     * Created by
     */
    private Long createdById;

    private String createdByCode;

    private String createdByName;

    private LocalDateTime createdAt;


    /*
     * Manifest additional information
     */
    private String note;


    /*
     * Summary
     */
    private Integer totalTransfers;

    private Integer totalPackages;

    private Integer totalProductQuantity;


    /*
     * Transport
     */
    private Long deliveryId;

    private String deliveryCode;

    private String transportStatus;

    private Long driverId;

    private String driverCode;

    private String driverName;

    private LocalDateTime startedAt;

    private LocalDateTime deliveredAt;


    /*
     * Transfers inside manifest
     */
    @Builder.Default
    private List<ShipmentManifestTransferResponse> transfers =
            new ArrayList<>();


    /*
     * Packages inside manifest
     */
    @Builder.Default
    private List<ShipmentManifestPackageResponse> packages =
            new ArrayList<>();
}