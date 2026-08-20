package com.toystorage.backend.services.receipts;

import com.toystorage.backend.dto.request.receipts.ReceiptInspectionRequest;
import com.toystorage.backend.dto.response.receipts.GoodsReceiptResponse;
import com.toystorage.backend.dto.response.receipts.ReceiptInspectionResponse;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.warehouses.WarehouseTaskType;
import com.toystorage.backend.services.warehouses.WarehouseTaskClaimService;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.enums.receipts.InspectionResult;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.mapper.receipts.GoodsReceiptMapper;
import com.toystorage.backend.mapper.receipts.ReceiptInspectionMapper;
import com.toystorage.backend.repository.products.ProductRepository;
import com.toystorage.backend.repository.receipts.GoodsReceiptRepository;
import com.toystorage.backend.repository.receipts.ReceiptInspectionRepository;
import com.toystorage.backend.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoodsReceivingService {

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final ReceiptInspectionRepository receiptInspectionRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WarehouseTaskClaimService
            taskClaimService;
    private final GoodsReceiptMapper goodsReceiptMapper;
    private final ReceiptInspectionMapper receiptInspectionMapper;


    // =====================================================
    // WAREHOUSE MANAGER CONFIRM VEHICLE ARRIVAL
    // CREATED -> CONFIRMED
    // =====================================================

    @Transactional
    public GoodsReceiptResponse confirmVehicleArrival(
            Long receiptId
    ) {

        GoodsReceipts receipt = getGoodsReceipt(receiptId);

        // Lấy manager đang đăng nhập
        Users manager = getCurrentUser();

        // Không cho manager kho A thao tác receipt kho B
        validateSameWarehouse(manager, receipt);

        if (receipt.getStatus() != GoodsReceiptStatus.CREATED) {
            throw new BadRequest(
                    "Only goods receipt with CREATED status can confirm vehicle arrival"
            );
        }

        receipt.setConfirmedBy(manager);
        receipt.setStatus(GoodsReceiptStatus.CONFIRMED);
        receipt.setUpdatedAt(LocalDateTime.now());

        return goodsReceiptMapper.toResponse(
                goodsReceiptRepository.save(receipt)
        );
    }

    // =====================================================
    // WAREHOUSE STAFF START RECEIVING
    // CONFIRMED -> RECEIVING
    // =====================================================

    @Transactional
    public GoodsReceiptResponse startReceiving(
            Long receiptId
    ) {

        GoodsReceipts receipt =
                getGoodsReceipt(
                        receiptId
                );

        Users staff =
                getCurrentUser();


        validateSameWarehouse(
                staff,
                receipt
        );


        /*
         * Staff này đã start rồi.
         */
        if (receipt.getStatus()
                == GoodsReceiptStatus.RECEIVING) {

            taskClaimService.validateOwner(
                    WarehouseTaskType.GOODS_RECEIVING,
                    receiptId,
                    staff
            );

            return goodsReceiptMapper
                    .toResponse(
                            receipt
                    );
        }


        if (receipt.getStatus()
                != GoodsReceiptStatus.CONFIRMED) {

            throw new BadRequest(
                    "Warehouse Staff can only receive goods "
                            + "after vehicle arrival has been confirmed"
            );
        }


        /*
         * Atomic claim.
         */
        taskClaimService.claim(
                WarehouseTaskType.GOODS_RECEIVING,
                receiptId,
                staff
        );


        LocalDateTime now =
                LocalDateTime.now();


        /*
         * receivedBy vẫn giữ để audit.
         */
        receipt.setReceivedBy(
                staff
        );

        receipt.setReceivedAt(
                now
        );

        receipt.setStatus(
                GoodsReceiptStatus.RECEIVING
        );

        receipt.setUpdatedAt(
                now
        );


        return goodsReceiptMapper
                .toResponse(
                        goodsReceiptRepository
                                .save(receipt)
                );
    }

    // =====================================================
    // WAREHOUSE STAFF INSPECT PRODUCT
    // =====================================================

    @Transactional
    public ReceiptInspectionResponse inspectProduct(
            Long receiptId,
            ReceiptInspectionRequest request
    ) {

        GoodsReceipts receipt = getGoodsReceipt(receiptId);

        Users staff = getCurrentUser();

        validateSameWarehouse(staff, receipt);

        taskClaimService.validateOwner(
                WarehouseTaskType.GOODS_RECEIVING,
                receiptId,
                staff
        );
        if (receipt.getStatus() != GoodsReceiptStatus.RECEIVING) {
            throw new BadRequest(
                    "Goods receipt must be in RECEIVING status before product inspection"
            );
        }


        boolean alreadyInspected =
                receiptInspectionRepository
                        .existsByGoodsReceiptIdAndProductId(
                                receiptId,
                                request.getProductId()
                        );

        if (alreadyInspected) {
            throw new BadRequest(
                    "Product "
                            + request.getProductId()
                            + " has already been inspected"
            );
        }

        Products product =
                productRepository
                        .findById(request.getProductId())
                        .orElseThrow(() ->
                                new NotFound(
                                        "Product not found with id: "
                                                + request.getProductId()
                                )
                        );

        InspectionResult result =
                resolveResult(
                        request.getExpectedQuantity(),
                        request.getActualQuantity()
                );

        ReceiptInspections inspection =
                ReceiptInspections.builder()
                        .receiptInspectionsCode(
                                generateInspectionCode()
                        )
                        .goodsReceipt(receipt)
                        .product(product)
                        .packageCode(request.getPackageCode())
                        .expectedQuantity(
                                request.getExpectedQuantity()
                        )
                        .actualQuantity(
                                request.getActualQuantity()
                        )
                        .inspectedResult(result)
                        .notes(request.getNotes())
                        .inspectedBy(staff)
                        .inspectedAt(LocalDateTime.now())
                        .build();

        ReceiptInspections saved =
                receiptInspectionRepository.save(inspection);

        return receiptInspectionMapper.toResponse(saved);
    }


    // =====================================================
    // FINISH INSPECTION
    // RECEIVING -> INSPECTED
    // =====================================================

    @Transactional
    public GoodsReceiptResponse finishInspection(
            Long receiptId
    ) {

        GoodsReceipts receipt = getGoodsReceipt(receiptId);

        Users staff = getCurrentUser();

        validateSameWarehouse(staff, receipt);

        taskClaimService.validateOwner(
                WarehouseTaskType.GOODS_RECEIVING,
                receiptId,
                staff
        );

        if (receipt.getStatus() != GoodsReceiptStatus.RECEIVING) {
            throw new BadRequest(
                    "Only goods receipt with RECEIVING status can finish inspection"
            );
        }

        if (receiptInspectionRepository
                .findByGoodsReceiptId(receiptId)
                .isEmpty()) {

            throw new BadRequest(
                    "Goods receipt cannot be inspected without any inspection records"
            );
        }

        receipt.setStatus(GoodsReceiptStatus.INSPECTED);
        receipt.setUpdatedAt(LocalDateTime.now());

        GoodsReceipts saved =
                goodsReceiptRepository.save(receipt);

        return goodsReceiptMapper.toResponse(saved);
    }


    // =====================================================
    // GET GOODS RECEIPT
    // =====================================================

    private GoodsReceipts getGoodsReceipt(
            Long receiptId
    ) {

        return goodsReceiptRepository
                .findById(receiptId)
                .orElseThrow(() ->
                        new NotFound(
                                "Goods receipt not found with id: "
                                        + receiptId
                        )
                );
    }


    // =====================================================
    // GET CURRENT LOGIN USER
    // =====================================================

    private Users getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new Unauthorized(
                    "User is not authenticated"
            );
        }

        String email = authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new NotFound(
                                "Authenticated user not found"
                        )
                );
    }


    // =====================================================
    // VALIDATE USER WAREHOUSE
    // =====================================================

    private void validateSameWarehouse(
            Users user,
            GoodsReceipts receipt
    ) {

        if (user.getWarehouse() == null) {
            throw new Forbidden(
                    "User is not assigned to any warehouse"
            );
        }

        if (receipt.getWarehouse() == null) {
            throw new BadRequest(
                    "Goods receipt is not assigned to a warehouse"
            );
        }

        Long userWarehouseId =
                user.getWarehouse().getId();

        Long receiptWarehouseId =
                receipt.getWarehouse().getId();

        if (!userWarehouseId.equals(receiptWarehouseId)) {
            throw new Forbidden(
                    "You are not allowed to operate on goods receipts from another warehouse"
            );
        }
    }


    // =====================================================
    // RESOLVE INSPECTION RESULT
    // =====================================================

    private InspectionResult resolveResult(
            Integer expectedQuantity,
            Integer actualQuantity
    ) {

        if (actualQuantity.equals(expectedQuantity)) {
            return InspectionResult.MATCHED;
        }

        if (actualQuantity < expectedQuantity) {
            return InspectionResult.SHORTAGE;
        }

        return InspectionResult.SURPLUS;
    }


    // =====================================================
    // GENERATE INSPECTION CODE
    // =====================================================

    private String generateInspectionCode() {

        return "RI-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }
}