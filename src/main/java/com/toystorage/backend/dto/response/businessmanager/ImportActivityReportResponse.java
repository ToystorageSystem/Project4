
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
public class ImportActivityReportResponse {

    // Khoang thoi gian bao cao
    private LocalDate fromDate;
    private LocalDate toDate;

    // Kho duoc loc (null = tat ca kho)
    private Long warehouseId;

    // Tong so phieu nhap
    private Long totalReceipts;

    // So phieu da hoan thanh
    private Long completedReceipts;

    // Tong so luong hang da nhap thanh cong
    private Long totalAcceptedQuantity;

    // Danh sach chi tiet
    private List<ImportRecord> records;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportRecord {

        private Long id;
        private String receiptCode;

        private Long warehouseId;
        private String warehouseName;

        private String status;

        private LocalDateTime createdAt;
        private LocalDateTime receivedAt;

        private Long acceptedQuantity;
    }
}
