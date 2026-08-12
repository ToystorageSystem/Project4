package com.toystorage.backend.dto.response.shipments;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentPackageResponse {

    private Long packageId;
    private String packageCode;

    private String sealNumber;
    private String status;

    private Integer totalQuantity;
}