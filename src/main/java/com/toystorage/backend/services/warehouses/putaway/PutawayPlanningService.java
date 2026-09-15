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

import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptItemRepository;
import com.toystorage.backend.repository.warehouses.putaway.PutawayTaskRepository;

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
                                PutawayTaskStatus.IN_PROGRESS,
                                PutawayTaskStatus.COMPLETED,
                                PutawayTaskStatus.CANCELLED
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