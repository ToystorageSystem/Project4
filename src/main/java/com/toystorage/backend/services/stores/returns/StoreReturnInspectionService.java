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
import com.toystorage.backend.enums.inventories.DiscrepancySeverity;
import com.toystorage.backend.services.inventories.discrepancy.DiscrepancySeverityPolicy;

import com.toystorage.backend.enums.stores.ReturnItemCondition;
import com.toystorage.backend.enums.stores.StoreReturnStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Forbidden;

import com.toystorage.backend.mapper.stores.returns.StoreReturnInspectionMapper;

import com.toystorage.backend.repository.inventories.discrepancy.DiscrepancyReportRepository;
import com.toystorage.backend.repository.stores.returns.StoreReturnItemRepository;
import com.toystorage.backend.repository.stores.returns.StoreReturnRepository;
import com.toystorage.backend.repository.auth.UserRoleRepository;

import com.toystorage.backend.entity.warehouses.DamagedGoodsItems;
import com.toystorage.backend.entity.warehouses.DamagedGoodsReports;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;

import com.toystorage.backend.enums.warehouses.DamageDisposition;
import com.toystorage.backend.enums.warehouses.DamageType;
import com.toystorage.backend.enums.warehouses.DamagedGoodsItemStatus;
import com.toystorage.backend.enums.warehouses.DamagedGoodsSourceType;
import com.toystorage.backend.enums.warehouses.DamagedGoodsStatus;

