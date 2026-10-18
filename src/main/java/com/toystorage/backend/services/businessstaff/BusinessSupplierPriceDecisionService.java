
package com.toystorage.backend.services.businessstaff;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toystorage.backend.dto.request.businessstaff.SupplierPriceDecisionRequest;
import com.toystorage.backend.dto.response.businessstaff.SupplierPriceDecisionResponse;
import com.toystorage.backend.entity.receipts.PurchaseOrders;
import com.toystorage.backend.entity.receipts.PurchaseOrderItems;
import com.toystorage.backend.entity.suppliers.SupplierProducts;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.receipts.PurchaseOrderStatus;
import com.toystorage.backend.enums.suppliers.SupplierItemResponseStatus;
import com.toystorage.backend.enums.suppliers.SupplierPriceDecisionAction;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.repository.businessstaff.BusinessPurchaseOrderRepository;
import com.toystorage.backend.repository.businessstaff.BusinessPurchaseOrderItemRepository;
import com.toystorage.backend.repository.businessstaff.BusinessSupplierPriceDecisionAuditRepository;
import com.toystorage.backend.repository.businessstaff.BusinessSupplierPriceProductRepository;
import com.toystorage.backend.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessSupplierPriceDecisionService {

    private final BusinessPurchaseOrderRepository orderRepository;
    private final BusinessPurchaseOrderItemRepository itemRepository;
    private final BusinessSupplierPriceProductRepository supplierProductRepository;
    private final BusinessSupplierPriceDecisionAuditRepository auditRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public SupplierPriceDecisionResponse decidePrice(
            Long purchaseOrderId,
            Long purchaseOrderItemId,
            SupplierPriceDecisionRequest request,
            Authentication authentication
    ) {

        // 1. Xac thuc nguoi thao tac
        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new Unauthorized("User is not authenticated");
        }

        Users actor = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new NotFound("Authenticated user not found")
                );

        if (request == null || request.decision() == null) {
            throw new BadRequest("Price decision is required");
        }

        String reason = request.reason() == null
                ? ""
                : request.reason().trim();

        if (reason.isBlank()) {
            throw new BadRequest("Decision reason is required");
        }

        SupplierPriceDecisionAction decision =
                request.decision();

        // 2. Khoa Purchase Order de tranh cap nhat dong thoi
        PurchaseOrders order = orderRepository
                .findLockedById(purchaseOrderId)
                .orElseThrow(() ->
                        new NotFound(
                                "Purchase Order not found: "
                                + purchaseOrderId
                        )
                );

        if (order.getStatus() != PurchaseOrderStatus.ORDERED) {
            throw new BadRequest(
                    "Price decision is allowed only for ORDERED orders"
            );
        }

        // 3. Lay cac item va kiem tra hang da nhan
        List<PurchaseOrderItems> orderItems =
                itemRepository
                        .findByPurchaseOrder_IdOrderByIdAsc(
                                purchaseOrderId
                        );

        boolean hasReceivedGoods = orderItems.stream()
                .anyMatch(item ->
                        item.getReceivedQuantity() != null
                        && item.getReceivedQuantity() > 0
                );

        if (hasReceivedGoods) {
            throw new BadRequest(
                    "Cannot decide a new price for a Purchase Order "
                    + "that has already received goods"
            );
        }

        // 4. Kiem tra item thuoc dung Purchase Order
        PurchaseOrderItems item = orderItems.stream()
                .filter(current ->
                        current.getId().equals(purchaseOrderItemId)
                )
                .findFirst()
                .orElseThrow(() ->
                        new NotFound(
                                "Purchase Order item not found in order: "
                                + purchaseOrderItemId
                        )
                );

        // 5. Chi xu ly bao gia da duoc ghi nhan la PRICE_CHANGED
        if (item.getSupplierResponseStatus()
                != SupplierItemResponseStatus.PRICE_CHANGED) {
            throw new BadRequest(
                    "Item must have PRICE_CHANGED status"
            );
        }

        BigDecimal originalPrice = item.getUnitPrice();
        BigDecimal quotedPrice = item.getConfirmedUnitPrice();

        if (originalPrice == null || quotedPrice == null) {
            throw new BadRequest(
                    "Original price and confirmed price are required"
            );
        }

        if (quotedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequest(
                    "Confirmed unit price must be greater than zero"
            );
        }

        if (quotedPrice.compareTo(originalPrice) == 0) {
            throw new BadRequest(
                    "Confirmed price must differ from current PO price"
            );
        }

        // MySQL dang luu gia voi 2 chu so thap phan.
        try {
            quotedPrice = quotedPrice.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException ex) {
            throw new BadRequest(
                    "Confirmed price must have at most 2 decimal places"
            );
        }

        String normalizedQuotedPrice = quotedPrice.toPlainString();

        // 6. Ngan quyet dinh trung cung item va cung muc gia
        long existingDecisions =
                auditRepository.countExistingDecision(
                        purchaseOrderId,
                        purchaseOrderItemId,
                        normalizedQuotedPrice
                );

        if (existingDecisions > 0) {
            throw new BadRequest(
                    "A decision already exists for this item and price"
            );
        }

        // 7. Tim gia mua hien tai cua dung NCC va san pham
        SupplierProducts supplierProduct =
                supplierProductRepository
                        .findLockedBySupplierAndProduct(
                                order.getSupplier().getId(),
                                item.getProduct().getId()
                        )
                        .orElse(null);

        if (decision == SupplierPriceDecisionAction.ACCEPT
                && supplierProduct == null) {
            throw new BadRequest(
                    "Supplier product mapping not found. "
                    + "Cannot accept the new price"
            );
        }

        BigDecimal previousSupplierPrice =
                supplierProduct == null
                        ? null
                        : supplierProduct.getPurchasePrice();

        // 8. Luu trang thai truoc khi quyet dinh
        Map<String, Object> oldValue = new LinkedHashMap<>();

        oldValue.put("purchaseOrderId", order.getId());
        oldValue.put("purchaseOrderItemId", item.getId());
        oldValue.put("unitPrice", originalPrice.toPlainString());
        oldValue.put(
                "supplierPurchasePrice",
                decimalText(previousSupplierPrice)
        );

        // 9. ACCEPT: cap nhat PO hien tai va gia NCC
        // REJECT: khong thay doi bat ky gia nao
        if (decision == SupplierPriceDecisionAction.ACCEPT) {

            item.setUnitPrice(quotedPrice);

            supplierProduct.setPurchasePrice(quotedPrice);
        }

        // 10. Luu day du thong tin quyet dinh vao audit
        Map<String, Object> newValue = new LinkedHashMap<>();

        newValue.put("eventType", "SUPPLIER_PRICE_DECISION");
        newValue.put("purchaseOrderId", order.getId());
        newValue.put("purchaseOrderItemId", item.getId());
        newValue.put("productId", item.getProduct().getId());
        newValue.put("supplierId", order.getSupplier().getId());
        newValue.put("decision", decision.name());
        newValue.put("reason", reason);

        newValue.put(
                "confirmedUnitPrice",
                normalizedQuotedPrice
        );

        newValue.put(
                "unitPrice",
                decimalText(item.getUnitPrice())
        );

        newValue.put(
                "supplierPurchasePrice",
                supplierProduct == null
                        ? null
                        : decimalText(supplierProduct.getPurchasePrice())
        );

        String activityCode =
                "AL-" + UUID.randomUUID()
                        .toString()
                        .replace("-", "");

        String auditAction =
                decision == SupplierPriceDecisionAction.ACCEPT
                        ? "APPROVE"
                        : "REJECT";

        int inserted = auditRepository.insertDecisionLog(
                activityCode,
                actor.getId(),
                auditAction,
                order.getId(),
                objectMapper.valueToTree(oldValue).toString(),
                objectMapper.valueToTree(newValue).toString()
        );

        if (inserted != 1) {
            throw new IllegalStateException(
                    "Failed to save price decision audit log"
            );
        }

        // 11. Tra ve ket qua
        return new SupplierPriceDecisionResponse(
                order.getId(),
                item.getId(),
                order.getSupplier().getId(),
                item.getProduct().getId(),
                decision,
                reason,
                originalPrice,
                quotedPrice,
                item.getUnitPrice(),
                previousSupplierPrice,
                supplierProduct == null
                        ? null
                        : supplierProduct.getPurchasePrice(),
                actor.getId()
        );
    }

    private String decimalText(BigDecimal value) {
        return value == null
                ? null
                : value.toPlainString();
    }
}
