package com.toystorage.backend.dto.request.warehouses;


import com.toystorage.backend.enums.warehouses.DamageDisposition;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HandleDamagedGoodsRequest {

    @NotNull
    @Min(1)
    private Integer confirmedQuantity;

    @NotNull
    private DamageDisposition disposition;

    private String resolutionNote;
}