import com.toystorage.backend.repository.warehouses.damagedgoods.DamagedGoodsItemRepository;
import com.toystorage.backend.repository.warehouses.damagedgoods.DamagedGoodsReportRepository;

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

    private final DiscrepancySeverityPolicy
            discrepancySeverityPolicy;

    private final UserRoleRepository
            userRoleRepository;

    private final StoreReturnLocationService
            locationService;

    private final StoreReturnInventoryService
            inventoryService;

    private final DamagedGoodsReportRepository
            damagedGoodsReportRepository;

    private final DamagedGoodsItemRepository
            damagedGoodsItemRepository;

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
        // SURPLUS
        // =================================================

        if (received > issued) {

            createDiscrepancyIfNotExists(
                    storeReturn,
                    item,
                    currentUser,
                    DiscrepancyType.SURPLUS,
                    "Store return surplus. Issued: "
                            + issued
                            + ", received: "
                            + received
                            + ", surplus: "
                            + (received - issued)
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

        int issued = item.getIssuedQuantity() != null
                ? item.getIssuedQuantity()
                : 0;

        int received = item.getReceivedQuantity() != null
                ? item.getReceivedQuantity()
                : 0;

        int abnormal = item.getConditionStatus() == ReturnItemCondition.NORMAL
                ? 0
                : received;

        DiscrepancySeverity severity =
                discrepancySeverityPolicy.determine(
                        issued,
                        received,
                        abnormal
                );

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

                        .severity(
                                severity
                        )

                        .blocking(
                                discrepancySeverityPolicy.isBlocking(severity)
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
                        .findByStoreReturnId(storeReturn.getId());

        boolean allInspected =
                !items.isEmpty()
                        && items.stream()
                        .allMatch(item ->
                                Boolean.TRUE.equals(item.getInspected())
                        );

        if (!allInspected) {
            return;
        }

        List<DiscrepancyReports> reports =
                discrepancyReportRepository
                        .findByReferenceTypeAndReferenceId(
                                DiscrepancyReferenceType.STORE_RETURN,
                                storeReturn.getId()
                        );

        for (StoreReturnItems item : items) {
            boolean blocked = reports.stream()
                    .filter(r -> r.getProductId() != null)
                    .filter(r -> r.getProductId().equals(item.getProduct().getId()))
                    .anyMatch(r ->
                            Boolean.TRUE.equals(r.getBlocking())
                                    && (r.getStatus() == DiscrepancyStatus.OPEN
                                    || r.getStatus() == DiscrepancyStatus.INVESTIGATING)
                    );

            if (!blocked) {
                postInventoryIfNeeded(storeReturn, item, currentUser);
            }
        }

        boolean hasBlockingUnresolved = reports.stream()
                .anyMatch(r ->
                        Boolean.TRUE.equals(r.getBlocking())
                                && (r.getStatus() == DiscrepancyStatus.OPEN
                                || r.getStatus() == DiscrepancyStatus.INVESTIGATING)
                );

        LocalDateTime now = LocalDateTime.now();

        storeReturn.setInspectionSubmittedAt(now);
        storeReturn.setInspectedBy(currentUser);
        storeReturn.setUpdatedAt(now);

        if (hasBlockingUnresolved) {
            storeReturn.setStatus(StoreReturnStatus.PENDING_CONFIRMATION);
        } else {
            storeReturn.setStatus(StoreReturnStatus.RECEIVED);
            storeReturn.setReceivedAt(now);
        }

        storeReturnRepository.save(storeReturn);
    }

    private void postInventoryIfNeeded(
            StoreReturns storeReturn,
            StoreReturnItems item,
            Users user
    ) {
        if (Boolean.TRUE.equals(item.getInventoryPosted())) {
            return;
        }

        int quantity = item.getReceivedQuantity() != null
                ? item.getReceivedQuantity()
                : 0;

        if (quantity <= 0) {
            item.setInventoryPosted(true);
            storeReturnItemRepository.save(item);
            return;
        }

        switch (item.getConditionStatus()) {
            case NORMAL -> {
                WarehouseLocations normalLocation =
                        locationService.getNormalLocation(
                                storeReturn.getWarehouse().getId()
                        );

                inventoryService.addAvailableInventory(
                        storeReturn,
                        item,
                        normalLocation,
                        quantity,
                        user
                );
            }

            case QUARANTINE, DAMAGED, EXPIRED -> {
                WarehouseLocations quarantine =
                        locationService.getQuarantineLocation(
                                storeReturn.getWarehouse().getId()
                        );

                inventoryService.addQuarantineInventory(
                        storeReturn,
                        item,
                        quarantine,
                        quantity,
                        user
                );
            }
        }

        item.setInventoryPosted(true);
        storeReturnItemRepository.save(item);
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

    private StoreReturnInspectionResponse buildResponse(
            StoreReturns storeReturn
    ) {

        // =====================================================
        // ITEMS
        // =====================================================

        List<StoreReturnItems> items =
                storeReturnItemRepository
                        .findByStoreReturnId(
                                storeReturn.getId()
                        );


        // =====================================================
        // DISCREPANCIES
        // =====================================================

        List<DiscrepancyReports> discrepancies =
                discrepancyReportRepository
                        .findByReferenceTypeAndReferenceId(

                                DiscrepancyReferenceType.STORE_RETURN,

                                storeReturn.getId()
                        );


        // =====================================================
        // RESPONSE
        // =====================================================

        return mapper
                .toResponse(
                        storeReturn,
                        items,
                        discrepancies
                );
    }
    @Transactional
    public void resolveAsWarehouse(
            Long returnId,
            Long discrepancyId,
            String reason
    ) {

        StoreReturns storeReturn =
                validationService.getReturn(
                        returnId
                );

        Users manager =
                validationService.getCurrentUser();

        validationService.validateWarehouse(
                manager,
                storeReturn
        );

        requireRole(manager, "WAREHOUSE_MANAGER");

        DiscrepancyReports report =
                discrepancyReportRepository
                        .findById(discrepancyId)
                        .orElseThrow(() ->
                                new NotFound(
                                        "Discrepancy not found: "
                                                + discrepancyId
                                )
                        );

        validateStoreReturnDiscrepancy(
                storeReturn,
                report
        );

        if (
                report.getStatus()
                        == DiscrepancyStatus.RESOLVED
        ) {

            throw new BadRequest(
                    "Discrepancy has already been resolved"
            );
        }

        report.setResponsibleParty(
                "WAREHOUSE_MANAGER"
        );

        report.setReviewedBy(
                manager
        );

        report.setReviewedAt(
                LocalDateTime.now()
        );

        report.setResolutionNote(
                reason
        );

        if (
                report.getDiscrepancyType()
                        == DiscrepancyType.DAMAGED
        ) {

            createDamagedGoodsReport(
                    storeReturn,
                    report,
                    manager,
                    reason
            );
        }

        report.setStatus(
                DiscrepancyStatus.RESOLVED
        );

        report.setResolvedBy(
                manager
        );

        report.setResolvedAt(
                LocalDateTime.now()
        );

        discrepancyReportRepository.save(
                report
        );
    }
    @Transactional
    public void sendToStoreManager(
            Long returnId,
            Long discrepancyId,
            String reason
    ) {

        StoreReturns storeReturn =
                validationService.getReturn(
                        returnId
                );

        Users manager =
                validationService.getCurrentUser();

        validationService.validateWarehouse(
                manager,
                storeReturn
        );

        requireRole(manager, "WAREHOUSE_MANAGER");

        DiscrepancyReports report =
                discrepancyReportRepository
                        .findById(discrepancyId)
                        .orElseThrow(() ->
                                new NotFound(
                                        "Discrepancy not found: "
                                                + discrepancyId
                                )
                        );

        validateStoreReturnDiscrepancy(
                storeReturn,
                report
        );

        if (
                report.getStatus()
                        == DiscrepancyStatus.RESOLVED
        ) {

            throw new BadRequest(
                    "Resolved discrepancy cannot be transferred"
            );
        }

        report.setResponsibleParty(
                "STORE_MANAGER"
        );

        report.setStatus(
                DiscrepancyStatus.INVESTIGATING
        );

        report.setReviewedBy(
                manager
        );

        report.setReviewedAt(
                LocalDateTime.now()
        );

        report.setResolutionNote(
                reason
        );

        discrepancyReportRepository.save(
                report
        );
    }
    @Transactional
    public void confirmByStore(
            Long returnId,
            Long discrepancyId,
            String note
    ) {
        StoreReturns storeReturn = validationService.getReturn(returnId);
        Users storeManager = validationService.getCurrentUser();

        requireRole(storeManager, "STORE_MANAGER");
        validateStoreUser(storeManager, storeReturn);

        DiscrepancyReports report = discrepancyReportRepository
                .findById(discrepancyId)
                .orElseThrow(() -> new NotFound(
                        "Discrepancy not found: " + discrepancyId
                ));

        validateStoreReturnDiscrepancy(storeReturn, report);

        if (!"STORE_MANAGER".equals(report.getResponsibleParty())) {
            throw new BadRequest(
                    "This discrepancy is not waiting for Store Manager confirmation"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        report.setStatus(DiscrepancyStatus.RESOLVED);
        report.setResolvedBy(storeManager);
        report.setResolvedAt(now);
        report.setResolutionNote(note);
        discrepancyReportRepository.save(report);
    }

    @Transactional
    public void rejectAndEscalateToBusiness(
            Long returnId,
            Long discrepancyId,
            String reason
    ) {
        StoreReturns storeReturn = validationService.getReturn(returnId);
        Users storeManager = validationService.getCurrentUser();

        requireRole(storeManager, "STORE_MANAGER");
        validateStoreUser(storeManager, storeReturn);

        DiscrepancyReports report = discrepancyReportRepository
                .findById(discrepancyId)
                .orElseThrow(() -> new NotFound(
                        "Discrepancy not found: " + discrepancyId
                ));

        validateStoreReturnDiscrepancy(storeReturn, report);

        report.setResponsibleParty("BUSINESS_MANAGER");
        report.setStatus(DiscrepancyStatus.INVESTIGATING);
        report.setReviewedBy(storeManager);
        report.setReviewedAt(LocalDateTime.now());
        report.setResolutionNote(reason);
        discrepancyReportRepository.save(report);
    }

    @Transactional
    public void resolveAsBusiness(
            Long returnId,
            Long discrepancyId,
            String decision
    ) {
        StoreReturns storeReturn = validationService.getReturn(returnId);
        Users businessManager = validationService.getCurrentUser();

        requireRole(businessManager, "BUSINESS_MANAGER");

        DiscrepancyReports report = discrepancyReportRepository
                .findById(discrepancyId)
                .orElseThrow(() -> new NotFound(
                        "Discrepancy not found: " + discrepancyId
                ));

        validateStoreReturnDiscrepancy(storeReturn, report);

        if (!"BUSINESS_MANAGER".equals(report.getResponsibleParty())) {
            throw new BadRequest(
                    "This discrepancy has not been escalated to Business Manager"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        report.setStatus(DiscrepancyStatus.RESOLVED);
        report.setReviewedBy(businessManager);
        report.setReviewedAt(now);
        report.setResolvedBy(businessManager);
        report.setResolvedAt(now);
        report.setResolutionNote(decision);
        discrepancyReportRepository.save(report);
    }

    private void validateStoreUser(
            Users storeUser,
            StoreReturns storeReturn
    ) {
        if (storeUser.getWarehouse() == null
                || storeReturn.getStore() == null
                || !storeUser.getWarehouse().getId()
                .equals(storeReturn.getStore().getId())) {
            throw new Forbidden(
                    "Store Return does not belong to your store"
            );
        }
    }

    private void requireRole(Users user, String roleName) {
        boolean matched = userRoleRepository
                .findByUser_Id(user.getId())
                .stream()
                .anyMatch(userRole ->
                        userRole.getRole() != null
                                && roleName.equals(
                                userRole.getRole().getRoleName()
                        )
                );

        if (!matched) {
            throw new Forbidden(
                    "Required role: " + roleName
            );
        }
    }

    private void validateStoreReturnDiscrepancy(
            StoreReturns storeReturn,
            DiscrepancyReports report
    ) {

        if (
                report.getReferenceType()
                        != DiscrepancyReferenceType.STORE_RETURN
        ) {

            throw new BadRequest(
                    "Discrepancy is not a store return discrepancy"
            );
        }

        if (
                !report.getReferenceId()
                        .equals(
                                storeReturn.getId()
                        )
        ) {

            throw new BadRequest(
                    "Discrepancy does not belong to this store return"
            );
        }

        if (
                report.getWarehouse() == null

                        ||

                        !report.getWarehouse()
                                .getId()
                                .equals(
                                        storeReturn
                                                .getWarehouse()
                                                .getId()
                                )
        ) {

            throw new BadRequest(
                    "Discrepancy belongs to another warehouse"
            );
        }
    }
    private void createDamagedGoodsReport(
            StoreReturns storeReturn,
            DiscrepancyReports discrepancy,
            Users manager,
            String reason
    ) {

        StoreReturnItems returnItem =
                storeReturnItemRepository
                        .findByStoreReturnId(
                                storeReturn.getId()
                        )
                        .stream()
                        .filter(item ->
                                item.getProduct() != null
                                        &&
                                        item.getProduct()
                                                .getId()
                                                .equals(
                                                        discrepancy.getProductId()
                                                )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new NotFound(
                                        "Store return item not found for discrepancy"
                                )
                        );

        WarehouseLocations quarantine =
                locationService
                        .getQuarantineLocation(
                                storeReturn
                                        .getWarehouse()
                                        .getId()
                        );

        String reportCode =
                "DGR-"
                        + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase();

        DamagedGoodsReports damagedReport =
                DamagedGoodsReports
                        .builder()

                        .reportCode(
                                reportCode
                        )

                        .damagedGoodsReportsCode(
                                reportCode
                        )

                        .warehouse(
                                storeReturn.getWarehouse()
                        )

                        .sourceType(
                                DamagedGoodsSourceType.STORE_RETURN
                        )

                        .sourceId(
                                storeReturn.getId()
                        )

                        .status(
                                DamagedGoodsStatus.REPORTED
                        )

                        .reportedBy(
                                manager
                        )

                        .description(
                                reason
                        )

                        .build();

        damagedReport =
                damagedGoodsReportRepository.save(
                        damagedReport
                );

        DamageType damageType =
                returnItem.getConditionStatus()
                        == ReturnItemCondition.EXPIRED
                        ? DamageType.EXPIRED
                        : DamageType.OTHER;

        DamagedGoodsItems damagedItem =
                DamagedGoodsItems
                        .builder()

                        .damagedGoodsReport(
                                damagedReport
                        )

                        .product(
                                returnItem.getProduct()
                        )

                        .location(
                                quarantine
                        )

                        .quantity(
                                returnItem.getReceivedQuantity()
                        )

                        .damageType(
                                damageType
                        )

                        .conditionNote(
                                returnItem.getNote()
                        )

                        .evidenceImage(
                                returnItem.getEvidenceImageUrl()
                        )

                        .disposition(
                                DamageDisposition.QUARANTINE
                        )

                        .status(
                                DamagedGoodsItemStatus.REPORTED
                        )

                        .damagedGoodsItemsCode(
                                "DGI-"
                                        + UUID.randomUUID()
                                        .toString()
                                        .replace("-", "")
                                        .substring(0, 12)
                                        .toUpperCase()
                        )

                        .build();

        damagedGoodsItemRepository.save(
                damagedItem
        );
    }
    @Transactional
    public StoreReturnInspectionResponse confirmStoreReturn(
            Long returnId
    ) {

        StoreReturns storeReturn =
                validationService
                        .getReturn(
                                returnId
                        );

        Users manager =
                validationService
                        .getCurrentUser();

        validationService
                .validateWarehouse(
                        manager,
                        storeReturn
                );

        requireRole(manager, "WAREHOUSE_MANAGER");

        if (
                storeReturn.getStatus()
                        != StoreReturnStatus.PENDING_CONFIRMATION
        ) {

            throw new BadRequest(
                    "Store return is not waiting for confirmation"
            );
        }

        List<DiscrepancyReports> reports =
                discrepancyReportRepository
                        .findByReferenceTypeAndReferenceId(
                                DiscrepancyReferenceType.STORE_RETURN,
                                returnId
                        );

        boolean hasUnresolved =
                reports.stream()
                        .anyMatch(report ->
                                Boolean.TRUE.equals(report.getBlocking())
                                        && (report.getStatus() == DiscrepancyStatus.OPEN
                                        || report.getStatus() == DiscrepancyStatus.INVESTIGATING)
                        );

        if (hasUnresolved) {

            throw new BadRequest(
                    "Store return still has unresolved MAJOR discrepancies"
            );
        }

        List<StoreReturnItems> items =
                storeReturnItemRepository
                        .findByStoreReturnId(
                                returnId
                        );

        for (StoreReturnItems item : items) {
            postInventoryIfNeeded(
                    storeReturn,
                    item,
                    manager
            );
        }

        storeReturn.setStatus(
                StoreReturnStatus.RECEIVED
        );

        storeReturn.setReceivedAt(
                LocalDateTime.now()
        );

        storeReturn.setUpdatedAt(
                LocalDateTime.now()
        );

        storeReturnRepository.save(
                storeReturn
        );

        return buildResponse(
                storeReturn
        );
    }
}
