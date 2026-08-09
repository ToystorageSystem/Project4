package com.toystorage.backend.dto.request.inventories;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveDiscrepancyRequest {

    @NotBlank(message = "Resolution note is required")
    private String resolutionNote;
}