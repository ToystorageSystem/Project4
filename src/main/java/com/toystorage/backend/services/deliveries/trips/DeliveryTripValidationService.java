package com.toystorage.backend.services.deliveries.trips;

import com.toystorage.backend.entity.deliveries.Deliveries;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.deliveries
        .DeliveryRepository;

import com.toystorage.backend.repository.users
        .UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;

import org.springframework.security.core.context
        .SecurityContextHolder;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeliveryTripValidationService {

    private final DeliveryRepository
            deliveryRepository;

    private final UserRepository
            userRepository;


    // =====================================================
    // CURRENT USER
    // =====================================================

    public Users getCurrentUser() {

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
    // MY DELIVERY
    // =====================================================

    public Deliveries getMyDelivery(
            Long deliveryId,
            Users driver
    ) {

        return deliveryRepository
                .findByIdAndDriverId(
                        deliveryId,
                        driver.getId()
                )

                .orElseThrow(() ->
                        new Forbidden(
                                "Delivery is not assigned to you"
                        )
                );
    }
}