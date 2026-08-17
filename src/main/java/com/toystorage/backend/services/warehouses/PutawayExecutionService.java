package com.toystorage.backend.services.warehouses;

import com.toystorage.backend.dto.request.warehouses.ExecutePutawayItemRequest;

import com.toystorage.backend.dto.response.warehouses.PutawayStaffItemResponse;
import com.toystorage.backend.dto.response.warehouses.PutawayStaffTaskResponse;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.PutawayTaskItems;
import com.toystorage.backend.entity.warehouses.PutawayTasks;

import com.toystorage.backend.enums.warehouses.PutawayTaskItemStatus;
import com.toystorage.backend.enums.warehouses.PutawayTaskStatus;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.mapper.warehouses.PutawayExecutionMapper;

import com.toystorage.backend.repository.warehouses.PutawayTaskItemRepository;
import com.toystorage.backend.repository.warehouses.PutawayTaskRepository;

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

    private final PutawayExecutionMapper
            mapper;


    // =====================================================
    // MY TASKS
    // =====================================================

    @Transactional(readOnly = true)
    public List<PutawayStaffTaskResponse>
    getMyTasks() {

        Users staff =
                validationService.getCurrentUser();

        List<PutawayTasks> tasks =
                putawayTaskRepository
                        .findByAssignedToIdAndStatusInOrderByCreatedAtDesc(
                                staff.getId(),
                                List.of(
                                        PutawayTaskStatus.ASSIGNED,
                                        PutawayTaskStatus.IN_PROGRESS
                                )
                        );

        return tasks.stream()
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
                validationService
                        .getAssignedTask(
                                taskId,
                                staff
                        );

        validationService
                .validateWarehouse(
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
                validationService.getCurrentUser();

        PutawayTasks task =
                validationService
                        .getAssignedTask(
                                taskId,
                                staff
                        );

        validationService.validateWarehouse(
                staff,
                task
        );

        if (task.getStatus()
                != PutawayTaskStatus.ASSIGNED) {

            throw new BadRequest(
                    "Only ASSIGNED task can be started"
            );
        }

        task.setStatus(
                PutawayTaskStatus.IN_PROGRESS
        );

        putawayTaskRepository.save(task);

        return buildResponse(task);
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
                validationService
                        .getAssignedTask(
                                taskId,
                                staff
                        );

        validationService.validateWarehouse(
                staff,
                task
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
                validationService
                        .getAssignedTask(
                                taskId,
                                staff
                        );

        validationService.validateWarehouse(
                staff,
                task
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