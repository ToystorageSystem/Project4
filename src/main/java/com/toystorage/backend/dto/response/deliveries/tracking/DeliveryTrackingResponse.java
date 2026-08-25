package com.toystorage.backend.dto.response.deliveries.tracking;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class DeliveryTrackingResponse {

    // =====================================================
    // DELIVERY
    // =====================================================

    private Long deliveryId;

    private String shipmentCode;

    private String deliveryStatus;


    // =====================================================
    // DRIVER
    // =====================================================

    private Long driverId;

    private String driverName;


    // =====================================================
    // TRACKING STATUS
    // =====================================================

    /*
     * true khi chuyến đang IN_TRANSIT.
     */
    private Boolean sharingLocation;


    // =====================================================
    // LATEST GPS
    // =====================================================

    private BigDecimal latitude;

    private BigDecimal longitude;

    private BigDecimal accuracyMeters;

    private BigDecimal speedMps;

    private BigDecimal heading;


    // =====================================================
    // LAST UPDATE
    // =====================================================

    /*
     * Thời điểm GPS được lấy trên thiết bị.
     */
    private LocalDateTime lastRecordedAt;


    /*
     * Thời điểm backend nhận được GPS.
     */
    private LocalDateTime lastReceivedAt;


    /*
     * Đã bao nhiêu giây kể từ lần server
     * nhận GPS gần nhất.
     */
    private Long secondsSinceLastUpdate;


    /*
     * true nếu lâu hơn ngưỡng cho phép
     * mà không nhận vị trí mới.
     *
     * Frontend có thể hiển thị:
     *
     * "Mất kết nối - cập nhật lần cuối 15 giây trước"
     */
    private Boolean stale;
}