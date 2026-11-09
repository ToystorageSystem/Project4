package com.toystorage.backend.dto.request.deliveries.tracking;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateDeliveryLocationRequest {

    /*
     * Vĩ độ.
     *
     * Khoảng hợp lệ:
     * -90 -> 90
     */
    @NotNull(
            message = "Latitude is required"
    )
    @DecimalMin(
            value = "-90.0",
            message = "Latitude must be greater than or equal to -90"
    )
    @DecimalMax(
            value = "90.0",
            message = "Latitude must be less than or equal to 90"
    )
    private BigDecimal latitude;


    /*
     * Kinh độ.
     *
     * Khoảng hợp lệ:
     * -180 -> 180
     */
    @NotNull(
            message = "Longitude is required"
    )
    @DecimalMin(
            value = "-180.0",
            message = "Longitude must be greater than or equal to -180"
    )
    @DecimalMax(
            value = "180.0",
            message = "Longitude must be less than or equal to 180"
    )
    private BigDecimal longitude;


    /*
     * Sai số GPS.
     *
     * Đơn vị:
     * mét.
     *
     * Ví dụ:
     * 3.5 = sai số khoảng 3.5 mét.
     */
    @DecimalMin(
            value = "0.0",
            message = "Accuracy must be greater than or equal to 0"
    )
    private BigDecimal accuracyMeters;


    /*
     * Tốc độ hiện tại.
     *
     * Đơn vị:
     * mét / giây.
     */
    @DecimalMin(
            value = "0.0",
            message = "Speed must be greater than or equal to 0"
    )
    private BigDecimal speedMps;


    /*
     * Hướng di chuyển.
     *
     * 0   = Bắc
     * 90  = Đông
     * 180 = Nam
     * 270 = Tây
     */
    @DecimalMin(
            value = "0.0",
            message = "Heading must be greater than or equal to 0"
    )
    @DecimalMax(
            value = "360.0",
            message = "Heading must be less than or equal to 360"
    )
    private BigDecimal heading;


    /*
     * Thời điểm thiết bị thực tế lấy được GPS.
     *
     * Có thể khác thời điểm server nhận
     * trong trường hợp thiết bị mất mạng.
     */
    private LocalDateTime recordedAt;
}