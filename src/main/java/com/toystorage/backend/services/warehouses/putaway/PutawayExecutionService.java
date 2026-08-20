package com.toystorage.backend.services.warehouses.putaway;

import com.toystorage.backend.dto.request.warehouses.putaway.ExecutePutawayItemRequest;

import com.toystorage.backend.dto.response.warehouses.putaway.PutawayStaffItemResponse;
import com.toystorage.backend.dto.response.warehouses.putaway.PutawayStaffTaskResponse;
import com.toystorage.backend.enums.warehouses.WarehouseTaskType;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.PutawayTaskItems;
import com.toystorage.backend.entity.warehouses.PutawayTasks;

import com.toystorage.backend.enums.warehouses.PutawayTaskItemStatus;
import com.toystorage.backend.enums.warehouses.PutawayTaskStatus;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.mapper.warehouses.putaway.PutawayExecutionMapper;

import com.toystorage.backend.repository.warehouses.PutawayTaskItemRepository;
import com.toystorage.backend.repository.warehouses.PutawayTaskRepository;

import com.toystorage.backend.services.warehouses.taskclaim.WarehouseTaskClaimService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PutawayExecutionService {

    private final PutawayTaskRepository
            putawayTaskRepository;

    private final PutawayTaskItemRepository
            putawayTaskItemRepository;

    private final PutawayExecutionValidationService
            validationService;

    private final PutawayInventoryService
            inventoryService;

    private final WarehouseTaskClaimService
            taskClaimService;

    private final PutawayExecutionMapper
            mapper;


    // =====================================================
    // MY TASKS
    // =====================================================

    @Transactional(readOnly = true)
    public List<PutawayStaffTaskResponse>
    getMyTasks() {

        Users staff =
                validationService
                        .getCurrentUser();


        if (staff.getWarehouse() == null) {

            throw new BadRequest(
                    "User is not assigned to warehouse"
            );
        }


        List<PutawayTasks> tasks =
                putawayTaskRepository
                        .findByWarehouseIdAndStatusInOrderByCreatedAtDesc(
                                staff.getWarehouse().getId(),
                                List.of(
                                        PutawayTaskStatus.AVAILABLE,
                                        PutawayTaskStatus.IN_PROGRESS
                                )
                        );


        return tasks.stream()
                .filter(task -> {

                    /*
                     * ASSIGNED = chưa claim:
                     * tất cả Staff trong warehouse nhìn thấy.
                     */
                    if (task.getStatus()
                            == PutawayTaskStatus.AVAILABLE) {

                        return true;
                    }


                    /*
                     * IN_PROGRESS:
                     * chỉ hiện task của chính mình.
                     */
                    return task.getAssignedTo() != null
                            && task.getAssignedTo()
                            .getId()
                            .equals(staff.getId());
                })
                .map(this::buildResponse)
                .toList();
    }


    // =====================================================
    // DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public PutawayStaffTaskResponse getTask(
            Long taskId
    ) {

        Users staff =
                validationService.getCurrentUser();

        PutawayTasks task =
                putawayTaskRepository
                        .findById(taskId)
                        .orElseThrow(() ->
                                new BadRequest(
                                        "Putaway task not found"
                                )
                        );

        validationService.validateWarehouse(
                staff,
                task
        );

        return buildResponse(task);
    }


    // =====================================================
    // START
    // =====================================================

    @Transactional
    public PutawayStaffTaskResponse startTask(
            Long taskId
    ) {

        Users staff =
                validationService
                        .getCurrentUser();


        PutawayTasks task =
                putawayTaskRepository
                        .findById(taskId)
                        .orElseThrow(() ->
                                new BadRequest(
                                        "Putaway task not found"
                                )
                        );


        validationService.validateWarehouse(
                staff,
                task
        );


        /*
         * Repo hiện tại dùng ASSIGNED.
         *
         * Ta vẫn giữ status này để không phải
         * migration enum/database.
         *
         * ASSIGNED bây giờ hiểu là:
         * "task sẵn sàng cho Staff claim".
         */
        if (task.getStatus()
                != PutawayTaskStatus.AVAILABLE) {

            throw new BadRequest(
                    "Only available putaway task can be started"
            );
        }


        taskClaimService.claim(
                WarehouseTaskType.PUTAWAY,
                taskId,
                staff
        );


        /*
         * Có thể giữ field assignedTo cũ để audit.
         *
         * Manager không set nữa.
         * Staff tự set khi claim.
         */
        task.setAssignedTo(
                staff
        );


        task.setStatus(
                PutawayTaskStatus.IN_PROGRESS
        );


        putawayTaskRepository.save(
                task
        );


        return buildResponse(
                task
        );
    }


    // =====================================================
    // PUTAWAY ONE ITEM
    // =====================================================

    @Transactional
    public PutawayStaffItemResponse putawayItem(
            Long taskId,
            Long itemId,
            ExecutePutawayItemRequest request
    ) {

        Users staff =
                validationService.getCurrentUser();

        PutawayTasks task =
                putawayTaskRepository
                        .findById(taskId)
                        .orElseThrow(() ->
                                new BadRequest(
                                        "Putaway task not found"
                                )
                        );

        validationService.validateWarehouse(
                staff,
                task
        );

        taskClaimService.validateOwner(
                WarehouseTaskType.PUTAWAY,
                taskId,
                staff
        );

        validationService.validateTaskCanExecute(
                task
        );

        PutawayTaskItems item =
                validationService
                        .getTaskItem(
                                taskId,
                                itemId
                        );

        validationService.validateScan(
                item,
                request
        );

        validationService.validateLocation(
                item.getToLocation()
        );

        validationService.validateQuantity(
                item,
                request.getQuantity()
        );


        inventoryService.moveInventory(
                task,
                item,
                staff,
                request.getQuantity()
        );


        int newQuantity =
                item.getPutawayQuantity()
                        + request.getQuantity();

        item.setPutawayQuantity(
                newQuantity
        );


        if (newQuantity
                == item.getExpectedQuantity()) {

            item.setStatus(
                    PutawayTaskItemStatus.COMPLETED
            );

        } else {

            item.setStatus(
                    PutawayTaskItemStatus.PUTAWAYING
            );
        }


        return mapper.toItemResponse(
                putawayTaskItemRepository
                        .save(item)
        );
    }


    // =====================================================
    // COMPLETE TASK
    // =====================================================

    @Transactional
    public PutawayStaffTaskResponse completeTask(
            Long taskId
    ) {

        Users staff =
                validationService.getCurrentUser();

        PutawayTasks task =
                putawayTaskRepository
                        .findById(taskId)
                        .orElseThrow(() ->
                                new BadRequest(
                                        "Putaway task not found"
                                )
                        );

        validationService.validateWarehouse(
                staff,
                task
        );

        taskClaimService.validateOwner(
                WarehouseTaskType.PUTAWAY,
                taskId,
                staff
        );

        if (task.getStatus()
                != PutawayTaskStatus.IN_PROGRESS) {

            throw new BadRequest(
                    "Putaway task must be IN_PROGRESS"
            );
        }


        List<PutawayTaskItems> items =
                putawayTaskItemRepository
                        .findByPutawayTaskId(
                                taskId
                        );


        boolean allCompleted =
                !items.isEmpty()
                        &&
                        items.stream()
                                .allMatch(item ->
                                        item.getStatus()
                                                == PutawayTaskItemStatus.COMPLETED

                                                &&

                                                item.getPutawayQuantity()
                                                        .equals(
                                                                item.getExpectedQuantity()
                                                        )
                                );


        if (!allCompleted) {

            throw new BadRequest(
                    "All putaway items must be completed first"
            );
        }


        task.setStatus(
                PutawayTaskStatus.COMPLETED
        );

        task.setCompletedAt(
                LocalDateTime.now()
        );

        putawayTaskRepository.save(
                task
        );

        taskClaimService.release(
                WarehouseTaskType.PUTAWAY,
                taskId,
                staff
        );


        return mapper.toResponse(
                task,
                items
        );
    }


    private PutawayStaffTaskResponse buildResponse(
            PutawayTasks task
    ) {

        List<PutawayTaskItems> items =
                putawayTaskItemRepository
                        .findByPutawayTaskId(
                                task.getId()
                        );

        return mapper.toResponse(
                task,
                items
        );
    }
}