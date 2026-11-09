package com.toystorage.backend.controllers.receipts.store;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.toystorage.backend.config.security.CustomUserDetails;
import com.toystorage.backend.dto.request.receipts.store.ConfirmReceivingRequest;
import com.toystorage.backend.dto.request.receipts.store.PackageCountRequest;
import com.toystorage.backend.dto.request.receipts.store.ScanPackageRequest;
import com.toystorage.backend.dto.response.receipts.store.ManifestDetailResponse;
import com.toystorage.backend.dto.response.receipts.store.ManifestSummaryResponse;
import com.toystorage.backend.dto.response.receipts.store.PackageCheckDetailResponse;
import com.toystorage.backend.dto.response.receipts.store.ReceivingResultResponse;
import com.toystorage.backend.services.receipts.store.StoreReceivingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Store receiving by manifest (Store Staff + Store Manager; confirm = Store Manager).
 * Access is checked in StoreAccessService (not @PreAuthorize: Global would turn it into 500).
 * POST/PUT need the X-CSRF-TOKEN header (team session security).
 */
@RestController
@RequestMapping("/api/store/receiving")
@RequiredArgsConstructor
public class StoreReceivingController {

    private final StoreReceivingService service;

    // ---------- manifests ----------

    @GetMapping("/manifests")
    public ResponseEntity<List<ManifestSummaryResponse>> listManifests(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(service.listManifests(principal.getId()));
    }

    @GetMapping("/manifests/{manifestCode}")
    public ResponseEntity<ManifestDetailResponse> getManifest(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String manifestCode) {
        return ResponseEntity.ok(service.getManifest(principal.getId(), manifestCode));
    }

    // ---------- F1: receive packages ----------

    @PostMapping("/manifests/{manifestCode}/packages/scan")
    public ResponseEntity<ManifestDetailResponse> scanPackage(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String manifestCode,
            @Valid @RequestBody ScanPackageRequest request) {
        return ResponseEntity.ok(service.scanPackage(principal.getId(), manifestCode, request));
    }

    @PostMapping("/manifests/{manifestCode}/complete-receiving")
    public ResponseEntity<ManifestDetailResponse> completeReceiving(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String manifestCode) {
        return ResponseEntity.ok(service.completeReceiving(principal.getId(), manifestCode));
    }

    // ---------- F2: count products in a package ----------

    @GetMapping("/packages/{packageCode}/items")
    public ResponseEntity<PackageCheckDetailResponse> getPackageItems(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String packageCode) {
        return ResponseEntity.ok(service.getPackageItems(principal.getId(), packageCode));
    }

    @PutMapping("/packages/{packageCode}/items")
    public ResponseEntity<PackageCheckDetailResponse> savePackageCounts(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String packageCode,
            @Valid @RequestBody PackageCountRequest request) {
        return ResponseEntity.ok(service.savePackageCounts(principal.getId(), packageCode, request));
    }

    @PostMapping("/manifests/{manifestCode}/finish-inspection")
    public ResponseEntity<ReceivingResultResponse> finishInspection(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String manifestCode) {
        return ResponseEntity.ok(service.finishInspection(principal.getId(), manifestCode));
    }

    // ---------- confirm (Store Manager) ----------

    @PostMapping("/manifests/{manifestCode}/confirm")
    public ResponseEntity<ReceivingResultResponse> confirm(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String manifestCode,
            @Valid @RequestBody(required = false) ConfirmReceivingRequest request) {
        return ResponseEntity.ok(service.confirm(principal.getId(), manifestCode, request));
    }

    // =====================================================================
    // Errors Global does not handle (it would answer 500). Same JSON shape as Global.
    // Only for this controller.
    // =====================================================================

    /** @Valid failed -> 400 with the first field message, e.g. "Seal status is required" */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getDefaultMessage())
                .orElse("Invalid request");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /** broken JSON or a wrong enum value, e.g. "sealStatus": "OK" -> 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request body");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value for '" + ex.getName() + "'");
    }

    /** two people changed the same stock row at the same time (@Version) -> try again */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return error(HttpStatus.CONFLICT, "Data was changed by someone else, please reload and try again");
    }

    /** a unique key was hit (should not happen thanks to the checks + lock) -> 409 instead of 500 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(DataIntegrityViolationException ex) {
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