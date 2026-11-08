package com.toystorage.backend.dto.request.products;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCategoryRequest {

    @NotBlank(message = "Category code is required")
    @Size(
            max = 50,
            message = "Category code must not exceed 50 characters"
    )
    private String categoryCode;

    @NotBlank(message = "Category name is required")
    @Size(
            max = 150,
            message = "Category name must not exceed 150 characters"
    )
    private String name;

    @Size(
            max = 1000,
            message = "Description must not exceed 1000 characters"
    )
    private String description;
}