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
public class InTransitShipmentListItemResponse {

    private Long deliveryId;

    private String deliveryCode;

    private String deliveryStatus;

    private Long manifestId;

    private String manifestCode;

    @Builder.Default
    private List<String> transferCodes =
            new ArrayList<>();

    @Builder.Default
    private List<String> packageCodes =
            new ArrayList<>();

    private Long fromWarehouseId;

    private String fromWarehouseCode;

    private String fromWarehouseName;

    private Long toWarehouseId;

    private String toWarehouseCode;

    private String toWarehouseName;

    private LocalDateTime startedAt;

    private LocalDateTime expectedDeliveryAt;

    private LocalDateTime deliveredAt;

    private Boolean overdue;

    private Integer totalTransfers;

    private Integer totalPackages;

    private Integer totalProductQuantity;
}