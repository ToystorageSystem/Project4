package com.toystorage.backend.dto.request.inventories;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReceivingShortageRequest {

    @NotNull(message = "Product id is required")
    private Long productId;

    @NotBlank(message = "Description is required")
    private String description;
}