package com.toystorage.backend.dto.response.products;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryResponse {

    private Long id;

    private String categoryCode;

    private String name;

    private String description;

    private String status;
}