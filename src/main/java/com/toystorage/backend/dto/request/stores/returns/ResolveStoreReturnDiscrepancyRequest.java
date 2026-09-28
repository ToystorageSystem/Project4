package com.toystorage.backend.dto.request.stores.returns;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveStoreReturnDiscrepancyRequest {

    @NotBlank(message = "Reason is required")
    private String reason;
}