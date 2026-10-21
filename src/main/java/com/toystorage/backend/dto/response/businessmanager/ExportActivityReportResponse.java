
package com.toystorage.backend.dto.response.businessmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportActivityReportResponse {

    // Khoang thoi gian bao cao
    private LocalDate fromDate;
    private LocalDate toDate;

    // Kho xuat duoc loc
    private Long warehouseId;

    // Tong so phieu xuat / dieu chuyen
    private Long totalExports;

    // So phieu da co thoi diem xuat hang
    private Long shippedExports;

    // Tong so luong hang da xuat van chuyen
    private Long totalShippedQuantity;

    // Danh sach chi tiet
    private List<ExportRecord> records;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExportRecord {

        private Long id;
        private String transferCode;

        // Kho xuat
        private Long fromWarehouseId;
        private String fromWarehouseName;

        // Kho nhan
        private Long toWarehouseId;
        private String toWarehouseName;

        private String status;

        private LocalDateTime createdAt;
        private LocalDateTime shippedAt;

        // So luong da xuat van chuyen
        private Long shippedQuantity;
    }
}
