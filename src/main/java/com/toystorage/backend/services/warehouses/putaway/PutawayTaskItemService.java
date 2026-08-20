package com.toystorage.backend.services.warehouses.putaway;

import com.toystorage.backend.dto.request.warehouses.putaway.CreatePutawayItemRequest;
import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.warehouses.PutawayTaskItems;
import com.toystorage.backend.entity.warehouses.PutawayTasks;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.enums.warehouses.PutawayTaskItemStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.repository.warehouses.PutawayTaskItemRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PutawayTaskItemService {

    private final PutawayTaskItemRepository
            putawayTaskItemRepository;

    private final PutawayValidationService
            validationService;


    @Transactional
    public void createItems(
            PutawayTasks task,
            GoodsReceipts receipt,
            List<GoodsReceiptItems> receiptItems,
            List<CreatePutawayItemRequest> requests,
            WarehouseLocations receivingLocation
    ) {

        for (CreatePutawayItemRequest request
                : requests) {

            GoodsReceiptItems receiptItem =
                    findReceiptItem(
                            receiptItems,
                            request.getProductId()
                    );

            WarehouseLocations destination =
                    validationService
                            .getDestinationLocation(
                                    receipt,
                                    request.getToLocationId()
                            );

            PutawayTaskItems taskItem =
                    new PutawayTaskItems();

            taskItem.setPutawayTaskItemsCode(
                    generateCode("PAI")
            );

            taskItem.setPutawayTask(task);

            taskItem.setProduct(
                    receiptItem.getProduct()
            );

            taskItem.setExpectedQuantity(
                    receiptItem.getAcceptedQuantity()
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
    }


    private GoodsReceiptItems findReceiptItem(
            List<GoodsReceiptItems> receiptItems,
            Long productId
    ) {

        GoodsReceiptItems item =
                receiptItems.stream()

                        .filter(receiptItem ->
                                receiptItem
                                        .getProduct()
                                        .getId()
                                        .equals(productId)
                        )

                        .findFirst()

                        .orElseThrow(() ->
                                new BadRequest(
                                        "Product "
                                                + productId
                                                + " does not belong to receipt"
                                )
                        );

        if (item.getAcceptedQuantity() == null
                || item.getAcceptedQuantity() <= 0) {

            throw new BadRequest(
                    "Product "
                            + productId
                            + " has no accepted quantity for putaway"
            );
        }

        return item;
    }


    private String generateCode(
            String prefix
    ) {

        return prefix
                + "-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }
}