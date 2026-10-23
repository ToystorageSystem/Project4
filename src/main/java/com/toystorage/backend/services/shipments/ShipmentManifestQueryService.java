package com.toystorage.backend.services.shipments;

import com.toystorage.backend.dto.response.shipments.ShipmentManifestDetailResponse;
import com.toystorage.backend.dto.response.shipments.ShipmentManifestListItemResponse;
import com.toystorage.backend.dto.response.shipments.ShipmentManifestPageResponse;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.shipments.ShipmentManifestPackage;
import com.toystorage.backend.entity.shipments.ShipmentManifestTransfer;
import com.toystorage.backend.entity.shipments.ShipmentManifests;

import com.toystorage.backend.enums.deliveries.DeliveryStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.mapper.shipments.ShipmentManifestMapper;

import com.toystorage.backend.repository.deliveries.DeliveryRepository;
import com.toystorage.backend.repository.shipments.ShipmentManifestPackageRepository;
import com.toystorage.backend.repository.shipments.ShipmentManifestRepository;
import com.toystorage.backend.repository.shipments.ShipmentManifestTransferRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ShipmentManifestQueryService {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final int MAX_PAGE_SIZE = 100;


    private final ShipmentManifestRepository
            shipmentManifestRepository;

    private final ShipmentManifestTransferRepository
            shipmentManifestTransferRepository;

    private final ShipmentManifestPackageRepository
            shipmentManifestPackageRepository;

    private final DeliveryRepository
            deliveryRepository;

    private final ShipmentManifestMapper
            mapper;


    // =====================================================
    // MANIFEST LIST
    // =====================================================

    @Transactional(readOnly = true)
    public ShipmentManifestPageResponse getShipmentManifests(
            String keyword,
            Long fromWarehouseId,
            Long toWarehouseId,
            String transportStatus,
            LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer size
    ) {

        String normalizedKeyword =
                normalizeKeyword(
                        keyword
                );


        DeliveryStatus normalizedTransportStatus =
                normalizeTransportStatus(
                        transportStatus
                );


        validateDateRange(
                fromDate,
                toDate
        );


        LocalDateTime createdFrom =
                fromDate != null
                        ? fromDate.atStartOfDay()
                        : null;


        /*
         * Dùng mốc exclusive.
         *
         * Ví dụ:
         * toDate = 2026-08-24
         *
         * query:
         * createdAt < 2026-08-25 00:00
         *
         * như vậy toàn bộ ngày 24/08 đều được tính.
         */
        LocalDateTime createdToExclusive =
                toDate != null
                        ? toDate
                                .plusDays(1)
                                .atStartOfDay()
                        : null;


        int safePage =
                normalizePage(
                        page
                );


        int safeSize =
                normalizeSize(
                        size
                );


        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize
                );


        Page<ShipmentManifests> result =
                shipmentManifestRepository
                        .searchShipmentManifests(
                                normalizedKeyword,
                                fromWarehouseId,
                                toWarehouseId,
                                normalizedTransportStatus,
                                createdFrom,
                                createdToExclusive,
                                pageable
                        );


        List<ShipmentManifestListItemResponse> items =
                result
                        .getContent()
                        .stream()
                        .map(
                                this::toListItem
                        )
                        .toList();


        return ShipmentManifestPageResponse
                .builder()

                .items(
                        items
                )

                .page(
                        result.getNumber()
                )

                .size(
                        result.getSize()
                )

                .totalElements(
                        result.getTotalElements()
                )

                .totalPages(
                        result.getTotalPages()
                )

                .first(
                        result.isFirst()
                )

                .last(
                        result.isLast()
                )

                .build();
    }


    // =====================================================
    // MANIFEST DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public ShipmentManifestDetailResponse getShipmentManifestDetail(
            Long manifestId
    ) {

        if (manifestId == null) {

            throw new BadRequest(
                    "Manifest id is required"
            );
        }


        ShipmentManifests manifest =
                shipmentManifestRepository
                        .findById(
                                manifestId
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Shipment manifest not found with id: "
                                                + manifestId
                                )
                        );


        /*
         * Delivery mới nhất của bảng kê.
         *
         * Manifest có thể chưa được đưa vào một chuyến giao,
         * vì vậy delivery có thể null.
         */
        Deliveries delivery =
                deliveryRepository
                        .findFirstByManifest_IdOrderByCreatedAtDesc(
                                manifestId
                        )
                        .orElse(
                                null
                        );


        /*
         * Danh sách phiếu điều chuyển.
         */
        List<ShipmentManifestTransfer> transferLinks =
                    shipmentManifestTransferRepository
                            .findByManifest_Id(
                                    manifestId
                            );


        /*
         * Danh sách kiện hàng.
         */
        List<ShipmentManifestPackage> packageLinks =
                shipmentManifestPackageRepository
                        .findByManifestId(
                                manifestId
                        );


        return mapper.toDetailResponse(
                manifest,
                delivery,
                transferLinks,
                packageLinks
        );
    }


    // =====================================================
    // LIST ITEM MAPPING
    // =====================================================

    private ShipmentManifestListItemResponse toListItem(
            ShipmentManifests manifest
    ) {

        Long manifestId =
                manifest.getId();


        /*
         * Lấy delivery mới nhất để hiển thị
         * trạng thái vận chuyển hiện tại.
         */
        Deliveries delivery =
                deliveryRepository
                        .findFirstByManifest_IdOrderByCreatedAtDesc(
                                manifestId
                        )
                        .orElse(
                                null
                        );


        long totalTransfers =
                shipmentManifestTransferRepository
                        .countByManifest_Id(
                                manifestId
                        );


        long totalPackages =
                shipmentManifestPackageRepository
                        .countByManifest_Id(
                                manifestId
                        );


        return mapper.toListItemResponse(
                manifest,
                delivery,
                totalTransfers,
                totalPackages
        );
    }


    // =====================================================
    // NORMALIZE KEYWORD
    // =====================================================

    private String normalizeKeyword(
            String keyword
    ) {

        if (keyword == null
                || keyword.isBlank()) {

            return null;
        }


        return keyword.trim();
    }


    // =====================================================
    // TRANSPORT STATUS
    // =====================================================

    private DeliveryStatus normalizeTransportStatus(
            String transportStatus
    ) {

        if (transportStatus == null
                || transportStatus.isBlank()) {

            return null;
        }


        String normalized =
                transportStatus
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );


        try {

            return DeliveryStatus.valueOf(
                    normalized
            );

        } catch (IllegalArgumentException ex) {

            throw new BadRequest(
                    "Invalid transport status. "
                            + "Allowed values: "
                            + "CREATED, ASSIGNED, READY_TO_SHIP, "
                            + "IN_TRANSIT, ARRIVED, DELIVERED, "
                            + "FAILED, CANCELLED"
            );
        }
    }


    // =====================================================
    // DATE RANGE
    // =====================================================

    private void validateDateRange(
            LocalDate fromDate,
            LocalDate toDate
    ) {

        if (fromDate != null
                && toDate != null
                && fromDate.isAfter(toDate)) {

            throw new BadRequest(
                    "From date must be before or equal to to date"
            );
        }
    }


    // =====================================================
    // PAGINATION
    // =====================================================

    private int normalizePage(
            Integer page
    ) {

        if (page == null) {

            return 0;
        }


        if (page < 0) {

            throw new BadRequest(
                    "Page must be greater than or equal to 0"
            );
        }


        return page;
    }


    private int normalizeSize(
            Integer size
    ) {

        if (size == null) {

            return DEFAULT_PAGE_SIZE;
        }


        if (size <= 0) {

            throw new BadRequest(
                    "Size must be greater than 0"
            );
        }


        if (size > MAX_PAGE_SIZE) {

            throw new BadRequest(
                    "Size must not exceed "
                            + MAX_PAGE_SIZE
            );
        }


        return size;
    }
}