package com.toystorage.backend.dto.request.products;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBrandRequest {

    @NotBlank(message = "Brand code is required")
    @Size(max = 50, message = "Brand code must not exceed 50 characters")
    private String brandCode;

    @NotBlank(message = "Brand name is required")
    @Size(max = 150, message = "Brand name must not exceed 150 characters")
    private String name;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}
