package com.toystorage.backend.services.transfers.picking;

import com.toystorage.backend.dto.request.transfers.picking.PickTransferItemRequest;

import com.toystorage.backend.dto.response.transfers.picking.TransferPickingItemResponse;
import com.toystorage.backend.dto.response.transfers.picking.TransferPickingResponse;

import com.toystorage.backend.enums.warehouses.WarehouseTaskType;
import com.toystorage.backend.services.warehouses.taskclaim.WarehouseTaskClaimService;

import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.transfers.StockTransferItems;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.transfers.TransferStatus;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.mapper.transfers.picking.TransferPickingMapper;

import com.toystorage.backend.repository.inventories.InventoryBalanceRepository;
import com.toystorage.backend.repository.transfers.picking.StockTransferItemRepository;
import com.toystorage.backend.repository.transfers.picking.StockTransferRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferPickingService {

    private final StockTransferRepository
            stockTransferRepository;

    private final StockTransferItemRepository
            stockTransferItemRepository;

    private final InventoryBalanceRepository
            inventoryBalanceRepository;

    private final TransferPickingValidationService
            validationService;
    private final WarehouseTaskClaimService
            taskClaimService;

    private final TransferPickingMapper
            mapper;


    // =====================================================
    // LIST TRANSFERS
    // =====================================================

    @Transactional(readOnly = true)
    public List<TransferPickingResponse>
    getMyWarehouseTransfers() {

        Users staff =
                validationService
                        .getCurrentUser();

        if (staff.getWarehouse() == null) {

            throw new BadRequest(
                    "User is not assigned to warehouse"
            );
        }


        List<StockTransfer> transfers =
                stockTransferRepository
                        .findByFromWarehouseIdAndStatusInOrderByCreatedAtDesc(
                                staff
                                        .getWarehouse()
                                        .getId(),

                                List.of(
                                        TransferStatus.CREATED,
                                        TransferStatus.PENDING_SOURCE_CONFIRMATION,
                                        TransferStatus.PICKING
                                )
                        );


        return transfers
                .stream()

                .map(
                        this::buildResponse
                )

                .toList();
    }


    // =====================================================
    // DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public TransferPickingResponse getTransfer(
            Long transferId
    ) {

        Users staff =
                validationService
                        .getCurrentUser();

        StockTransfer transfer =
                validationService
                        .getTransfer(
                                transferId
                        );

        validationService
                .validateWarehouse(
                        staff,
                        transfer
                );

        return buildResponse(
                transfer
        );
    }


    // =====================================================
    // START PICKING
    // =====================================================

    @Transactional
    public TransferPickingResponse startPicking(
            Long transferId
    ) {

        Users staff =
                validationService
                        .getCurrentUser();


        StockTransfer transfer =
                validationService
                        .getTransfer(
                                transferId
                        );


        validationService
                .validateWarehouse(
                        staff,
                        transfer
                );


        /*
         * Cho phép chính owner gọi Start lại.
         */
        if (transfer.getStatus()
                == TransferStatus.PICKING) {

            taskClaimService.validateOwner(
                    WarehouseTaskType.TRANSFER_PICKING,
                    transferId,
                    staff
            );

            return buildResponse(
                    transfer
            );
        }


        if (
                transfer.getStatus() != TransferStatus.CREATED
                        && transfer.getStatus()
                        != TransferStatus.PENDING_SOURCE_CONFIRMATION
        ) {

            throw new BadRequest(
                    "Transfer cannot start picking from status "
                            + transfer.getStatus()
            );
        }


        if (transfer.getConfirmedAt() == null) {

            throw new BadRequest(
                    "Transfer must be confirmed before picking"
            );
        }


        /*
         * CLAIM TRƯỚC.
         *
         * Nếu 2 Staff bấm đồng thời:
         * chỉ 1 INSERT thành công.
         */
        taskClaimService.claim(
                WarehouseTaskType.TRANSFER_PICKING,
                transferId,
                staff
        );


        transfer.setStatus(
                TransferStatus.PICKING
        );

        transfer.setUpdatedAt(
                LocalDateTime.now()
        );


        stockTransferRepository.save(
                transfer
        );


        return buildResponse(
                transfer
        );
    }


    // =====================================================
    // PICK ITEM
    // =====================================================

    @Transactional
    public TransferPickingItemResponse pickItem(
            Long transferId,
            Long itemId,
            PickTransferItemRequest request
    ) {

        Users staff =
                validationService
                        .getCurrentUser();


        StockTransfer transfer =
                validationService
                        .getTransfer(
                                transferId
                        );


        validationService
                .validateWarehouse(
                        staff,
                        transfer
                );

        taskClaimService.validateOwner(
                WarehouseTaskType.TRANSFER_PICKING,
                transferId,
                staff
        );


        validationService
                .validatePickingStatus(
                        transfer
                );


        StockTransferItems item =
                validationService
                        .getItem(
                                transferId,
                                itemId
                        );


        validationService
                .validateQuantity(
                        item,
                        request.getQuantity()
                );


        InventoryBalances balance =
                validationService
                        .validateScanAndLocation(
                                transfer,
                                item,
                                request.getProductBarcode(),
                                request.getLocationCode()
                        );


        int currentPicked =
                item.getPickedQuantity() == null
                        ? 0
                        : item.getPickedQuantity();


        int pickedAfter =
                currentPicked
                        + request.getQuantity();


        item.setPickedQuantity(
                pickedAfter
        );


        /*
         * QUAN TRỌNG:
         *
         * Không giảm inventory ở đây.
         *
         * Transfer confirmation đã xử lý
         * inventory trước đó.
         */
        stockTransferItemRepository
                .save(
                        item
                );


        return mapper
                .toItemResponse(
                        item,
                        balance
                );
    }


    // =====================================================
    // COMPLETE PICKING
    // =====================================================

    @Transactional
    public TransferPickingResponse completePicking(
            Long transferId
    ) {

        Users staff =
                validationService
                        .getCurrentUser();


        StockTransfer transfer =
                validationService
                        .getTransfer(
                                transferId
                        );


        validationService
                .validateWarehouse(
                        staff,
                        transfer
                );

        taskClaimService.validateOwner(
                WarehouseTaskType.TRANSFER_PICKING,
                transferId,
                staff
        );


        validationService
                .validatePickingStatus(
                        transfer
                );



        List<StockTransferItems> items =
                stockTransferItemRepository
                        .findByStockTransferId(
                                transferId
                        );


        if (items.isEmpty()) {

            throw new BadRequest(
                    "Transfer contains no items"
            );
        }


        boolean incomplete =
                items.stream()
                        .anyMatch(item -> {

                            int picked =
                                    item.getPickedQuantity() == null
                                            ? 0
                                            : item.getPickedQuantity();

                            return picked
                                    < item.getApprovedQuantity();
                        });


        if (incomplete) {

            throw new BadRequest(
                    "Picking cannot be completed "
                            + "while products are still missing"
            );
        }


        transfer.setStatus(
             TransferStatus.PACKING
        );


        transfer.setUpdatedAt(
                LocalDateTime.now()
        );


        stockTransferRepository
                .save(
                        transfer
                );
        taskClaimService.release(
                WarehouseTaskType.TRANSFER_PICKING,
                transferId,
                staff
        );

        return buildResponse(
                transfer
        );
    }


    // =====================================================
    // RESPONSE
    // =====================================================

    private TransferPickingResponse buildResponse(
            StockTransfer transfer
    ) {

        List<StockTransferItems> items =
                stockTransferItemRepository
                        .findByStockTransferId(
                                transfer.getId()
                        );


        List<TransferPickingItemResponse> responses =
                new ArrayList<>();


        for (StockTransferItems item : items) {

            InventoryBalances balance =
                    inventoryBalanceRepository
                            .findFirstByWarehouseIdAndProductId(
                                    transfer
                                            .getFromWarehouse()
                                            .getId(),

                                    item.getProduct()
                                            .getId()
                            )
                            .orElse(null);


            responses.add(
                    mapper.toItemResponse(
                            item,
                            balance
                    )
            );
        }


        return mapper
                .toResponse(
                        transfer,
                        responses
                );
    }
}