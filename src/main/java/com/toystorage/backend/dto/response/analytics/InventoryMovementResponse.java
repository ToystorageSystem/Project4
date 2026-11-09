package com.toystorage.backend.dto.response.analytics;

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
public class InventoryMovementResponse {

    // =========================================================
    // WAREHOUSE
    // =========================================================

    private Long warehouseId;

    private String warehouseCode;

    private String warehouseName;

    private String warehouseType;


    // =========================================================
    // PRODUCT
    // =========================================================

    private Long productId;

    private String productCode;

    private String productName;

    private String baseUnit;


    // =========================================================
    // CURRENT INVENTORY
    // =========================================================

    /**
     * Tổng tồn vật lý hiện tại.
     */
    private Long currentQuantity;

    /**
     * Tổng số lượng đang được giữ chỗ.
     */
    private Long reservedQuantity;

    /**
     * Tồn khả dụng hiện tại.
     *
     * Được tính lại:
     *
     * currentQuantity - reservedQuantity
     */
    private Long availableQuantity;


    // =========================================================
    // INVENTORY MOVEMENT
    // =========================================================

    /**
     * Tổng lượng tăng tồn trong khoảng thời gian phân tích.
     */
    private Long totalInbound;

    /**
     * Tổng lượng giảm tồn trong khoảng thời gian phân tích.
     *
     * Trả về số dương để frontend dễ hiển thị.
     */
    private Long totalOutbound;

    /**
     * Biến động ròng:
     *
     * totalInbound - totalOutbound
     */
    private Long netMovement;

    /**
     * Tổng mức biến động tuyệt đối:
     *
     * inbound + outbound
     *
     * Dùng để đánh giá mức độ luân chuyển.
     */
    private Long totalMovement;


    // =========================================================
    // MOVEMENT INFORMATION
    // =========================================================

    /**
     * Số transaction tồn kho trong khoảng thời gian.
     */
    private Long transactionCount;

    /**
     * Thời điểm phát sinh giao dịch tồn kho gần nhất.
     */
    private LocalDateTime lastMovementAt;


    // =========================================================
    // ANALYTICS
    // =========================================================

    /**
     * Phân loại sơ bộ:
     *
     * NO_MOVEMENT
     * LOW_MOVEMENT
     * NORMAL
     * HIGH_MOVEMENT
     *
     * Không dùng policy giả lập.
     */
    private String movementStatus;

    /**
     * Nhận xét dựa trên dữ liệu transaction thực tế.
     */
    private String recommendation;
}