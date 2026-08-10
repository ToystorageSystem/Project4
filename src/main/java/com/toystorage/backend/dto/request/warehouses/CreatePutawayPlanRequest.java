package com.toystorage.backend.dto.request.warehouses;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreatePutawayPlanRequest {

    @NotNull(message = "Staff id is required")
    private Long staffId;

    @Valid
    @NotEmpty(message = "Putaway items are required")
    private List<CreatePutawayItemRequest> items;
}