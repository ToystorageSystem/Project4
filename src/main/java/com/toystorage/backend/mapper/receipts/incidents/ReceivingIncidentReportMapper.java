package com.toystorage.backend.mapper.receipts.incidents;

import com.toystorage.backend.dto.response.receipts.incidents.ReceivingIncidentReportItemResponse;
import com.toystorage.backend.dto.response.receipts.incidents.ReceivingIncidentReportResponse;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.receipts.ReceivingIncidentReportItems;
import com.toystorage.backend.entity.receipts.ReceivingIncidentReports;
import com.toystorage.backend.entity.users.Users;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReceivingIncidentReportMapper {

    public ReceivingIncidentReportResponse toResponse(ReceivingIncidentReports report) {
        Users staff = report.getWarehouseStaff();
        Users manager = report.getWarehouseManager();

        List<ReceivingIncidentReportItemResponse> items =
                report.getItems() == null
                        ? List.of()
                        : report.getItems().stream()
                                .map(this::toItemResponse)
                                .toList();

        return ReceivingIncidentReportResponse.builder()
                .id(report.getId())
                .reportCode(report.getReportCode())
                .goodsReceiptId(report.getGoodsReceipt().getId())
                .receiptCode(report.getGoodsReceipt().getReceiptCode())
                .warehouseId(report.getWarehouse().getId())
                .warehouseCode(report.getWarehouse().getWarehousesCode())
                .warehouseName(report.getWarehouse().getName())
                .warehouseStaffId(staff != null ? staff.getId() : null)
                .warehouseStaffCode(staff != null ? staff.getUserCode() : null)
                .warehouseStaffName(staff != null ? staff.getName() : null)
                .warehouseManagerId(manager != null ? manager.getId() : null)
                .warehouseManagerCode(manager != null ? manager.getUserCode() : null)
                .warehouseManagerName(manager != null ? manager.getName() : null)
                .sourceDecision(report.getSourceDecision().name())
                .penaltyAction(report.getPenaltyAction())
                .managerNote(report.getManagerNote())
                .totalIssueProducts(items.size())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .items(items)
                .build();
    }

    public ReceivingIncidentReportItemResponse toItemResponse(
            ReceivingIncidentReportItems item
    ) {
        Products product = item.getProduct();

        return ReceivingIncidentReportItemResponse.builder()
                .id(item.getId())
                .discrepancyReportId(item.getDiscrepancyReport().getId())
                .productId(product.getId())
                .productCode(product.getProductsCode())
                .productName(product.getName())
                .barcode(product.getBarcode())
                .discrepancyType(item.getDiscrepancyType().name())
                .expectedQuantity(item.getExpectedQuantity())
                .actualQuantity(item.getActualQuantity())
                .acceptedQuantity(item.getAcceptedQuantity())
                .damagedQuantity(item.getDamagedQuantity())
                .shortageQuantity(item.getShortageQuantity())
                .surplusQuantity(item.getSurplusQuantity())
                .reason(item.getReason())
                .build();
    }
}
