package com.toystorage.backend.dto.response.deliveries.handover;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DeliveryHandoverResponse {

    private Long handoverId;

    private Long deliveryId;

    private String shipmentCode;

    private String deliveryStatus;

    private Integer expectedPackageCount;

    private Integer handedOverPackageCount;

    private Integer missingPackageCount;

    private Integer abnormalPackageCount;

    private Long handedOverById;

    private String handedOverByName;

    private Long receivedById;

    private String receivedByName;

    private String note;

    private LocalDateTime completedAt;

    private List<DeliveryHandoverItemResponse> packages;
}