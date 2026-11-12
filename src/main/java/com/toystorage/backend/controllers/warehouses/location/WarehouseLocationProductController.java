package com.toystorage.backend.controllers.warehouses.location;

import com.toystorage.backend.services.warehouses.location.WarehouseLocationProductService;
import com.toystorage.backend.services.warehouses.location.WarehouseLocationProductService.AssignRequest;
import com.toystorage.backend.services.warehouses.location.WarehouseLocationProductService.LocationProducts;
import com.toystorage.backend.services.warehouses.location.WarehouseLocationProductService.ProductRef;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** San pham gan voi vi tri / khu vuc kho. Manager sua; Manager va Staff xem. */
@RestController
@RequestMapping("/api/warehouse/location-products")
//@PreAuthorize("@managerOpsAccess.isManager(authentication) or @managerOpsAccess.isStaff(authentication)")
@RequiredArgsConstructor
public class WarehouseLocationProductController {
    private final WarehouseLocationProductService service;

    @GetMapping
    public List<LocationProducts> all() { return service.forWarehouse(); }

    @GetMapping("/options")
    public List<ProductRef> options(@RequestParam(required = false) String keyword) { return service.productOptions(keyword); }

    @GetMapping("/locations/{locationId}")
    public List<ProductRef> forLocation(@PathVariable Long locationId) { return service.forLocation(locationId); }

    @PutMapping("/locations/{locationId}")
//    @PreAuthorize("@managerOpsAccess.isManager(authentication)")
    public List<ProductRef> replace(@PathVariable Long locationId, @RequestBody AssignRequest request) {
        return service.replace(locationId, request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("status", ex.getStatusCode().value(),
                "message", ex.getReason() == null ? "Request failed" : ex.getReason()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> denied(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(Map.of("status", 403, "message", "You do not have permission to perform this action."));
    }
}
