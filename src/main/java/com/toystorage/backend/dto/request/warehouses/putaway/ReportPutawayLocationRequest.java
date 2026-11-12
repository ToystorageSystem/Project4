package com.toystorage.backend.dto.request.warehouses.putaway;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportPutawayLocationRequest {

    @NotBlank(message = "Reason is required")
    private String reason;
}