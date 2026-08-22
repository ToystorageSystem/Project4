package com.toystorage.backend.dto.response.transfers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransferWarehouseOptionResponse {

    private Long id;

    private String code;

    private String name;

    private String type;

    private String address;
}