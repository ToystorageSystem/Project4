package com.toystorage.backend.dto.response.transfers.picking;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TransferPickingResponse {

    private Long transferId;

    private String transferCode;

    private String status;

    private Long fromWarehouseId;
    private String fromWarehouseName;

    private Long toWarehouseId;
    private String toWarehouseName;

    private List<TransferPickingItemResponse> items;
}