package com.toystorage.backend.dto.response.packages;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PackingPackageItemResponse {

    private Long productId;
    private String productName;

    private Integer packedQuantity;
}