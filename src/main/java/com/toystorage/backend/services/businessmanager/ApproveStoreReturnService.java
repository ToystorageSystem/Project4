
package com.toystorage.backend.services.businessmanager;

import com.toystorage.backend.dto.request.businessmanager.ApproveStoreReturnRequest;
import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.inventories.InventoryTransactions;
import com.toystorage.backend.entity.stores.StoreReturnItems;
import com.toystorage.backend.entity.stores.StoreReturns;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.transfers.StockTransferItems;
import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.inventories.InventoryReferenceType;
import com.toystorage.backend.enums.inventories.InventoryTransactionType;
import com.toystorage.backend.enums.stores.StoreReturnStatus;
import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.enums.transfers.TransferType;
import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;
import com.toystorage.backend.enums.warehouses.WarehouseLocationType;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.inventories.InventoryBalanceRepository;
import com.toystorage.backend.repository.inventories.InventoryTransactionRepository;
import com.toystorage.backend.repository.stores.returns.StoreReturnItemRepository;
import com.toystorage.backend.repository.stores.returns.StoreReturnRepository;
import com.toystorage.backend.repository.users.ActivityLogRepository;
import com.toystorage.backend.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApproveStoreReturnService {

    private final StoreReturnRepository storeReturnRepository;
    private final StoreReturnItemRepository storeReturnItemRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ROLE-BIZ-MANAGER')")
    public void approve(
            Long returnId,
            ApproveStoreReturnRequest request
    ) {

        if (returnId == null || returnId <= 0
                || request == null
                || request.getItems() == null
                || request.getItems().isEmpty()) {

            throw new BadRequest(
                    "Invalid store return approval request"
            );
        }

        Users manager = currentManager();

        StoreReturns storeReturn =
                storeReturnRepository
                        .findByIdForApproval(returnId)
                        .orElseThrow(() ->
                                new NotFound(
                                        "Store return not found: " + returnId
                                )
                        );

        // 1. Check return status
        if (storeReturn.getStatus()
                != StoreReturnStatus.PENDING_APPROVAL
                || storeReturn.getApprovedAt() != null) {

            throw new BadRequest(
                    "Store return is not pending approval"
            );
        }

        if (storeReturn.getStore() == null
                || storeReturn.getWarehouse() == null) {

            throw new BadRequest(
                    "Store return warehouse information is missing"
            );
        }

        // 2. Validate linked transfer
        StockTransfer transfer =
                storeReturn.getStockTransfer();

        validateTransfer(storeReturn, transfer);

        // 3. Load return items
        List<StoreReturnItems> items =
                storeReturnItemRepository
                        .findByStoreReturnId(returnId);

        if (items.isEmpty()) {
            throw new BadRequest(
                    "Store return has no items"
            );
        }

        Map<Long, Integer> approvalByItemId =
                new HashMap<>();

        for (ApproveStoreReturnRequest.ItemApproval approved
                : request.getItems()) {

            if (approved == null
                    || approved.getStoreReturnItemId() == null
                    || approved.getStoreReturnItemId() <= 0
                    || approved.getApprovedQuantity() == null
                    || approved.getApprovedQuantity() < 0) {

                throw new BadRequest(
                        "Invalid item approval"
                );
            }

            if (approvalByItemId.putIfAbsent(
                    approved.getStoreReturnItemId(),
                    approved.getApprovedQuantity()
            ) != null) {

                throw new BadRequest(
                        "Duplicate store return item ID"
                );
            }
        }

        if (approvalByItemId.size() != items.size()) {
            throw new BadRequest(
                    "Approval must include every return item exactly once"
            );
        }

        // 4. Validate quantities
        Map<Long, Integer> requestedByProduct =
                new HashMap<>();

        Map<Long, Integer> approvedByProduct =
                new HashMap<>();

        int totalApproved = 0;

        for (StoreReturnItems item : items) {

            Integer qty =
                    approvalByItemId.get(item.getId());

            if (qty == null) {
                throw new BadRequest(
                        "An item does not belong to the approval request"
                );
            }

            if (item.getRequestedQuantity() == null
                    || item.getRequestedQuantity() <= 0
                    || qty > item.getRequestedQuantity()) {

                throw new BadRequest(
                        "Approved quantity exceeds requested quantity for item "
                                + item.getId()
                );
            }

            if (item.getProduct() == null
                    || item.getFromLocation() == null
                    || item.getFromLocation().getWarehouse() == null
                    || !storeReturn.getStore().getId().equals(
                            item.getFromLocation()
                                    .getWarehouse()
                                    .getId()
                    )) {

                throw new BadRequest(
                        "Return item location does not belong to the source store"
                );
            }

            Long productId =
                    item.getProduct().getId();

            requestedByProduct.merge(
                    productId,
                    item.getRequestedQuantity(),
                    Math::addExact
            );

            approvedByProduct.merge(
                    productId,
                    qty,
                    Math::addExact
            );

            totalApproved =
                    Math.addExact(totalApproved, qty);
        }

        if (totalApproved <= 0) {
            throw new BadRequest(
                    "At least one item must have an approved quantity greater than zero"
            );
        }

        // 5. Check linked transfer items
        Set<Long> transferProductIds =
                new HashSet<>();

        List<StockTransferItems> transferItems =
                transfer.getItems();

        if (transferItems == null
                || transferItems.size()
                != requestedByProduct.size()) {

            throw new BadRequest(
                    "Linked stock transfer items do not match the store return"
            );
        }

        for (StockTransferItems transferItem
                : transferItems) {

            Long productId =
                    transferItem.getProduct().getId();

            if (!transferProductIds.add(productId)
                    || !requestedByProduct.containsKey(productId)
                    || !requestedByProduct.get(productId).equals(
                            transferItem.getRequestedQuantity()
                    )) {

                throw new BadRequest(
                        "Linked stock transfer has mismatched product quantities"
                );
            }
        }

        // 6. Lock inventory in a stable order
        List<StoreReturnItems> orderedItems =
                new ArrayList<>(items);

        orderedItems.sort(
                Comparator
                        .comparing(
                                (StoreReturnItems i) ->
                                        i.getFromLocation().getId()
                        )
                        .thenComparing(
                                i -> i.getProduct().getId()
                        )
        );

        for (StoreReturnItems item : orderedItems) {

            int approvedQty =
                    approvalByItemId.get(item.getId());

            item.setApprovedQuantity(approvedQty);

            if (approvedQty == 0) {
                continue;
            }

            // Lock inventory row
            InventoryBalances balance =
                    inventoryBalanceRepository
                            .findForStoreReturnApproval(
                                    storeReturn.getStore().getId(),
                                    item.getFromLocation().getId(),
                                    item.getProduct().getId()
                            )
                            .orElseThrow(() ->
                                    new BadRequest(
                                            "Inventory not found for return item "
                                                    + item.getId()
                                    )
                            );

            int before = balance.getQuantity();
            int reserved = balance.getReservedQuantity();

            WarehouseLocationType locationType =
                    item.getFromLocation().getLocationType();

            boolean nonSaleable =
                    locationType == WarehouseLocationType.QUARANTINE
                            || locationType == WarehouseLocationType.DAMAGED;

            int usable = nonSaleable
                    ? before - reserved
                    : balance.getAvailableQuantity();

            if (before < 0
                    || reserved < 0
                    || reserved > before
                    || usable < approvedQty
                    || before - approvedQty < reserved) {

                throw new BadRequest(
                        "Insufficient eligible inventory for return item "
                                + item.getId()
                );
            }

            // 7. Deduct inventory
            int after = before - approvedQty;

            balance.setQuantity(after);

            inventoryBalanceRepository.save(balance);

            // 8. Record inventory transaction
            InventoryTransactions transaction =
                    new InventoryTransactions();

            transaction.setInventoryTransactionsCode(
                    "IT-" + UUID.randomUUID()
            );

            transaction.setWarehouse(
                    storeReturn.getStore()
            );

            transaction.setLocation(
                    item.getFromLocation()
            );

            transaction.setProduct(
                    item.getProduct()
            );

            transaction.setTransactionType(
                    InventoryTransactionType.STORE_RETURN_OUT
            );

            transaction.setQuantityBefore(before);

            transaction.setQuantityChange(-approvedQty);

            transaction.setQuantityAfter(after);

            transaction.setReferenceType(
                    InventoryReferenceType.STORE_RETURN
            );

            transaction.setReferenceId(returnId);

            transaction.setPerformedBy(manager);

            inventoryTransactionRepository.save(
                    transaction
            );
        }

        // 9. Update stock transfer items
        for (StockTransferItems transferItem
                : transferItems) {

            transferItem.setApprovedQuantity(
                    approvedByProduct.get(
                            transferItem.getProduct().getId()
                    )
            );
        }

        // 10. Approve return and transfer
        LocalDateTime now = LocalDateTime.now();

        transfer.setStatus(
                TransferStatus.CREATED
        );

        transfer.setConfirmedBy(manager);
        transfer.setConfirmedAt(now);

        storeReturn.setApprovedBy(manager);
        storeReturn.setApprovedAt(now);

        storeReturn.setStatus(
                StoreReturnStatus.APPROVED
        );

        storeReturnItemRepository.saveAll(items);
        storeReturnRepository.save(storeReturn);

        // 11. Record activity log
        ActivityLogs audit =
                ActivityLogs.builder()
                        .user(manager)
                        .action(ActivityAction.APPROVE)
                        .entityType(
                                ActivityEntityType.STORE_RETURN
                        )
                        .entityId(returnId)
                        .oldValue(
                                "{\"status\":\"PENDING_APPROVAL\"}"
                        )
                        .newValue(
                                "{\"status\":\"APPROVED\",\"totalApprovedQuantity\":"
                                        + totalApproved + "}"
                        )
                        .build();

        activityLogRepository.save(audit);
    }

    // Get authenticated Business Manager
    private Users currentManager() {

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

    // Validate linked Stock Transfer
    private void validateTransfer(
            StoreReturns storeReturn,
            StockTransfer transfer
    ) {

        if (transfer == null
                || transfer.getFromWarehouse() == null
                || transfer.getToWarehouse() == null
                || !storeReturn.getStore().getId().equals(
                        transfer.getFromWarehouse().getId()
                )
                || !storeReturn.getWarehouse().getId().equals(
                        transfer.getToWarehouse().getId()
                )) {

            throw new BadRequest(
                    "Linked stock transfer has invalid source or destination"
            );
        }

        if (transfer.getTransferType()
                != TransferType.STORE_RETURN
                && transfer.getTransferType()
                != TransferType.SURPLUS_RETURN
                && transfer.getTransferType()
                != TransferType.DAMAGED_RETURN
                && transfer.getTransferType()
                != TransferType.RECALL) {

            throw new BadRequest(
                    "Linked stock transfer is not a return transfer"
            );
        }

        if (transfer.getStatus()
                != TransferStatus.DRAFT) {

            throw new BadRequest(
                    "Linked stock transfer is not DRAFT; refusing double stock deduction"
            );
        }
    }
}
