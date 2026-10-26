package com.toystorage.backend.controllers.stores;

import com.toystorage.backend.dto.stores.StoreDetailsRequest;
import com.toystorage.backend.dto.stores.StoreDetailsResponse;
import com.toystorage.backend.services.stores.StoreManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreManagementController {
    private final StoreManagementService service;

    @GetMapping
    @PreAuthorize("hasAuthority('STORE_VIEW')")
    public ResponseEntity<List<StoreDetailsResponse>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{warehouseId}")
    @PreAuthorize("hasAuthority('STORE_VIEW')")
    public ResponseEntity<StoreDetailsResponse> get(@PathVariable Long warehouseId) {
        return ResponseEntity.ok(service.get(warehouseId));
    }

    @PutMapping("/{warehouseId}")
    @PreAuthorize("hasAuthority('STORE_MANAGE')")
    public ResponseEntity<StoreDetailsResponse> upsert(@PathVariable Long warehouseId,
                                                       @RequestBody StoreDetailsRequest request) {
        return ResponseEntity.ok(service.upsert(warehouseId, request));
    }
}
