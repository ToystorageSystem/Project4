package com.toystorage.backend.services.warehouses.damagedgoods;

import com.toystorage.backend.dto.request.warehouses.damagedgoods.CreateStaffDamagedGoodsRequest;

import com.toystorage.backend.dto.response.warehouses.damagedgoods.StaffDamagedGoodsReportResponse;

import com.toystorage.backend.entity.warehouses.DamagedGoodsItems;
import com.toystorage.backend.entity.warehouses.DamagedGoodsReports;

import com.toystorage.backend.entity.products.Products;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.entity.warehouses.WarehouseLocations;

import com.toystorage.backend.enums.warehouses.DamageDisposition;
import com.toystorage.backend.enums.warehouses.DamagedGoodsItemStatus;
import com.toystorage.backend.enums.warehouses.DamagedGoodsStatus;

import com.toystorage.backend.mapper.warehouses.damagedgoods.StaffDamagedGoodsMapper;

import com.toystorage.backend.repository.warehouses.damagedgoods.DamagedGoodsItemRepository;
import com.toystorage.backend.repository.warehouses.damagedgoods.DamagedGoodsReportRepository;

import com.toystorage.backend.services.cloudinary.CloudinaryService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffDamagedGoodsCreationService {

    private final DamagedGoodsReportRepository
            reportRepository;

    private final DamagedGoodsItemRepository
            itemRepository;

    private final StaffDamagedGoodsValidationService
            validationService;

    private final StaffDamagedGoodsMapper
            mapper;

    private final CloudinaryService
            cloudinaryService;


    // =====================================================
    // CREATE REPORT
    // =====================================================

    @Transactional
    public StaffDamagedGoodsReportResponse create(
            CreateStaffDamagedGoodsRequest request,
            MultipartFile image
    ) {

        Users staff =
                validationService
                        .getCurrentUser();


        Long warehouseId =
                validationService
                        .getWarehouseId(
                                staff
                        );


        Products product =
                validationService
                        .getProduct(
                                request.getProductId()
                        );


        WarehouseLocations location =
                validationService
                        .getLocation(
                                request.getLocationId(),
                                warehouseId
                        );


        validationService
                .validateQuantity(
                        warehouseId,
                        location.getId(),
                        product.getId(),
                        request.getQuantity()
                );


        // =================================================
        // IMAGE
        // =================================================

        String imageUrl =
                null;


        if (image != null
                && !image.isEmpty()) {

            imageUrl =
                    cloudinaryService
                            .uploadImage(
                                    image
                            );
        }


        LocalDateTime now =
                LocalDateTime.now();


        // =================================================
        // REPORT
        // =================================================

        DamagedGoodsReports report =
                DamagedGoodsReports
                        .builder()

                        .reportCode(
                                generateReportCode()
                        )

                        .damagedGoodsReportsCode(
                                generateCode("DGR")
                        )

                        .warehouse(
                                staff.getWarehouse()
                        )

                        .sourceType(
                                request.getSourceType()
                        )

                        .sourceId(
                                request.getSourceId()
                        )

                        .status(
                                DamagedGoodsStatus.REPORTED
                        )

                        .reportedBy(
                                staff
                        )

                        .description(
                                request.getDescription()
                        )

                        .createdAt(now)

                        .updatedAt(now)

                        .build();


        report =
                reportRepository.save(
                        report
                );


        // =================================================
        // ITEM
        // =================================================

        DamagedGoodsItems item =
                DamagedGoodsItems
                        .builder()

                        .damagedGoodsReport(
                                report
                        )

                        .product(
                                product
                        )

                        /*
                         * Đây là location PHÁT HIỆN.
                         *
                         * Manager xử lý sau sẽ quyết định
                         * location quarantine/damaged.
                         */
                        .location(
                                location
                        )

                        .quantity(
                                request.getQuantity()
                        )

                        .damageType(
                                request.getDamageType()
                        )

                        .conditionNote(
                                request.getConditionNote()
                        )

                        .evidenceImage(
                                imageUrl
                        )

                        /*
                         * Staff không quyết định disposition.
                         *
                         * Đặt QUARANTINE/HOLD nếu enum của project
                         * có giá trị tương ứng.
                         *
                         * Ở schema hiện tại có QUARANTINE.
                         */
                        .disposition(
                                DamageDisposition.QUARANTINE
                        )

                        .status(
                                DamagedGoodsItemStatus.REPORTED
                        )

                        .damagedGoodsItemsCode(
                                generateCode("DGI")
                        )

                        .build();


        itemRepository.save(
                item
        );


        /*
         * QUAN TRỌNG:
         *
         * KHÔNG:
         * - giảm quantity
         * - dispose
         * - return supplier
         * - tạo inventory adjustment
         *
         * Manager xác nhận bước sau.
         */


        return mapper.toResponse(
                report,
                List.of(item)
        );
    }


    private String generateReportCode() {

        return "DMG-"
                + System.currentTimeMillis();
    }


    private String generateCode(
            String prefix
    ) {

        return prefix
                + "-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }
}