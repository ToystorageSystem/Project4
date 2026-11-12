package com.toystorage.backend.controllers.inventories.lowstock;

import com.toystorage.backend.services.inventories.lowstock.LowStockReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.toystorage.backend.dto.request.warehouses.manager.ManagerOpsDTO.*;

/** Business Operations: tiep nhan bao cao sap het hang tu kho va ghi nhan ke hoach bo sung. */
@RestController
@RequestMapping("/api/business/low-stock-reports")
//@PreAuthorize("@managerOpsAccess.isBusiness(authentication)")
@RequiredArgsConstructor
public class BusinessLowStockReportController {
    private final LowStockReportService service;

    @GetMapping
    public List<LowStockReportSummary> list(@RequestParam(required = false) String status) {
        return service.businessReports(status);
    }

    @GetMapping("/{id}")
    public LowStockReportDetail detail(@PathVariable Long id) { return service.businessDetail(id); }

    @PatchMapping("/{id}/acknowledge")
    public LowStockReportDetail acknowledge(@PathVariable Long id, @RequestBody(required = false) BusinessHandleRequest request) {
        return service.acknowledge(id, request);
    }

    @PatchMapping("/{id}/resolve")
    public LowStockReportDetail resolve(@PathVariable Long id, @RequestBody BusinessHandleRequest request) {
        return service.resolve(id, request);
    }
}
