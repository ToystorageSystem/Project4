package com.toystorage.backend.controllers.stores;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.toystorage.backend.config.security.CustomUserDetails;
import com.toystorage.backend.dto.response.common.PageResponse;
import com.toystorage.backend.dto.response.stores.DashboardResponse;
import com.toystorage.backend.dto.response.stores.StockTransactionLine;
import com.toystorage.backend.enums.stores.StockTransactionType;
import com.toystorage.backend.services.stores.StoreReportService;

import lombok.RequiredArgsConstructor;

/** Read-only endpoints for the store manager web. */
@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreReportController {

    private final StoreReportService reportService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal CustomUserDetails principal) {
        return reportService.dashboard(principal.getId());
    }

    /** all filters optional; type = RECEIVE | MOVE_TO_SHELF | MOVE_TO_BACKROOM | SALE | ADJUST */
    @GetMapping("/stock-transactions")
    public PageResponse<StockTransactionLine> stockTransactions(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) StockTransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reportService.stockTransactions(principal.getId(), productId, type, from, to, page, size);
    }

    // ---------- local error handler (Global would turn this into 500) ----------

    /** wrong type / date format / number in query string */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("message", "Invalid value for " + ex.getName());
        return ResponseEntity.badRequest().body(body);
    }
}
