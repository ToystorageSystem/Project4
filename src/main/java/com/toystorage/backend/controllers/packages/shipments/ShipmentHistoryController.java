package com.toystorage.backend.controllers.packages.shipments;

import com.toystorage.backend.dto.response.shipments.ShipmentHistoryResponse;

import com.toystorage.backend.services.packages.shipments.ShipmentHistoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(
        "/api/warehouse/shipment-history"
)
@RequiredArgsConstructor
public class ShipmentHistoryController {

    private final ShipmentHistoryService
            shipmentHistoryService;


    @GetMapping
    public ResponseEntity<
            Page<ShipmentHistoryResponse>
            >
    getShipmentHistory(

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "6"
            )
            int size,

            @RequestParam(
                    defaultValue = ""
            )
            String keyword
    ) {

        return ResponseEntity.ok(
                shipmentHistoryService
                        .getHistory(
                                page,
                                size,
                                keyword
                        )
        );
    }
}