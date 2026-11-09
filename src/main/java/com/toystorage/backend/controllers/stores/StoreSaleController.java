package com.toystorage.backend.controllers.stores;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.toystorage.backend.config.security.CustomUserDetails;
import com.toystorage.backend.dto.request.stores.CreateSaleRequest;
import com.toystorage.backend.dto.response.common.PageResponse;
import com.toystorage.backend.dto.response.stores.SaleDetail;
import com.toystorage.backend.dto.response.stores.SaleSummary;
import com.toystorage.backend.services.stores.StoreSaleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Simulated sale: shelf first, then backroom. */
@RestController
@RequestMapping("/api/store/sales")
@RequiredArgsConstructor
public class StoreSaleController {

    private final StoreSaleService saleService;

    @PostMapping
    public ResponseEntity<SaleDetail> createSale(@AuthenticationPrincipal CustomUserDetails principal,
                                                 @Valid @RequestBody CreateSaleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saleService.createSale(principal.getId(), request));
    }

    @GetMapping
    public PageResponse<SaleSummary> listSales(@AuthenticationPrincipal CustomUserDetails principal,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return saleService.listSales(principal.getId(), page, size);
    }

    @GetMapping("/{id}")
    public SaleDetail getSale(@AuthenticationPrincipal CustomUserDetails principal,
                              @PathVariable Long id) {
        return saleService.getSale(principal.getId(), id);
    }

    // ---------- local error handlers (Global would turn these into 500) ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        FieldError first = ex.getBindingResult().getFieldError();
        String message = first != null ? first.getDefaultMessage() : "Invalid request";
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request body");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value for " + ex.getName());
    }

    /** 2 sales of the same product at the same time -> @Version conflict */
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(RuntimeException ex) {
        return error(HttpStatus.CONFLICT, "Data was changed by someone else, please reload and try again");
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}