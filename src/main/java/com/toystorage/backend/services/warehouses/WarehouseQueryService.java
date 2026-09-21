package com.toystorage.backend.services.warehouses;

import com.toystorage.backend.dto.response.warehouses.WarehouseResponse;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.enums.warehouses.WarehouseType;
import com.toystorage.backend.repository.warehouses.WarehouseRepository;

import jakarta.persistence.criteria.Predicate;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseQueryService {

    private final WarehouseRepository warehouseRepository;


    // =========================================================
    // LIST + SEARCH + FILTER
    // =========================================================

    public Page<WarehouseResponse> getWarehouses(

            String keyword,

            WarehouseType type,

            WarehouseStatus status,

            Pageable pageable
    ) {

        Specification<Warehouses> specification =
                (root, query, criteriaBuilder) -> {

                    List<Predicate> predicates =
                            new ArrayList<>();


                    // =====================================================
                    // SEARCH BY CODE OR NAME
                    // =====================================================

                    if (
                            keyword != null
                                    && !keyword.trim().isEmpty()
                    ) {

                        String searchValue =
                                "%"
                                        + keyword
                                        .trim()
                                        .toLowerCase()
                                        + "%";


                        Predicate codePredicate =
                                criteriaBuilder.like(

                                        criteriaBuilder.lower(
                                                root.get(
                                                        "warehousesCode"
                                                )
                                        ),

                                        searchValue
                                );


                        Predicate namePredicate =
                                criteriaBuilder.like(

                                        criteriaBuilder.lower(
                                                root.get(
                                                        "name"
                                                )
                                        ),

                                        searchValue
                                );


                        predicates.add(

                                criteriaBuilder.or(
                                        codePredicate,
                                        namePredicate
                                )
                        );
                    }


                    // =====================================================
                    // FILTER BY TYPE
                    // =====================================================

                    if (type != null) {

                        predicates.add(

                                criteriaBuilder.equal(
                                        root.get("type"),
                                        type
                                )
                        );
                    }


                    // =====================================================
                    // FILTER BY STATUS
                    // =====================================================

                    if (status != null) {

                        predicates.add(

                                criteriaBuilder.equal(
                                        root.get("status"),
                                        status
                                )
                        );
                    }


                    return criteriaBuilder.and(

                            predicates.toArray(
                                    new Predicate[0]
                            )
                    );
                };


        return warehouseRepository
                .findAll(
                        specification,
                        pageable
                )
                .map(
                        this::toResponse
                );
    }


    // =========================================================
    // DETAIL
    // =========================================================

    public Optional<WarehouseResponse> getWarehouseById(
            Long warehouseId
    ) {

        return warehouseRepository
                .findById(
                        warehouseId
                )
                .map(
                        this::toResponse
                );
    }


    // =========================================================
    // SELECTABLE LOCATIONS FOR NEW BUSINESS OPERATIONS
    //
    // IMPORTANT:
    // ONLY ACTIVE LOCATIONS ARE RETURNED.
    //
    // type == null
    //      -> all ACTIVE warehouses/stores
    //
    // type == MAIN_WAREHOUSE
    //      -> only ACTIVE MAIN_WAREHOUSE
    //
    // type == STORE
    //      -> only ACTIVE STORE
    // =========================================================

    public List<WarehouseResponse> getSelectableWarehouses(
            WarehouseType type
    ) {

        List<Warehouses> warehouses;


        if (type == null) {

            warehouses =
                    warehouseRepository
                            .findByStatus(
                                    WarehouseStatus.ACTIVE
                            );

        } else {

            warehouses =
                    warehouseRepository
                            .findByTypeAndStatus(
                                    type,
                                    WarehouseStatus.ACTIVE
                            );
        }


        return warehouses
                .stream()
                .sorted(
                        (first, second) ->
                                first
                                        .getWarehousesCode()
                                        .compareToIgnoreCase(
                                                second.getWarehousesCode()
                                        )
                )
                .map(
                        this::toResponse
                )
                .toList();
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