package com.toystorage.backend.controllers.stores.returns;

import com.toystorage.backend.dto.request.stores.returns.InspectStoreReturnItemRequest;
import com.toystorage.backend.dto.request.stores.returns.ResolveStoreReturnDiscrepancyRequest;
import com.toystorage.backend.dto.response.stores.returns.StoreReturnInspectionResponse;
import com.toystorage.backend.services.stores.returns.StoreReturnInspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse/store-returns")
@RequiredArgsConstructor
public class StoreReturnInspectionController {

    private final StoreReturnInspectionService storeReturnInspectionService;

    @GetMapping
    public ResponseEntity<List<StoreReturnInspectionResponse>> getStoreReturns() {
        return ResponseEntity.ok(
                storeReturnInspectionService.getStoreReturns()
        );
    }

    @GetMapping("/{returnId}")
    public ResponseEntity<StoreReturnInspectionResponse> getStoreReturn(
            @PathVariable Long returnId
    ) {
        return ResponseEntity.ok(
                storeReturnInspectionService.getStoreReturn(returnId)
        );
    }

    @PatchMapping("/{returnId}/items/{itemId}/inspect")
    public ResponseEntity<StoreReturnInspectionResponse> inspectItem(
            @PathVariable Long returnId,
            @PathVariable Long itemId,
            @Valid @RequestBody InspectStoreReturnItemRequest request
    ) {
        return ResponseEntity.ok(
                storeReturnInspectionService.inspectItem(
                        returnId,
                        itemId,
                        request
                )
        );
    }

    @PatchMapping("/{returnId}/discrepancies/{discrepancyId}/warehouse")
    public ResponseEntity<Void> resolveAsWarehouse(
            @PathVariable Long returnId,
            @PathVariable Long discrepancyId,
            @Valid @RequestBody ResolveStoreReturnDiscrepancyRequest request
    ) {
        storeReturnInspectionService.resolveAsWarehouse(
                returnId,
                discrepancyId,
                request.getReason()
        );
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{returnId}/discrepancies/{discrepancyId}/send-store")
    public ResponseEntity<Void> sendToStoreManager(
            @PathVariable Long returnId,
            @PathVariable Long discrepancyId,
            @Valid @RequestBody ResolveStoreReturnDiscrepancyRequest request
    ) {
        storeReturnInspectionService.sendToStoreManager(
                returnId,
                discrepancyId,
                request.getReason()
        );
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{returnId}/discrepancies/{discrepancyId}/store-confirm")
    public ResponseEntity<Void> confirmByStore(
            @PathVariable Long returnId,
            @PathVariable Long discrepancyId,
            @Valid @RequestBody ResolveStoreReturnDiscrepancyRequest request
    ) {
        storeReturnInspectionService.confirmByStore(
                returnId,
                discrepancyId,
                request.getReason()
        );
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{returnId}/discrepancies/{discrepancyId}/store-reject")
    public ResponseEntity<Void> rejectAndEscalateToBusiness(
            @PathVariable Long returnId,
            @PathVariable Long discrepancyId,
            @Valid @RequestBody ResolveStoreReturnDiscrepancyRequest request
    ) {
        storeReturnInspectionService.rejectAndEscalateToBusiness(
                returnId,
                discrepancyId,
                request.getReason()
        );
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{returnId}/discrepancies/{discrepancyId}/business-resolve")
    public ResponseEntity<Void> resolveAsBusiness(
            @PathVariable Long returnId,
            @PathVariable Long discrepancyId,
            @Valid @RequestBody ResolveStoreReturnDiscrepancyRequest request
    ) {
        storeReturnInspectionService.resolveAsBusiness(
                returnId,
                discrepancyId,
                request.getReason()
        );
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{returnId}/confirm")
    public ResponseEntity<StoreReturnInspectionResponse> confirmStoreReturn(
            @PathVariable Long returnId
    ) {
        return ResponseEntity.ok(
                storeReturnInspectionService.confirmStoreReturn(returnId)
        );
    }
}
