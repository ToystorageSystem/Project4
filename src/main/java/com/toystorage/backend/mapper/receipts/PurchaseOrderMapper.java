package com.toystorage.backend.mapper.receipts;

import com.toystorage.backend.dto.response.receipts.PurchaseOrderDetailResponse;
import com.toystorage.backend.dto.response.receipts.PurchaseOrderItemResponse;
import com.toystorage.backend.dto.response.receipts.PurchaseOrderOptionResponse;
import com.toystorage.backend.dto.response.receipts.PurchaseOrderProductOptionResponse;
import com.toystorage.backend.dto.response.receipts.PurchaseOrderSummaryResponse;
import com.toystorage.backend.entity.receipts.PurchaseOrderItems;
import com.toystorage.backend.entity.receipts.PurchaseOrders;
import com.toystorage.backend.entity.suppliers.SupplierProducts;
import com.toystorage.backend.entity.suppliers.Suppliers;
import com.toystorage.backend.entity.warehouses.Warehouses;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
public class PurchaseOrderMapper {

    // =====================================================
    // SUPPLIER OPTION
    // =====================================================

    public PurchaseOrderOptionResponse toSupplierOption(
            Suppliers supplier
    ) {

        if (supplier == null) {
            return null;
        }

        return PurchaseOrderOptionResponse.builder()
                .id(supplier.getId())
                .code(supplier.getSuppliersCode())
                .name(supplier.getName())
                .build();
    }


    // =====================================================
    // WAREHOUSE OPTION
    // =====================================================

    public PurchaseOrderOptionResponse toWarehouseOption(
            Warehouses warehouse
    ) {

        if (warehouse == null) {
            return null;
        }

        return PurchaseOrderOptionResponse.builder()
                .id(warehouse.getId())
                .code(warehouse.getWarehousesCode())
                .name(warehouse.getName())
                .build();
    }


    // =====================================================
    // PRODUCT OPTION OF SUPPLIER
    // =====================================================

    public PurchaseOrderProductOptionResponse toProductOption(
            SupplierProducts supplierProduct
    ) {

        if (supplierProduct == null) {
            return null;
        }

        return PurchaseOrderProductOptionResponse.builder()

                .supplierProductLinkId(
                        supplierProduct.getId()
                )

                .productId(
                        supplierProduct.getProduct() != null
                                ? supplierProduct.getProduct().getId()
                                : null
                )

                .productCode(
                        supplierProduct.getProduct() != null
                                ? supplierProduct
                                .getProduct()
                                .getProductsCode()
                                : null
                )

                .productName(
                        supplierProduct.getProduct() != null
                                ? supplierProduct
                                .getProduct()
                                .getName()
                                : null
                )

                .baseUnit(
                        supplierProduct.getProduct() != null
                                ? supplierProduct
                                .getProduct()
                                .getBaseUnit()
                                : null
                )

                .supplierProductCode(
                        supplierProduct.getSupplierProductCode()
                )

                .purchasePrice(
                        supplierProduct.getPurchasePrice()
                )

                .leadTimeDays(
                        supplierProduct.getLeadTimeDays()
                )

                .minimumOrderQuantity(
                        supplierProduct.getMinimumOrderQuantity()
                )

                .defaultSupplier(
                        Boolean.TRUE.equals(
                                supplierProduct.getIsDefault()
                        )
                )

                .build();
    }


    // =====================================================
    // PURCHASE ORDER ITEM
    // =====================================================

    public PurchaseOrderItemResponse toItemResponse(
            PurchaseOrderItems item
    ) {

        if (item == null) {
            return null;
        }

        BigDecimal lineTotal =
                calculateLineTotal(item);

        return PurchaseOrderItemResponse.builder()

                .id(item.getId())

                .itemCode(
                        item.getPurchaseOrderItemsCode()
                )

                .productId(
                        item.getProduct() != null
                                ? item.getProduct().getId()
                                : null
                )

                .productCode(
                        item.getProduct() != null
                                ? item.getProduct().getProductsCode()
                                : null
                )

                .productName(
                        item.getProduct() != null
                                ? item.getProduct().getName()
                                : null
                )

                .baseUnit(
                        item.getProduct() != null
                                ? item.getProduct().getBaseUnit()
                                : null
                )

                .orderedQuantity(
                        item.getOrderedQuantity()
                )

                .receivedQuantity(
                        item.getReceivedQuantity()
                )

                .unitPrice(
                        item.getUnitPrice()
                )

                .lineTotal(
                        lineTotal
                )

                .build();
    }


    // =====================================================
    // PURCHASE ORDER SUMMARY
    // =====================================================

