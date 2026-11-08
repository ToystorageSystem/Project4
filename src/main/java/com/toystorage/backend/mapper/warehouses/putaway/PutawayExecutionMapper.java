package com.toystorage.backend.mapper.warehouses.putaway;

import com.toystorage.backend.dto.response.warehouses.putaway.PutawayStaffItemResponse;
import com.toystorage.backend.dto.response.warehouses.putaway.PutawayStaffTaskResponse;
import com.toystorage.backend.entity.warehouses.PutawayTaskItems;
import com.toystorage.backend.entity.warehouses.PutawayTasks;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class PutawayExecutionMapper {
    public PutawayStaffItemResponse toItemResponse(PutawayTaskItems item) {
        // Destination is intentionally absent until the staff's first scan.
        WarehouseLocations from = item.getFromLocation();
        WarehouseLocations to = item.getToLocation();
        int putaway = item.getPutawayQuantity() == null ? 0 : item.getPutawayQuantity();
        int expected = item.getExpectedQuantity();
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
