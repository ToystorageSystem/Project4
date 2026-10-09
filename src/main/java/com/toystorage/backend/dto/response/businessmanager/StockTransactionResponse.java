
package com.toystorage.backend.dto.response.businessmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransactionResponse {

    private Long id;

    // IMPORT: Phieu nhap
    // EXPORT: Phieu xuat
    private String type;

    // Ma phieu nhap hoac phieu xuat
    private String transactionCode;

    // Trang thai hien tai
    private String status;

    // Kho thuc hien nghiep vu
    private Long warehouseId;
    private String warehouseName;

    // Kho dich (neu la phieu dieu chuyen)
    private Long destinationWarehouseId;
    private String destinationWarehouseName;

    // Nguoi thuc hien
    private Long performedById;
    private String performedByName;

    // Thoi gian tao phieu
    private LocalDateTime createdAt;
}
