package com.toystorage.backend.mapper.transfers;

import com.toystorage.backend.dto.response.transfers.StockTransferDetailResponse;
import com.toystorage.backend.dto.response.transfers.StockTransferItemResponse;
import com.toystorage.backend.dto.response.transfers.StockTransferProductOptionResponse;
import com.toystorage.backend.dto.response.transfers.StockTransferWarehouseOptionResponse;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.transfers.StockTransferItems;
import com.toystorage.backend.entity.warehouses.Warehouses;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class StockTransferCreationMapper {

    // =====================================================
    // WAREHOUSE OPTION
    // =====================================================

    public StockTransferWarehouseOptionResponse toWarehouseOption(
            Warehouses warehouse
    ) {

        if (warehouse == null) {
            return null;
        }

        return StockTransferWarehouseOptionResponse.builder()

                .id(
                        warehouse.getId()
                )

                .code(
                        warehouse.getWarehousesCode()
                )

                .name(
                        warehouse.getName()
                )

                .type(
                        warehouse.getType() != null
                                ? warehouse.getType().name()
                                : null
                )

                .address(
                        warehouse.getAddress()
                )

                .build();
    }


    // =====================================================
    // PRODUCT OPTION
    // =====================================================

    public StockTransferProductOptionResponse toProductOption(
            Products product,
            Number availableQuantity
    ) {

        if (product == null) {
            return null;
        }

        int available =
                availableQuantity != null
                        ? availableQuantity.intValue()
                        : 0;

        return StockTransferProductOptionResponse.builder()

                .productId(
                        product.getId()
                )

                .productCode(
                        product.getProductsCode()
                )

                .productName(
                        product.getName()
                )

                .barcode(
                        product.getBarcode()
                )

                .baseUnit(
                        product.getBaseUnit()
                )

                .availableQuantity(
                        available
                )

                .build();
    }


    // =====================================================
    // TRANSFER ITEM
    // =====================================================

    public StockTransferItemResponse toItemResponse(
            StockTransferItems item
    ) {

        if (item == null) {
            return null;
        }

        Products product =
                item.getProduct();

        return StockTransferItemResponse.builder()

                .id(
                        item.getId()
                )

                .itemCode(
                        item.getStockTransferItemsCode()
                )

                .productId(
                        product != null
                                ? product.getId()
                                : null
                )

                .productCode(
                        product != null
                                ? product.getProductsCode()
                                : null
                )

                .productName(
                        product != null
                                ? product.getName()
                                : null
                )

                .barcode(
                        product != null
                                ? product.getBarcode()
                                : null
                )

                .baseUnit(
                        product != null
                                ? product.getBaseUnit()
                                : null
                )

                .requestedQuantity(
                        valueOrZero(
                                item.getRequestedQuantity()
                        )
                )

                .approvedQuantity(
                        valueOrZero(
                                item.getApprovedQuantity()
                        )
                )

                .pickedQuantity(
                        valueOrZero(
                                item.getPickedQuantity()
                        )
                )

                .packedQuantity(
                        valueOrZero(
                                item.getPackedQuantity()
                        )
                )

                .shippedQuantity(
                        valueOrZero(
                                item.getShippedQuantity()
                        )
                )

                .receivedQuantity(
                        valueOrZero(
                                item.getReceivedQuantity()
                        )
                )

                .shortageQuantity(
                        valueOrZero(
                                item.getShortageQuantity()
                        )
                )

                .surplusQuantity(
                        valueOrZero(
                                item.getSurplusQuantity()
                        )
                )

                .build();
    }


    // =====================================================
    // TRANSFER DETAIL
    // =====================================================

    public StockTransferDetailResponse toDetailResponse(
            StockTransfer transfer
    ) {

        if (transfer == null) {
            return null;
        }

        List<StockTransferItems> transferItems =
                transfer.getItems() != null
                        ? transfer.getItems()
                        : Collections.emptyList();

        List<StockTransferItemResponse> itemResponses =
                transferItems.stream()
                        .map(this::toItemResponse)
                        .toList();

        int totalRequestedQuantity =
                transferItems.stream()
                        .map(StockTransferItems::getRequestedQuantity)
                        .filter(quantity -> quantity != null)
                        .mapToInt(Integer::intValue)
                        .sum();

        Warehouses fromWarehouse =
                transfer.getFromWarehouse();

        Warehouses toWarehouse =
                transfer.getToWarehouse();

        return StockTransferDetailResponse.builder()

                .id(
                        transfer.getId()
                )

                .transferCode(
                        transfer.getTransferCode()
                )

                .status(
                        transfer.getStatus() != null
                                ? transfer.getStatus().name()
                                : null
                )

                .transferType(
                        transfer.getTransferType() != null
                                ? transfer.getTransferType().name()
                                : null
                )

                // =========================
                // SOURCE
                // =========================

                .fromWarehouseId(
                        fromWarehouse != null
                                ? fromWarehouse.getId()
                                : null
                )

                .fromWarehouseCode(
                        fromWarehouse != null
                                ? fromWarehouse.getWarehousesCode()
                                : null
                )

                .fromWarehouseName(
                        fromWarehouse != null
                                ? fromWarehouse.getName()
                                : null
                )

                .fromWarehouseType(
                        fromWarehouse != null
                                && fromWarehouse.getType() != null
                                ? fromWarehouse.getType().name()
                                : null
                )

                // =========================
                // DESTINATION
                // =========================

                .toWarehouseId(
                        toWarehouse != null
                                ? toWarehouse.getId()
                                : null
                )

                .toWarehouseCode(
                        toWarehouse != null
                                ? toWarehouse.getWarehousesCode()
                                : null
                )

                .toWarehouseName(
                        toWarehouse != null
                                ? toWarehouse.getName()
                                : null
                )

                .toWarehouseType(
                        toWarehouse != null
                                && toWarehouse.getType() != null
                                ? toWarehouse.getType().name()
                                : null
                )

                // =========================
                // BUSINESS INFORMATION
                // =========================

                .expectedShipmentDate(
                        transfer.getExpectedShipmentDate()
                )

                .expectedReceiptDate(
                        transfer.getExpectedReceiptDate()
                )

                .reasonCode(
                        transfer.getReasonCode() != null
                                ? transfer.getReasonCode().name()
                                : null
                )

                .reasonNote(
                        transfer.getReasonNote()
                )

                .notes(
                        transfer.getNotes()
                )

                // =========================
                // SUMMARY
                // =========================

                .totalProducts(
                        transferItems.size()
                )

                .totalRequestedQuantity(
                        totalRequestedQuantity
                )

                // =========================
                // CREATOR
                // =========================

                .createdById(
                        transfer.getCreatedBy() != null
                                ? transfer.getCreatedBy().getId()
                                : null
                )

                .createdByCode(
                        transfer.getCreatedBy() != null
                                ? transfer.getCreatedBy().getUserCode()
                                : null
                )

                .createdByName(
                        transfer.getCreatedBy() != null
                                ? transfer.getCreatedBy().getName()
                                : null
                )

                // =========================
                // CONFIRMATION / SUBMIT
                // =========================

                .confirmedById(
                        transfer.getConfirmedBy() != null
                                ? transfer.getConfirmedBy().getId()
                                : null
                )

                .confirmedByCode(
                        transfer.getConfirmedBy() != null
                                ? transfer.getConfirmedBy().getUserCode()
                                : null
                )

                .confirmedByName(
                        transfer.getConfirmedBy() != null
                                ? transfer.getConfirmedBy().getName()
                                : null
                )

                .confirmedAt(
                        transfer.getConfirmedAt()
                )

                .createdAt(
                        transfer.getCreatedAt()
                )

                .updatedAt(
                        transfer.getUpdatedAt()
                )

                .items(
                        itemResponses
                )

                .build();
    }


    private int valueOrZero(
            Integer value
    ) {

        return value != null
                ? value
                : 0;
    }
}