package com.toystorage.backend.controllers.stores;

import com.toystorage.backend.dto.request.stores.ShelfRequest;
import com.toystorage.backend.dto.request.stores.ShelfStatusRequest;
import com.toystorage.backend.dto.response.stores.ShelfAdminItem;
import com.toystorage.backend.services.stores.StoreShelfAdminService;
import org.springframework.web.bind.annotation.PatchMapping;
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
import org.springframework.validation.FieldError;
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
import com.toystorage.backend.dto.request.stores.AssignShelfRequest;
import com.toystorage.backend.dto.request.stores.MinShelfRequest;
import com.toystorage.backend.dto.request.stores.MoveStockRequest;
import com.toystorage.backend.dto.response.stores.LowShelfItem;
import com.toystorage.backend.dto.response.stores.ShelfDetail;
import com.toystorage.backend.dto.response.stores.ShelfSummary;
import com.toystorage.backend.dto.response.stores.StoreProductDetail;
import com.toystorage.backend.services.stores.StoreShelfService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Shelf placement: shelves, assign product to shelf, move backroom <-> shelf, low-shelf alert. */
@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreShelfController {

    private final StoreShelfService shelfService;
    private final StoreShelfAdminService shelfAdminService;

    @GetMapping("/shelves")
    public List<ShelfSummary> listShelves(@AuthenticationPrincipal CustomUserDetails principal) {
        return shelfService.listShelves(principal.getId());
    }

    @GetMapping("/shelves/{id}")
    public ShelfDetail getShelf(@AuthenticationPrincipal CustomUserDetails principal,
                                @PathVariable Long id) {
        return shelfService.getShelf(principal.getId(), id);
    }

    // ---------- shelf management: store manager only (checked in service) ----------

    /** literal path -> wins over /shelves/{id} */
    @GetMapping("/shelves/manage")
    public List<ShelfAdminItem> listAllShelves(@AuthenticationPrincipal CustomUserDetails principal) {
        return shelfAdminService.listAll(principal.getId());
    }

    @PostMapping("/shelves")
    public ResponseEntity<ShelfAdminItem> createShelf(@AuthenticationPrincipal CustomUserDetails principal,
                                                      @Valid @RequestBody ShelfRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shelfAdminService.create(principal.getId(), request));
    }

    @PutMapping("/shelves/{id}")
    public ShelfAdminItem updateShelf(@AuthenticationPrincipal CustomUserDetails principal,
                                      @PathVariable Long id,
                                      @Valid @RequestBody ShelfRequest request) {
        return shelfAdminService.update(principal.getId(), id, request);
    }

    @PatchMapping("/shelves/{id}/status")
    public ShelfAdminItem changeShelfStatus(@AuthenticationPrincipal CustomUserDetails principal,
                                            @PathVariable Long id,
                                            @Valid @RequestBody ShelfStatusRequest request) {
        return shelfAdminService.changeStatus(principal.getId(), id, request);
    }

    /** literal path -> wins over /products/{id} of StoreProductController */
    @GetMapping("/products/low-shelf")
    public List<LowShelfItem> lowShelf(@AuthenticationPrincipal CustomUserDetails principal) {
        return shelfService.lowShelf(principal.getId());
    }

    /** body {"shelfId": 5} or {"shelfId": null} to remove */
    @PutMapping("/products/{productId}/shelf")
    public StoreProductDetail assignShelf(@AuthenticationPrincipal CustomUserDetails principal,
                                          @PathVariable Long productId,
                                          @RequestBody AssignShelfRequest request) {
        return shelfService.assignShelf(principal.getId(), productId, request);
    }

    @PostMapping("/products/{productId}/move")
    public StoreProductDetail moveStock(@AuthenticationPrincipal CustomUserDetails principal,
                                        @PathVariable Long productId,
                                        @Valid @RequestBody MoveStockRequest request) {
        return shelfService.moveStock(principal.getId(), productId, request);
    }

    /** store manager only (checked in service) */
    @PutMapping("/products/{productId}/min-shelf")
    public StoreProductDetail changeMinShelf(@AuthenticationPrincipal CustomUserDetails principal,
                                             @PathVariable Long productId,
                                             @Valid @RequestBody MinShelfRequest request) {
        return shelfService.changeMinShelf(principal.getId(), productId, request);
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