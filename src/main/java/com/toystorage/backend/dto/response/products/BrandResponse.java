package com.toystorage.backend.dto.response.products;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BrandResponse {

    private Long id;

    private String brandCode;

    private String name;

    private String country;

    private String description;

    private String status;
}
