package com.toystorage.backend.stockcount;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class StockCountDTO {
    private StockCountDTO() {}
    public record Plan(Long id, String code, String type, LocalDate date, String status,
                       String warehouse, Long assignedToId, String assignedTo,
                       String scopeType, String scopeValue, int total, int counted, int differences,
                       String confirmedBy, LocalDateTime confirmedAt) {}
    public record Item(Long id, Long productId, String sku, String name, String barcode,
                       Long locationId, String location, String zone, String shelf,
                       int system, Integer first, Integer second, Integer finalQuantity,
                       Integer difference, boolean recountRequested, String firstBy,
                       String secondBy, String correctionReason) {}
    public record Detail(Plan plan, List<Item> items, List<AuditEntry> audit,
                         int catalogTotal, int coveredSkus, List<String> missingSkus) {}
    public record AuditEntry(String action, String actor, Long itemId,
                             Integer before, Integer after, String reason, LocalDateTime at) {}
    public record Notice(Long id, Long stockCountId, String code, LocalDate date, String type, LocalDateTime createdAt) {}
    public record Staff(Long id, String name, String email) {}
    public record ScopeRequest(Long staffId, String scopeType, String scopeValue) {}
    public record CreateRequest(String scopeType, String scopeValue, Long staffId) {}
    public record CountRequest(Integer quantity, String barcode, String locationCode) {}
    public record RecountRequest(List<Long> itemIds, Long staffId, String reason) {}
    public record CorrectionRequest(Integer quantity, String reason) {}
    public record CountLocation(Long id, String code, String name, String zone, String shelf) {}
    public record StartManagerRequest(Long fallbackLocationId) {}
    public record ManagerCountLine(Long itemId, java.math.BigDecimal quantity) {}
    public record ManagerCountRequest(List<ManagerCountLine> items) {}
    // A manager explicitly reviews EVERY item, including lines with zero difference.
    // The finalQuantity echo catches a stale detail view or concurrent corrections.
    public record ConfirmAllLine(Long itemId, Integer finalQuantity) {}
    public record ConfirmAllRequest(List<ConfirmAllLine> items) {}
}
