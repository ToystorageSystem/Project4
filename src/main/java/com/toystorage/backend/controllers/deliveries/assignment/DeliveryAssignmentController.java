package com.toystorage.backend.controllers.deliveries.assignment;

import com.toystorage.backend.dto.response.deliveries.assignment
        .DeliveryAssignmentResponse;

import com.toystorage.backend.services.deliveries.assignment
        .DeliveryAssignmentService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/deliveries/assignment"
)
@RequiredArgsConstructor
public class DeliveryAssignmentController {

    private final DeliveryAssignmentService
            assignmentService;


    // =====================================================
    // AUTO ASSIGN
    // =====================================================

    @PostMapping(
            "/{deliveryId}/auto"
    )
    public ResponseEntity<DeliveryAssignmentResponse>
    autoAssign(
            @PathVariable
            Long deliveryId
    ) {

        return ResponseEntity.ok(
                assignmentService
                        .autoAssign(
                                deliveryId
                        )
        );
    }


    // =====================================================
    // REASSIGN
    // =====================================================

    @PostMapping(
            "/{deliveryId}/reassign"
    )
    public ResponseEntity<DeliveryAssignmentResponse>
    reassign(
            @PathVariable
            Long deliveryId
    ) {

        return ResponseEntity.ok(
                assignmentService
                        .reassign(
                                deliveryId
                        )
        );
    }
}