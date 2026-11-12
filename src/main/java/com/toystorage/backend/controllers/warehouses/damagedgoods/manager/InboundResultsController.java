package com.toystorage.backend.controllers.warehouses.damagedgoods.manager;

import com.toystorage.backend.services.warehouses.manager.InboundResultsService;
import com.toystorage.backend.services.warehouses.manager.InboundResultsService.DecisionRequest;
import com.toystorage.backend.services.warehouses.manager.InboundResultsService.Issue;
import com.toystorage.backend.services.warehouses.manager.InboundResultsService.Lot;
import com.toystorage.backend.services.warehouses.manager.InboundResultsService.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Warehouse Manager: ket qua nhap hang (NCC + cua hang tra), lo hang hong, quyet dinh chenh lech. */
@RestController
@RequestMapping("/api/warehouse/manager/inbound-results")
//@PreAuthorize("@managerOpsAccess.isManager(authentication)")
@RequiredArgsConstructor
public class InboundResultsController {
    private final InboundResultsService service;

    /** type = SUPPLIER (mac dinh) hoac STORE_RETURN. */
    @GetMapping
    public List<Result> results(@RequestParam(required = false) String type, @RequestParam(required = false) Integer days) {
        return service.results(type, days);
    }

    @GetMapping("/stock-count-issues")
    public List<Issue> stockCountIssues(@RequestParam(required = false) Integer days) { return service.stockCountIssues(days); }

    @GetMapping("/damaged-lots")
    public List<Lot> damagedLots() { return service.damagedLots(); }

    /** decision = INTERNAL (luu noi bo) hoac BUSINESS (gui Business giai quyet). */
    @PostMapping("/discrepancies/{discrepancyId}/decision")
    public Issue decide(@PathVariable Long discrepancyId, @RequestBody DecisionRequest request) {
        return service.decide(discrepancyId, request);
    }
}
