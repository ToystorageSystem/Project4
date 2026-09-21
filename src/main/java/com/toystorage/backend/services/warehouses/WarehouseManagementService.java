package com.toystorage.backend.services.warehouses;

import com.toystorage.backend.dto.request.warehouses.CreateWarehouseRequest;
import com.toystorage.backend.dto.request.warehouses.UpdateWarehouseRequest;

import com.toystorage.backend.dto.response.warehouses.WarehouseDeactivationImpactResponse;
import com.toystorage.backend.dto.response.warehouses.WarehouseResponse;

import com.toystorage.backend.entity.warehouses.Warehouses;

import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;

import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.enums.warehouses.WarehouseType;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.repository.warehouses.WarehouseRepository;

import com.toystorage.backend.services.users.ActivityLogService;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WarehouseManagementService {

    private final WarehouseRepository warehouseRepository;

    private final ActivityLogService activityLogService;

    private final WarehouseImpactService warehouseImpactService;


    // =========================================================
    // CREATE WAREHOUSE / STORE
    // =========================================================

    @Transactional
    public WarehouseResponse createWarehouse(

            CreateWarehouseRequest request,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        String name =
                request
                        .getName()
                        .trim();

        String address =
                request
                        .getAddress()
                        .trim();

        String phone =
                request
                        .getPhone()
                        .trim();


        String warehouseCode =
                generateWarehouseCode(
                        request.getType()
                );


        Warehouses warehouse =
                Warehouses
                        .builder()
                        .warehousesCode(
                                warehouseCode
                        )
                        .name(
                                name
                        )
                        .type(
                                request.getType()
                        )
                        .address(
                                address
                        )
                        .phone(
                                phone
                        )
                        .status(
                                WarehouseStatus.ACTIVE
                        )
                        .build();


        Warehouses saved =
                warehouseRepository
                        .saveAndFlush(
                                warehouse
                        );


        activityLogService.log(

                authentication,

                httpRequest,

                ActivityAction.CREATE,

                ActivityEntityType.WAREHOUSE,

                saved.getId(),

                null,

                snapshot(saved)
        );


        return toResponse(
                saved
        );
    }


    // =========================================================
    // UPDATE WAREHOUSE / STORE
    // =========================================================

    @Transactional
    public WarehouseResponse updateWarehouse(

            Long warehouseId,

            UpdateWarehouseRequest request,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        Warehouses warehouse =
                getWarehouseEntity(
                        warehouseId
                );


        Map<String, Object> oldValue =
                snapshot(
                        warehouse
                );


        String name =
                request
                        .getName()
                        .trim();

        String address =
                request
                        .getAddress()
                        .trim();

        String phone =
                request
                        .getPhone()
                        .trim();


        boolean changed =
                !Objects.equals(
                        warehouse.getName(),
                        name
                )
                        || !Objects.equals(
                        warehouse.getAddress(),
                        address
                )
                        || !Objects.equals(
                        warehouse.getPhone(),
                        phone
                );


        if (!changed) {

            throw new BadRequest(
                    "Thông tin địa điểm không có thay đổi"
            );
        }


        warehouse.setName(
                name
        );

        warehouse.setAddress(
                address
        );

        warehouse.setPhone(
                phone
        );


        Warehouses saved =
                warehouseRepository
                        .saveAndFlush(
                                warehouse
                        );


        activityLogService.log(

                authentication,

                httpRequest,

                ActivityAction.UPDATE,

                ActivityEntityType.WAREHOUSE,

                saved.getId(),

                oldValue,

                snapshot(saved)
        );


        return toResponse(
                saved
        );
    }


    // =========================================================
    // DEACTIVATE WAREHOUSE / STORE
    // ACTIVE -> INACTIVE
    // =========================================================

    @Transactional
    public WarehouseResponse deactivateWarehouse(

            Long warehouseId,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        Warehouses warehouse =
                getWarehouseEntity(
                        warehouseId
                );


        if (
                warehouse.getStatus()
                        == WarehouseStatus.INACTIVE
        ) {

            throw new BadRequest(
                    "Địa điểm đã ở trạng thái INACTIVE"
            );
        }


        // =====================================================
        // CHECK OPEN BUSINESS DOCUMENTS
        // =====================================================

        WarehouseDeactivationImpactResponse impact =
                warehouseImpactService
                        .getDeactivationImpact(
                                warehouseId
                        );


        if (!impact.isCanDeactivate()) {

            throw new BadRequest(
                    "Không thể vô hiệu hóa địa điểm vì còn "
                            + impact.getTotalBlockingRecords()
                            + " phiếu nghiệp vụ chưa hoàn thành: "
                            + impact.getBlockingByModule()
            );
        }


        Map<String, Object> oldValue =
                snapshot(
                        warehouse
                );


        warehouse.setStatus(
                WarehouseStatus.INACTIVE
        );


        Warehouses saved =
                warehouseRepository
                        .saveAndFlush(
                                warehouse
                        );


        activityLogService.log(

                authentication,

                httpRequest,

                ActivityAction.UPDATE,

                ActivityEntityType.WAREHOUSE,

                saved.getId(),

                oldValue,

                snapshot(saved)
        );


        return toResponse(
                saved
        );
    }


    // =========================================================
    // ACTIVATE WAREHOUSE / STORE
    // INACTIVE -> ACTIVE
    // =========================================================

    @Transactional
    public WarehouseResponse activateWarehouse(

            Long warehouseId,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        Warehouses warehouse =
                getWarehouseEntity(
                        warehouseId
                );


        if (
                warehouse.getStatus()
                        == WarehouseStatus.ACTIVE
        ) {

            throw new BadRequest(
                    "Địa điểm đang ở trạng thái ACTIVE"
            );
        }


        Map<String, Object> oldValue =
                snapshot(
                        warehouse
                );


        warehouse.setStatus(
                WarehouseStatus.ACTIVE
        );


        Warehouses saved =
                warehouseRepository
                        .saveAndFlush(
                                warehouse
                        );


        activityLogService.log(

                authentication,

                httpRequest,

                ActivityAction.UPDATE,

                ActivityEntityType.WAREHOUSE,

                saved.getId(),

                oldValue,

                snapshot(saved)
        );


        return toResponse(
                saved
        );
    }


    // =========================================================
    // GET ENTITY
    // =========================================================

    private Warehouses getWarehouseEntity(
            Long warehouseId
    ) {

        return warehouseRepository
                .findById(
                        warehouseId
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Không tìm thấy kho hoặc cửa hàng"
                        )
                );
    }


    // =========================================================
    // GENERATE UNIQUE WAREHOUSE CODE
    // =========================================================

    private String generateWarehouseCode(
            WarehouseType type
    ) {

        if (type == null) {

            throw new BadRequest(
                    "Loại địa điểm không được để trống"
            );
        }


        String prefix =
                type == WarehouseType.MAIN_WAREHOUSE
                        ? "WH-"
                        : "STORE-";


        String code;


        do {

            code =
                    prefix
                            + UUID
                            .randomUUID()
                            .toString()
                            .replace(
                                    "-",
                                    ""
                            )
                            .substring(
                                    0,
                                    8
                            )
                            .toUpperCase();

        } while (
                warehouseRepository
                        .existsByWarehousesCodeIgnoreCase(
                                code
                        )
        );


        return code;
    }


    // =========================================================
    // SNAPSHOT FOR ACTIVITY LOG
    // =========================================================

    private Map<String, Object> snapshot(
            Warehouses warehouse
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();


        data.put(
                "id",
                warehouse.getId()
        );

        data.put(
                "warehousesCode",
                warehouse.getWarehousesCode()
        );

        data.put(
                "name",
                warehouse.getName()
        );

        data.put(
                "type",
                warehouse.getType() != null
                        ? warehouse.getType().name()
                        : null
        );

        data.put(
                "address",
                warehouse.getAddress()
        );

        data.put(
                "phone",
                warehouse.getPhone()
        );

        data.put(
                "status",
                warehouse.getStatus() != null
                        ? warehouse.getStatus().name()
                        : null
        );


        return data;
    }


    // =========================================================
    // ENTITY -> RESPONSE
    // =========================================================

    private WarehouseResponse toResponse(
            Warehouses warehouse
    ) {

        return WarehouseResponse
                .builder()
                .id(
                        warehouse.getId()
                )
                .warehousesCode(
                        warehouse.getWarehousesCode()
                )
                .name(
                        warehouse.getName()
                )
                .type(
                        warehouse.getType()
                )
                .address(
                        warehouse.getAddress()
                )
                .phone(
                        warehouse.getPhone()
                )
                .status(
                        warehouse.getStatus()
                )
                .createdAt(
                        warehouse.getCreatedAt()
                )
                .updatedAt(
                        warehouse.getUpdatedAt()
                )
                .build();
    }
}