package com.toystorage.backend.controllers.warehouses.damagedgoods.manager;

import com.toystorage.backend.services.warehouses.manager.StaffPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static com.toystorage.backend.dto.request.warehouses.manager.ManagerOpsDTO.*;

/** Warehouse Manager: danh gia hieu suat lam viec cua nhan vien. */
@RestController
@RequestMapping("/api/warehouse/manager/performance")
//@PreAuthorize("@managerOpsAccess.isManager(authentication)")
@RequiredArgsConstructor
public class StaffPerformanceController {
    private final StaffPerformanceService service;

    @GetMapping
    public PerformanceBoard board(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.board(from, to);
    }

    @GetMapping("/{staffId}")
    public StaffPerformanceDetail detail(@PathVariable Long staffId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.detail(staffId, from, to);
    }

    @PostMapping("/{staffId}/reviews")
    public Review review(@PathVariable Long staffId, @RequestBody ReviewRequest request) {
        return service.addReview(staffId, request);
    }
}
