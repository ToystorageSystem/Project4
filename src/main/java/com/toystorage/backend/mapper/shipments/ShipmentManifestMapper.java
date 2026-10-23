package com.toystorage.backend.mapper.shipments;

import com.toystorage.backend.dto.response.shipments.ShipmentManifestDetailResponse;
import com.toystorage.backend.dto.response.shipments.ShipmentManifestListItemResponse;
import com.toystorage.backend.dto.response.shipments.ShipmentManifestPackageResponse;
import com.toystorage.backend.dto.response.shipments.ShipmentManifestProductResponse;
import com.toystorage.backend.dto.response.shipments.ShipmentManifestTransferResponse;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.shipments.ShipmentManifestPackage;
import com.toystorage.backend.entity.shipments.ShipmentManifestTransfer;
import com.toystorage.backend.entity.shipments.ShipmentManifests;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.transfers.StockTransferItems;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;

import com.toystorage.backend.repository.packages.packing.PackageItemRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ShipmentManifestMapper {

    private final PackageItemRepository
            packageItemRepository;


    // =====================================================
    // LIST ITEM
    // =====================================================

    public ShipmentManifestListItemResponse toListItemResponse(
            ShipmentManifests manifest,
            Deliveries delivery,
            long totalTransfers,
            long totalPackages
    ) {

        Warehouses fromWarehouse =
                manifest.getFromWarehouse();

        Warehouses toWarehouse =
                manifest.getToWarehouse();

        Users createdBy =
                manifest.getCreatedBy();


        return ShipmentManifestListItemResponse
                .builder()

                // =================================================
                // MANIFEST
                // =================================================

                .manifestId(
                        manifest.getId()
                )

                .manifestCode(
                        manifest.getManifestCode()
                )

                .manifestStatus(
                        manifest.getStatus() != null
                                ? manifest.getStatus().name()
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


                // =================================================
                // CREATED BY
                // =================================================

                .createdById(
                        createdBy != null
                                ? createdBy.getId()
                                : null
                )

                .createdByCode(
                        createdBy != null
                                ? createdBy.getUserCode()
                                : null
                )

                .createdByName(
                        createdBy != null
                                ? createdBy.getName()
                                : null
                )

                .createdAt(
                        manifest.getCreatedAt()
                )


                // =================================================
                // SUMMARY
                // =================================================

                .totalTransfers(
                        safeLongToInteger(totalTransfers)
                )

                .totalPackages(
                        safeLongToInteger(totalPackages)
                )


                // =================================================
                // TRANSPORT
                // =================================================

                .transportStatus(
                        delivery != null
                                && delivery.getDeliveryStatus() != null
                                ? delivery.getDeliveryStatus().name()
                                : null
                )

                .build();
    }


    // =====================================================
    // DETAIL
    // =====================================================

    public ShipmentManifestDetailResponse toDetailResponse(
            ShipmentManifests manifest,
            Deliveries delivery,
            List<ShipmentManifestTransfer> transferLinks,
            List<ShipmentManifestPackage> packageLinks
    ) {

        Warehouses fromWarehouse =
                manifest.getFromWarehouse();

        Warehouses toWarehouse =
                manifest.getToWarehouse();

        Users createdBy =
                manifest.getCreatedBy();

        Users driver =
                delivery != null
                        ? delivery.getDriver()
                        : null;


        /*
         * Map transfer list.
         */
        List<ShipmentManifestTransferResponse> transfers =
                transferLinks == null
                        ? List.of()
                        : transferLinks.stream()
                                .map(this::toTransferResponse)
                                .toList();


        /*
         * Map package list.
         */
        List<ShipmentManifestPackageResponse> packages =
                packageLinks == null
                        ? List.of()
                        : packageLinks.stream()
                                .map(link ->
                                        toPackageResponse(
                                                link.getPackageEntity()
                                        )
                                )
                                .toList();


        /*
         * Tổng số lượng sản phẩm của toàn bộ kiện hàng.
         */
        int totalProductQuantity =
                packages.stream()
                        .map(
                                ShipmentManifestPackageResponse
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


        return ShipmentManifestDetailResponse
                .builder()

                // =================================================
                // MANIFEST
                // =================================================

                .manifestId(
                        manifest.getId()
                )

                .manifestCode(
                        manifest.getManifestCode()
                )

                .manifestStatus(
                        manifest.getStatus() != null
                                ? manifest.getStatus().name()
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
                // CREATED BY
                // =================================================

                .createdById(
                        createdBy != null
                                ? createdBy.getId()
                                : null
                )

                .createdByCode(
                        createdBy != null
                                ? createdBy.getUserCode()
                                : null
                )

                .createdByName(
                        createdBy != null
                                ? createdBy.getName()
                                : null
                )

                .createdAt(
                        manifest.getCreatedAt()
                )


                // =================================================
                // ADDITIONAL INFORMATION
                // =================================================

                .note(
                        manifest.getNote()
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
                // DELIVERY / TRANSPORT
                // =================================================

                .deliveryId(
                        delivery != null
                                ? delivery.getId()
                                : null
                )

                .deliveryCode(
                        delivery != null
                                ? delivery.getShipmentCode()
                                : null
                )

                .transportStatus(
                        delivery != null
                                && delivery.getDeliveryStatus() != null
                                ? delivery.getDeliveryStatus().name()
                                : null
                )

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

                .startedAt(
                        delivery != null
                                ? delivery.getStartedAt()
                                : null
                )

                .deliveredAt(
                        delivery != null
                                ? delivery.getDeliveredAt()
                                : null
                )


                // =================================================
                // CHILD DATA
                // =================================================

                .transfers(
                        transfers
                )

                .packages(
                        packages
                )

                .build();
    }


    // =====================================================
    // TRANSFER
    // =====================================================

    private ShipmentManifestTransferResponse toTransferResponse(
            ShipmentManifestTransfer link
    ) {

        StockTransfer transfer =
                link.getTransfer();

        if (transfer == null) {
            return ShipmentManifestTransferResponse
                    .builder()
                    .build();
        }


        List<StockTransferItems> items =
                transfer.getItems();


        int totalProducts =
                items != null
                        ? items.size()
                        : 0;


        int totalRequestedQuantity =
                items == null
                        ? 0
                        : items.stream()
                                .map(
                                        StockTransferItems
                                                ::getRequestedQuantity
                                )
                                .filter(
                                        quantity ->
                                                quantity != null
                                )
                                .mapToInt(
                                        Integer::intValue
                                )
                                .sum();


        return ShipmentManifestTransferResponse
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
                        totalProducts
                )

                .totalRequestedQuantity(
                        totalRequestedQuantity
                )

                .build();
    }


    // =====================================================
    // PACKAGE
    // =====================================================

    private ShipmentManifestPackageResponse toPackageResponse(
            Packages packageEntity
    ) {

        if (packageEntity == null) {
            return ShipmentManifestPackageResponse
                    .builder()
                    .build();
        }


        List<PackageItems> items =
                packageItemRepository
                        .findByPackageEntityId(
                                packageEntity.getId()
                        );


        List<ShipmentManifestProductResponse> products =
                items.stream()
                        .map(this::toProductResponse)
                        .toList();


        int totalQuantity =
                items.stream()
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


        Users packedBy =
                packageEntity.getPackedBy();


        return ShipmentManifestPackageResponse
                .builder()

                .packageId(
                        packageEntity.getId()
                )

                .packageCode(
                        packageEntity.getPackagesCode()
                )

                .sealNumber(
                        packageEntity.getSealNumber()
                )

                .packageStatus(
                        packageEntity.getStatus() != null
                                ? packageEntity.getStatus().name()
                                : null
                )

                .packedById(
                        packedBy != null
                                ? packedBy.getId()
                                : null
                )

                .packedByCode(
                        packedBy != null
                                ? packedBy.getUserCode()
                                : null
                )

                .packedByName(
                        packedBy != null
                                ? packedBy.getName()
                                : null
                )

                .packedAt(
                        packageEntity.getPackedAt()
                )

                .sealedAt(
                        packageEntity.getSealedAt()
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


    // =====================================================
    // PRODUCT
    // =====================================================

    private ShipmentManifestProductResponse toProductResponse(
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
    // UTILITY
    // =====================================================

    private Integer safeLongToInteger(
            long value
    ) {

        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) value;
    }
}