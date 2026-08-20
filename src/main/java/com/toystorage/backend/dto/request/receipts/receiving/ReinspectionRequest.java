package com.toystorage.backend.dto.request.receipts.receiving;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReinspectionRequest {

    @NotBlank(message = "Reason is required")
    private String reason;
}