package com.toystorage.backend.mapper.transfers;

import com.toystorage.backend.dto.response.transfers.TransferPickingItemResponse;
import com.toystorage.backend.dto.response.transfers.TransferPickingResponse;

import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.transfers.StockTransferItems;
import com.toystorage.backend.entity.transfers.StockTransfer;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransferPickingMapper {

    public TransferPickingItemResponse toItemResponse(
            StockTransferItems item,
            InventoryBalances balance
    ) {

        int pickedQuantity =
                item.getPickedQuantity() == null
                        ? 0
                        : item.getPickedQuantity();

        int remainingQuantity =
                Math.max(
                        item.getApprovedQuantity()
                                - pickedQuantity,
                        0
                );


        return TransferPickingItemResponse
                .builder()

                .itemId(
                        item.getId()
                )

                .productId(
                        item.getProduct()
                                .getId()
                )

                .productCode(
                        item.getProduct()
                                .getProductsCode()
                )

                .productName(
                        item.getProduct()
                                .getName()
                )

                .barcode(
                        item.getProduct()
                                .getBarcode()
                )

                .approvedQuantity(
                        item.getApprovedQuantity()
                )

                .pickedQuantity(
                        pickedQuantity
                )

                .remainingQuantity(
                        remainingQuantity
                )

                .shortageQuantity(
                        item.getShortageQuantity()
                )

                .locationCode(
                        balance != null
                                ? balance
                                .getLocation()
                                .getWarehouseCode()
                                : null
                )

                .zone(
                        balance != null
                                ? balance
                                .getLocation()
                                .getZone()
                                : null
                )

                .shelf(
                        balance != null
                                ? balance
                                .getLocation()
                                .getShelf()
                                : null
                )

                .build();
    }


    public TransferPickingResponse toResponse(
            StockTransfer transfer,
            List<TransferPickingItemResponse> items
    ) {

        return TransferPickingResponse
                .builder()

                .transferId(
                        transfer.getId()
                )

                .transferCode(
                        transfer.getTransferCode()
                )

                .status(
                        transfer.getStatus()
                                .name()
                )

                .fromWarehouseId(
                        transfer
                                .getFromWarehouse()
                                .getId()
                )

                .fromWarehouseName(
                        transfer
                                .getFromWarehouse()
                                .getName()
                )

                .toWarehouseId(
                        transfer
                                .getToWarehouse()
                                .getId()
                )

                .toWarehouseName(
                        transfer
                                .getToWarehouse()
                                .getName()
                )

                .items(
                        items
                )

                .build();
    }
}