package com.toystorage.backend.controllers.stores;

import com.toystorage.backend.config.security.CustomUserDetails;
import com.toystorage.backend.dto.response.common.PageResponse;
import com.toystorage.backend.dto.response.stores.StoreProductDetail;
import com.toystorage.backend.dto.response.stores.StoreProductSummary;
import com.toystorage.backend.services.stores.StoreProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Store app – product lookup (phase 1). Read only (GET) -> no CSRF header needed.
 * Access (store role + user's store) is checked in StoreAccessService.
 */
@RestController
@RequestMapping("/api/store/products")
@RequiredArgsConstructor
public class StoreProductController {

    private final StoreProductService storeProductService;

    /** GET /api/store/products?keyword=&categoryId=&page=0&size=20 */
    @GetMapping
    public ResponseEntity<PageResponse<StoreProductSummary>> search(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                storeProductService.search(principal.getId(), keyword, categoryId, page, size));
    }

    /** GET /api/store/products/barcode/{barcode} */
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<StoreProductDetail> getByBarcode(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String barcode) {
        return ResponseEntity.ok(storeProductService.getByBarcode(principal.getId(), barcode));
    }

    /** GET /api/store/products/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<StoreProductDetail> getById(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(storeProductService.getById(principal.getId(), id));
    }

    /**
     * "abc" for id / page / size / categoryId -> 400.
     * Without this, Global's @ExceptionHandler(Exception.class) would answer 500.
     * Same JSON shape as Global.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("message", "Invalid value for '" + ex.getName() + "'");
        return ResponseEntity.badRequest().body(body);
    }
}