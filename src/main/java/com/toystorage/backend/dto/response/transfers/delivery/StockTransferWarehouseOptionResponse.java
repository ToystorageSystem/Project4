package com.toystorage.backend.dto.response.transfers.delivery;

import lombok.*;

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