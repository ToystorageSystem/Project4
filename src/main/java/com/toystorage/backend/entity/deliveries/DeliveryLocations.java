package com.toystorage.backend.entity.deliveries;

import com.toystorage.backend.entity.users.Users;

import jakarta.persistence.*;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "delivery_locations",
        indexes = {

                @Index(
                        name = "idx_delivery_location_delivery_recorded",
                        columnList = "shipment_id, recorded_at"
                ),

                @Index(
                        name = "idx_delivery_location_driver",
                        columnList = "driver_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryLocations {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =====================================================
    // DELIVERY
    // =====================================================

    /*
     * Chuyến giao hàng được ghi nhận vị trí.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "shipment_id",
            nullable = false
    )
    private Deliveries delivery;


    // =====================================================
    // DRIVER
    // =====================================================

    /*
     * Delivery Staff thực tế gửi vị trí này.
     *
     * Lưu riêng driver giúp audit chính xác
     * ngay cả khi delivery.driver thay đổi sau này.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "driver_id",
            nullable = false
    )
    private Users driver;


    // =====================================================
    // GPS
    // =====================================================

    /*
     * Vĩ độ.
     *
     * scale = 8 giữ được dữ liệu tọa độ
     * khá chi tiết từ thiết bị.
     */
    @Column(
            name = "latitude",
            nullable = false,
            precision = 11,
            scale = 8
    )
    private BigDecimal latitude;


    /*
     * Kinh độ.
     */
    @Column(
            name = "longitude",
            nullable = false,
            precision = 12,
            scale = 8
    )
    private BigDecimal longitude;


    /*
     * Sai số GPS.
     *
     * Đơn vị: mét.
     *
     * Ví dụ:
     * 3.500 = sai số khoảng 3.5 mét.
     */
    @Column(
            name = "accuracy_meters",
            precision = 10,
            scale = 3
    )
    private BigDecimal accuracyMeters;


    /*
     * Tốc độ do thiết bị cung cấp.
     *
     * Đơn vị:
     * mét / giây.
     */
    @Column(
            name = "speed_mps",
            precision = 10,
            scale = 3
    )
    private BigDecimal speedMps;


    /*
     * Hướng di chuyển.
     *
     * 0 - 360 độ.
     */
    @Column(
            name = "heading",
            precision = 6,
            scale = 2
    )
    private BigDecimal heading;


    // =====================================================
    // TIME
    // =====================================================

    /*
     * Thời điểm thiết bị lấy được vị trí.
     *
     * Có thể cũ hơn receivedAt nếu thiết bị
     * mất mạng rồi gửi lại sau.
     */
    @Column(
            name = "recorded_at",
            nullable = false
    )
    private LocalDateTime recordedAt;


    /*
     * Thời điểm server thực tế nhận GPS.
     */
    @Column(
            name = "received_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime receivedAt;


    // =====================================================
    // CODE
    // =====================================================

    /*
     * Mã nội bộ duy nhất của bản ghi GPS.
     */
    @Column(
            name = "shipment_locations_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String shipmentLocationsCode;


    // =====================================================
    // CREATE
    // =====================================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();


        if (recordedAt == null) {
            recordedAt = now;
        }


        if (receivedAt == null) {
            receivedAt = now;
        }
    }
}