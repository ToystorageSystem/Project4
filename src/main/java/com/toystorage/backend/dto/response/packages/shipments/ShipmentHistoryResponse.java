package com.toystorage.backend.dto.response.shipments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentHistoryResponse {

    private Long transferId;

    private String transferCode;

    private String transferStatus;


    private Long manifestId;

    private String manifestCode;

    private String manifestStatus;


    private Long fromWarehouseId;

    private String fromWarehouseName;


    private Long toWarehouseId;

    private String toWarehouseName;


    private Integer totalPackages;

    private Integer totalQuantity;


    private Long createdBy;

    private String createdByName;


    private LocalDateTime createdAt;
}