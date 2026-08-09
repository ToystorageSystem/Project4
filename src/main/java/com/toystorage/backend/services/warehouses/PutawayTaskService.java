package com.toystorage.backend.services.warehouses;

import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.PutawayTaskItems;
import com.toystorage.backend.entity.warehouses.PutawayTasks;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.enums.warehouses.PutawayTaskItemStatus;
import com.toystorage.backend.enums.warehouses.PutawayTaskStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.repository.warehouses.PutawayTaskItemRepository;
import com.toystorage.backend.repository.warehouses.PutawayTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PutawayTaskService {

    private final PutawayTaskRepository putawayTaskRepository;
    private final PutawayTaskItemRepository putawayTaskItemRepository;

    private final WarehouseLocationService warehouseLocationService;

    @Transactional
    public PutawayTasks createFromGoodsReceipt(
            GoodsReceipts receipt,
            List<GoodsReceiptItems> items,
            Users manager,
            WarehouseLocations receivingLocation
    ) {

        if (putawayTaskRepository
                .existsByGoodsReceiptId(receipt.getId())) {

            throw new BadRequest(
                    "Putaway task already exists for this receipt"
            );
        }

        PutawayTasks task =
                new PutawayTasks();

        task.setPutawayTasksCode(
                generateCode("PA")
        );

        task.setGoodsReceipt(receipt);

        task.setWarehouse(
                receipt.getWarehouse()
        );

        task.setCreatedBy(manager);

        task.setStatus(
                PutawayTaskStatus.PENDING
        );

        task.setCreatedAt(
                LocalDateTime.now()
        );

        PutawayTasks savedTask =
                putawayTaskRepository.save(task);

        for (GoodsReceiptItems item : items) {

            if (item.getAcceptedQuantity() == null
                    || item.getAcceptedQuantity() <= 0) {
                continue;
            }

            WarehouseLocations destination =
                    warehouseLocationService
                            .getDestinationLocation(
                                    receipt.getWarehouse().getId(),
                                    item.getProduct().getId()
                            );

            PutawayTaskItems taskItem =
                    new PutawayTaskItems();

            taskItem.setPutawayTaskItemsCode(
                    generateCode("PAI")
            );

            taskItem.setPutawayTask(savedTask);

            taskItem.setProduct(
                    item.getProduct()
            );

            taskItem.setExpectedQuantity(
                    item.getAcceptedQuantity()
            );

            taskItem.setPutawayQuantity(0);

            taskItem.setStatus(
                    PutawayTaskItemStatus.PENDING
            );

            taskItem.setFromLocation(
                    receivingLocation
            );

            taskItem.setToLocation(
                    destination
            );

            putawayTaskItemRepository.save(
                    taskItem
            );
        }

        return savedTask;
    }

    private String generateCode(String prefix) {

        return prefix
                + "-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }
}