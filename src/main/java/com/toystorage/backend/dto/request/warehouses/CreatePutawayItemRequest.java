package com.toystorage.backend.dto.request.warehouses;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePutawayItemRequest {

    @NotNull
    private Long productId;

    @NotNull
    private Long toLocationId;
}