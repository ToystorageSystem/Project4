package com.toystorage.backend.controllers.warehouses.damagedgoods.manager;

import com.toystorage.backend.services.warehouses.manager.DailyTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.toystorage.backend.dto.request.warehouses.manager.ManagerOpsDTO.*;

/** Warehouse Manager: phan cong cong viec hom nay cho nhan vien. */
@RestController
@RequestMapping("/api/warehouse/manager/daily-tasks")
@PreAuthorize("@managerOpsAccess.isManager(authentication)")
@RequiredArgsConstructor
public class ManagerDailyTaskController {
    private final DailyTaskService service;

    @GetMapping
    public DayBoard board(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.board(date);
    }

    @GetMapping("/staff")
    public List<StaffOption> staff() { return service.staff(); }

    @PostMapping
    public Task create(@RequestBody TaskRequest request) { return service.create(request); }

    @PostMapping("/bulk")
    public List<Task> createBulk(@RequestBody BulkTaskRequest request) { return service.createBulk(request); }

    @PostMapping("/copy")
    public List<Task> copy(@RequestBody CopyTasksRequest request) { return service.copy(request); }

    @PutMapping("/{id}")
    public Task update(@PathVariable Long id, @RequestBody TaskRequest request) { return service.update(id, request); }

    @PostMapping("/{id}/complete")
    public Task complete(@PathVariable Long id, @RequestBody(required = false) StaffProgressRequest request) {
        return service.managerComplete(id, request);
    }

    @PostMapping("/{id}/cancel")
    public Task cancel(@PathVariable Long id, @RequestBody(required = false) CancelRequest request) {
        return service.cancel(id, request);
    }
}
