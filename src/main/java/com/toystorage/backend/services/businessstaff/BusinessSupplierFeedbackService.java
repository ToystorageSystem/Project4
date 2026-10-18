
package com.toystorage.backend.services.businessstaff;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toystorage.backend.dto.request.businessstaff.SupplierFeedbackRequest;
import com.toystorage.backend.dto.request.businessstaff.SupplierItemFeedbackRequest;
import com.toystorage.backend.dto.response.businessstaff.SupplierFeedbackResponse;
import com.toystorage.backend.entity.receipts.PurchaseOrders;
import com.toystorage.backend.entity.receipts.PurchaseOrderItems;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.receipts.PurchaseOrderStatus;
import com.toystorage.backend.enums.suppliers.SupplierResponseStatus;
import com.toystorage.backend.enums.suppliers.SupplierItemResponseStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.repository.businessstaff.BusinessPurchaseOrderRepository;
import com.toystorage.backend.repository.businessstaff.BusinessPurchaseOrderItemRepository;
import com.toystorage.backend.repository.businessstaff.BusinessSupplierAuditRepository;
import com.toystorage.backend.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BusinessSupplierFeedbackService {

    private final BusinessPurchaseOrderRepository orderRepository;
    private final BusinessPurchaseOrderItemRepository itemRepository;
    private final BusinessSupplierAuditRepository auditRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public SupplierFeedbackResponse recordFeedback(
            Long purchaseOrderId,
            SupplierFeedbackRequest request,
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new Unauthorized("User is not authenticated");
        }

        Users actor = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new NotFound("Authenticated user not found")
                );

        PurchaseOrders order = orderRepository
                .findLockedById(purchaseOrderId)
                .orElseThrow(() ->
                        new NotFound("Purchase Order not found: "
                                + purchaseOrderId)
                );

        if (order.getStatus() != PurchaseOrderStatus.ORDERED
                && order.getStatus()
                    != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new BadRequest(
                    "Supplier feedback is allowed only for "
                    + "ORDERED or PARTIALLY_RECEIVED orders"
            );
        }

        List<PurchaseOrderItems> orderItems =
                itemRepository
                        .findByPurchaseOrder_IdOrderByIdAsc(
                                purchaseOrderId
                        );

        if (orderItems.isEmpty()) {
            throw new BadRequest(
                    "Purchase Order has no items"
            );
        }

        String oldValue = snapshot(order, orderItems);

        Map<Long, PurchaseOrderItems> itemMap = new HashMap<>();

        for (PurchaseOrderItems item : orderItems) {
            itemMap.put(item.getId(), item);
        }

        Set<Long> processedIds = new HashSet<>();

        if (request.items() != null) {
            for (SupplierItemFeedbackRequest feedback
                    : request.items()) {

                if (feedback == null) {
                    throw new BadRequest(
                            "Supplier item feedback cannot be null"
                    );
                }

                if (!processedIds.add(
                        feedback.purchaseOrderItemId())) {
                    throw new BadRequest(
                            "Duplicate Purchase Order item: "
                            + feedback.purchaseOrderItemId()
                    );
                }

                PurchaseOrderItems item = itemMap.get(
                        feedback.purchaseOrderItemId()
                );

                if (item == null) {
                    throw new BadRequest(
                            "Item does not belong to Purchase Order: "
                            + feedback.purchaseOrderItemId()
                    );
                }

                validateItem(item, feedback);

                item.setSupplierResponseStatus(
                        feedback.supplierResponseStatus()
                );

                if (feedback.confirmedQuantity() != null) {
                    item.setConfirmedQuantity(
                            feedback.confirmedQuantity()
                    );
                }

                if (feedback.confirmedUnitPrice() != null) {
                    item.setConfirmedUnitPrice(
                            feedback.confirmedUnitPrice()
                    );
                }

                if (feedback.supplierResponseNote() != null) {
                    item.setSupplierResponseNote(
                            feedback.supplierResponseNote()
                    );
                }
            }
        }

        // Kiem tra trang thai tong sau khi cap nhat cac item
        validateOrderStatus(
                request.supplierResponseStatus(),
                orderItems
        );

        order.setSupplierResponseStatus(
                request.supplierResponseStatus()
        );

        if (request.supplierResponseNote() != null) {
            order.setSupplierResponseNote(
                    request.supplierResponseNote()
            );
        }

        order.setSupplierRespondedAt(
                request.supplierResponseStatus()
                        == SupplierResponseStatus.PENDING_RESPONSE
                        ? null
                        : LocalDateTime.now()
        );

        String newValue = snapshot(order, orderItems);

        String activityCode =
                "AL-" + UUID.randomUUID()
                        .toString()
                        .replace("-", "");

        auditRepository.insertFeedbackLog(
                actor.getId(),
                order.getId(),
                oldValue,
                newValue,
                activityCode
        );

        return toResponse(order, orderItems);
    }

    private void validateItem(
            PurchaseOrderItems item,
            SupplierItemFeedbackRequest feedback
    ) {
        Integer quantity = feedback.confirmedQuantity() != null
                ? feedback.confirmedQuantity()
                : item.getConfirmedQuantity();

        BigDecimal price = feedback.confirmedUnitPrice() != null
                ? feedback.confirmedUnitPrice()
                : item.getConfirmedUnitPrice();

        SupplierItemResponseStatus status =
                feedback.supplierResponseStatus();

        int ordered = item.getOrderedQuantity();

        int received = item.getReceivedQuantity() == null
                ? 0
                : item.getReceivedQuantity();

        // 1. Kiem tra so luong co ban
        if (quantity != null) {

            if (quantity < received) {
                throw new BadRequest(
                        "Confirmed quantity cannot be less "
                        + "than already received quantity"
                );
            }

            if (quantity > ordered) {
                throw new BadRequest(
                        "Confirmed quantity exceeds ordered quantity"
                );
            }
        }

        // 2. Kiem tra gia xac nhan
        if (price != null
                && price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequest(
                    "Confirmed unit price must be greater than zero"
            );
        }

        // 3. NCC co the cung cap du hang
        if (status == SupplierItemResponseStatus.AVAILABLE) {

            if (quantity == null || quantity != ordered) {
                throw new BadRequest(
                        "AVAILABLE requires confirmed quantity "
                        + "equal to ordered quantity"
                );
            }
        }

        // 4. NCC chi cung cap mot phan
        if (status == SupplierItemResponseStatus.PARTIALLY_AVAILABLE) {

            if (quantity == null
                    || quantity <= 0
                    || quantity >= ordered) {
                throw new BadRequest(
                        "PARTIALLY_AVAILABLE requires confirmed "
                        + "quantity between zero and ordered quantity"
                );
            }
        }

        // 5. NCC het hang hoac ngung cung cap
        // Khong the xac nhan it hon so luong da nhan.
        // Neu khong the giao them, quantity = received.
        if (status == SupplierItemResponseStatus.OUT_OF_STOCK
                || status == SupplierItemResponseStatus.DISCONTINUED) {

            if (quantity == null || quantity != received) {
                throw new BadRequest(
                        status + " requires confirmed quantity "
                        + "equal to already received quantity"
                );
            }
        }

        // 6. NCC bao gia moi
        if (status == SupplierItemResponseStatus.PRICE_CHANGED) {

            if (price == null) {
                throw new BadRequest(
                        "New price is required for PRICE_CHANGED"
                );
            }

            if (item.getUnitPrice() != null
                    && price.compareTo(item.getUnitPrice()) == 0) {
                throw new BadRequest(
                        "PRICE_CHANGED requires a price different "
                        + "from the current PO unit price"
                );
            }
        }
    }

    private void validateOrderStatus(
            SupplierResponseStatus status,
            List<PurchaseOrderItems> items
    ) {
        if (status == null) {
            throw new BadRequest(
                    "Supplier response status is required"
            );
        }

        switch (status) {

            case PENDING_RESPONSE -> {
                boolean allPending = items.stream()
                        .allMatch(item ->
                                item.getSupplierResponseStatus()
                                        == SupplierItemResponseStatus.PENDING
                        );

                if (!allPending) {
                    throw new BadRequest(
                            "PENDING_RESPONSE requires all items "
                            + "to have PENDING status"
                    );
                }
            }

            case AVAILABLE -> {
                boolean allAvailable = items.stream()
                        .allMatch(item ->
                                item.getSupplierResponseStatus()
                                        == SupplierItemResponseStatus.AVAILABLE
                        );

                if (!allAvailable) {
                    throw new BadRequest(
                            "AVAILABLE requires all items "
                            + "to have AVAILABLE status"
                    );
                }
            }

            case PARTIALLY_AVAILABLE -> {
                boolean hasShortage = items.stream()
                        .anyMatch(item ->
                                item.getSupplierResponseStatus()
                                        == SupplierItemResponseStatus.PARTIALLY_AVAILABLE
                                || item.getSupplierResponseStatus()
                                        == SupplierItemResponseStatus.OUT_OF_STOCK
                                || item.getSupplierResponseStatus()
                                        == SupplierItemResponseStatus.DISCONTINUED
                        );

                boolean hasSupply = items.stream()
                        .anyMatch(item -> {

                            SupplierItemResponseStatus itemStatus =
                                    item.getSupplierResponseStatus();

                            Integer confirmed =
                                    item.getConfirmedQuantity();

                            boolean supplyStatus =
                                    itemStatus == SupplierItemResponseStatus.AVAILABLE
                                    || itemStatus == SupplierItemResponseStatus.PARTIALLY_AVAILABLE
                                    || itemStatus == SupplierItemResponseStatus.PRICE_CHANGED;

                            return supplyStatus
                                    && confirmed != null
                                    && confirmed > 0;
                        });

                if (!hasShortage || !hasSupply) {
                    throw new BadRequest(
                            "PARTIALLY_AVAILABLE requires at least "
                            + "one limited item and one item "
                            + "with confirmed available quantity"
                    );
                }
            }

            case OUT_OF_STOCK -> {
                boolean allOutOfStock = items.stream()
                        .allMatch(item ->
                                item.getSupplierResponseStatus()
                                        == SupplierItemResponseStatus.OUT_OF_STOCK
                        );

                if (!allOutOfStock) {
                    throw new BadRequest(
                            "OUT_OF_STOCK requires all items "
                            + "to have OUT_OF_STOCK status"
                    );
                }
            }

            case DISCONTINUED -> {
                boolean hasDiscontinued = items.stream()
                        .anyMatch(item ->
                                item.getSupplierResponseStatus()
                                        == SupplierItemResponseStatus.DISCONTINUED
                        );

                if (!hasDiscontinued) {
                    throw new BadRequest(
                            "DISCONTINUED requires at least "
                            + "one discontinued item"
                    );
                }
            }

            case PRICE_CHANGED -> {
                boolean hasPriceChanged = items.stream()
                        .anyMatch(item ->
                                item.getSupplierResponseStatus()
                                        == SupplierItemResponseStatus.PRICE_CHANGED
                        );

                if (!hasPriceChanged) {
                    throw new BadRequest(
                            "PRICE_CHANGED requires at least "
                            + "one item with PRICE_CHANGED status"
                    );
                }
            }

            case DELAYED, DECLINED -> {
                // Item-level enum khong co DELAYED hoac DECLINED.
                // Giu nguyen cach xu ly hien tai.
            }
        }
    }

    private String snapshot(
            PurchaseOrders order,
            List<PurchaseOrderItems> items
    ) {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("purchaseOrderId", order.getId());

        data.put("supplierResponseStatus",
                order.getSupplierResponseStatus());

        data.put("supplierResponseNote",
                order.getSupplierResponseNote());

        data.put("supplierRespondedAt",
                order.getSupplierRespondedAt() == null
                        ? null
                        : order.getSupplierRespondedAt().toString());

        List<Map<String, Object>> itemData = new ArrayList<>();

        for (PurchaseOrderItems item : items) {
            Map<String, Object> row = new LinkedHashMap<>();

            row.put("itemId", item.getId());

            row.put("supplierResponseStatus",
                    item.getSupplierResponseStatus());

            row.put("confirmedQuantity",
                    item.getConfirmedQuantity());

            row.put("confirmedUnitPrice",
                    item.getConfirmedUnitPrice());

            row.put("supplierResponseNote",
                    item.getSupplierResponseNote());

            itemData.add(row);
        }

        data.put("items", itemData);

        return objectMapper.valueToTree(data).toString();
    }

    private SupplierFeedbackResponse toResponse(
            PurchaseOrders order,
            List<PurchaseOrderItems> items
    ) {
        List<SupplierFeedbackResponse.ItemResponse> results =
                items.stream()
                        .map(item ->
                                new SupplierFeedbackResponse.ItemResponse(
                                        item.getId(),
                                        item.getProduct().getId(),
                                        item.getSupplierResponseStatus(),
                                        item.getConfirmedQuantity(),
                                        item.getUnitPrice(),
                                        item.getConfirmedUnitPrice(),
                                        item.getSupplierResponseNote()
                                )
                        )
                        .toList();

        return new SupplierFeedbackResponse(
                order.getId(),
                order.getOrderCode(),
                order.getSupplierResponseStatus(),
                order.getSupplierResponseNote(),
                order.getSupplierRespondedAt(),
                results
        );
    }
}
