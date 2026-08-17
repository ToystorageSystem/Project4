package com.toystorage.backend.mapper.warehouses;

import com.toystorage.backend.dto.response.warehouses.PutawayStaffItemResponse;
import com.toystorage.backend.dto.response.warehouses.PutawayStaffTaskResponse;

import com.toystorage.backend.entity.warehouses.PutawayTaskItems;
import com.toystorage.backend.entity.warehouses.PutawayTasks;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PutawayExecutionMapper {

    public PutawayStaffItemResponse toItemResponse(
            PutawayTaskItems item
    ) {

        int remaining =
                Math.max(
                        item.getExpectedQuantity()
                                - item.getPutawayQuantity(),
                        0
                );

        return PutawayStaffItemResponse.builder()

                .itemId(item.getId())

                .productId(
                        item.getProduct().getId()
                )

                .productCode(
                        item.getProduct()
                                .getProductsCode()
                )

                .productName(
                        item.getProduct().getName()
                )

                .barcode(
                        item.getProduct().getBarcode()
                )

                .expectedQuantity(
                        item.getExpectedQuantity()
                )

                .putawayQuantity(
                        item.getPutawayQuantity()
                )

                .remainingQuantity(
                        remaining
                )

                .fromLocationId(
                        item.getFromLocation().getId()
                )

                .fromLocationCode(
                        item.getFromLocation()
                                .getWarehouseCode()
                )

                .toLocationId(
                        item.getToLocation().getId()
                )

                .toLocationCode(
                        item.getToLocation()
                                .getWarehouseCode()
                )

                .zone(
                        item.getToLocation().getZone()
                )

                .shelf(
                        item.getToLocation().getShelf()
                )

                .status(
                        item.getStatus().name()
                )

                .build();
    }


    public PutawayStaffTaskResponse toResponse(
            PutawayTasks task,
            List<PutawayTaskItems> items
    ) {

        return PutawayStaffTaskResponse.builder()

                .taskId(task.getId())

                .taskCode(
                        task.getPutawayTasksCode()
                )

                .status(
                        task.getStatus().name()
                )

                .goodsReceiptId(
                        task.getGoodsReceipt().getId()
                )

                .receiptCode(
                        task.getGoodsReceipt()
                                .getReceiptCode()
                )

                .warehouseId(
                        task.getWarehouse().getId()
                )

                .warehouseName(
                        task.getWarehouse().getName()
                )

                .assignedTo(
                        task.getAssignedTo() != null
                                ? task.getAssignedTo().getId()
                                : null
                )

                .assignedToName(
                        task.getAssignedTo() != null
                                ? task.getAssignedTo().getName()
                                : null
                )

                .createdAt(
                        task.getCreatedAt()
                )

                .completedAt(
                        task.getCompletedAt()
                )

                .items(
                        items.stream()
                                .map(this::toItemResponse)
                                .toList()
                )

                .build();
    }
}