
package com.toystorage.backend.dto.response.businessmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferActivityReportResponse {

    // Khoang thoi gian loc bao cao
    private LocalDate fromDate;
    private LocalDate toDate;

    // Kho gui hoac kho nhan
    private Long warehouseId;

    // Tong so phieu dieu chuyen
    private Long totalTransfers;

    // So phieu dieu chuyen da hoan thanh
    private Long completedTransfers;

    // So luong hang da xuat van chuyen
    private Long totalShippedQuantity;

    // Thong ke so phieu theo trang thai
    private Map<String, Long> transfersByStatus;

    // Danh sach phieu dieu chuyen
    private List<TransferRecord> records;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferRecord {

        private Long id;
        private String transferCode;

        // Kho gui hang
        private Long fromWarehouseId;
        private String fromWarehouseName;

        // Kho nhan hang
        private Long toWarehouseId;
        private String toWarehouseName;

        // Trang thai dieu chuyen
        private String status;

        // Thoi gian
        private LocalDateTime createdAt;
        private LocalDateTime shippedAt;

        // So luong da xuat van chuyen
        private Long shippedQuantity;
    }
}
