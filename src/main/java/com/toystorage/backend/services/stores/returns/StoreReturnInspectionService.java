package com.toystorage.backend.services.stores.returns;


import com.toystorage.backend.dto.request.stores.returns.InspectStoreReturnItemRequest;
import com.toystorage.backend.dto.response.stores.returns.StoreReturnInspectionResponse;

import com.toystorage.backend.entity.inventories.DiscrepancyReports;
import com.toystorage.backend.entity.stores.StoreReturnItems;
import com.toystorage.backend.entity.stores.StoreReturns;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.inventories.DiscrepancyReferenceType;
import com.toystorage.backend.enums.inventories.DiscrepancyStatus;
import com.toystorage.backend.enums.inventories.DiscrepancyType;

import com.toystorage.backend.enums.stores.ReturnItemCondition;
import com.toystorage.backend.enums.stores.StoreReturnStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.mapper.stores.returns.StoreReturnInspectionMapper;

import com.toystorage.backend.repository.inventories.discrepancy.DiscrepancyReportRepository;
import com.toystorage.backend.repository.stores.returns.StoreReturnItemRepository;
import com.toystorage.backend.repository.stores.returns.StoreReturnRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class StoreReturnInspectionService {


    // =====================================================
    // REPOSITORIES
    // =====================================================

    private final StoreReturnRepository
            storeReturnRepository;

    private final StoreReturnItemRepository
            storeReturnItemRepository;

    private final DiscrepancyReportRepository
            discrepancyReportRepository;


    // =====================================================
    // SERVICES
    // =====================================================

    private final StoreReturnInspectionValidationService
            validationService;


    // =====================================================
    // MAPPER
    // =====================================================

    private final StoreReturnInspectionMapper
            mapper;


    // =====================================================
    // LIST
    // =====================================================

    @Transactional(readOnly = true)
    public List<StoreReturnInspectionResponse>
    getStoreReturns() {

        Users currentUser =
                validationService
                        .getCurrentUser();


        Long warehouseId =
                validationService
                        .getWarehouseId(
                                currentUser
                        );


        return storeReturnRepository
                .findByWarehouseIdOrderByCreatedAtDesc(
                        warehouseId
                )

                .stream()

                .map(
                        this::buildResponse
                )

                .toList();
    }


    // =====================================================
    // DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public StoreReturnInspectionResponse
    getStoreReturn(
            Long returnId
    ) {

        StoreReturns storeReturn =
                validationService
                        .getReturn(
                                returnId
                        );


        Users currentUser =
                validationService
                        .getCurrentUser();


        validationService
                .validateWarehouse(
                        currentUser,
                        storeReturn
                );


        return buildResponse(
                storeReturn
        );
    }


    // =====================================================
    // INSPECT ONE ITEM
    // =====================================================

    @Transactional
    public StoreReturnInspectionResponse
    inspectItem(
            Long returnId,
            Long itemId,
            InspectStoreReturnItemRequest request
    ) {

        // =================================================
        // GET RETURN
        // =================================================

        StoreReturns storeReturn =
                validationService
                        .getReturn(
                                returnId
                        );


        // =================================================
        // CURRENT USER
        // =================================================

        Users currentUser =
                validationService
                        .getCurrentUser();


        // =================================================
        // VALIDATE WAREHOUSE
        // =================================================

        validationService
                .validateWarehouse(
                        currentUser,
                        storeReturn
                );


        // =================================================
        // VALIDATE STATUS
        // =================================================

        validationService
                .validateCanReceive(
                        storeReturn
                );


        // =================================================
        // SHIPPED -> INSPECTING
        // =================================================

        if (
                storeReturn.getStatus()
                        == StoreReturnStatus.SHIPPED
        ) {

            storeReturn.setStatus(
                    StoreReturnStatus.INSPECTING
            );

            storeReturn.setUpdatedAt(
                    LocalDateTime.now()
            );

            storeReturnRepository.save(
                    storeReturn
            );
        }


        // =================================================
        // GET ITEM
        // =================================================

        StoreReturnItems item =
                storeReturnItemRepository
                        .findById(
                                itemId
                        )

                        .orElseThrow(
                                () ->
                                        new NotFound(
                                                "Store return item not found: "
                                                        + itemId
                                        )
                        );


        // =================================================
        // VALIDATE ITEM BELONGS TO RETURN
        // =================================================

        if (
                item.getStoreReturn()
                        == null

                        ||

                        !item.getStoreReturn()
                                .getId()
                                .equals(
                                        returnId
                                )
        ) {

            throw new BadRequest(
                    "Item does not belong to store return"
            );
        }


        // =================================================
        // PREVENT DUPLICATE INSPECTION
        // =================================================

        if (
                Boolean.TRUE.equals(
                        item.getInspected()
                )
        ) {

            throw new BadRequest(
                    "Store return item has already been inspected"
            );
        }


        // =================================================
        // QUANTITY VALIDATION
        // =================================================

        Integer receivedQuantity =
                request.getReceivedQuantity();

        Integer issuedQuantity =
                item.getIssuedQuantity() != null
                        ? item.getIssuedQuantity()
                        : 0;


        if (
                receivedQuantity == null
        ) {

            throw new BadRequest(
                    "Received quantity is required"
            );
        }


        if (
                receivedQuantity < 0
        ) {

            throw new BadRequest(
                    "Received quantity cannot be negative"
            );
        }


        if (
                receivedQuantity
                        > issuedQuantity
        ) {

            throw new BadRequest(
                    "Received quantity cannot exceed issued quantity"
            );
        }


        // =================================================
        // CONDITION
        // =================================================

        if (
                request.getConditionStatus()
                        == null

                        ||

                        request.getConditionStatus()
                                .isBlank()
        ) {

            throw new BadRequest(
                    "Condition status is required"
            );
        }


        String condition =
                request
                        .getConditionStatus()
                        .trim()
                        .toUpperCase();


        // =================================================
        // INSPECTION RESULT
        // =================================================

        switch (
                condition
        ) {


            // =============================================
            // NORMAL
            // =============================================

            case "NORMAL" -> {

                item.setReceivedQuantity(
                        receivedQuantity
                );

                item.setApprovedQuantity(
                        receivedQuantity
                );

                item.setRejectedQuantity(
                        0
                );

                item.setConditionStatus(
                        ReturnItemCondition.NORMAL
                );

                item.setInspected(
                        true
                );
            }


            // =============================================
            // QUARANTINE
            // =============================================

            case "QUARANTINE" -> {

                item.setReceivedQuantity(
                        receivedQuantity
                );

                item.setApprovedQuantity(
                        0
                );

                item.setRejectedQuantity(
                        receivedQuantity
                );

                item.setConditionStatus(
                        ReturnItemCondition.QUARANTINE
                );

                item.setInspected(
                        true
                );
            }


            // =============================================
            // DAMAGED
            // =============================================

            case "DAMAGED" -> {

                item.setReceivedQuantity(
                        receivedQuantity
                );

                item.setApprovedQuantity(
                        0
                );

                item.setRejectedQuantity(
                        receivedQuantity
                );

                item.setConditionStatus(
                        ReturnItemCondition.DAMAGED
                );

                item.setInspected(
                        true
                );
            }


            // =============================================
            // EXPIRED
            // =============================================

            case "EXPIRED" -> {

                item.setReceivedQuantity(
                        receivedQuantity
                );

                item.setApprovedQuantity(
                        0
                );

                item.setRejectedQuantity(
                        receivedQuantity
                );

                item.setConditionStatus(
                        ReturnItemCondition.EXPIRED
                );

                item.setInspected(
                        true
                );
            }


            // =============================================
            // UNSUPPORTED
            // =============================================

            default ->
                    throw new BadRequest(
                            "Unsupported condition status: "
                                    + condition
                    );
        }


        // =================================================
        // NOTE
        // =================================================

        item.setNote(
                request.getNote()
        );


        // =================================================
        // SAVE INSPECTION
        // =================================================

        storeReturnItemRepository.save(
                item
        );


        // =================================================
        // AUTO CREATE DISCREPANCY
        // =================================================

        syncDiscrepancy(
                storeReturn,
                item,
                currentUser
        );


        // =================================================
        // CHECK ALL ITEMS
        // =================================================

        completeReturnIfFinished(
                storeReturn,
                currentUser
        );


        return buildResponse(
                storeReturn
        );
    }


    // =====================================================
    // SYNC DISCREPANCY
    // =====================================================

    private void syncDiscrepancy(
            StoreReturns storeReturn,
            StoreReturnItems item,
            Users currentUser
    ) {

        int issued =
                item.getIssuedQuantity() != null
                        ? item.getIssuedQuantity()
                        : 0;


        int received =
                item.getReceivedQuantity() != null
                        ? item.getReceivedQuantity()
                        : 0;


        // =================================================
        // SHORTAGE
        // =================================================

        if (
                received
                        < issued
        ) {

            createDiscrepancyIfNotExists(
                    storeReturn,
                    item,
                    currentUser,
                    DiscrepancyType.SHORTAGE,

                    "Store return shortage. "
                            + "Issued: "
                            + issued
                            + ", received: "
                            + received
                            + ", missing: "
                            + (
                            issued
                                    - received
                    )
            );
        }


        // =================================================
        // DAMAGED
        // =================================================

        if (
                item.getConditionStatus()
                        == ReturnItemCondition.DAMAGED
        ) {

            createDiscrepancyIfNotExists(
                    storeReturn,
                    item,
                    currentUser,
                    DiscrepancyType.DAMAGED,
                    "Damaged product detected during store return inspection"
            );
        }


        // =================================================
        // EXPIRED
        //
        // Project hiện chưa có DiscrepancyType.EXPIRED.
        // Tạm dùng DAMAGED để đưa vào cùng quy trình
        // hàng không đạt chất lượng.
        // =================================================

        if (
                item.getConditionStatus()
                        == ReturnItemCondition.EXPIRED
        ) {

            createDiscrepancyIfNotExists(
                    storeReturn,
                    item,
                    currentUser,
                    DiscrepancyType.DAMAGED,
                    "Expired product detected during store return inspection"
            );
        }
    }


    // =====================================================
    // CREATE DISCREPANCY
    // =====================================================

    private void createDiscrepancyIfNotExists(
            StoreReturns storeReturn,
            StoreReturnItems item,
            Users currentUser,
            DiscrepancyType discrepancyType,
            String description
    ) {

        boolean exists =
                discrepancyReportRepository
                        .existsByReferenceTypeAndReferenceIdAndProductIdAndDiscrepancyTypeAndStatusIn(

                                DiscrepancyReferenceType.STORE_RETURN,

                                storeReturn.getId(),

                                item.getProduct()
                                        .getId(),

                                discrepancyType,

                                List.of(
                                        DiscrepancyStatus.OPEN,
                                        DiscrepancyStatus.INVESTIGATING
                                )
                        );


        if (exists) {

            return;
        }


        String reportCode =
                generateDiscrepancyCode();


        DiscrepancyReports report =
                DiscrepancyReports
                        .builder()

                        .reportCode(
                                reportCode
                        )

                        .discrepancyReportsCode(
                                reportCode
                        )

                        .referenceType(
                                DiscrepancyReferenceType.STORE_RETURN
                        )

                        .referenceId(
                                storeReturn.getId()
                        )

                        .productId(
                                item.getProduct()
                                        .getId()
                        )

                        .warehouse(
                                storeReturn.getWarehouse()
                        )

                        .discrepancyType(
                                discrepancyType
                        )

                        .status(
                                DiscrepancyStatus.OPEN
                        )

                        .reportedBy(
                                currentUser
                        )

                        /*
                         * Sau khi Staff kiểm xong,
                         * Warehouse Manager là người review đầu tiên.
                         */
                        .responsibleParty(
                                "WAREHOUSE_MANAGER"
                        )

                        .description(
                                description
                        )

                        .evidenceImageUrl(
                                item.getEvidenceImageUrl()
                        )

                        .build();


        discrepancyReportRepository.save(
                report
        );
    }


    // =====================================================
    // COMPLETE INSPECTION
    // =====================================================

    private void completeReturnIfFinished(
            StoreReturns storeReturn,
            Users currentUser
    ) {

        List<StoreReturnItems> items =
                storeReturnItemRepository
                        .findByStoreReturnId(
                                storeReturn.getId()
                        );


        // =================================================
        // ALL ITEMS MUST BE INSPECTED
        // =================================================

        boolean allInspected =
                !items.isEmpty()

                        &&

                        items.stream()
                                .allMatch(
                                        item ->
                                                Boolean.TRUE.equals(
                                                        item.getInspected()
                                                )
                                );


        if (!allInspected) {

            return;
        }


        // =================================================
        // INSPECTION FINISHED
        //
        // IMPORTANT:
        //
        // Không cộng inventory ở đây.
        // Không set RECEIVED ở đây.
        //
        // Warehouse Manager phải review trước.
        // =================================================

        storeReturn.setStatus(
                StoreReturnStatus.PENDING_CONFIRMATION
        );


        storeReturn.setInspectionSubmittedAt(
                LocalDateTime.now()
        );


        /*
         * Nếu entity StoreReturns của bạn có inspectedBy
         * thì lưu người hoàn thành inspection.
         */
        storeReturn.setInspectedBy(
                currentUser
        );


        storeReturn.setUpdatedAt(
                LocalDateTime.now()
        );


        storeReturnRepository.save(
                storeReturn
        );
    }


    // =====================================================
    // GENERATE DISCREPANCY CODE
    // =====================================================

    private String generateDiscrepancyCode() {

        return "DR-"
                + UUID.randomUUID()
                .toString()
                .replace(
                        "-",
                        ""
                )
                .substring(
                        0,
                        12
                )
                .toUpperCase();
    }


    // =====================================================
    // RESPONSE
    // =====================================================

    private StoreReturnInspectionResponse
    buildResponse(
            StoreReturns storeReturn
    ) {

        List<StoreReturnItems> items =
                storeReturnItemRepository
                        .findByStoreReturnId(
                                storeReturn.getId()
                        );


        return mapper
                .toResponse(
                        storeReturn,
                        items
                );
    }
}