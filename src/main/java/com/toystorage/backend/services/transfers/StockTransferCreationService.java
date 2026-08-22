package com.toystorage.backend.services.transfers;

import com.toystorage.backend.dto.request.transfers.CancelStockTransferRequest;
import com.toystorage.backend.dto.request.transfers.CreateStockTransferItemRequest;
import com.toystorage.backend.dto.request.transfers.CreateStockTransferRequest;
import com.toystorage.backend.dto.request.transfers.UpdateStockTransferRequest;
import com.toystorage.backend.dto.response.transfers.StockTransferDetailResponse;
import com.toystorage.backend.dto.response.transfers.StockTransferProductOptionResponse;
import com.toystorage.backend.dto.response.transfers.StockTransferWarehouseOptionResponse;
import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.inventories.InventoryTransactions;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.transfers.StockTransferItems;
import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.inventories.InventoryReferenceType;
import com.toystorage.backend.enums.inventories.InventoryTransactionType;
import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.enums.transfers.TransferReasonCode;
import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.enums.transfers.TransferType;
import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.enums.warehouses.WarehouseType;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.mapper.transfers.StockTransferCreationMapper;
import com.toystorage.backend.repository.inventories.InventoryBalanceRepository;
import com.toystorage.backend.repository.inventories.InventoryTransactionRepository;
import com.toystorage.backend.repository.products.ProductRepository;
import com.toystorage.backend.repository.transfers.picking.StockTransferItemRepository;
import com.toystorage.backend.repository.transfers.picking.StockTransferRepository;
import com.toystorage.backend.repository.users.ActivityLogRepository;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.warehouses.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockTransferCreationService {

    private final StockTransferRepository
            stockTransferRepository;

    private final StockTransferItemRepository
            stockTransferItemRepository;

    private final WarehouseRepository
            warehouseRepository;

    private final ProductRepository
            productRepository;

    private final InventoryBalanceRepository
            inventoryBalanceRepository;

    private final InventoryTransactionRepository
            inventoryTransactionRepository;

    private final UserRepository
            userRepository;

    private final ActivityLogRepository
            activityLogRepository;

    private final StockTransferCreationMapper
            mapper;


    // =====================================================
    // SOURCE OPTIONS
    // MAIN_WAREHOUSE + STORE
    // =====================================================

    @Transactional(readOnly = true)
    public List<StockTransferWarehouseOptionResponse>
    getSourceOptions() {

        List<Warehouses> sources =
                new ArrayList<>();

        sources.addAll(
                warehouseRepository
                        .findByTypeAndStatusOrderByNameAsc(
                                WarehouseType.MAIN_WAREHOUSE,
                                WarehouseStatus.ACTIVE
                        )
        );

        sources.addAll(
                warehouseRepository
                        .findByTypeAndStatusOrderByNameAsc(
                                WarehouseType.STORE,
                                WarehouseStatus.ACTIVE
                        )
        );

        return sources.stream()
                .sorted(
                        Comparator.comparing(
                                Warehouses::getName,
                                Comparator.nullsLast(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                )
                .map(
                        mapper::toWarehouseOption
                )
                .toList();
    }


    // =====================================================
    // DESTINATION OPTIONS
    // CHỈ STORE
    // =====================================================

    @Transactional(readOnly = true)
    public List<StockTransferWarehouseOptionResponse>
    getDestinationOptions() {

        return warehouseRepository
                .findByTypeAndStatusOrderByNameAsc(
                        WarehouseType.STORE,
                        WarehouseStatus.ACTIVE
                )
                .stream()
                .map(
                        mapper::toWarehouseOption
                )
                .toList();
    }


    // =====================================================
    // PRODUCTS + AVAILABLE INVENTORY AT SOURCE
    // =====================================================

    @Transactional(readOnly = true)
    public List<StockTransferProductOptionResponse>
    getProductsBySource(
            Long sourceId
    ) {

        getActiveSource(
                sourceId
        );

        return inventoryBalanceRepository
                .findAvailableProductsByWarehouseId(
                        sourceId,
                        ProductStatus.ACTIVE
                )
                .stream()
                .map(row -> {

                    Products product =
                            (Products) row[0];

                    Number availableQuantity =
                            (Number) row[1];

                    return mapper.toProductOption(
                            product,
                            availableQuantity
                    );
                })
                .toList();
    }


    // =====================================================
    // CREATE DRAFT
    // =====================================================

    @Transactional
    public StockTransferDetailResponse create(
            CreateStockTransferRequest request
    ) {

        Users currentUser =
                getCurrentUser();

        validateWarehousePair(
                request.getFromWarehouseId(),
                request.getToWarehouseId()
        );

        Warehouses source =
                getActiveSource(
                        request.getFromWarehouseId()
                );

        Warehouses destination =
                getActiveDestination(
                        request.getToWarehouseId()
                );

        validateDates(
                request.getExpectedShipmentDate(),
                request.getExpectedReceiptDate()
        );

        validateReason(
                request.getReasonCode(),
                request.getReasonNote()
        );

        String transferCode =
                generateTransferCode();

        StockTransfer transfer =
                StockTransfer.builder()

                        .transferCode(
                                transferCode
                        )

                        .stockTransfersCode(
                                transferCode
                        )

                        .fromWarehouse(
                                source
                        )

                        .toWarehouse(
                                destination
                        )

                        .transferType(
                                TransferType.NORMAL
                        )

                        .reasonCode(
                                request.getReasonCode()
                        )

                        .reasonNote(
                                normalizeText(
                                        request.getReasonNote()
                                )
                        )

                        .notes(
                                normalizeText(
                                        request.getNotes()
                                )
                        )

                        .expectedShipmentDate(
                                request.getExpectedShipmentDate()
                        )

                        .expectedReceiptDate(
                                request.getExpectedReceiptDate()
                        )

                        .status(
                                TransferStatus.DRAFT
                        )

                        .createdBy(
                                currentUser
                        )

                        .build();


        List<StockTransferItems> items =
                buildItems(
                        transfer,
                        request.getItems()
                );


        for (StockTransferItems item : items) {

            transfer.addItem(
                    item
            );
        }


        transfer =
                stockTransferRepository.save(
                        transfer
                );


        saveHistory(
                currentUser,
                ActivityAction.CREATE,
                transfer,
                null,
                snapshot(
                        transfer,
                        transfer.getItems()
                )
        );


        return getDetailedResponse(
                transfer.getId()
        );
    }


    // =====================================================
    // DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public StockTransferDetailResponse getDetail(
            Long transferId
    ) {

        return mapper.toDetailResponse(
                getDetailedTransfer(
                        transferId
                )
        );
    }


    // =====================================================
    // SUBMIT
    // DRAFT -> CREATED
    // RESERVE AVAILABLE INVENTORY
    // =====================================================

    @Transactional
    public StockTransferDetailResponse submit(
            Long transferId
    ) {

        Users currentUser =
                getCurrentUser();


        StockTransfer transfer =
                getDetailedTransfer(
                        transferId
                );


        if (
                transfer.getStatus()
                        != TransferStatus.DRAFT
        ) {

            throw new BadRequest(
                    "Only DRAFT stock transfers can be submitted"
            );
        }


        validateWarehousePair(
                transfer.getFromWarehouse().getId(),
                transfer.getToWarehouse().getId()
        );

        validateActiveWarehouseEntities(
                transfer
        );

        validateDates(
                transfer.getExpectedShipmentDate(),
                transfer.getExpectedReceiptDate()
        );

        validateReason(
                transfer.getReasonCode(),
                transfer.getReasonNote()
        );


        List<StockTransferItems> items =
                transfer.getItems();

        if (
                items == null
                        || items.isEmpty()
        ) {

            throw new BadRequest(
                    "Stock transfer must contain at least one product"
            );
        }


        String oldValue =
                snapshot(
                        transfer,
                        items
                );


        List<StockTransferItems> sortedItems =
                items.stream()
                        .sorted(
                                Comparator.comparing(
                                        item ->
                                                item.getProduct()
                                                        .getId()
                                )
                        )
                        .toList();


        for (StockTransferItems item : sortedItems) {

            validateProductBeforeSubmit(
                    item
            );

            reserveInventory(
                    transfer,
                    item,
                    currentUser
            );

            item.setApprovedQuantity(
                    item.getRequestedQuantity()
            );
        }


        stockTransferItemRepository.saveAll(
                items
        );


        transfer.setStatus(
                TransferStatus.CREATED
        );

        transfer.setConfirmedBy(
                currentUser
        );

        transfer.setConfirmedAt(
                LocalDateTime.now()
        );


        transfer =
                stockTransferRepository.save(
                        transfer
                );


        saveHistory(
                currentUser,
                ActivityAction.UPDATE,
                transfer,
                oldValue,
                snapshot(
                        transfer,
                        items
                )
        );


        return getDetailedResponse(
                transfer.getId()
        );
    }


    // =====================================================
    // UPDATE STOCK TRANSFER
    // DRAFT / CREATED / PENDING_SOURCE_CONFIRMATION
    // =====================================================

    @Transactional
    public StockTransferDetailResponse update(
            Long transferId,
            UpdateStockTransferRequest request
    ) {

        Users currentUser =
                getCurrentUser();


        StockTransfer transfer =
                getDetailedTransfer(
                        transferId
                );


        validateCanModifyTransfer(
                transfer,
                currentUser
        );


        TransferStatus currentStatus =
                transfer.getStatus();


        /*
         * Chỉ DRAFT được đổi điểm nhận.
         */
        if (
                currentStatus != TransferStatus.DRAFT
                        && !Objects.equals(
                        request.getToWarehouseId(),
                        transfer.getToWarehouse().getId()
                )
        ) {

            throw new BadRequest(
                    "Destination can only be changed while stock transfer is DRAFT"
            );
        }


        validateWarehousePair(
                transfer.getFromWarehouse().getId(),
                request.getToWarehouseId()
        );


        Warehouses destination =
                getActiveDestination(
                        request.getToWarehouseId()
                );


        validateDates(
                request.getExpectedShipmentDate(),
                request.getExpectedReceiptDate()
        );


        validateReason(
                request.getReasonCode(),
                request.getReasonNote()
        );


        String oldValue =
                snapshot(
                        transfer,
                        transfer.getItems()
                );


        boolean maintainReservation =
                currentStatus == TransferStatus.CREATED
                        || hasActiveReservation(
                        transfer.getId()
                );


        if (maintainReservation) {

            releaseReservedInventory(
                    transfer,
                    currentUser
            );
        }


        transfer.setToWarehouse(
                destination
        );

        transfer.setExpectedShipmentDate(
                request.getExpectedShipmentDate()
        );

        transfer.setExpectedReceiptDate(
                request.getExpectedReceiptDate()
        );

        transfer.setReasonCode(
                request.getReasonCode()
        );

        transfer.setReasonNote(
                normalizeText(
                        request.getReasonNote()
                )
        );

        transfer.setNotes(
                normalizeText(
                        request.getNotes()
                )
        );


        List<StockTransferItems> finalItems =
                applyUpdatedItems(
                        transfer,
                        request.getItems()
                );


        if (maintainReservation) {

            List<StockTransferItems> sortedItems =
                    finalItems.stream()
                            .sorted(
                                    Comparator.comparing(
                                            item ->
                                                    item.getProduct()
                                                            .getId()
                                    )
                            )
                            .toList();


            for (StockTransferItems item : sortedItems) {

                validateProductBeforeSubmit(
                        item
                );

                reserveInventory(
                        transfer,
                        item,
                        currentUser
                );

                item.setApprovedQuantity(
                        item.getRequestedQuantity()
                );
            }

        } else {

            for (StockTransferItems item : finalItems) {

                item.setApprovedQuantity(
                        0
                );
            }
        }


        stockTransferItemRepository.saveAll(
                finalItems
        );


        transfer =
                stockTransferRepository.save(
                        transfer
                );


        saveHistory(
                currentUser,
                ActivityAction.UPDATE,
                transfer,
                oldValue,
                snapshot(
                        transfer,
                        finalItems
                )
        );


        return getDetailedResponse(
                transfer.getId()
        );
    }


    // =====================================================
    // CANCEL STOCK TRANSFER
    // =====================================================

    @Transactional
    public StockTransferDetailResponse cancel(
            Long transferId,
            CancelStockTransferRequest request
    ) {

        Users currentUser =
                getCurrentUser();


        StockTransfer transfer =
                getDetailedTransfer(
                        transferId
                );


        validateCanModifyTransfer(
                transfer,
                currentUser
        );


        String cancelReason =
                normalizeText(
                        request.getReason()
                );


        if (cancelReason == null) {

            throw new BadRequest(
                    "Cancel reason is required"
            );
        }


        String oldValue =
                snapshot(
                        transfer,
                        transfer.getItems()
                );


        releaseReservedInventory(
                transfer,
                currentUser
        );


        transfer.setRejectionReason(
                cancelReason
        );

        transfer.setStatus(
                TransferStatus.CANCELLED
        );


        transfer =
                stockTransferRepository.save(
                        transfer
                );


        saveHistory(
                currentUser,
                ActivityAction.CANCEL,
                transfer,
                oldValue,
                snapshot(
                        transfer,
                        transfer.getItems()
                )
        );


        return getDetailedResponse(
                transfer.getId()
        );
    }


    // =====================================================
    // BUILD ITEMS
    // =====================================================

    private List<StockTransferItems> buildItems(
            StockTransfer transfer,
            List<CreateStockTransferItemRequest> requests
    ) {

        if (
                requests == null
                        || requests.isEmpty()
        ) {

            throw new BadRequest(
                    "Stock transfer must contain at least one product"
            );
        }


        Set<Long> productIds =
                new HashSet<>();


        List<StockTransferItems> result =
                new ArrayList<>();


        for (
                CreateStockTransferItemRequest request
                : requests
        ) {

            Long productId =
                    request.getProductId();


            if (
                    !productIds.add(
                            productId
                    )
            ) {

                throw new BadRequest(
                        "Duplicate product in stock transfer: "
                                + productId
                );
            }


            Products product =
                    productRepository
                            .findById(
                                    productId
                            )
                            .orElseThrow(() ->
                                    new NotFound(
                                            "Product not found with id: "
                                                    + productId
                                    )
                            );


            if (
                    product.getStatus()
                            != ProductStatus.ACTIVE
            ) {

                throw new BadRequest(
                        "Product is not active: "
                                + productId
                );
            }


            int requestedQuantity =
                    request.getRequestedQuantity();


            long availableQuantity =
                    inventoryBalanceRepository
                            .sumAvailableQuantity(
                                    transfer
                                            .getFromWarehouse()
                                            .getId(),

                                    productId
                            );


            if (
                    requestedQuantity
                            > availableQuantity
            ) {

                throw new BadRequest(
                        "Requested quantity for product "
                                + productId
                                + " exceeds available inventory. "
                                + "Available: "
                                + availableQuantity
                );
            }


            StockTransferItems item =
                    StockTransferItems.builder()

                            .stockTransfer(
                                    transfer
                            )

                            .product(
                                    product
                            )

                            .requestedQuantity(
                                    requestedQuantity
                            )

                            .approvedQuantity(
                                    0
                            )

                            .pickedQuantity(
                                    0
                            )

                            .packedQuantity(
                                    0
                            )

                            .shippedQuantity(
                                    0
                            )

                            .receivedQuantity(
                                    0
                            )

                            .shortageQuantity(
                                    0
                            )

                            .surplusQuantity(
                                    0
                            )

                            .stockTransferItemsCode(
                                    generateItemCode()
                            )

                            .build();


            result.add(
                    item
            );
        }


        return result;
    }


    // =====================================================
    // APPLY UPDATED ITEMS
    // ADD / REMOVE / CHANGE REQUESTED QUANTITY
    // =====================================================

    private List<StockTransferItems> applyUpdatedItems(
            StockTransfer transfer,
            List<CreateStockTransferItemRequest> requests
    ) {

        List<StockTransferItems> requestedItems =
                buildItems(
                        transfer,
                        requests
                );


        Map<Long, StockTransferItems> existingByProductId =
                new HashMap<>();


        for (StockTransferItems existingItem : transfer.getItems()) {

            existingByProductId.put(
                    existingItem.getProduct().getId(),
                    existingItem
            );
        }


        List<StockTransferItems> finalItems =
                new ArrayList<>();


        List<StockTransferItems> newItems =
                new ArrayList<>();


        for (StockTransferItems requestedItem : requestedItems) {

            Long productId =
                    requestedItem.getProduct().getId();


            StockTransferItems existingItem =
                    existingByProductId.remove(
                            productId
                    );


            if (existingItem != null) {

                existingItem.setRequestedQuantity(
                        requestedItem.getRequestedQuantity()
                );

                finalItems.add(
                        existingItem
                );

            } else {

                newItems.add(
                        requestedItem
                );

                finalItems.add(
                        requestedItem
                );
            }
        }


        List<StockTransferItems> removedItems =
                new ArrayList<>(
                        existingByProductId.values()
                );


        if (!removedItems.isEmpty()) {

            transfer.getItems().removeAll(
                    removedItems
            );

            stockTransferItemRepository.deleteAll(
                    removedItems
            );
        }


        for (StockTransferItems newItem : newItems) {

            transfer.addItem(
                    newItem
            );
        }


        return finalItems;
    }


    // =====================================================
    // RESERVE INVENTORY
    // =====================================================

    private void reserveInventory(
            StockTransfer transfer,
            StockTransferItems item,
            Users currentUser
    ) {

        List<InventoryBalances> balances =
                inventoryBalanceRepository
                        .findByWarehouseIdAndProductIdForUpdate(
                                transfer
                                        .getFromWarehouse()
                                        .getId(),

                                item.getProduct()
                                        .getId()
                        );


        int totalAvailable =
                balances.stream()

                        .map(
                                InventoryBalances::getAvailableQuantity
                        )

                        .filter(
                                quantity ->
                                        quantity != null
                        )

                        .mapToInt(
                                Integer::intValue
                        )

                        .sum();


        int requested =
                item.getRequestedQuantity();


        if (
                requested
                        > totalAvailable
        ) {

            throw new BadRequest(
                    "Product "
                            + item.getProduct().getId()
                            + " no longer has enough available inventory. "
                            + "Available: "
                            + totalAvailable
                            + ", requested: "
                            + requested
            );
        }


        int remaining =
                requested;


        for (InventoryBalances balance : balances) {

            if (remaining <= 0) {
                break;
            }


            int availableBefore =
                    balance.getAvailableQuantity() == null
                            ? 0
                            : balance.getAvailableQuantity();


            if (availableBefore <= 0) {
                continue;
            }


            int reserveQuantity =
                    Math.min(
                            availableBefore,
                            remaining
                    );


            int reservedBefore =
                    balance.getReservedQuantity() == null
                            ? 0
                            : balance.getReservedQuantity();


            balance.setReservedQuantity(
                    reservedBefore
                            + reserveQuantity
            );


            inventoryBalanceRepository.save(
                    balance
            );


            createTransferOutTransaction(
                    transfer,
                    item,
                    balance,
                    currentUser,
                    availableBefore,
                    reserveQuantity
            );


            remaining -=
                    reserveQuantity;
        }


        if (remaining != 0) {

            throw new BadRequest(
                    "Unable to reserve complete inventory for product "
                            + item.getProduct().getId()
            );
        }
    }


    // =====================================================
    // CREATE TRANSFER OUT TRANSACTION
    // =====================================================

    private void createTransferOutTransaction(
            StockTransfer transfer,
            StockTransferItems item,
            InventoryBalances balance,
            Users currentUser,
            int availableBefore,
            int reservedQuantity
    ) {

        InventoryTransactions transaction =
                InventoryTransactions.builder()

                        .inventoryTransactionsCode(
                                generateTransactionCode()
                        )

                        .warehouse(
                                transfer.getFromWarehouse()
                        )

                        .location(
                                balance.getLocation()
                        )

                        .product(
                                item.getProduct()
                        )

                        .quantityBefore(
                                availableBefore
                        )

                        .quantityChange(
                                -reservedQuantity
                        )

                        .quantityAfter(
                                availableBefore
                                        - reservedQuantity
                        )

                        .referenceId(
                                transfer.getId()
                        )

                        .referenceType(
                                InventoryReferenceType.STOCK_TRANSFER
                        )

                        .transactionType(
                                InventoryTransactionType.TRANSFER_OUT
                        )

                        .performedBy(
                                currentUser
                        )

                        .build();


        inventoryTransactionRepository.save(
                transaction
        );
    }


    // =====================================================
    // ACTIVE RESERVATION OF THIS TRANSFER
    // =====================================================

    private boolean hasActiveReservation(
            Long transferId
    ) {

        List<InventoryTransactions> history =
                getReservationHistory(
                        transferId
                );


        Map<ReservationKey, Integer> netByBalance =
                buildReservationNetByBalance(
                        history
                );


        return netByBalance
                .values()
                .stream()
                .anyMatch(
                        value ->
                                value != null
                                        && value < 0
                );
    }


    // =====================================================
    // RELEASE RESERVED INVENTORY OF THIS TRANSFER ONLY
    // =====================================================

    private void releaseReservedInventory(
            StockTransfer transfer,
            Users currentUser
    ) {

        List<InventoryTransactions> history =
                getReservationHistory(
                        transfer.getId()
                );


        if (history.isEmpty()) {
            return;
        }


        Map<ReservationKey, Integer> netByBalance =
                buildReservationNetByBalance(
                        history
                );


        List<Map.Entry<ReservationKey, Integer>> activeReservations =
                netByBalance
                        .entrySet()
                        .stream()

                        .filter(
                                entry ->
                                        entry.getValue() != null
                                                && entry.getValue() < 0
                        )

                        .sorted(
                                Comparator
                                        .comparing(
                                                (
                                                        Map.Entry<
                                                                ReservationKey,
                                                                Integer
                                                                > entry
                                                ) ->
                                                        entry.getKey()
                                                                .productId()
                                        )
                                        .thenComparing(
                                                entry ->
                                                        entry.getKey()
                                                                .locationId()
                                        )
                        )

                        .toList();


        for (
                Map.Entry<ReservationKey, Integer> entry
                : activeReservations
        ) {

            ReservationKey key =
                    entry.getKey();


            int releaseQuantity =
                    -entry.getValue();


            InventoryBalances balance =
                    inventoryBalanceRepository
                            .findExactBalanceForUpdate(
                                    key.warehouseId(),
                                    key.locationId(),
                                    key.productId()
                            )
                            .orElseThrow(() ->
                                    new BadRequest(
                                            "Reserved inventory balance no longer exists for product "
                                                    + key.productId()
                                    )
                            );


            int reservedBefore =
                    balance.getReservedQuantity() == null
                            ? 0
                            : balance.getReservedQuantity();


            if (
                    releaseQuantity
                            > reservedBefore
            ) {

                throw new BadRequest(
                        "Reserved inventory is inconsistent for product "
                                + key.productId()
                                + ". Reserved: "
                                + reservedBefore
                                + ", transfer requires release: "
                                + releaseQuantity
                );
            }


            int availableBefore =
                    balance.getAvailableQuantity() == null
                            ? 0
                            : balance.getAvailableQuantity();


            balance.setReservedQuantity(
                    reservedBefore
                            - releaseQuantity
            );


            inventoryBalanceRepository.save(
                    balance
            );


            createTransferCancelledTransaction(
                    transfer,
                    balance,
                    currentUser,
                    availableBefore,
                    releaseQuantity
            );
        }


        inventoryBalanceRepository.flush();
    }


    // =====================================================
    // CALCULATE RESERVATION NET
    // =====================================================

    private Map<ReservationKey, Integer>
    buildReservationNetByBalance(
            List<InventoryTransactions> history
    ) {

        Map<ReservationKey, Integer> netByBalance =
                new HashMap<>();


        for (InventoryTransactions transaction : history) {

            if (
                    transaction.getWarehouse() == null
                            || transaction.getLocation() == null
                            || transaction.getProduct() == null
                            || transaction.getQuantityChange() == null
            ) {

                continue;
            }


            ReservationKey key =
                    new ReservationKey(
                            transaction.getWarehouse().getId(),
                            transaction.getLocation().getId(),
                            transaction.getProduct().getId()
                    );


            netByBalance.merge(
                    key,
                    transaction.getQuantityChange(),
                    Integer::sum
            );
        }


        return netByBalance;
    }


    // =====================================================
    // GET RESERVATION HISTORY
    // =====================================================

    private List<InventoryTransactions> getReservationHistory(
            Long transferId
    ) {

        return inventoryTransactionRepository
                .findReservationHistory(
                        InventoryReferenceType.STOCK_TRANSFER,
                        transferId,

                        List.of(
                                InventoryTransactionType.TRANSFER_OUT,
                                InventoryTransactionType.TRANSFER_CANCELLED
                        )
                );
    }


    // =====================================================
    // CREATE TRANSFER CANCELLED TRANSACTION
    // =====================================================

    private void createTransferCancelledTransaction(
            StockTransfer transfer,
            InventoryBalances balance,
            Users currentUser,
            int availableBefore,
            int releaseQuantity
    ) {

        InventoryTransactions transaction =
                InventoryTransactions.builder()

                        .inventoryTransactionsCode(
                                generateTransactionCode()
                        )

                        .warehouse(
                                balance.getWarehouse()
                        )

                        .location(
                                balance.getLocation()
                        )

                        .product(
                                balance.getProduct()
                        )

                        .quantityBefore(
                                availableBefore
                        )

                        .quantityChange(
                                releaseQuantity
                        )

                        .quantityAfter(
                                availableBefore
                                        + releaseQuantity
                        )

                        .referenceId(
                                transfer.getId()
                        )

                        .referenceType(
                                InventoryReferenceType.STOCK_TRANSFER
                        )

                        .transactionType(
                                InventoryTransactionType.TRANSFER_CANCELLED
                        )

                        .performedBy(
                                currentUser
                        )

                        .build();


        inventoryTransactionRepository.save(
                transaction
        );
    }


    // =====================================================
    // RESERVATION KEY
    // =====================================================

    private record ReservationKey(
            Long warehouseId,
            Long locationId,
            Long productId
    ) {
    }


    // =====================================================
    // VALIDATE PRODUCT AGAIN BEFORE SUBMIT
    // =====================================================

    private void validateProductBeforeSubmit(
            StockTransferItems item
    ) {

        if (
                item.getProduct() == null
                        || item.getProduct().getStatus()
                        != ProductStatus.ACTIVE
        ) {

            throw new BadRequest(
                    "Stock transfer contains inactive product"
            );
        }


        if (
                item.getRequestedQuantity() == null
                        || item.getRequestedQuantity() <= 0
        ) {

            throw new BadRequest(
                    "Requested quantity must be greater than 0"
            );
        }
    }


    // =====================================================
    // VALIDATE SOURCE + DESTINATION
    // =====================================================

    private void validateWarehousePair(
            Long sourceId,
            Long destinationId
    ) {

        if (
                sourceId == null
                        || destinationId == null
        ) {

            throw new BadRequest(
                    "Source and destination are required"
            );
        }


        if (
                sourceId.equals(
                        destinationId
                )
        ) {

            throw new BadRequest(
                    "Source and destination cannot be the same"
            );
        }
    }


    // =====================================================
    // ACTIVE SOURCE
    // MAIN_WAREHOUSE OR STORE
    // =====================================================

    private Warehouses getActiveSource(
            Long warehouseId
    ) {

        Warehouses warehouse =
                getWarehouse(
                        warehouseId
                );


        if (
                warehouse.getStatus()
                        != WarehouseStatus.ACTIVE
        ) {

            throw new BadRequest(
                    "Source warehouse/store is not active"
            );
        }


        if (
                warehouse.getType()
                        != WarehouseType.MAIN_WAREHOUSE

                        && warehouse.getType()
                        != WarehouseType.STORE
        ) {

            throw new BadRequest(
                    "Invalid source warehouse type"
            );
        }


        return warehouse;
    }


    // =====================================================
    // ACTIVE DESTINATION
    // STORE ONLY
    // =====================================================

    private Warehouses getActiveDestination(
            Long warehouseId
    ) {

        Warehouses warehouse =
                getWarehouse(
                        warehouseId
                );


        if (
                warehouse.getStatus()
                        != WarehouseStatus.ACTIVE
        ) {

            throw new BadRequest(
                    "Destination store is not active"
            );
        }


        if (
                warehouse.getType()
                        != WarehouseType.STORE
        ) {

            throw new BadRequest(
                    "Stock transfer destination must be a STORE"
            );
        }


        return warehouse;
    }


    private void validateActiveWarehouseEntities(
            StockTransfer transfer
    ) {

        getActiveSource(
                transfer.getFromWarehouse()
                        .getId()
        );

        getActiveDestination(
                transfer.getToWarehouse()
                        .getId()
        );
    }


    private Warehouses getWarehouse(
            Long warehouseId
    ) {

        return warehouseRepository
                .findById(
                        warehouseId
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Warehouse/store not found with id: "
                                        + warehouseId
                        )
                );
    }


    // =====================================================
    // DATES
    // =====================================================

    private void validateDates(
            LocalDate expectedShipmentDate,
            LocalDate expectedReceiptDate
    ) {

        if (
                expectedShipmentDate == null
                        || expectedReceiptDate == null
        ) {

            throw new BadRequest(
                    "Expected shipment and receipt dates are required"
            );
        }


        if (
                expectedShipmentDate.isBefore(
                        LocalDate.now()
                )
        ) {

            throw new BadRequest(
                    "Expected shipment date cannot be in the past"
            );
        }


        if (
                expectedReceiptDate.isBefore(
                        expectedShipmentDate
                )
        ) {

            throw new BadRequest(
                    "Expected receipt date cannot be before expected shipment date"
            );
        }
    }


    // =====================================================
    // REASON
    // =====================================================

    private void validateReason(
            TransferReasonCode reasonCode,
            String reasonNote
    ) {

        if (reasonCode == null) {

            throw new BadRequest(
                    "Transfer reason is required"
            );
        }


        if (
                reasonCode
                        == TransferReasonCode.OTHER

                        && (
                        reasonNote == null
                                || reasonNote.isBlank()
                )
        ) {

            throw new BadRequest(
                    "Reason note is required when reason code is OTHER"
            );
        }
    }


    // =====================================================
    // VALIDATE UPDATE / CANCEL
    // =====================================================

    private void validateCanModifyTransfer(
            StockTransfer transfer,
            Users currentUser
    ) {

        TransferStatus status =
                transfer.getStatus();


        /*
         * Mapping Task #12:
         *
         * DRAFT                       -> cho phép
         * CREATED                     -> cho phép
         * PENDING_SOURCE_CONFIRMATION -> cho phép
         *
         * PICKING trở đi -> không sửa/hủy.
         */
        if (
                status != TransferStatus.DRAFT
                        && status != TransferStatus.CREATED
                        && status != TransferStatus.PENDING_SOURCE_CONFIRMATION
        ) {

            throw new BadRequest(
                    "Stock transfer can no longer be updated or cancelled in status: "
                            + status
            );
        }


        /*
         * Creator được sửa/hủy phiếu của mình.
         */
        if (
                transfer.getCreatedBy() != null
                        && Objects.equals(
                        transfer.getCreatedBy().getId(),
                        currentUser.getId()
                )
        ) {

            return;
        }


        /*
         * Business Manager hoặc Admin được override.
         */
        if (hasBusinessOverridePermission()) {
            return;
        }


        throw new Forbidden(
                "Only the creator, Business Manager, or Admin can update or cancel this stock transfer"
        );
    }


    private boolean hasBusinessOverridePermission() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (
                authentication == null
                        || !authentication.isAuthenticated()
        ) {

            return false;
        }


        return authentication
                .getAuthorities()
                .stream()

                .map(
                        GrantedAuthority::getAuthority
                )

                .anyMatch(
                        authority ->
                                "ROLE_BUSINESS_MANAGER"
                                        .equals(authority)

                                        || "ROLE_ADMIN"
                                        .equals(authority)
                );
    }


    // =====================================================
    // CURRENT USER
    // =====================================================

    private Users getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (
                authentication == null
                        || !authentication.isAuthenticated()
                        || "anonymousUser".equals(
                        authentication.getPrincipal()
                )
        ) {

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


    // =====================================================
    // GET TRANSFER
    // =====================================================

    private StockTransfer getDetailedTransfer(
            Long transferId
    ) {

        return stockTransferRepository
                .findDetailedById(
                        transferId
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Stock transfer not found with id: "
                                        + transferId
                        )
                );
    }


    private StockTransferDetailResponse getDetailedResponse(
            Long transferId
    ) {

        return mapper.toDetailResponse(
                getDetailedTransfer(
                        transferId
                )
        );
    }


    // =====================================================
    // CODE GENERATION
    // =====================================================

    private String generateTransferCode() {

        String code;

        do {

            code =
                    "ST-"
                            + LocalDate.now()
                            .format(
                                    DateTimeFormatter.BASIC_ISO_DATE
                            )
                            + "-"
                            + randomPart(
                                    8
                            );

        } while (
                stockTransferRepository
                        .existsByTransferCode(
                                code
                        )

                        || stockTransferRepository
                        .existsByStockTransfersCode(
                                code
                        )
        );


        return code;
    }


    private String generateItemCode() {

        String code;

        do {

            code =
                    "STI-"
                            + randomPart(
                                    12
                            );

        } while (
                stockTransferItemRepository
                        .existsByStockTransferItemsCode(
                                code
                        )
        );


        return code;
    }


    private String generateTransactionCode() {

        return "IT-"
                + randomPart(
                        16
                );
    }


    private String randomPart(
            int length
    ) {

        return UUID.randomUUID()
                .toString()
                .replace(
                        "-",
                        ""
                )
                .substring(
                        0,
                        length
                )
                .toUpperCase();
    }


    // =====================================================
    // ACTIVITY LOG
    // =====================================================

    private void saveHistory(
            Users user,
            ActivityAction action,
            StockTransfer transfer,
            String oldValue,
            String newValue
    ) {

        ActivityLogs log =
                ActivityLogs.builder()

                        .user(
                                user
                        )

                        .action(
                                action
                        )

                        .entityType(
                                ActivityEntityType.STOCK_TRANSFER
                        )

                        .entityId(
                                transfer.getId()
                        )

                        .oldValue(
                                oldValue
                        )

                        .newValue(
                                newValue
                        )

                        .build();


        activityLogRepository.save(
                log
        );
    }


    // =====================================================
    // AUDIT SNAPSHOT
    // =====================================================

    private String snapshot(
            StockTransfer transfer,
            List<StockTransferItems> items
    ) {

        StringBuilder builder =
                new StringBuilder();


        builder.append(
                "transferCode="
        ).append(
                transfer.getTransferCode()
        );


        builder.append(
                ", fromWarehouseId="
        ).append(
                transfer.getFromWarehouse() != null
                        ? transfer.getFromWarehouse().getId()
                        : null
        );


        builder.append(
                ", toWarehouseId="
        ).append(
                transfer.getToWarehouse() != null
                        ? transfer.getToWarehouse().getId()
                        : null
        );


        builder.append(
                ", status="
        ).append(
                transfer.getStatus()
        );


        builder.append(
                ", expectedShipmentDate="
        ).append(
                transfer.getExpectedShipmentDate()
        );


        builder.append(
                ", expectedReceiptDate="
        ).append(
                transfer.getExpectedReceiptDate()
        );


        builder.append(
                ", reasonCode="
        ).append(
                transfer.getReasonCode()
        );


        builder.append(
                ", reasonNote="
        ).append(
                transfer.getReasonNote()
        );


        builder.append(
                ", notes="
        ).append(
                transfer.getNotes()
        );


        builder.append(
                ", cancelReason="
        ).append(
                transfer.getStatus() == TransferStatus.CANCELLED
                        ? transfer.getRejectionReason()
                        : null
        );


        builder.append(
                ", items=["
        );


        if (items != null) {

            for (int i = 0; i < items.size(); i++) {

                StockTransferItems item =
                        items.get(i);


                if (i > 0) {
                    builder.append(", ");
                }


                builder.append(
                        "{productId="
                ).append(
                        item.getProduct() != null
                                ? item.getProduct().getId()
                                : null
                ).append(
                        ", requestedQuantity="
                ).append(
                        item.getRequestedQuantity()
                ).append(
                        ", approvedQuantity="
                ).append(
                        item.getApprovedQuantity()
                ).append(
                        "}"
                );
            }
        }


        builder.append(
                "]"
        );


        return builder.toString();
    }


    // =====================================================
    // NORMALIZE TEXT
    // =====================================================

    private String normalizeText(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return null;
        }


        return value.trim();
    }
}