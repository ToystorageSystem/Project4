package com.toystorage.backend.controllers.warehouses.damagedgoods.manager;

import com.toystorage.backend.services.warehouses.manager.WarehouseOperationsOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.toystorage.backend.dto.request.warehouses.manager.ManagerOpsDTO.Overview;

/** Warehouse Manager: tong quan nhap / xuat / dieu chuyen, xu ly hang hong-thieu, lich kiem ke. */
@RestController
@RequestMapping("/api/warehouse/manager/operations")
@PreAuthorize("@managerOpsAccess.isManager(authentication)")
@RequiredArgsConstructor
public class WarehouseOperationsController {
    private final WarehouseOperationsOverviewService service;

    @GetMapping("/overview")
    public Overview overview() { return service.overview(); }
}
