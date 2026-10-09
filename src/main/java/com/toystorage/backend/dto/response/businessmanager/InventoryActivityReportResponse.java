
package com.toystorage.backend.dto.response.businessmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryActivityReportResponse {

    // Thoi diem lay bao cao
    private LocalDateTime snapshotAt;

    // Kho duoc loc (null = tat ca kho)
    private Long warehouseId;

    // Tong so san pham khac nhau
    private Long totalProducts;

    // Tong so dong ton kho theo vi tri
    private Long totalBalanceRecords;

    // Tong ton vat ly
    private Long totalQuantity;

    // Tong ton giu cho
    private Long totalReservedQuantity;

    // Tong ton kha dung
    private Long totalAvailableQuantity;

    // Danh sach chi tiet
    private List<InventoryRecord> records;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryRecord {

        private Long inventoryBalanceId;

        // Thong tin kho
        private Long warehouseId;
        private String warehouseName;

        // Thong tin vi tri
        private Long locationId;
        private String locationCode;
        private String locationName;
        private String locationType;

        // Thong tin san pham
        private Long productId;
        private String productCode;
        private String productName;

        // So luong ton kho
        private Integer quantity;
        private Integer reservedQuantity;
        private Integer availableQuantity;
    }
}
