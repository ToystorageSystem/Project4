package com.toystorage.backend.services.shipments;

import com.toystorage.backend.dto.response.shipments.InTransitPackageResponse;
import com.toystorage.backend.dto.response.shipments.InTransitShipmentDetailResponse;
import com.toystorage.backend.dto.response.shipments.InTransitShipmentListItemResponse;
import com.toystorage.backend.dto.response.shipments.InTransitShipmentPageResponse;
import com.toystorage.backend.dto.response.shipments.InTransitStatusHistoryResponse;
import com.toystorage.backend.dto.response.shipments.InTransitTransferProductResponse;
import com.toystorage.backend.dto.response.shipments.InTransitTransferResponse;
import com.toystorage.backend.dto.response.shipments.ShipmentManifestProductResponse;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.deliveries.DeliveryAssignmentHistory;
import com.toystorage.backend.entity.deliveries.DeliveryPackages;
import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.shipments.ShipmentManifestTransfer;
import com.toystorage.backend.entity.shipments.ShipmentManifests;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.transfers.StockTransferItems;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;

import com.toystorage.backend.enums.deliveries.DeliveryStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.repository.deliveries.DeliveryAssignmentHistoryRepository;
import com.toystorage.backend.repository.deliveries.DeliveryPackageRepository;
import com.toystorage.backend.repository.deliveries.DeliveryRepository;
import com.toystorage.backend.repository.packages.packing.PackageItemRepository;
import com.toystorage.backend.repository.shipments.ShipmentManifestTransferRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InTransitTrackingService {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final int MAX_PAGE_SIZE = 100;


    private final DeliveryRepository
            deliveryRepository;

    private final DeliveryPackageRepository
            deliveryPackageRepository;

    private final DeliveryAssignmentHistoryRepository
            deliveryAssignmentHistoryRepository;

    private final ShipmentManifestTransferRepository
            shipmentManifestTransferRepository;

    private final PackageItemRepository
            packageItemRepository;


    // =====================================================
    // LIST
    // =====================================================

    @Transactional(readOnly = true)
    public InTransitShipmentPageResponse getShipments(
            String keyword,
            Long fromWarehouseId,
            Long toWarehouseId,
            String status,
            Integer page,
            Integer size
    ) {

        String normalizedKeyword =
                normalizeKeyword(
                        keyword
                );

        DeliveryStatus normalizedStatus =
                normalizeStatus(
                        status
                );

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

        Page<Deliveries> result =
                deliveryRepository
                        .searchInTransitShipments(
                                normalizedKeyword,
                                fromWarehouseId,
                                toWarehouseId,
                                normalizedStatus,
                                pageable
                        );

        List<InTransitShipmentListItemResponse> items =
                result
                        .getContent()
                        .stream()
                        .map(
                                this::toListItem
                        )
                        .toList();

        return InTransitShipmentPageResponse
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
    // DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public InTransitShipmentDetailResponse getShipmentDetail(
            Long deliveryId
    ) {

        if (deliveryId == null) {

            throw new BadRequest(
                    "Delivery id is required"
            );
        }

        Deliveries delivery =
                deliveryRepository
                        .findById(
                                deliveryId
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Delivery not found with id: "
                                                + deliveryId
                                )
                        );

        ShipmentManifests manifest =
                delivery.getManifest();

        List<ShipmentManifestTransfer> transferLinks =
                getTransferLinks(
                        delivery
                );

        List<DeliveryPackages> packageLinks =
                deliveryPackageRepository
                        .findByDeliveryId(
                                delivery.getId()
                        );

        List<InTransitTransferResponse> transfers =
                transferLinks
                        .stream()
                        .map(
                                this::toTransferResponse
                        )
                        .toList();

        List<InTransitPackageResponse> packages =
                packageLinks
                        .stream()
                        .map(
                                this::toPackageResponse
                        )
                        .toList();

        int totalProductQuantity =
                packages
                        .stream()
                        .map(
                                InTransitPackageResponse
                                        ::getTotalQuantity
                        )
                        .filter(
                                quantity ->
                                        quantity != null
                        )
                        .mapToInt(
                                Integer::intValue
                        )
                        .sum();

        LocalDateTime expectedDeliveryAt =
                resolveExpectedDeliveryAt(
                        delivery,
                        transferLinks
                );

        Warehouses fromWarehouse =
                delivery.getFromWarehouse();

        Warehouses toWarehouse =
                delivery.getToWarehouse();

        Users driver =
                delivery.getDriver();

        Users handedOverBy =
                delivery.getHandedOverBy();

        return InTransitShipmentDetailResponse
                .builder()

                // =================================================
                // DELIVERY
                // =================================================

                .deliveryId(
                        delivery.getId()
                )

                .deliveryCode(
                        delivery.getShipmentCode()
                )

                .deliveryStatus(
                        delivery.getDeliveryStatus() != null
                                ? delivery.getDeliveryStatus().name()
                                : null
                )


                // =================================================
                // MANIFEST
                // =================================================

                .manifestId(
                        manifest != null
                                ? manifest.getId()
                                : null
                )

                .manifestCode(
                        manifest != null
                                ? manifest.getManifestCode()
                                : null
                )

                .manifestStatus(
                        manifest != null
                                && manifest.getStatus() != null
                                ? manifest.getStatus().name()
                                : null
                )

                .manifestNote(
                        manifest != null
                                ? manifest.getNote()
                                : null
                )


                // =================================================
                // FROM
                // =================================================

                .fromWarehouseId(
                        fromWarehouse != null
                                ? fromWarehouse.getId()
                                : null
                )

                .fromWarehouseCode(
                        fromWarehouse != null
                                ? fromWarehouse.getWarehousesCode()
                                : null
                )

                .fromWarehouseName(
                        fromWarehouse != null
                                ? fromWarehouse.getName()
                                : null
                )

                .fromWarehouseAddress(
                        fromWarehouse != null
                                ? fromWarehouse.getAddress()
                                : null
                )


                // =================================================
                // TO
                // =================================================

                .toWarehouseId(
                        toWarehouse != null
                                ? toWarehouse.getId()
                                : null
                )

                .toWarehouseCode(
                        toWarehouse != null
                                ? toWarehouse.getWarehousesCode()
                                : null
                )

                .toWarehouseName(
                        toWarehouse != null
                                ? toWarehouse.getName()
                                : null
                )

                .toWarehouseAddress(
                        toWarehouse != null
                                ? toWarehouse.getAddress()
                                : null
                )


                // =================================================
                // DRIVER
                // =================================================

                .driverId(
                        driver != null
                                ? driver.getId()
                                : null
                )

                .driverCode(
                        driver != null
                                ? driver.getUserCode()
                                : null
                )

                .driverName(
                        driver != null
                                ? driver.getName()
                                : null
                )


                // =================================================
                // HANDOVER
                // =================================================

                .handedOverById(
                        handedOverBy != null
                                ? handedOverBy.getId()
                                : null
                )

                .handedOverByCode(
                        handedOverBy != null
                                ? handedOverBy.getUserCode()
                                : null
                )

                .handedOverByName(
                        handedOverBy != null
                                ? handedOverBy.getName()
                                : null
                )

                .handedOverAt(
                        delivery.getHandedOverAt()
                )


                // =================================================
                // TIME
                // =================================================

                .expectedPickupAt(
                        delivery.getExpectedPickupAt()
                )

                .expectedDeliveryAt(
                        expectedDeliveryAt
                )

                .startedAt(
                        delivery.getStartedAt()
                )

                .deliveredAt(
                        delivery.getDeliveredAt()
                )

                .createdAt(
                        delivery.getCreatedAt()
                )

                .updatedAt(
                        delivery.getUpdatedAt()
                )


                // =================================================
                // OVERDUE
                // =================================================

                .overdue(
                        isOverdue(
                                delivery,
                                expectedDeliveryAt
                        )
                )


                // =================================================
                // SUMMARY
                // =================================================

                .totalTransfers(
                        transfers.size()
                )

                .totalPackages(
                        packages.size()
                )

                .totalProductQuantity(
                        totalProductQuantity
                )


                // =================================================
                // DETAIL
                // =================================================

                .transfers(
                        transfers
                )

                .packages(
                        packages
                )

                .statusHistory(
                        buildStatusHistory(
                                delivery
                        )
                )

                .build();
    }


    // =====================================================
    // LIST ITEM MAPPING
    // =====================================================

    private InTransitShipmentListItemResponse toListItem(
            Deliveries delivery
    ) {

        List<ShipmentManifestTransfer> transferLinks =
                getTransferLinks(
                        delivery
                );

        List<DeliveryPackages> packageLinks =
                deliveryPackageRepository
                        .findByDeliveryId(
                                delivery.getId()
                        );

        List<String> transferCodes =
                transferLinks
                        .stream()
                        .map(
                                ShipmentManifestTransfer::getTransfer
                        )
                        .filter(
                                transfer ->
                                        transfer != null
                        )
                        .map(
                                StockTransfer::getTransferCode
                        )
                        .filter(
                                code ->
                                        code != null
                        )
                        .distinct()
                        .toList();

        List<String> packageCodes =
                packageLinks
                        .stream()
                        .map(
                                DeliveryPackages::getPackageEntity
                        )
                        .filter(
                                pack ->
                                        pack != null
                        )
                        .map(
                                Packages::getPackagesCode
                        )
                        .filter(
                                code ->
                                        code != null
                        )
                        .distinct()
                        .toList();

        int totalProductQuantity =
                packageLinks
                        .stream()
                        .map(
                                DeliveryPackages::getPackageEntity
                        )
                        .filter(
                                pack ->
                                        pack != null
                        )
                        .mapToInt(
                                pack ->
                                        calculatePackageQuantity(
                                                pack.getId()
                                        )
                        )
                        .sum();

        LocalDateTime expectedDeliveryAt =
                resolveExpectedDeliveryAt(
                        delivery,
                        transferLinks
                );

        Warehouses fromWarehouse =
                delivery.getFromWarehouse();

        Warehouses toWarehouse =
                delivery.getToWarehouse();

        ShipmentManifests manifest =
                delivery.getManifest();

        return InTransitShipmentListItemResponse
                .builder()

                .deliveryId(
                        delivery.getId()
                )

                .deliveryCode(
                        delivery.getShipmentCode()
                )

                .deliveryStatus(
                        delivery.getDeliveryStatus() != null
                                ? delivery.getDeliveryStatus().name()
                                : null
                )

                .manifestId(
                        manifest != null
                                ? manifest.getId()
                                : null
                )

                .manifestCode(
                        manifest != null
                                ? manifest.getManifestCode()
                                : null
                )

                .transferCodes(
                        transferCodes
                )

                .packageCodes(
                        packageCodes
                )

                .fromWarehouseId(
                        fromWarehouse != null
                                ? fromWarehouse.getId()
                                : null
                )

                .fromWarehouseCode(
                        fromWarehouse != null
                                ? fromWarehouse.getWarehousesCode()
                                : null
                )

                .fromWarehouseName(
                        fromWarehouse != null
                                ? fromWarehouse.getName()
                                : null
                )

                .toWarehouseId(
                        toWarehouse != null
                                ? toWarehouse.getId()
                                : null
                )

                .toWarehouseCode(
                        toWarehouse != null
                                ? toWarehouse.getWarehousesCode()
                                : null
                )

                .toWarehouseName(
                        toWarehouse != null
                                ? toWarehouse.getName()
                                : null
                )

                .startedAt(
                        delivery.getStartedAt()
                )

                .expectedDeliveryAt(
                        expectedDeliveryAt
                )

                .deliveredAt(
                        delivery.getDeliveredAt()
                )

                .overdue(
                        isOverdue(
                                delivery,
                                expectedDeliveryAt
                        )
                )

                .totalTransfers(
                        transferCodes.size()
                )

                .totalPackages(
                        packageCodes.size()
                )

                .totalProductQuantity(
                        totalProductQuantity
                )

                .build();
    }


    // =====================================================
    // TRANSFER
    // =====================================================

    private InTransitTransferResponse toTransferResponse(
            ShipmentManifestTransfer link
    ) {

        StockTransfer transfer =
                link.getTransfer();

        if (transfer == null) {

            return InTransitTransferResponse
                    .builder()
                    .build();
        }

        List<StockTransferItems> items =
                transfer.getItems() == null
                        ? List.of()
                        : transfer.getItems();

        List<InTransitTransferProductResponse> products =
                items
                        .stream()
                        .map(
                                this::toTransferProductResponse
                        )
                        .toList();

        int totalRequestedQuantity =
                items
                        .stream()
                        .map(
                                StockTransferItems::getRequestedQuantity
                        )
                        .filter(
                                quantity ->
                                        quantity != null
                        )
                        .mapToInt(
                                Integer::intValue
                        )
                        .sum();

        int totalShippedQuantity =
                items
                        .stream()
                        .map(
                                StockTransferItems::getShippedQuantity
                        )
                        .filter(
                                quantity ->
                                        quantity != null
                        )
                        .mapToInt(
                                Integer::intValue
                        )
                        .sum();

        int totalReceivedQuantity =
                items
                        .stream()
                        .map(
                                StockTransferItems::getReceivedQuantity
                        )
                        .filter(
                                quantity ->
                                        quantity != null
                        )
                        .mapToInt(
                                Integer::intValue
                        )
                        .sum();

        return InTransitTransferResponse
                .builder()

                .transferId(
                        transfer.getId()
                )

                .transferCode(
                        transfer.getTransferCode()
                )

                .transferStatus(
                        transfer.getStatus() != null
                                ? transfer.getStatus().name()
                                : null
                )

                .transferType(
                        transfer.getTransferType() != null
                                ? transfer.getTransferType().name()
                                : null
                )

                .expectedShipmentDate(
                        transfer.getExpectedShipmentDate()
                )

                .expectedReceiptDate(
                        transfer.getExpectedReceiptDate()
                )

                .totalProducts(
                        products.size()
                )

                .totalRequestedQuantity(
                        totalRequestedQuantity
                )

                .totalShippedQuantity(
                        totalShippedQuantity
                )

                .totalReceivedQuantity(
                        totalReceivedQuantity
                )

                .products(
                        products
                )

                .build();
    }


    private InTransitTransferProductResponse toTransferProductResponse(
            StockTransferItems item
    ) {

        Products product =
                item.getProduct();

        return InTransitTransferProductResponse
                .builder()

                .productId(
                        product != null
                                ? product.getId()
                                : null
                )

                .productCode(
                        product != null
                                ? product.getProductsCode()
                                : null
                )

                .barcode(
                        product != null
                                ? product.getBarcode()
                                : null
                )

                .productName(
                        product != null
                                ? product.getName()
                                : null
                )

                .baseUnit(
                        product != null
                                ? product.getBaseUnit()
                                : null
                )

                .requestedQuantity(
                        item.getRequestedQuantity()
                )

                .approvedQuantity(
                        item.getApprovedQuantity()
                )

                .packedQuantity(
                        item.getPackedQuantity()
                )

                .shippedQuantity(
                        item.getShippedQuantity()
                )

                .receivedQuantity(
                        item.getReceivedQuantity()
                )

                .build();
    }


    // =====================================================
    // PACKAGE
    // =====================================================

    private InTransitPackageResponse toPackageResponse(
            DeliveryPackages link
    ) {

        Packages pack =
                link.getPackageEntity();

        if (pack == null) {

            return InTransitPackageResponse
                    .builder()
                    .build();
        }

        List<PackageItems> packageItems =
                packageItemRepository
                        .findByPackageEntityId(
                                pack.getId()
                        );

        List<ShipmentManifestProductResponse> products =
                packageItems
                        .stream()
                        .map(
                                this::toPackageProductResponse
                        )
                        .toList();

        int totalQuantity =
                packageItems
                        .stream()
                        .map(
                                PackageItems::getQuantity
                        )
                        .filter(
                                quantity ->
                                        quantity != null
                        )
                        .mapToInt(
                                Integer::intValue
                        )
                        .sum();

        return InTransitPackageResponse
                .builder()

                .packageId(
                        pack.getId()
                )

                .packageCode(
                        pack.getPackagesCode()
                )

                .sealNumber(
                        pack.getSealNumber()
                )

                .packageStatus(
                        pack.getStatus() != null
                                ? pack.getStatus().name()
                                : null
                )

                .packedAt(
                        pack.getPackedAt()
                )

                .sealedAt(
                        pack.getSealedAt()
                )

                .totalProducts(
                        products.size()
                )

                .totalQuantity(
                        totalQuantity
                )

                .products(
                        products
                )

                .build();
    }


    private ShipmentManifestProductResponse toPackageProductResponse(
            PackageItems item
    ) {

        Products product =
                item.getProduct();

        return ShipmentManifestProductResponse
                .builder()

                .productId(
                        product != null
                                ? product.getId()
                                : null
                )

                .productCode(
                        product != null
                                ? product.getProductsCode()
                                : null
                )

                .barcode(
                        product != null
                                ? product.getBarcode()
                                : null
                )

                .productName(
                        product != null
                                ? product.getName()
                                : null
                )

                .baseUnit(
                        product != null
                                ? product.getBaseUnit()
                                : null
                )

                .quantity(
                        item.getQuantity()
                )

                .build();
    }


    // =====================================================
    // STATUS HISTORY
    // =====================================================

    private List<InTransitStatusHistoryResponse> buildStatusHistory(
            Deliveries delivery
    ) {

        List<InTransitStatusHistoryResponse> history =
                new ArrayList<>();


        /*
         * CREATED luôn có thể xác định từ createdAt.
         */
        addHistory(
                history,
                DeliveryStatus.CREATED.name(),
                delivery.getCreatedAt(),
                null,
                "Delivery created"
        );


        /*
         * delivery_assignment_history lưu lịch sử phân công
         * tài xế thật của Delivery.
         *
         * driver là người ĐƯỢC phân công, không phải chắc chắn
         * là người THỰC HIỆN thao tác phân công, vì vậy changedBy
         * để null để tránh gán sai nghĩa dữ liệu.
         */
        List<DeliveryAssignmentHistory> assignments =
                deliveryAssignmentHistoryRepository
                        .findByDelivery_IdOrderByAssignedAtAsc(
                                delivery.getId()
                        );

        for (DeliveryAssignmentHistory assignment : assignments) {

            Users assignedDriver =
                    assignment.getDriver();

            String note =
                    assignedDriver != null
                            ? "Assigned to driver "
                                    + assignedDriver.getName()
                            : "Driver assigned";

            addHistory(
                    history,
                    DeliveryStatus.ASSIGNED.name(),
                    assignment.getAssignedAt(),
                    null,
                    note
            );
        }


        /*
         * Khi startedAt tồn tại thì hệ thống đã bắt đầu
         * vận chuyển thực tế.
         */
        if (delivery.getStartedAt() != null) {

            addHistory(
                    history,
                    DeliveryStatus.IN_TRANSIT.name(),
                    delivery.getStartedAt(),
                    delivery.getHandedOverBy(),
                    "Shipment started"
            );
        }


        /*
         * Khi có deliveredAt thì đây là thời điểm
         * bàn giao hoàn tất.
         */
        if (delivery.getDeliveredAt() != null) {

            addHistory(
                    history,
                    DeliveryStatus.DELIVERED.name(),
                    delivery.getDeliveredAt(),
                    null,
                    "Shipment delivered"
            );
        }


        /*
         * Schema hiện tại không có bảng lưu toàn bộ
         * lịch sử chuyển trạng thái Delivery.
         *
         * ASSIGNED đã được lấy từ delivery_assignment_history.
         * Với các trạng thái READY_TO_SHIP, ARRIVED,
         * FAILED, CANCELLED chưa có bảng lịch sử trạng thái
         * đầy đủ, nên chỉ dùng updatedAt cho snapshot
         * trạng thái hiện tại nếu chưa có milestone trên.
         */
        if (delivery.getDeliveryStatus() != null
                && !containsStatus(
                        history,
                        delivery.getDeliveryStatus().name()
                )) {

            addHistory(
                    history,
                    delivery.getDeliveryStatus().name(),
                    delivery.getUpdatedAt(),
                    null,
                    "Current delivery status"
            );
        }


        history.sort(
                Comparator.comparing(
                        InTransitStatusHistoryResponse::getChangedAt,
                        Comparator.nullsLast(
                                Comparator.naturalOrder()
                        )
                )
        );

        return history;
    }


    private void addHistory(
            List<InTransitStatusHistoryResponse> history,
            String status,
            LocalDateTime changedAt,
            Users changedBy,
            String note
    ) {

        history.add(
                InTransitStatusHistoryResponse
                        .builder()

                        .status(
                                status
                        )

                        .changedAt(
                                changedAt
                        )

                        .changedById(
                                changedBy != null
                                        ? changedBy.getId()
                                        : null
                        )

                        .changedByCode(
                                changedBy != null
                                        ? changedBy.getUserCode()
                                        : null
                        )

                        .changedByName(
                                changedBy != null
                                        ? changedBy.getName()
                                        : null
                        )

                        .note(
                                note
                        )

                        .build()
        );
    }


    private boolean containsStatus(
            List<InTransitStatusHistoryResponse> history,
            String status
    ) {

        return history
                .stream()
                .anyMatch(
                        item ->
                                status.equals(
                                        item.getStatus()
                                )
                );
    }


    // =====================================================
    // HELPERS
    // =====================================================

    private List<ShipmentManifestTransfer> getTransferLinks(
            Deliveries delivery
    ) {

        ShipmentManifests manifest =
                delivery.getManifest();

        if (manifest == null
                || manifest.getId() == null) {

            return List.of();
        }

        return shipmentManifestTransferRepository
                .findByManifest_Id(
                        manifest.getId()
                );
    }


    private int calculatePackageQuantity(
            Long packageId
    ) {

        if (packageId == null) {

            return 0;
        }

        return packageItemRepository
                .findByPackageEntityId(
                        packageId
                )
                .stream()
                .map(
                        PackageItems::getQuantity
                )
                .filter(
                        quantity ->
                                quantity != null
                )
                .mapToInt(
                        Integer::intValue
                )
                .sum();
    }


    /*
     * Delivery mới sẽ dùng deliveries.expected_delivery_at.
     *
     * Với dữ liệu cũ chưa có field này,
     * fallback sang expectedReceiptDate của Stock Transfer.
     */
    private LocalDateTime resolveExpectedDeliveryAt(
            Deliveries delivery,
            List<ShipmentManifestTransfer> transferLinks
    ) {

        if (delivery.getExpectedDeliveryAt() != null) {

            return delivery.getExpectedDeliveryAt();
        }

        LocalDate latestExpectedReceiptDate =
                transferLinks
                        .stream()

                        .map(
                                ShipmentManifestTransfer::getTransfer
                        )

                        .filter(
                                transfer ->
                                        transfer != null
                        )

                        .map(
                                StockTransfer::getExpectedReceiptDate
                        )

                        .filter(
                                date ->
                                        date != null
                        )

                        .max(
                                Comparator.naturalOrder()
                        )

                        .orElse(
                                null
                        );

        if (latestExpectedReceiptDate == null) {

            return null;
        }

        /*
         * expectedReceiptDate chỉ có DATE.
         *
         * Xem cuối ngày đó là deadline để tránh
         * cảnh báo quá hạn ngay từ 00:00.
         */
        return latestExpectedReceiptDate
                .atTime(
                        LocalTime.MAX
                );
    }


    private boolean isOverdue(
            Deliveries delivery,
            LocalDateTime expectedDeliveryAt
    ) {

        if (expectedDeliveryAt == null) {

            return false;
        }

        DeliveryStatus status =
                delivery.getDeliveryStatus();

        /*
         * Các trạng thái kết thúc không cảnh báo overdue.
         */
        if (status == DeliveryStatus.DELIVERED
                || status == DeliveryStatus.FAILED
                || status == DeliveryStatus.CANCELLED) {

            return false;
        }

        return LocalDateTime
                .now()
                .isAfter(
                        expectedDeliveryAt
                );
    }


    // =====================================================
    // NORMALIZE STATUS
    // =====================================================

    private DeliveryStatus normalizeStatus(
            String status
    ) {

        if (status == null
                || status.isBlank()) {

            return null;
        }

        String normalized =
                status
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
                    "Invalid delivery status. "
                            + "Allowed values: "
                            + "CREATED, ASSIGNED, READY_TO_SHIP, "
                            + "IN_TRANSIT, ARRIVED, DELIVERED, "
                            + "FAILED, CANCELLED"
            );
        }
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

        return Math.min(
                size,
                MAX_PAGE_SIZE
        );
    }
}