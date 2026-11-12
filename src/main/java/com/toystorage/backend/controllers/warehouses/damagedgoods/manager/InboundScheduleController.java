package com.toystorage.backend.controllers.warehouses.damagedgoods.manager;

import com.toystorage.backend.services.warehouses.manager.InboundScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.toystorage.backend.dto.request.warehouses.manager.ManagerOpsDTO.Delivery;

/** Warehouse Manager: hang du kien ve kho theo ngay (de phan nguoi vao vai tro nhan hang). */
@RestController
@RequestMapping("/api/warehouse/manager/inbound-schedule")
//@PreAuthorize("@managerOpsAccess.isManager(authentication)")
@RequiredArgsConstructor
public class InboundScheduleController {
    private final InboundScheduleService service;

    @GetMapping
    public List<Delivery> schedule(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.schedule(from, to);
    }
}
