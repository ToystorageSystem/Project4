package com.toystorage.backend.controllers.inventories.lowstock;

import com.toystorage.backend.services.inventories.lowstock.LowStockReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.toystorage.backend.dto.request.warehouses.manager.ManagerOpsDTO.*;

/** Warehouse Manager: theo doi hang sap het va gui bao cao cho Business. */
@RestController
@RequestMapping("/api/warehouse/manager/low-stock")
//@PreAuthorize("@managerOpsAccess.isManager(authentication)")
@RequiredArgsConstructor
public class ManagerLowStockController {
    private final LowStockReportService service;

    /** onlyLow=false tra ve toan bo san pham (de dat muc toi thieu). */
    @GetMapping
    public LowStockPreview preview(@RequestParam(required = false) Integer threshold,
                                   @RequestParam(defaultValue = "true") boolean onlyLow) {
        return onlyLow ? service.preview(threshold) : service.allProducts(threshold);
    }

    @PatchMapping("/products/{productId}/minimum")
    public LowStockLine minimum(@PathVariable Long productId, @RequestBody MinimumRequest request) {
        return service.updateMinimum(productId, request);
    }

    @PostMapping("/reports")
    public LowStockReportDetail send(@RequestBody(required = false) SendLowStockRequest request) {
        return service.send(request);
    }

    @GetMapping("/reports")
    public List<LowStockReportSummary> reports() { return service.managerReports(); }

    @GetMapping("/reports/{id}")
    public LowStockReportDetail detail(@PathVariable Long id) { return service.managerDetail(id); }
}
