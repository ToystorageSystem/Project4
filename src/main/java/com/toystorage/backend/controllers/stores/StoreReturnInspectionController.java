package com.toystorage.backend.controllers.stores;


import com.toystorage.backend.dto.request.stores.InspectStoreReturnItemRequest;
import com.toystorage.backend.dto.response.stores.StoreReturnInspectionResponse;

import com.toystorage.backend.services.stores.StoreReturnInspectionService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse/store-returns")
@RequiredArgsConstructor
public class StoreReturnInspectionController {

    private final StoreReturnInspectionService
            storeReturnInspectionService;


    @GetMapping
//    @PreAuthorize(
//            "hasAuthority('STORE_RETURN_INSPECTION_VIEW')"
//    )
    public ResponseEntity<List<StoreReturnInspectionResponse>>
    getStoreReturns() {

        return ResponseEntity.ok(
                storeReturnInspectionService
                        .getStoreReturns()
        );
    }


    @GetMapping("/{returnId}")
//    @PreAuthorize(
//            "hasAuthority('STORE_RETURN_INSPECTION_VIEW')"
//    )
    public ResponseEntity<StoreReturnInspectionResponse>
    getStoreReturn(
            @PathVariable Long returnId
    ) {

        return ResponseEntity.ok(
                storeReturnInspectionService
                        .getStoreReturn(returnId)
        );
    }


    @PatchMapping(
            "/{returnId}/items/{itemId}/inspect"
    )
//    @PreAuthorize(
//            "hasAuthority('STORE_RETURN_INSPECTION_HANDLE')"
//    )
    public ResponseEntity<StoreReturnInspectionResponse>
    inspectItem(
            @PathVariable Long returnId,
            @PathVariable Long itemId,

            @Valid
            @RequestBody
            InspectStoreReturnItemRequest request
    ) {

        return ResponseEntity.ok(
                storeReturnInspectionService
                        .inspectItem(
                                returnId,
                                itemId,
                                request
                        )
        );
    }
}