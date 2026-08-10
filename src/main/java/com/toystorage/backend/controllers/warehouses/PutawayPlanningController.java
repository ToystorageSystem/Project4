package com.toystorage.backend.controllers.warehouses;

import com.toystorage.backend.dto.request.warehouses.CreatePutawayPlanRequest;
import com.toystorage.backend.dto.response.warehouses.PutawayPlanResponse;
import com.toystorage.backend.services.warehouses.PutawayPlanningService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/putaway-plans")
@RequiredArgsConstructor
public class PutawayPlanningController {

    private final PutawayPlanningService
            putawayPlanningService;


    @PostMapping("/goods-receipts/{receiptId}")
//    @PreAuthorize(
//            "hasAuthority('PUTAWAY_PLAN_CREATE')"
//    )
    public ResponseEntity<PutawayPlanResponse>
    createPlan(
            @PathVariable Long receiptId,

            @Valid
            @RequestBody
            CreatePutawayPlanRequest request
    ) {

        return ResponseEntity.ok(
                putawayPlanningService
                        .createPlan(
                                receiptId,
                                request
                        )
        );
    }


    @GetMapping
//    @PreAuthorize(
//            "hasAuthority('PUTAWAY_PLAN_VIEW')"
//    )
    public ResponseEntity<List<PutawayPlanResponse>>
    getPlans() {

        return ResponseEntity.ok(
                putawayPlanningService
                        .getPlans()
        );
    }


    @GetMapping("/{taskId}")
//    @PreAuthorize(
//            "hasAuthority('PUTAWAY_PLAN_VIEW')"
//    )
    public ResponseEntity<PutawayPlanResponse>
    getPlan(
            @PathVariable Long taskId
    ) {

        return ResponseEntity.ok(
                putawayPlanningService
                        .getPlan(taskId)
        );
    }


    @PatchMapping("/{taskId}/confirm-completion")
//    @PreAuthorize(
//            "hasAuthority('PUTAWAY_PLAN_CONFIRM')"
//    )
    public ResponseEntity<PutawayPlanResponse>
    confirmCompletion(
            @PathVariable Long taskId
    ) {

        return ResponseEntity.ok(
                putawayPlanningService
                        .confirmCompletion(taskId)
        );
    }
}