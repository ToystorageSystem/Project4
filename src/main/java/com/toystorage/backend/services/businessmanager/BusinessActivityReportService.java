
package com.toystorage.backend.services.businessmanager;


import com.toystorage.backend.dto.response.businessmanager.InventoryActivityReportResponse;
import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.repository.businessmanager.BusinessInventoryReportRepository;
import com.toystorage.backend.dto.response.businessmanager.TransferActivityReportResponse;
import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.dto.response.businessmanager.ImportActivityReportResponse;
import com.toystorage.backend.dto.response.businessmanager.ExportActivityReportResponse;

import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.transfers.StockTransfer;

import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.repository.businessmanager.BusinessActivityReportRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessActivityReportService {

    private final BusinessActivityReportRepository reportRepository;
    private final BusinessInventoryReportRepository inventoryRepository;

    // ==========================================
    // 1. BAO CAO NHAP HANG
    // ==========================================

    public ImportActivityReportResponse getImportReport(
            LocalDate fromDate,
            LocalDate toDate,
            Long warehouseId
    ) {

        validateFilters(fromDate, toDate, warehouseId);

        LocalDateTime fromTime = fromDate == null
                ? null
                : fromDate.atStartOfDay();

        LocalDateTime toTimeExclusive = toDate == null
                ? null
                : toDate.plusDays(1).atStartOfDay();

        List<GoodsReceipts> receipts =
                reportRepository.findImportReceipts(
                        warehouseId,
                        fromTime,
                        toTimeExclusive
                );

        List<Long> receiptIds = receipts.stream()
                .map(GoodsReceipts::getId)
                .toList();

        Map<Long, Long> acceptedByReceipt = new HashMap<>();

        if (!receiptIds.isEmpty()) {

            List<Object[]> quantities =
                    reportRepository.sumAcceptedQuantities(
                            receiptIds
                    );

            for (Object[] row : quantities) {

                Long receiptId =
                        ((Number) row[0]).longValue();

                Long quantity = row[1] == null
                        ? 0L
                        : ((Number) row[1]).longValue();

                acceptedByReceipt.put(
                        receiptId,
                        quantity
                );
            }
        }

        long completedCount = 0;
        long totalAcceptedQuantity = 0;

        List<ImportActivityReportResponse.ImportRecord>
                records = new ArrayList<>();

        for (GoodsReceipts receipt : receipts) {

            boolean completed =
                    receipt.getStatus()
                            == GoodsReceiptStatus.COMPLETED;

            long acceptedQuantity = completed
                    ? acceptedByReceipt.getOrDefault(
                            receipt.getId(), 0L
                    )
                    : 0L;

            if (completed) {
                completedCount++;
            }

            totalAcceptedQuantity += acceptedQuantity;

            records.add(
                    ImportActivityReportResponse.ImportRecord
                            .builder()
                            .id(receipt.getId())
                            .receiptCode(receipt.getReceiptCode())
                            .warehouseId(
                                    receipt.getWarehouse().getId()
                            )
                            .warehouseName(
                                    receipt.getWarehouse().getName()
                            )
                            .status(
                                    receipt.getStatus() == null
                                            ? null
                                            : receipt.getStatus().name()
                            )
                            .createdAt(receipt.getCreatedAt())
                            .receivedAt(receipt.getReceivedAt())
                            .acceptedQuantity(acceptedQuantity)
                            .build()
            );
        }

        return ImportActivityReportResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .warehouseId(warehouseId)
                .totalReceipts((long) receipts.size())
                .completedReceipts(completedCount)
                .totalAcceptedQuantity(totalAcceptedQuantity)
                .records(records)
                .build();
    }

    // ==========================================
    // 2. BAO CAO XUAT HANG
    // ==========================================

    public ExportActivityReportResponse getExportReport(
            LocalDate fromDate,
            LocalDate toDate,
            Long warehouseId
    ) {

        // 1. Kiem tra dieu kien loc
        validateFilters(fromDate, toDate, warehouseId);

        // 2. Chuyen ngay thanh khoang thoi gian
        LocalDateTime fromTime = fromDate == null
                ? null
                : fromDate.atStartOfDay();

        LocalDateTime toTimeExclusive = toDate == null
                ? null
                : toDate.plusDays(1).atStartOfDay();

        // 3. Truy van phieu xuat
        List<StockTransfer> transfers =
                reportRepository.findExportTransfers(
                        warehouseId,
                        fromTime,
                        toTimeExclusive
                );

        // 4. Lay danh sach ID phieu xuat
        List<Long> transferIds = transfers.stream()
                .map(StockTransfer::getId)
                .toList();

        Map<Long, Long> shippedByTransfer = new HashMap<>();

        // 5. Tinh so luong shipped cua tung phieu
        if (!transferIds.isEmpty()) {

            List<Object[]> quantities =
                    reportRepository.sumShippedQuantities(
                            transferIds
                    );

            for (Object[] row : quantities) {

                Long transferId =
                        ((Number) row[0]).longValue();

                Long quantity = row[1] == null
                        ? 0L
                        : ((Number) row[1]).longValue();

                shippedByTransfer.put(
                        transferId,
                        quantity
                );
            }
        }

        // 6. Tong hop bao cao
        long shippedCount = 0;
        long totalShippedQuantity = 0;

        List<ExportActivityReportResponse.ExportRecord>
                records = new ArrayList<>();

        for (StockTransfer transfer : transfers) {

            // Phieu da ghi nhan thoi diem xuat hang
            boolean shipped = transfer.getShippedAt() != null;

            long shippedQuantity = shipped
                    ? shippedByTransfer.getOrDefault(
                            transfer.getId(), 0L
                    )
                    : 0L;

            if (shipped) {
                shippedCount++;
            }

            totalShippedQuantity += shippedQuantity;

            // 7. Chuyen Entity sang DTO
            records.add(
                    ExportActivityReportResponse.ExportRecord
                            .builder()
                            .id(transfer.getId())
                            .transferCode(
                                    transfer.getTransferCode()
                            )
                            .fromWarehouseId(
                                    transfer.getFromWarehouse().getId()
                            )
                            .fromWarehouseName(
                                    transfer.getFromWarehouse().getName()
                            )
                            .toWarehouseId(
                                    transfer.getToWarehouse().getId()
                            )
                            .toWarehouseName(
                                    transfer.getToWarehouse().getName()
                            )
                            .status(
                                    transfer.getStatus() == null
                                            ? null
                                            : transfer.getStatus().name()
                            )
                            .createdAt(transfer.getCreatedAt())
                            .shippedAt(transfer.getShippedAt())
                            .shippedQuantity(shippedQuantity)
                            .build()
            );
        }

        // 8. Tra ket qua bao cao
        return ExportActivityReportResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .warehouseId(warehouseId)
                .totalExports((long) transfers.size())
                .shippedExports(shippedCount)
                .totalShippedQuantity(totalShippedQuantity)
                .records(records)
                .build();
    }

    // ==========================================
    // 3. KIEM TRA BO LOC DUNG CHUNG
    // ==========================================

    private void validateFilters(
            LocalDate fromDate,
            LocalDate toDate,
            Long warehouseId
    ) {

        if (fromDate != null
                && toDate != null
                && fromDate.isAfter(toDate)) {

            throw new BadRequest(
                    "fromDate must not be after toDate"
            );
        }

        if (warehouseId != null && warehouseId <= 0) {

            throw new BadRequest(
                    "warehouseId must be positive"
            );
        }
    }

    // ==========================================
    // 4. BAO CAO DIEU CHUYEN
    // ==========================================

    public TransferActivityReportResponse getTransferReport(
            LocalDate fromDate,
            LocalDate toDate,
            Long warehouseId
    ) {

        // 1. Kiem tra bo loc
        validateFilters(fromDate, toDate, warehouseId);

        // 2. Chuyen ngay sang LocalDateTime
        LocalDateTime fromTime = fromDate == null
                ? null
                : fromDate.atStartOfDay();

        LocalDateTime toTimeExclusive = toDate == null
                ? null
                : toDate.plusDays(1).atStartOfDay();

        // 3. Lay danh sach phieu dieu chuyen
        List<StockTransfer> transfers =
                reportRepository.findTransferActivities(
                        warehouseId,
                        fromTime,
                        toTimeExclusive
                );

        // 4. Lay danh sach ID
        List<Long> transferIds = transfers.stream()
                .map(StockTransfer::getId)
                .toList();

        // 5. Tinh so luong shipped theo tung phieu
        Map<Long, Long> shippedByTransfer = new HashMap<>();

        if (!transferIds.isEmpty()) {

            List<Object[]> quantities =
                    reportRepository.sumShippedQuantities(
                            transferIds
                    );

            for (Object[] row : quantities) {

                Long transferId =
                        ((Number) row[0]).longValue();

                Long quantity = row[1] == null
                        ? 0L
                        : ((Number) row[1]).longValue();

                shippedByTransfer.put(
                        transferId,
                        quantity
                );
            }
        }

        // 6. Khoi tao so lieu tong hop
        long completedCount = 0;
        long totalShippedQuantity = 0;

        Map<String, Long> transfersByStatus =
                new HashMap<>();

        List<TransferActivityReportResponse.TransferRecord>
                records = new ArrayList<>();

        // 7. Duyet danh sach phieu dieu chuyen
        for (StockTransfer transfer : transfers) {

            String status = transfer.getStatus() == null
                    ? "UNKNOWN"
                    : transfer.getStatus().name();

            // Dem so phieu theo tung trang thai
            transfersByStatus.merge(
                    status,
                    1L,
                    Long::sum
            );

            // Dem phieu COMPLETED
            if (transfer.getStatus()
                    == TransferStatus.COMPLETED) {

                completedCount++;
            }

            // Chi tinh luong shipped khi da xuat hang
            boolean shipped =
                    transfer.getShippedAt() != null;

            long shippedQuantity = shipped
                    ? shippedByTransfer.getOrDefault(
                            transfer.getId(),
                            0L
                    )
                    : 0L;

            totalShippedQuantity += shippedQuantity;

            // 8. Chuyen Entity sang DTO
            records.add(
                    TransferActivityReportResponse.TransferRecord
                            .builder()
                            .id(transfer.getId())
                            .transferCode(
                                    transfer.getTransferCode()
                            )
                            .fromWarehouseId(
                                    transfer.getFromWarehouse().getId()
                            )
                            .fromWarehouseName(
                                    transfer.getFromWarehouse().getName()
                            )
                            .toWarehouseId(
                                    transfer.getToWarehouse().getId()
                            )
                            .toWarehouseName(
                                    transfer.getToWarehouse().getName()
                            )
                            .status(status)
                            .createdAt(transfer.getCreatedAt())
                            .shippedAt(transfer.getShippedAt())
                            .shippedQuantity(shippedQuantity)
                            .build()
            );
        }

        // 9. Tra ket qua bao cao
        return TransferActivityReportResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .warehouseId(warehouseId)
                .totalTransfers((long) transfers.size())
                .completedTransfers(completedCount)
                .totalShippedQuantity(totalShippedQuantity)
                .transfersByStatus(transfersByStatus)
                .records(records)
                .build();
    }

    // ==========================================
    // 5. BAO CAO TON KHO HIEN TAI
    // ==========================================

    public InventoryActivityReportResponse getInventoryReport(
            Long warehouseId
    ) {

        if (warehouseId != null && warehouseId <= 0) {
            throw new BadRequest(
                    "warehouseId must be positive"
            );
        }

        List<InventoryBalances> balances =
                inventoryRepository.findInventoryReport(warehouseId);

        long totalQuantity = 0;
        long totalReservedQuantity = 0;
        long totalAvailableQuantity = 0;

        List<InventoryActivityReportResponse.InventoryRecord>
                records = new ArrayList<>();

        for (InventoryBalances balance : balances) {

            int quantity = balance.getQuantity() == null
                    ? 0 : balance.getQuantity();

            int reservedQuantity =
                    balance.getReservedQuantity() == null
                            ? 0 : balance.getReservedQuantity();

            int availableQuantity =
                    balance.getAvailableQuantity() == null
                            ? 0 : balance.getAvailableQuantity();

            totalQuantity += quantity;
            totalReservedQuantity += reservedQuantity;
            totalAvailableQuantity += availableQuantity;

            records.add(
                    InventoryActivityReportResponse.InventoryRecord
                            .builder()
                            .inventoryBalanceId(balance.getId())

                            .warehouseId(
                                    balance.getWarehouse().getId()
                            )
                            .warehouseName(
                                    balance.getWarehouse().getName()
                            )

                            .locationId(
                                    balance.getLocation().getId()
                            )
                            .locationCode(
                                    balance.getLocation()
                                            .getWarehouseLocationsCode()
                            )
                            .locationName(
                                    balance.getLocation().getName()
                            )
                            .locationType(
                                    balance.getLocation().getLocationType() == null
                                            ? null
                                            : balance.getLocation()
                                                    .getLocationType().name()
                            )

                            .productId(
                                    balance.getProduct().getId()
                            )
                            .productCode(
                                    balance.getProduct().getProductsCode()
                            )
                            .productName(
                                    balance.getProduct().getName()
                            )

                            .quantity(quantity)
                            .reservedQuantity(reservedQuantity)
                            .availableQuantity(availableQuantity)
                            .build()
            );
        }

        long totalProducts = balances.stream()
                .map(balance -> balance.getProduct().getId())
                .distinct()
                .count();

        return InventoryActivityReportResponse.builder()
                .snapshotAt(LocalDateTime.now())
                .warehouseId(warehouseId)
                .totalProducts(totalProducts)
                .totalBalanceRecords((long) balances.size())
                .totalQuantity(totalQuantity)
                .totalReservedQuantity(totalReservedQuantity)
                .totalAvailableQuantity(totalAvailableQuantity)
                .records(records)
                .build();
    }


}
