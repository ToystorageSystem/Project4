package com.toystorage.backend.dto.response.warehouses.putaway;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PutawayStaffItemResponse {

    private Long itemId;

    private Long productId;
    private String productCode;
    private String productName;
    private String barcode;

    private Integer expectedQuantity;
    private Integer putawayQuantity;
    private Integer remainingQuantity;

    private Long fromLocationId;
    private String fromLocationCode;

    private Long toLocationId;
    private String toLocationCode;

    private String zone;
    private String shelf;

    private String status;

    /* Where to put the goods (when no shelf has been scanned yet). */
    private Long suggestedLocationId;

    private String suggestedLocationCode;

    private String suggestedZone;

    private String suggestedShelf;

    private String suggestedReason;
}