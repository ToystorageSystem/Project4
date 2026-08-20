package com.toystorage.backend.services.warehouses.putaway;

import com.toystorage.backend.dto.request.warehouses.putaway.ExecutePutawayItemRequest;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.PutawayTaskItems;
import com.toystorage.backend.entity.warehouses.PutawayTasks;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;

import com.toystorage.backend.enums.warehouses.PutawayTaskStatus;
import com.toystorage.backend.enums.warehouses.WarehouseLocationType;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.warehouses.putaway.PutawayTaskItemRepository;
import com.toystorage.backend.repository.warehouses.putaway.PutawayTaskRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PutawayExecutionValidationService {

    private final UserRepository userRepository;

    private final PutawayTaskRepository
            putawayTaskRepository;

    private final PutawayTaskItemRepository
            putawayTaskItemRepository;


    public Users getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new Unauthorized(
                    "User is not authenticated"
            );
        }

        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new NotFound(
                                "Authenticated user not found"
                        )
                );
    }


    public PutawayTasks getAssignedTask(
            Long taskId,
            Users staff
    ) {

        return putawayTaskRepository
                .findByIdAndAssignedToId(
                        taskId,
                        staff.getId()
                )
                .orElseThrow(() ->
                        new Forbidden(
                                "Putaway task is not assigned to you"
                        )
                );
    }


    public PutawayTaskItems getTaskItem(
            Long taskId,
            Long itemId
    ) {

        return putawayTaskItemRepository
                .findByIdAndPutawayTaskId(
                        itemId,
                        taskId
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Putaway task item not found"
                        )
                );
    }


    public void validateWarehouse(
            Users staff,
            PutawayTasks task
    ) {

        if (staff.getWarehouse() == null) {

            throw new Forbidden(
                    "User is not assigned to warehouse"
            );
        }

        if (!staff.getWarehouse()
                .getId()
                .equals(
                        task.getWarehouse().getId()
                )) {

            throw new Forbidden(
                    "Putaway task belongs to another warehouse"
            );
        }
    }


    public void validateTaskCanExecute(
            PutawayTasks task
    ) {

        if (task.getStatus()
                != PutawayTaskStatus.AVAILABLE

                && task.getStatus()
                != PutawayTaskStatus.IN_PROGRESS) {

            throw new BadRequest(
                    "Putaway task cannot be executed "
                            + "in status "
                            + task.getStatus()
            );
        }
    }


    public void validateScan(
            PutawayTaskItems item,
            ExecutePutawayItemRequest request
    ) {

        if (!item.getProduct()
                .getBarcode()
                .equals(request.getProductBarcode())) {

            throw new BadRequest(
                    "Scanned product does not match "
                            + "assigned product"
            );
        }

        if (!item.getToLocation()
                .getWarehouseCode()
                .equalsIgnoreCase(
                        request.getLocationCode()
                )) {

            throw new BadRequest(
                    "Scanned location does not match "
                            + "assigned putaway location"
            );
        }
    }


    public void validateLocation(
            WarehouseLocations location
    ) {

        if (location.getStatus()
                != WarehouseStatus.ACTIVE) {

            throw new BadRequest(
                    "Destination location is not active"
            );
        }

        /*
         * Normal putaway chỉ được vào NORMAL.
         *
         * DAMAGED / QUARANTINE không được đưa vào
         * vị trí hàng bình thường.
         */
        if (location.getLocationType()
                != WarehouseLocationType.NORMAL) {

            throw new BadRequest(
                    "Destination must be a NORMAL storage location"
            );
        }
    }


    public void validateQuantity(
            PutawayTaskItems item,
            int quantity
    ) {

        int after =
                item.getPutawayQuantity()
                        + quantity;

        if (after
                > item.getExpectedQuantity()) {

            throw new BadRequest(
                    "Putaway quantity exceeds "
                            + "assigned quantity"
            );
        }
    }
}