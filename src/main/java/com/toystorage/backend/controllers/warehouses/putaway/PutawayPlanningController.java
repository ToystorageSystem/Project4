package com.toystorage.backend.controllers.warehouses.putaway;

import com.toystorage.backend.dto.request.warehouses.putaway.CreatePutawayPlanRequest;
import com.toystorage.backend.dto.response.warehouses.putaway.PutawayPlanResponse;
import com.toystorage.backend.services.warehouses.putaway.PutawayPlanningService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/putaway-plans")
@RequiredArgsConstructor
public class PutawayPlanningController {

    private final PutawayPlanningService
            putawayPlanningService;





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

}