package com.toystorage.backend.dto.request.packages;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SealPackageRequest {

    @NotBlank(
            message = "Seal number is required"
    )
    private String sealNumber;
}
