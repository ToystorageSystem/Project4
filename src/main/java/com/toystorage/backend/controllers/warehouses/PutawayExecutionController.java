package com.toystorage.backend.controllers.warehouses;

import com.toystorage.backend.dto.request.warehouses.ExecutePutawayItemRequest;

import com.toystorage.backend.dto.response.warehouses.PutawayStaffItemResponse;
import com.toystorage.backend.dto.response.warehouses.PutawayStaffTaskResponse;

import com.toystorage.backend.services.warehouses.PutawayExecutionService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse/putaway")
@RequiredArgsConstructor
public class PutawayExecutionController {

    private final PutawayExecutionService
            putawayExecutionService;


    @GetMapping("/my-tasks")
//    @PreAuthorize(
//            "hasAuthority('PUTAWAY_PERFORM')"
//    )
    public ResponseEntity<List<PutawayStaffTaskResponse>>
    getMyTasks() {

        return ResponseEntity.ok(
                putawayExecutionService
                        .getMyTasks()
        );
    }


    @GetMapping("/{taskId}")
//    @PreAuthorize(
//            "hasAuthority('PUTAWAY_PERFORM')"
//    )
    public ResponseEntity<PutawayStaffTaskResponse>
    getTask(
            @PathVariable Long taskId
    ) {

        return ResponseEntity.ok(
                putawayExecutionService
                        .getTask(taskId)
        );
    }


    @PatchMapping("/{taskId}/start")
//    @PreAuthorize(
//            "hasAuthority('PUTAWAY_PERFORM')"
//    )
    public ResponseEntity<PutawayStaffTaskResponse>
    startTask(
            @PathVariable Long taskId
    ) {

        return ResponseEntity.ok(
                putawayExecutionService
                        .startTask(taskId)
        );
    }


    @PatchMapping(
            "/{taskId}/items/{itemId}"
    )
//    @PreAuthorize(
//            "hasAuthority('PUTAWAY_PERFORM')"
//    )
    public ResponseEntity<PutawayStaffItemResponse>
    putawayItem(
            @PathVariable Long taskId,
            @PathVariable Long itemId,

            @Valid
            @RequestBody
            ExecutePutawayItemRequest request
    ) {

        return ResponseEntity.ok(
                putawayExecutionService
                        .putawayItem(
                                taskId,
                                itemId,
                                request
                        )
        );
    }


    @PatchMapping("/{taskId}/complete")
//    @PreAuthorize(
//            "hasAuthority('PUTAWAY_PERFORM')"
//    )
    public ResponseEntity<PutawayStaffTaskResponse>
    completeTask(
            @PathVariable Long taskId
    ) {

        return ResponseEntity.ok(
                putawayExecutionService
                        .completeTask(taskId)
        );
    }
}