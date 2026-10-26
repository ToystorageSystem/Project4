package com.toystorage.backend.stockcount;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import static com.toystorage.backend.stockcount.StockCountDTO.*;

/** Warehouse Manager API. Does not replace or change Staff routes. */
@RestController
@RequestMapping("/api/warehouse/manager/stock-counts")
@PreAuthorize("@stockCountManagerAccess.canManage(authentication)")
@RequiredArgsConstructor
public class StockCountManagerController {
    private final StockCountWorkflowService service;

    @GetMapping
    public List<Plan> list() { return service.list(); }

    @GetMapping("/{id}")
    public Detail detail(@PathVariable Long id) { return service.detail(id); }

    // Only real active storage locations from the logged-in Manager's warehouse.
    @GetMapping("/locations")
    public List<CountLocation> locations() { return service.locations(); }

    // A manager selects assigned employees, but this package does not modify staff screens.
    @GetMapping("/staff")
    public List<Staff> availableEmployees() { return service.staff(); }

    @PostMapping
    public Plan create(@RequestBody CreateRequest request) { return service.createAdHoc(request); }

    @PostMapping("/full")
    public Plan createFullToday() { return service.createFullToday(); }

    @PatchMapping("/{id}/assign")
    public Plan assign(@PathVariable Long id, @RequestBody ScopeRequest request) { return service.assign(id, request); }

    @PostMapping("/{id}/recount")
    public Plan recount(@PathVariable Long id, @RequestBody RecountRequest request) {
        return service.requestRecount(id, request);
    }

    @PatchMapping("/{id}/items/{itemId}/correct")
    public Detail correct(@PathVariable Long id, @PathVariable Long itemId,
                          @RequestBody CorrectionRequest request) {
        return service.correct(id, itemId, request);
    }

    // One Manager can take responsibility for an unassigned PLANNED count.
    // This snapshots actual balances on the server; the preview never changes inventory.
    @PostMapping("/{id}/start-manager")
    public Plan startManager(@PathVariable Long id, @RequestBody(required = false) StartManagerRequest request) {
        return service.startManager(id, request);
    }

    // One atomic transaction: save counts, verify snapshots, update stock and audit.
    @PostMapping("/{id}/confirm-manager")
    public Plan confirmManager(@PathVariable Long id, @RequestBody ManagerCountRequest request) {
        return service.confirmManager(id, request);
    }

    @PostMapping("/{id}/confirm")
    public Plan confirm(@PathVariable Long id, @RequestBody ConfirmAllRequest request) {
        return service.confirmAll(id, request);
    }

    @PostMapping("/{id}/cancel")
    public Plan cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return service.cancel(id, body == null ? null : body.get("reason"));
    }

    @GetMapping("/notifications")
    public List<Notice> notifications() { return service.notifications(); }

    @PatchMapping("/notifications/{id}/read")
    public void markRead(@PathVariable Long id) { service.readNotice(id); }
}
