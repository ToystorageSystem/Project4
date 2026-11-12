package com.toystorage.backend.mapper.warehouses.putaway;

import com.toystorage.backend.dto.response.warehouses.putaway.PutawayStaffItemResponse;
import com.toystorage.backend.dto.response.warehouses.putaway.PutawayStaffTaskResponse;
import com.toystorage.backend.entity.warehouses.PutawayTaskItems;
import com.toystorage.backend.entity.warehouses.PutawayTasks;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.services.warehouses.putaway.PutawaySuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PutawayExecutionMapper {

    private final PutawaySuggestionService suggestionService;

    public PutawayStaffItemResponse toItemResponse(PutawayTaskItems item) {
        // Destination is intentionally absent until the staff's first scan.
        WarehouseLocations from = item.getFromLocation();
        WarehouseLocations to = item.getToLocation();
        int putaway = item.getPutawayQuantity() == null ? 0 : item.getPutawayQuantity();
        int expected = item.getExpectedQuantity();
        PutawaySuggestionService.Suggestion hint = to != null || item.getPutawayTask() == null
                || item.getPutawayTask().getWarehouse() == null || putaway >= expected
                ? null
                : suggestionService.suggest(item.getPutawayTask().getWarehouse().getId(),
                        item.getProduct().getId()).orElse(null);
        return PutawayStaffItemResponse.builder()
                .itemId(item.getId())
                .productId(item.getProduct().getId())
                .productCode(item.getProduct().getProductsCode())
                .productName(item.getProduct().getName())
                .barcode(item.getProduct().getBarcode())
                .expectedQuantity(expected)
                .putawayQuantity(putaway)
                .remainingQuantity(Math.max(expected - putaway, 0))
                .fromLocationId(from == null ? null : from.getId())
                .fromLocationCode(from == null ? null : from.getWarehouseCode())
                .toLocationId(to == null ? null : to.getId())
                .toLocationCode(to == null ? null : to.getWarehouseCode())
                .zone(to == null ? null : to.getZone())
                .shelf(to == null ? null : to.getShelf())
                .status(item.getStatus().name())
                .suggestedLocationId(hint == null ? null : hint.locationId())
                .suggestedLocationCode(hint == null ? null : hint.locationCode())
                .suggestedZone(hint == null ? null : hint.zone())
                .suggestedShelf(hint == null ? null : hint.shelf())
                .suggestedReason(hint == null ? null : hint.reason())
                .build();
    }

    public PutawayStaffTaskResponse toResponse(PutawayTasks task, List<PutawayTaskItems> items) {
        return PutawayStaffTaskResponse.builder()
                .taskId(task.getId())
                .taskCode(task.getPutawayTasksCode())
                .status(task.getStatus().name())
                .goodsReceiptId(task.getGoodsReceipt().getId())
                .receiptCode(task.getGoodsReceipt().getReceiptCode())
                .warehouseId(task.getWarehouse().getId())
                .warehouseName(task.getWarehouse().getName())
                .assignedTo(task.getAssignedTo() == null ? null : task.getAssignedTo().getId())
                .assignedToName(task.getAssignedTo() == null ? null : task.getAssignedTo().getName())
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt())
                .items(items.stream().map(this::toItemResponse).toList())
                .build();
    }
}
