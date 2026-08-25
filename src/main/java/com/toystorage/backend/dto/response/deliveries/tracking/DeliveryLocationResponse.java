package com.toystorage.backend.dto.response.deliveries.tracking;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class DeliveryLocationResponse {

    /*
     * ID bản ghi location.
     *
     * Có thể null nếu location chỉ đang
     * được broadcast realtime mà chưa persist DB.
     */
    private Long locationId;


    // =====================================================
    // DELIVERY
    // =====================================================

    private Long deliveryId;

    private String shipmentCode;


    // =====================================================
    // DRIVER
    // =====================================================

    private Long driverId;

    private String driverName;


    // =====================================================
    // GPS
    // =====================================================

    private BigDecimal latitude;

    private BigDecimal longitude;

    private BigDecimal accuracyMeters;

    private BigDecimal speedMps;

    private BigDecimal heading;


    // =====================================================
    // TIME
    // =====================================================

    /*
     * Thời điểm thiết bị lấy GPS.
     */
    private LocalDateTime recordedAt;


    /*
     * Thời điểm server nhận GPS.
     */
    private LocalDateTime receivedAt;


    // =====================================================
    // STORAGE
    // =====================================================

    /*
     * true:
     * location này vừa được lưu xuống DB.
     *
     * false:
     * chỉ broadcast realtime / giữ latest cache.
     */
    private Boolean persisted;
}