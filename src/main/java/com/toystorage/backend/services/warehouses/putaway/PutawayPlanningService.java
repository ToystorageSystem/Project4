package com.toystorage.backend.services.warehouses.putaway;

import com.toystorage.backend.dto.request.warehouses.putaway.CreatePutawayPlanRequest;
import com.toystorage.backend.dto.response.warehouses.putaway.PutawayPlanResponse;

import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.PutawayTasks;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;

import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.enums.warehouses.PutawayTaskStatus;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.repository.receipts.GoodsReceiptItemRepository;
import com.toystorage.backend.repository.warehouses.PutawayTaskRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PutawayPlanningService {

    private final PutawayTaskRepository putawayTaskRepository;

    private final GoodsReceiptItemRepository goodsReceiptItemRepository;

    private final PutawayValidationService validationService;

    private final PutawayTaskItemService taskItemService;

    private final PutawayProgressService progressService;


    // =====================================================
    // CREATE PLAN
    // =====================================================

    @Transactional
    public PutawayPlanResponse createPlan(
            Long receiptId,
            CreatePutawayPlanRequest request
    ) {

        Users manager =
                validationService.getCurrentUser();

        GoodsReceipts receipt =
                validationService.getReceipt(
                        receiptId
                );

        validationService.validateReceiptWarehouse(
                manager,
                receipt
        );

        if (receipt.getStatus()
                != GoodsReceiptStatus.COMPLETED) {

            throw new BadRequest(
                    "Putaway plan can only be created "
                            + "after receiving is completed"
            );
        }

        if (putawayTaskRepository
                .existsByGoodsReceiptId(receiptId)) {

            throw new BadRequest(
                    "Putaway plan already exists for this receipt"
            );
        }


        WarehouseLocations receivingLocation =
                validationService
                        .getReceivingLocation(
                                receipt.getWarehouse().getId()
                        );

        List<GoodsReceiptItems> receiptItems =
                goodsReceiptItemRepository
                        .findByGoodsReceiptId(
                                receiptId
                        );

        if (receiptItems.isEmpty()) {

            throw new BadRequest(
                    "Goods receipt contains no items"
            );
        }

        validationService.validatePutawayItems(
                receiptItems,
                request.getItems()
        );

        PutawayTasks task =
                createMasterTask(
                        receipt,
                        manager
                );

        taskItemService.createItems(
                task,
                receipt,
                receiptItems,
                request.getItems(),
                receivingLocation
        );

        return progressService
                .buildResponse(task);
    }


    // =====================================================
    // LIST
    // =====================================================

    @Transactional(readOnly = true)
    public List<PutawayPlanResponse> getPlans() {

        Users manager =
                validationService.getCurrentUser();

        Long warehouseId =
                validationService
                        .getWarehouseId(manager);

        return putawayTaskRepository
                .findByWarehouseIdAndStatusInOrderByCreatedAtDesc(
                        warehouseId,
                        List.of(
                                PutawayTaskStatus.PENDING,
                                PutawayTaskStatus.AVAILABLE,
                                PutawayTaskStatus.IN_PROGRESS
                        )
                )

                .stream()

                .map(
                        progressService::buildResponse
                )

                .toList();
    }


    // =====================================================
    // DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public PutawayPlanResponse getPlan(
            Long taskId
    ) {

        PutawayTasks task =
                validationService.getTask(
                        taskId
                );

        Users manager =
                validationService.getCurrentUser();

        validationService.validateTaskWarehouse(
                manager,
                task
        );

        return progressService
                .buildResponse(task);
    }


    // =====================================================
    // CONFIRM COMPLETION
    // =====================================================

    @Transactional
    public PutawayPlanResponse confirmCompletion(
            Long taskId
    ) {

        PutawayTasks task =
                validationService.getTask(
                        taskId
                );

        Users manager =
                validationService.getCurrentUser();

        validationService.validateTaskWarehouse(
                manager,
                task
        );

        progressService.validateCompleted(
                task
        );

        task.setStatus(
                PutawayTaskStatus.COMPLETED
        );

        task.setCompletedAt(
                LocalDateTime.now()
        );

        PutawayTasks saved =
                putawayTaskRepository.save(
                        task
                );

        return progressService
                .buildResponse(saved);
    }


    // =====================================================
    // CREATE MASTER TASK
    // =====================================================

    private PutawayTasks createMasterTask(
            GoodsReceipts receipt,
            Users manager
    ) {

        PutawayTasks task =
                new PutawayTasks();

        task.setPutawayTasksCode(
                generateCode("PA")
        );

        task.setGoodsReceipt(
                receipt
        );

        task.setWarehouse(
                receipt.getWarehouse()
        );

        task.setCreatedBy(
                manager
        );

        /*
         * Chưa ai nhận.
         */
        task.setAssignedTo(
                null
        );

        task.setStatus(
                PutawayTaskStatus.AVAILABLE
        );

        task.setCreatedAt(
                LocalDateTime.now()
        );

        return putawayTaskRepository.save(
                task
        );
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