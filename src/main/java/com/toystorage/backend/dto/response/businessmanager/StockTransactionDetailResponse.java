
package com.toystorage.backend.dto.response.businessmanager;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockTransactionDetailResponse {

    // Thong tin chung cua phieu
    private Long id;
    private String type;
    private String transactionCode;
    private String status;

    // Kho thuc hien
    private Long warehouseId;
    private String warehouseName;

    // Kho dich (doi voi phieu xuat)
    private Long destinationWarehouseId;
    private String destinationWarehouseName;

    // Nguoi thuc hien
    private Long performedById;
    private String performedByName;

    // Thoi gian tao phieu
    private LocalDateTime createdAt;

    // Danh sach san pham trong phieu
    @Builder.Default
    private List<StockTransactionItemResponse> items = List.of();
}
