package com.toystorage.backend.controllers.warehouses.damagedgoods.manager;

import com.toystorage.backend.services.warehouses.manager.DailyTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.toystorage.backend.dto.request.warehouses.manager.ManagerOpsDTO.*;

/** Warehouse Staff: xem viec duoc giao hom nay, bat dau va bao hoan thanh. */
@RestController
@RequestMapping("/api/warehouse/staff/daily-tasks")
//@PreAuthorize("@managerOpsAccess.isStaff(authentication)")
@RequiredArgsConstructor
public class StaffDailyTaskController {
    private final DailyTaskService service;

    @GetMapping
    public List<Task> myTasks(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.myTasks(date);
    }

    /** Vai tro hom nay va cac viec co the tu nhan. */
    @GetMapping("/queue")
    public StaffQueue queue(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.staffQueue(date);
    }

    /** Tu nhan mot viec trong vai tro cua minh. */
    @PostMapping("/take")
    public Task take(@RequestBody TakeJobRequest request) { return service.takeJob(request); }

    @PostMapping("/{id}/start")
    public Task start(@PathVariable Long id) { return service.start(id); }

    @PostMapping("/{id}/complete")
    public Task complete(@PathVariable Long id, @RequestBody(required = false) StaffProgressRequest request) {
        return service.staffComplete(id, request);
    }
}
