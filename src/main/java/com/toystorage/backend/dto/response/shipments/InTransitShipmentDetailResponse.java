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
public class InTransitShipmentDetailResponse {

    private Long deliveryId;

    private String deliveryCode;

    private String deliveryStatus;

    private Long manifestId;

    private String manifestCode;

    private String manifestStatus;

    private String manifestNote;

    private Long fromWarehouseId;

    private String fromWarehouseCode;

    private String fromWarehouseName;

    private String fromWarehouseAddress;

    private Long toWarehouseId;

    private String toWarehouseCode;

    private String toWarehouseName;

    private String toWarehouseAddress;

    private Long driverId;

    private String driverCode;

    private String driverName;

    private Long handedOverById;

    private String handedOverByCode;

    private String handedOverByName;

    private LocalDateTime handedOverAt;

    private LocalDateTime expectedPickupAt;

    private LocalDateTime expectedDeliveryAt;

    private LocalDateTime startedAt;

    private LocalDateTime deliveredAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Boolean overdue;

    private Integer totalTransfers;

    private Integer totalPackages;

    private Integer totalProductQuantity;

    @Builder.Default
    private List<InTransitTransferResponse> transfers =
            new ArrayList<>();

    @Builder.Default
    private List<InTransitPackageResponse> packages =
            new ArrayList<>();

    @Builder.Default
    private List<InTransitStatusHistoryResponse> statusHistory =
            new ArrayList<>();
}