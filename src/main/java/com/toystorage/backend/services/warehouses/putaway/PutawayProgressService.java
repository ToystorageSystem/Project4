package com.toystorage.backend.services.warehouses.putaway;

import com.toystorage.backend.dto.response.warehouses.putaway.PutawayPlanItemResponse;
import com.toystorage.backend.dto.response.warehouses.putaway.PutawayPlanResponse;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.PutawayTaskItems;
import com.toystorage.backend.entity.warehouses.PutawayTasks;
import com.toystorage.backend.enums.warehouses.PutawayTaskItemStatus;
import com.toystorage.backend.enums.warehouses.PutawayTaskStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.mapper.warehouses.putaway.PutawayPlanMapper;
import com.toystorage.backend.repository.warehouses.putaway.PutawayTaskItemRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PutawayProgressService {

    private final PutawayTaskItemRepository
            putawayTaskItemRepository;

    private final PutawayPlanMapper
            putawayPlanMapper;


    @Transactional(readOnly = true)
    public PutawayPlanResponse buildResponse(
            PutawayTasks task
    ) {

        List<PutawayTaskItems> items =
                putawayTaskItemRepository
                        .findByPutawayTaskId(
                                task.getId()
                        );

        List<PutawayPlanItemResponse> itemResponses =
                items.stream()
                        .map(
                                putawayPlanMapper::toItemResponse
                        )
                        .toList();

        int totalItems =
                items.size();

        int completedItems =
                (int) items.stream()

                        .filter(item ->
                                item.getStatus()
                                        == PutawayTaskItemStatus.COMPLETED
                        )

                        .count();

        int totalQuantity =
                items.stream()

                        .mapToInt(
                                PutawayTaskItems::getExpectedQuantity
                        )

                        .sum();

        int putawayQuantity =
                items.stream()

                        .mapToInt(
                                PutawayTaskItems::getPutawayQuantity
                        )

                        .sum();

        double progress =
                totalQuantity == 0
                        ? 0
                        : ((double) putawayQuantity
                        / totalQuantity) * 100;

        Users staff =
                task.getAssignedTo();

        Users creator =
                task.getCreatedBy();

        return PutawayPlanResponse.builder()

                .id(task.getId())

                .putawayCode(
                        task.getPutawayTasksCode()
                )

                .goodsReceiptId(
                        task.getGoodsReceipt().getId()
                )

                .warehouseId(
                        task.getWarehouse().getId()
                )

                .status(
                        task.getStatus().name()
                )

                .assignedTo(
                        staff != null
                                ? staff.getId()
                                : null
                )

                .assignedToName(
                        staff != null
                                ? staff.getName()
                                : null
                )

                .createdBy(
                        creator != null
                                ? creator.getId()
                                : null
                )

                .createdByName(
                        creator != null
                                ? creator.getName()
                                : null
                )

                .totalItems(
                        totalItems
                )

                .completedItems(
                        completedItems
                )

                .totalQuantity(
                        totalQuantity
                )

                .putawayQuantity(
                        putawayQuantity
                )

                .progressPercent(
                        Math.round(
                                progress * 100.0
                        ) / 100.0
                )

                .createdAt(
                        task.getCreatedAt()
                )

                .completedAt(
                        task.getCompletedAt()
                )

                .items(
                        itemResponses
                )

                .build();
    }


    public void validateCompleted(
            PutawayTasks task
    ) {

        if (task.getStatus()
                == PutawayTaskStatus.COMPLETED) {

            throw new BadRequest(
                    "Putaway task has already been completed"
            );
        }

        List<PutawayTaskItems> items =
                putawayTaskItemRepository
                        .findByPutawayTaskId(
                                task.getId()
                        );

        if (items.isEmpty()) {

            throw new BadRequest(
                    "Putaway task contains no items"
            );
        }

        boolean allCompleted =
                items.stream()

                        .allMatch(item ->
                                item.getStatus()
                                        == PutawayTaskItemStatus.COMPLETED

                                        &&

                                        item.getPutawayQuantity() != null

                                        &&

                                        item.getPutawayQuantity()
                                                .equals(
                                                        item.getExpectedQuantity()
                                                )
                        );

        if (!allCompleted) {

            throw new BadRequest(
                    "Putaway cannot be confirmed because "
                            + "some items are not completed"
            );
        }
    }
}