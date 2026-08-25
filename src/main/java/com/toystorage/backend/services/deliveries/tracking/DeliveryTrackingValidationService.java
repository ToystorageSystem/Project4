package com.toystorage.backend.services.deliveries.tracking;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.deliveries.DeliveryStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.deliveries.DeliveryRepository;
import com.toystorage.backend.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeliveryTrackingValidationService {

    private final DeliveryRepository
            deliveryRepository;

    private final UserRepository
            userRepository;


    // =====================================================
    // CURRENT USER
    // =====================================================

    public Users getCurrentUser() {

        Authentication authentication =
                getAuthentication();


        return userRepository
                .findByEmail(
                        authentication.getName()
                )

                .orElseThrow(() ->
                        new NotFound(
                                "Authenticated user not found"
                        )
                );
    }


    // =====================================================
    // GET DELIVERY
    // =====================================================

    public Deliveries getDelivery(
            Long deliveryId
    ) {

        return deliveryRepository
                .findById(
                        deliveryId
                )

                .orElseThrow(() ->
                        new NotFound(
                                "Delivery not found: "
                                        + deliveryId
                        )
                );
    }


    // =====================================================
    // VALIDATE DRIVER
    // =====================================================

    /*
     * Chỉ đúng Delivery Staff được assign
     * mới được gửi GPS cho chuyến.
     */
    public void validateDriver(
            Deliveries delivery,
            Users driver
    ) {

        if (delivery.getDriver() == null) {

            throw new Forbidden(
                    "Delivery does not have an assigned driver"
            );
        }


        if (!delivery
                .getDriver()
                .getId()
                .equals(
                        driver.getId()
                )) {

            throw new Forbidden(
                    "This delivery is not assigned to you"
            );
        }
    }


    // =====================================================
    // VALIDATE TRACKING STATUS
    // =====================================================

    /*
     * Chỉ IN_TRANSIT mới được ghi nhận GPS.
     *
     * ACCEPTED chưa phải đang vận chuyển.
     */
    public void validateCanShareLocation(
            Deliveries delivery
    ) {

        if (delivery.getDeliveryStatus()
                != DeliveryStatus.IN_TRANSIT) {

            throw new BadRequest(
                    "Location can only be shared "
                            + "while delivery is IN_TRANSIT"
            );
        }
    }


    // =====================================================
    // VALIDATE VIEW PERMISSION
    // =====================================================

    /*
     * Được xem nếu:
     *
     * 1. Chính driver của chuyến.
     * 2. User thuộc warehouse/store nguồn.
     * 3. User thuộc warehouse/store đích.
     * 4. Business có quyền theo dõi.
     */
    public void validateCanView(
            Deliveries delivery,
            Users user
    ) {

        // =================================================
        // DRIVER
        // =================================================

        if (delivery.getDriver() != null
                && delivery
                .getDriver()
                .getId()
                .equals(
                        user.getId()
                )) {

            return;
        }


        // =================================================
        // FROM / TO WAREHOUSE
        // =================================================

        if (user.getWarehouse() != null) {

            Long warehouseId =
                    user
                            .getWarehouse()
                            .getId();


            if (delivery.getFromWarehouse() != null
                    && delivery
                    .getFromWarehouse()
                    .getId()
                    .equals(
                            warehouseId
                    )) {

                return;
            }


            if (delivery.getToWarehouse() != null
                    && delivery
                    .getToWarehouse()
                    .getId()
                    .equals(
                            warehouseId
                    )) {

                return;
            }
        }


        // =================================================
        // BUSINESS
        // =================================================

        Authentication authentication =
                getAuthentication();


        boolean business =
                authentication
                        .getAuthorities()
                        .stream()

                        .map(
                                GrantedAuthority::getAuthority
                        )

                        .anyMatch(authority ->
                                "ROLE_BUSINESS".equals(
                                        authority
                                )
                                        || "BUSINESS".equals(
                                        authority
                                )
                        );


        if (business) {
            return;
        }


        throw new Forbidden(
                "You do not have permission "
                        + "to track this delivery"
        );
    }


    // =====================================================
    // AUTHENTICATION
    // =====================================================

    private Authentication getAuthentication() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new Unauthorized(
                    "User is not authenticated"
            );
        }


        return authentication;
    }
}