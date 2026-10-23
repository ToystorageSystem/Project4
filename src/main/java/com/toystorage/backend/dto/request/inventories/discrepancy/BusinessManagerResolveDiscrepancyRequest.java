package com.toystorage.backend.dto.request.inventories.discrepancy;

import com.toystorage.backend.enums.inventories.ResolutionAction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BusinessManagerResolveDiscrepancyRequest {

    @NotNull(message = "Resolution action is required")
    private ResolutionAction resolutionAction;

    @NotBlank(message = "Resolution note is required")
    private String resolutionNote;

    @Valid
    @NotEmpty(message = "Final quantity items are required")
    private List<BusinessManagerResolveDiscrepancyItemRequest> items;
}