    public PurchaseOrderSummaryResponse toSummaryResponse(
            PurchaseOrders order,
            List<PurchaseOrderItems> items
    ) {

        if (order == null) {
            return null;
        }

        List<PurchaseOrderItems> safeItems =
                items != null
                        ? items
                        : Collections.emptyList();

        return PurchaseOrderSummaryResponse.builder()

                .id(order.getId())

                .orderCode(
                        order.getOrderCode()
                )

                .supplierId(
                        order.getSupplier() != null
                                ? order.getSupplier().getId()
                                : null
                )

                .supplierCode(
                        order.getSupplier() != null
                                ? order.getSupplier().getSuppliersCode()
                                : null
                )

                .supplierName(
                        order.getSupplier() != null
                                ? order.getSupplier().getName()
                                : null
                )

                .warehouseId(
                        order.getWarehouse() != null
                                ? order.getWarehouse().getId()
                                : null
                )

                .warehouseCode(
                        order.getWarehouse() != null
                                ? order.getWarehouse().getWarehousesCode()
                                : null
                )

                .warehouseName(
                        order.getWarehouse() != null
                                ? order.getWarehouse().getName()
                                : null
                )

                .status(
                        order.getStatus() != null
                                ? order.getStatus().name()
                                : null
                )

                .expectedDeliveryDate(
                        order.getExpectedDeliveryDate()
                )

                .totalProducts(
                        safeItems.size()
                )

                .totalOrderedQuantity(
                        calculateTotalQuantity(
                                safeItems
                        )
                )

                .totalAmount(
                        calculateTotalAmount(
                                safeItems
                        )
                )

                .createdById(
                        order.getCreatedBy() != null
                                ? order.getCreatedBy().getId()
                                : null
                )

                .createdByCode(
                        order.getCreatedBy() != null
                                ? order.getCreatedBy().getUserCode()
                                : null
                )

                .createdByName(
                        order.getCreatedBy() != null
                                ? order.getCreatedBy().getName()
                                : null
                )

                .approvedById(
                        order.getApprovedBy() != null
                                ? order.getApprovedBy().getId()
                                : null
                )

                .approvedByCode(
                        order.getApprovedBy() != null
                                ? order.getApprovedBy().getUserCode()
                                : null
                )

                .approvedByName(
                        order.getApprovedBy() != null
                                ? order.getApprovedBy().getName()
                                : null
                )

                .createdAt(
                        order.getCreatedAt()
                )

                .updatedAt(
                        order.getUpdatedAt()
                )

                .build();
    }


    // =====================================================
    // PURCHASE ORDER DETAIL
    // =====================================================

    public PurchaseOrderDetailResponse toDetailResponse(
            PurchaseOrders order,
            List<PurchaseOrderItems> items
    ) {

        if (order == null) {
            return null;
        }

        List<PurchaseOrderItems> safeItems =
                items != null
                        ? items
                        : Collections.emptyList();

        List<PurchaseOrderItemResponse> itemResponses =
                safeItems
                        .stream()
                        .map(this::toItemResponse)
                        .toList();

        return PurchaseOrderDetailResponse.builder()

                .id(order.getId())

                .orderCode(
                        order.getOrderCode()
                )

                .supplierId(
                        order.getSupplier() != null
                                ? order.getSupplier().getId()
                                : null
                )

                .supplierCode(
                        order.getSupplier() != null
                                ? order.getSupplier().getSuppliersCode()
                                : null
                )

                .supplierName(
                        order.getSupplier() != null
                                ? order.getSupplier().getName()
                                : null
                )

                .warehouseId(
                        order.getWarehouse() != null
                                ? order.getWarehouse().getId()
                                : null
                )

                .warehouseCode(
                        order.getWarehouse() != null
                                ? order.getWarehouse().getWarehousesCode()
                                : null
                )

                .warehouseName(
                        order.getWarehouse() != null
                                ? order.getWarehouse().getName()
                                : null
                )

                .status(
                        order.getStatus() != null
                                ? order.getStatus().name()
                                : null
                )

                .expectedDeliveryDate(
                        order.getExpectedDeliveryDate()
                )

                .note(
                        order.getNote()
                )

                .rejectionReason(
                        order.getRejectionReason()
                )

                .cancelReason(
                        order.getCancelReason()
                )

                .totalProducts(
                        safeItems.size()
                )

                .totalOrderedQuantity(
                        calculateTotalQuantity(
                                safeItems
                        )
                )

                .totalAmount(
                        calculateTotalAmount(
                                safeItems
                        )
                )

                .createdById(
                        order.getCreatedBy() != null
                                ? order.getCreatedBy().getId()
                                : null
                )

                .createdByCode(
                        order.getCreatedBy() != null
                                ? order.getCreatedBy().getUserCode()
                                : null
                )

                .createdByName(
                        order.getCreatedBy() != null
                                ? order.getCreatedBy().getName()
                                : null
                )

                .approvedById(
                        order.getApprovedBy() != null
                                ? order.getApprovedBy().getId()
                                : null
                )

                .approvedByCode(
                        order.getApprovedBy() != null
                                ? order.getApprovedBy().getUserCode()
                                : null
                )

                .approvedByName(
                        order.getApprovedBy() != null
                                ? order.getApprovedBy().getName()
                                : null
                )

                .approvedAt(
                        order.getApprovedAt()
                )

                .createdAt(
                        order.getCreatedAt()
                )

                .updatedAt(
                        order.getUpdatedAt()
                )

                .items(
                        itemResponses
                )

                .build();
    }


    // =====================================================
    // CALCULATE LINE TOTAL
    // =====================================================

    private BigDecimal calculateLineTotal(
            PurchaseOrderItems item
    ) {

        if (
                item.getOrderedQuantity() == null
                        || item.getUnitPrice() == null
        ) {
            return BigDecimal.ZERO;
        }

        return item
                .getUnitPrice()
                .multiply(
                        BigDecimal.valueOf(
                                item.getOrderedQuantity()
                        )
                );
    }


    // =====================================================
    // CALCULATE TOTAL QUANTITY
    // =====================================================

    private Integer calculateTotalQuantity(
            List<PurchaseOrderItems> items
    ) {

        return items
                .stream()
                .map(PurchaseOrderItems::getOrderedQuantity)
                .filter(quantity -> quantity != null)
                .mapToInt(Integer::intValue)
                .sum();
    }


    // =====================================================
    // CALCULATE TOTAL AMOUNT
    // =====================================================

    private BigDecimal calculateTotalAmount(
            List<PurchaseOrderItems> items
    ) {

        return items
                .stream()
                .map(this::calculateLineTotal)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }
}