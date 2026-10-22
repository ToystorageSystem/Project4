
package com.toystorage.backend.controllers.businessmanager;

import com.toystorage.backend.dto.response.businessmanager.StockTransactionResponse;
import com.toystorage.backend.dto.response.businessmanager.StockTransactionDetailResponse;

import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.enums.transfers.TransferStatus;

import com.toystorage.backend.services.businessmanager.StockTransactionService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/business-manager/stock-transactions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ROLE-BIZ-MANAGER')")
public class StockTransactionController {

    private final StockTransactionService stockTransactionService;

    // ==========================================
    // 1. DANH SACH PHIEU NHAP
    // ==========================================

    @GetMapping("/imports")
    public ResponseEntity<Page<StockTransactionResponse>> getImports(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) GoodsReceiptStatus status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                stockTransactionService.getImportTransactions(
                        keyword,
                        status,
                        warehouseId,
                        fromDate == null
                                ? null : fromDate.atStartOfDay(),
                        toDate == null
                                ? null : toDate.plusDays(1).atStartOfDay(),
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // 2. DANH SACH PHIEU XUAT
    // ==========================================

    @GetMapping("/exports")
    public ResponseEntity<Page<StockTransactionResponse>> getExports(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                stockTransactionService.getExportTransactions(
                        keyword,
                        status,
                        warehouseId,
                        fromDate == null
                                ? null : fromDate.atStartOfDay(),
                        toDate == null
                                ? null : toDate.plusDays(1).atStartOfDay(),
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // 3. CHI TIET PHIEU NHAP - ISSUE #21
    // ==========================================

    @GetMapping("/imports/{id}")
    public ResponseEntity<StockTransactionDetailResponse> getImportDetail(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                stockTransactionService.getImportTransactionDetail(id)
        );
    }

    // ==========================================
    // 4. CHI TIET PHIEU XUAT - ISSUE #21
    // ==========================================

    @GetMapping("/exports/{id}")
    public ResponseEntity<StockTransactionDetailResponse> getExportDetail(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                stockTransactionService.getExportTransactionDetail(id)
        );
    }

    // ==========================================
    // 5. PHAN TRANG
    // ==========================================

    private Pageable createPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        return PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }
}
