package com.toystorage.backend.dto.request.stores;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnedPackageScanRequest {

    @NotBlank(
            message = "Package barcode is required"
    )
    private String packageBarcode;
}