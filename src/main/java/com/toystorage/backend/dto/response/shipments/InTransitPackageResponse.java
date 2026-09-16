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
public class InTransitPackageResponse {

    private Long packageId;

    private String packageCode;

    private String sealNumber;

    private String packageStatus;

    private LocalDateTime packedAt;

    private LocalDateTime sealedAt;

    private Integer totalProducts;

    private Integer totalQuantity;

    @Builder.Default
    private List<ShipmentManifestProductResponse> products =
            new ArrayList<>();
}