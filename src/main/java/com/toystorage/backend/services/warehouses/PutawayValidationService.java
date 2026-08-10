package com.toystorage.backend.services.warehouses;

import com.toystorage.backend.dto.request.warehouses.CreatePutawayItemRequest;
import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.PutawayTasks;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.repository.receipts.GoodsReceiptRepository;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.warehouses.PutawayTaskRepository;
import com.toystorage.backend.repository.warehouses.WarehouseLocationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PutawayValidationService {

    private final UserRepository userRepository;

    private final GoodsReceiptRepository goodsReceiptRepository;

    private final PutawayTaskRepository putawayTaskRepository;

    private final WarehouseLocationRepository
            warehouseLocationRepository;


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
                .findByEmail(
                        authentication.getName()
                )

                .orElseThrow(() ->
                        new NotFound(
                                "Authenticated user not found"
                        )
                );
    }


    public Users getStaff(
            Long staffId
    ) {

        return userRepository
                .findById(staffId)

                .orElseThrow(() ->
                        new NotFound(
                                "Warehouse staff not found: "
                                        + staffId
                        )
                );
    }


    public GoodsReceipts getReceipt(
            Long receiptId
    ) {

        return goodsReceiptRepository
                .findById(receiptId)

                .orElseThrow(() ->
                        new NotFound(
                                "Goods receipt not found: "
                                        + receiptId
                        )
                );
    }


    public PutawayTasks getTask(
            Long taskId
    ) {

        return putawayTaskRepository
                .findById(taskId)

                .orElseThrow(() ->
                        new NotFound(
                                "Putaway task not found: "
                                        + taskId
                        )
                );
    }


    public Long getWarehouseId(
            Users user
    ) {

        if (user.getWarehouse() == null) {

            throw new Forbidden(
                    "User is not assigned to a warehouse"
            );
        }

        return user.getWarehouse().getId();
    }


    public WarehouseLocations getReceivingLocation(
            Long warehouseId
    ) {

        return warehouseLocationRepository
                .findFirstByWarehouseIdAndWarehouseCodeAndStatus(
                        warehouseId,
                        "RECEIVING",
                        WarehouseStatus.ACTIVE
                )

                .orElseThrow(() ->
                        new NotFound(
                                "Receiving location not found"
                        )
                );
    }


    public WarehouseLocations getDestinationLocation(
            GoodsReceipts receipt,
            Long locationId
    ) {

        WarehouseLocations location =
                warehouseLocationRepository
                        .findById(locationId)

                        .orElseThrow(() ->
                                new NotFound(
                                        "Warehouse location not found: "
                                                + locationId
                                )
                        );

        if (!location.getWarehouse()
                .getId()
                .equals(
                        receipt.getWarehouse()
                                .getId()
                )) {

            throw new Forbidden(
                    "Destination location belongs to another warehouse"
            );
        }

        if (location.getStatus()
                != WarehouseStatus.ACTIVE) {

            throw new BadRequest(
                    "Destination location is inactive"
            );
        }

        return location;
    }


    public void validateReceiptWarehouse(
            Users manager,
            GoodsReceipts receipt
    ) {

        if (!getWarehouseId(manager)
                .equals(
                        receipt.getWarehouse().getId()
                )) {

            throw new Forbidden(
                    "You cannot create putaway plan "
                            + "for another warehouse"
            );
        }
    }


    public void validateStaffWarehouse(
            Users staff,
            GoodsReceipts receipt
    ) {

        if (staff.getWarehouse() == null
                || !staff.getWarehouse()
                .getId()
                .equals(
                        receipt.getWarehouse()
                                .getId()
                )) {

            throw new BadRequest(
                    "Assigned staff must belong "
                            + "to the same warehouse"
            );
        }
    }


    public void validateTaskWarehouse(
            Users manager,
            PutawayTasks task
    ) {

        if (!getWarehouseId(manager)
                .equals(
                        task.getWarehouse().getId()
                )) {

            throw new Forbidden(
                    "You cannot access putaway task "
                            + "from another warehouse"
            );
        }
    }


    public void validatePutawayItems(
            List<GoodsReceiptItems> receiptItems,
            List<CreatePutawayItemRequest> requests
    ) {

        Set<Long> requestedProductIds =
                requests.stream()
                        .map(
                                CreatePutawayItemRequest::getProductId
                        )
                        .collect(
                                Collectors.toSet()
                        );

        if (requestedProductIds.size()
                != requests.size()) {

            throw new BadRequest(
                    "Duplicate product in putaway plan"
            );
        }

        List<GoodsReceiptItems> acceptedItems =
                receiptItems.stream()

                        .filter(item ->
                                item.getAcceptedQuantity() != null
                                        && item.getAcceptedQuantity() > 0
                        )

                        .toList();

        if (acceptedItems.size()
                != requests.size()) {

            throw new BadRequest(
                    "All accepted products must have "
                            + "a putaway destination"
            );
        }

        for (GoodsReceiptItems item
                : acceptedItems) {

            if (!requestedProductIds.contains(
                    item.getProduct().getId()
            )) {

                throw new BadRequest(
                        "Missing putaway destination for product: "
                                + item.getProduct().getId()
                );
            }
        }
    }
}