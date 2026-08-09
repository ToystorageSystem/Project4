package com.toystorage.backend.dto.request.inventories;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EscalateDiscrepancyRequest {

    @NotBlank(message = "Reason is required")
    private String reason;
}