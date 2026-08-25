package com.toystorage.backend.mapper.deliveries.tracking;

import com.toystorage.backend.dto.response.deliveries.tracking
        .DeliveryLocationResponse;

import com.toystorage.backend.dto.response.deliveries.tracking
        .DeliveryTrackingResponse;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.deliveries.DeliveryLocations;

import org.springframework.stereotype.Component;

@Component
public class DeliveryLocationMapper {


    // =====================================================
    // LOCATION RESPONSE
    // =====================================================

    /*
     * Dùng sau khi Delivery Staff gửi GPS.
     *
     * persisted = true:
     * location này đã được lưu DB.
     *
     * persisted = false:
     * location mới chỉ được giữ realtime
     * và broadcast qua WebSocket.
     */
    public DeliveryLocationResponse toLocationResponse(
            DeliveryLocations location,
            boolean persisted
    ) {

        if (location == null) {
            return null;
        }


        return DeliveryLocationResponse
                .builder()

                // =================================================
                // LOCATION
                // =================================================

                /*
                 * Nếu chưa persist DB thì ID có thể null.
                 */
                .locationId(
                        location.getId()
                )


                // =================================================
                // DELIVERY
                // =================================================

                .deliveryId(
                        location.getDelivery() != null
                                ? location
                                .getDelivery()
                                .getId()
                                : null
                )

                .shipmentCode(
                        location.getDelivery() != null
                                ? location
                                .getDelivery()
                                .getShipmentCode()
                                : null
                )


                // =================================================
                // DRIVER
                // =================================================

                .driverId(
                        location.getDriver() != null
                                ? location
                                .getDriver()
                                .getId()
                                : null
                )

                .driverName(
                        location.getDriver() != null
                                ? location
                                .getDriver()
                                .getName()
                                : null
                )


                // =================================================
                // GPS
                // =================================================

                .latitude(
                        location.getLatitude()
                )

                .longitude(
                        location.getLongitude()
                )

                .accuracyMeters(
                        location.getAccuracyMeters()
                )

                .speedMps(
                        location.getSpeedMps()
                )

                .heading(
                        location.getHeading()
                )


                // =================================================
                // TIME
                // =================================================

                .recordedAt(
                        location.getRecordedAt()
                )

                .receivedAt(
                        location.getReceivedAt()
                )


                // =================================================
                // STORAGE
                // =================================================

                .persisted(
                        persisted
                )

                .build();
    }


    // =====================================================
    // TRACKING RESPONSE
    // =====================================================

    /*
     * Dùng cho:
     *
     * GET /api/deliveries/{deliveryId}/tracking/latest
     *
     * secondsSinceLastUpdate và stale được tính
     * trong Service vì đây là business/runtime logic.
     *
     * Mapper chỉ có nhiệm vụ chuyển dữ liệu
     * sang DTO response.
     */
    public DeliveryTrackingResponse toTrackingResponse(
            Deliveries delivery,
            DeliveryLocations latestLocation,
            Long secondsSinceLastUpdate,
            boolean stale
    ) {

        if (delivery == null) {
            return null;
        }


        return DeliveryTrackingResponse
                .builder()


                // =================================================
                // DELIVERY
                // =================================================

                .deliveryId(
                        delivery.getId()
                )

                .shipmentCode(
                        delivery.getShipmentCode()
                )

                .deliveryStatus(
                        delivery.getDeliveryStatus() != null
                                ? delivery
                                .getDeliveryStatus()
                                .name()
                                : null
                )


                // =================================================
                // DRIVER
                // =================================================

                .driverId(
                        delivery.getDriver() != null
                                ? delivery
                                .getDriver()
                                .getId()
                                : null
                )

                .driverName(
                        delivery.getDriver() != null
                                ? delivery
                                .getDriver()
                                .getName()
                                : null
                )


                // =================================================
                // TRACKING STATUS
                // =================================================

                /*
                 * Không tạo thêm column sharingLocation
                 * trong Deliveries.
                 *
                 * IN_TRANSIT chính là trạng thái cho phép
                 * chia sẻ GPS.
                 */
                .sharingLocation(
                        delivery.getDeliveryStatus() != null
                                && "IN_TRANSIT".equals(
                                delivery
                                        .getDeliveryStatus()
                                        .name()
                        )
                )


                // =================================================
                // LATEST GPS
                // =================================================

                .latitude(
                        latestLocation != null
                                ? latestLocation.getLatitude()
                                : null
                )

                .longitude(
                        latestLocation != null
                                ? latestLocation.getLongitude()
                                : null
                )

                .accuracyMeters(
                        latestLocation != null
                                ? latestLocation
                                .getAccuracyMeters()
                                : null
                )

                .speedMps(
                        latestLocation != null
                                ? latestLocation
                                .getSpeedMps()
                                : null
                )

                .heading(
                        latestLocation != null
                                ? latestLocation
                                .getHeading()
                                : null
                )


                // =================================================
                // LAST UPDATE
                // =================================================

                .lastRecordedAt(
                        latestLocation != null
                                ? latestLocation
                                .getRecordedAt()
                                : null
                )

                .lastReceivedAt(
                        latestLocation != null
                                ? latestLocation
                                .getReceivedAt()
                                : null
                )

                .secondsSinceLastUpdate(
                        secondsSinceLastUpdate
                )

                .stale(
                        stale
                )

                .build();
    }
}