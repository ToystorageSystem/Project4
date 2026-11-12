package com.toystorage.backend.controllers.warehouses.damagedgoods.manager;

import com.toystorage.backend.controllers.inventories.lowstock.BusinessLowStockReportController;
import com.toystorage.backend.controllers.inventories.lowstock.ManagerLowStockController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Tra loi loi dung ma HTTP (400/403/404/409) cho cac API quan ly kho moi.
 * Chi ap dung cho cac controller moi, khong thay doi xu ly loi chung cua he thong.
 */
@RestControllerAdvice(assignableTypes = {
        ManagerDailyTaskController.class, StaffDailyTaskController.class, StaffPerformanceController.class,
        WarehouseOperationsController.class, ManagerLowStockController.class, BusinessLowStockReportController.class
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ManagerOpsErrorAdvice {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "status", ex.getStatusCode().value(),
                "message", ex.getReason() == null ? "Request failed" : ex.getReason()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> denied(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(Map.of("status", 403,
                "message", "You do not have permission to perform this action."));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> invalid(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("status", 400, "message", "Invalid input data."));
    }
}
