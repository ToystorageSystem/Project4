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
public class ShipmentManifestPackageResponse {

    /*
     * Package
     */
    private Long packageId;

    private String packageCode;

    private String sealNumber;

    private String packageStatus;


    /*
     * Packing information
     */
    private Long packedById;

    private String packedByCode;

    private String packedByName;

    private LocalDateTime packedAt;

    private LocalDateTime sealedAt;


    /*
     * Summary
     */
    private Integer totalProducts;

    private Integer totalQuantity;


    /*
     * Products inside package
     */
    @Builder.Default
    private List<ShipmentManifestProductResponse> products =
            new ArrayList<>();
}