package com.toystorage.backend.services.suppliers;

import com.toystorage.backend.dto.request.suppliers.CancelSupplierInvoiceRequest;
import com.toystorage.backend.dto.request.suppliers.CreateSupplierInvoiceRequest;
import com.toystorage.backend.dto.request.suppliers.UpdateSupplierInvoiceRequest;
import com.toystorage.backend.dto.response.suppliers.SupplierInvoiceDetailResponse;
import com.toystorage.backend.dto.response.suppliers.SupplierInvoicePurchaseOrderOptionResponse;
import com.toystorage.backend.dto.response.suppliers.SupplierInvoiceSummaryResponse;
import com.toystorage.backend.dto.response.suppliers.SupplierInvoiceSupplierOptionResponse;
import com.toystorage.backend.entity.receipts.PurchaseOrderItems;
import com.toystorage.backend.entity.receipts.PurchaseOrders;
import com.toystorage.backend.entity.suppliers.SupplierInvoices;
import com.toystorage.backend.entity.suppliers.Suppliers;
import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.enums.receipts.PurchaseOrderStatus;
import com.toystorage.backend.enums.suppliers.SupplierInvoiceStatus;
import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.mapper.suppliers.SupplierInvoiceMapper;
import com.toystorage.backend.repository.receipts.PurchaseOrderItemRepository;
import com.toystorage.backend.repository.receipts.PurchaseOrderRepository;
import com.toystorage.backend.repository.suppliers.SupplierInvoiceRepository;
import com.toystorage.backend.repository.suppliers.SupplierRepository;
import com.toystorage.backend.repository.users.ActivityLogRepository;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.services.cloudinary.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierInvoiceService {

    /*
     * Chỉ các PO đã được đặt hàng / đang nhận / hoàn tất
     * mới có thể được liên kết với hóa đơn.
     */
    private static final List<PurchaseOrderStatus>
            INVOICE_ELIGIBLE_PO_STATUSES =
            List.of(
                    PurchaseOrderStatus.ORDERED,
                    PurchaseOrderStatus.PARTIALLY_RECEIVED,
                    PurchaseOrderStatus.COMPLETED
            );

    private final SupplierInvoiceRepository
            supplierInvoiceRepository;

    private final SupplierRepository
            supplierRepository;

    private final PurchaseOrderRepository
            purchaseOrderRepository;

    private final PurchaseOrderItemRepository
            purchaseOrderItemRepository;

    private final UserRepository
            userRepository;

    private final ActivityLogRepository
            activityLogRepository;

    private final SupplierInvoiceMapper
            supplierInvoiceMapper;

    private final CloudinaryService
            cloudinaryService;


    // =====================================================
    // GET INVOICE LIST
    // =====================================================

    @Transactional(readOnly = true)
    public List<SupplierInvoiceSummaryResponse>
    getSupplierInvoices(
            String keyword,
            SupplierInvoiceStatus status
    ) {

        List<SupplierInvoices> invoices =
                supplierInvoiceRepository.search(
                        normalizeKeyword(keyword),
                        status
                );

        if (invoices.isEmpty()) {
            return List.of();
        }

        List<Long> purchaseOrderIds =
                invoices.stream()
                        .map(
                                SupplierInvoices::getPurchaseOrder
                        )
                        .filter(
                                purchaseOrder ->
                                        purchaseOrder != null
                        )
                        .map(
                                PurchaseOrders::getId
                        )
                        .distinct()
                        .toList();

        Map<Long, List<PurchaseOrderItems>>
                itemsByPurchaseOrderId =
                loadItemsByPurchaseOrderId(
                        purchaseOrderIds
                );

        return invoices.stream()
                .map(invoice ->
                        supplierInvoiceMapper
                                .toSummaryResponse(
                                        invoice,
                                        itemsByPurchaseOrderId
                                                .getOrDefault(
                                                        invoice
                                                                .getPurchaseOrder()
                                                                .getId(),
                                                        Collections
                                                                .emptyList()
                                                )
                                )
                )
                .toList();
    }


    // =====================================================
    // GET INVOICE DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public SupplierInvoiceDetailResponse
    getSupplierInvoiceDetail(
            Long invoiceId
    ) {

        SupplierInvoices invoice =
                getInvoice(
                        invoiceId
                );

        List<PurchaseOrderItems> items =
                purchaseOrderItemRepository
                        .findByPurchaseOrder_IdOrderByIdAsc(
                                invoice
                                        .getPurchaseOrder()
                                        .getId()
                        );

        return supplierInvoiceMapper
                .toDetailResponse(
                        invoice,
                        items
                );
    }


    // =====================================================
    // GET SUPPLIER OPTIONS
    // =====================================================

    @Transactional(readOnly = true)
    public List<SupplierInvoiceSupplierOptionResponse>
    getSupplierOptions() {

        return supplierRepository
                .findByStatusOrderByNameAsc(
                        CommonStatus.ACTIVE
                )
                .stream()
                .map(
                        supplierInvoiceMapper::
                                toSupplierOption
                )
                .toList();
    }


    // =====================================================
    // GET PURCHASE ORDER OPTIONS BY SUPPLIER
    // =====================================================

    @Transactional(readOnly = true)
    public List<SupplierInvoicePurchaseOrderOptionResponse>
    getPurchaseOrderOptions(
            Long supplierId
    ) {

        /*
         * Đảm bảo Supplier tồn tại và ACTIVE.
         */
        getActiveSupplier(
                supplierId
        );

        List<PurchaseOrders> purchaseOrders =
                purchaseOrderRepository
                        .findInvoiceEligibleBySupplierId(
                                supplierId,
                                INVOICE_ELIGIBLE_PO_STATUSES
                        );

        if (purchaseOrders.isEmpty()) {
            return List.of();
        }

        List<Long> purchaseOrderIds =
                purchaseOrders.stream()
                        .map(
                                PurchaseOrders::getId
                        )
                        .toList();

        Map<Long, List<PurchaseOrderItems>>
                itemsByPurchaseOrderId =
                loadItemsByPurchaseOrderId(
                        purchaseOrderIds
                );

        return purchaseOrders.stream()
                .map(purchaseOrder ->
                        supplierInvoiceMapper
                                .toPurchaseOrderOption(
                                        purchaseOrder,
                                        itemsByPurchaseOrderId
                                                .getOrDefault(
                                                        purchaseOrder
                                                                .getId(),
                                                        Collections
                                                                .emptyList()
                                                )
                                )
                )
                .toList();
    }


    // =====================================================
    // CREATE SUPPLIER INVOICE
    // =====================================================

    @Transactional
    public SupplierInvoiceDetailResponse create(
            CreateSupplierInvoiceRequest request
    ) {

        Users currentUser =
                getCurrentUser();

        String invoiceNumber =
                normalizeRequiredText(
                        request.getInvoiceNumber(),
                        "Invoice number is required"
                );

        validateDates(
                request.getInvoiceDate(),
                request.getDueDate()
        );

        validateAmounts(
                request.getSubtotal(),
                request.getTaxAmount(),
                request.getTotalAmount()
        );

        Suppliers supplier =
                getActiveSupplier(
                        request.getSupplierId()
                );

        /*
         * Số hóa đơn chỉ unique trong cùng Supplier.
         */
        if (
                supplierInvoiceRepository
                        .existsBySupplier_IdAndInvoiceNumberIgnoreCase(
                                supplier.getId(),
                                invoiceNumber
                        )
        ) {

            throw new BadRequest(
                    "Invoice number already exists for this supplier"
            );
        }

        /*
         * Kiểm tra:
         * - PO tồn tại.
         * - PO thuộc Supplier đã chọn.
         * - PO không phải DRAFT/CANCELLED.
         */
        PurchaseOrders purchaseOrder =
                getEligiblePurchaseOrder(
                        request.getPurchaseOrderId(),
                        supplier.getId()
                );

        /*
         * Backend tự tính lại total.
         */
        BigDecimal calculatedTotal =
                request
                        .getSubtotal()
                        .add(
                                request.getTaxAmount()
                        );

        SupplierInvoices invoice =
                SupplierInvoices.builder()
                        .invoiceCode(
                                generateInvoiceCode()
                        )
                        .invoiceNumber(
                                invoiceNumber
                        )
                        .supplier(
                                supplier
                        )
                        .purchaseOrder(
                                purchaseOrder
                        )
                        .invoiceDate(
                                request.getInvoiceDate()
                        )
                        .dueDate(
                                request.getDueDate()
                        )
                        .subtotal(
                                request.getSubtotal()
                        )
                        .taxAmount(
                                request.getTaxAmount()
                        )
                        .totalAmount(
                                calculatedTotal
                        )

                        /*
                         * Snapshot MST của Supplier
                         * tại thời điểm tạo invoice.
                         */
                        .supplierTaxCode(
                                supplier.getTaxCode()
                        )
                        .status(
                                SupplierInvoiceStatus.DRAFT
                        )
                        .uploadedBy(
                                currentUser
                        )
                        .build();

        invoice =
                supplierInvoiceRepository
                        .save(
                                invoice
                        );

        saveHistory(
                currentUser,
                ActivityAction.CREATE,
                invoice,
                null,
                snapshot(invoice)
        );

        List<PurchaseOrderItems> items =
                purchaseOrderItemRepository
                        .findByPurchaseOrder_IdOrderByIdAsc(
                                purchaseOrder.getId()
                        );

        return supplierInvoiceMapper
                .toDetailResponse(
                        invoice,
                        items
                );
    }


    // =====================================================
    // UPDATE SUPPLIER INVOICE
    // =====================================================

    @Transactional
    public SupplierInvoiceDetailResponse update(
            Long invoiceId,
            UpdateSupplierInvoiceRequest request
    ) {

        Users currentUser =
                getCurrentUser();

        SupplierInvoices invoice =
                getInvoice(
                        invoiceId
                );

        /*
         * Chỉ hóa đơn chưa xác nhận mới được sửa.
         */
        assertEditable(
                invoice
        );

        String oldValue =
                snapshot(
                        invoice
                );

        String invoiceNumber =
                normalizeRequiredText(
                        request.getInvoiceNumber(),
                        "Invoice number is required"
                );

        validateDates(
                request.getInvoiceDate(),
                request.getDueDate()
        );

        validateAmounts(
                request.getSubtotal(),
                request.getTaxAmount(),
                request.getTotalAmount()
        );

        Suppliers supplier =
                getActiveSupplier(
                        request.getSupplierId()
                );

        /*
         * Kiểm tra trùng invoice number,
         * nhưng bỏ qua chính invoice đang update.
         */
        if (
                supplierInvoiceRepository
                        .existsBySupplier_IdAndInvoiceNumberIgnoreCaseAndIdNot(
                                supplier.getId(),
                                invoiceNumber,
                                invoice.getId()
                        )
        ) {

            throw new BadRequest(
                    "Invoice number already exists for this supplier"
            );
        }

        PurchaseOrders purchaseOrder =
                getEligiblePurchaseOrder(
                        request.getPurchaseOrderId(),
                        supplier.getId()
                );

        BigDecimal calculatedTotal =
                request
                        .getSubtotal()
                        .add(
                                request.getTaxAmount()
                        );

        invoice.setInvoiceNumber(
                invoiceNumber
        );

        invoice.setSupplier(
                supplier
        );

        invoice.setPurchaseOrder(
                purchaseOrder
        );

        invoice.setInvoiceDate(
                request.getInvoiceDate()
        );

        invoice.setDueDate(
                request.getDueDate()
        );

        invoice.setSubtotal(
                request.getSubtotal()
        );

        invoice.setTaxAmount(
                request.getTaxAmount()
        );

        invoice.setTotalAmount(
                calculatedTotal
        );

        invoice.setSupplierTaxCode(
                supplier.getTaxCode()
        );

        invoice =
                supplierInvoiceRepository
                        .save(
                                invoice
                        );

        saveHistory(
                currentUser,
                ActivityAction.UPDATE,
                invoice,
                oldValue,
                snapshot(invoice)
        );

        List<PurchaseOrderItems> items =
                purchaseOrderItemRepository
                        .findByPurchaseOrder_IdOrderByIdAsc(
                                purchaseOrder.getId()
                        );

        return supplierInvoiceMapper
                .toDetailResponse(
                        invoice,
                        items
                );
    }


    // =====================================================
    // CANCEL SUPPLIER INVOICE
    // =====================================================

    @Transactional
    public SupplierInvoiceDetailResponse cancel(
            Long invoiceId,
            CancelSupplierInvoiceRequest request
    ) {

        Users currentUser =
                getCurrentUser();

        SupplierInvoices invoice =
                getInvoice(
                        invoiceId
                );

        /*
         * VERIFIED / PARTIALLY_PAID / PAID /
         * OVERDUE / CANCELLED không được hủy
         * bằng chức năng nhập sai.
         */
        assertEditable(
                invoice
        );

        String oldValue =
                snapshot(
                        invoice
                );

        String reason =
                normalizeRequiredText(
                        request.getReason(),
                        "Cancel reason is required"
                );

        invoice.setStatus(
                SupplierInvoiceStatus.CANCELLED
        );

        /*
         * Schema hiện tại chưa có cancel_reason,
         * nên lưu lý do vào verification_note.
         */
        invoice.setVerificationNote(
                "Cancelled: " + reason
        );

        invoice =
                supplierInvoiceRepository
                        .save(
                                invoice
                        );

        saveHistory(
                currentUser,
                ActivityAction.CANCEL,
                invoice,
                oldValue,
                snapshot(invoice)
        );

        List<PurchaseOrderItems> items =
                purchaseOrderItemRepository
                        .findByPurchaseOrder_IdOrderByIdAsc(
                                invoice
                                        .getPurchaseOrder()
                                        .getId()
                        );

        return supplierInvoiceMapper
                .toDetailResponse(
                        invoice,
                        items
                );
    }


    // =====================================================
    // UPLOAD SUPPLIER INVOICE ATTACHMENT
    // =====================================================

    @Transactional
    public SupplierInvoiceDetailResponse uploadAttachment(
            Long invoiceId,
            MultipartFile file
    ) {

        Users currentUser =
                getCurrentUser();

        SupplierInvoices invoice =
                getInvoice(
                        invoiceId
                );

        /*
         * Chỉ hóa đơn chưa xác nhận mới được
         * thay đổi file đính kèm.
         */
        assertEditable(
                invoice
        );

        validateAttachment(
                file
        );

        String attachmentType =
                detectAttachmentType(
                        file
                );

        String oldValue =
                snapshot(
                        invoice
                );

        String fileUrl =
                cloudinaryService
                        .uploadSupplierInvoiceFile(
                                file
                        );

        switch (attachmentType) {

            case "IMAGE" ->
                    invoice.setImageFileUrl(
                            fileUrl
                    );

            case "PDF" ->
                    invoice.setPdfFileUrl(
                            fileUrl
                    );

            case "XML" ->
                    invoice.setXmlFileUrl(
                            fileUrl
                    );

            default ->
                    throw new BadRequest(
                            "Unsupported supplier invoice attachment type"
                    );
        }

        invoice =
                supplierInvoiceRepository
                        .save(
                                invoice
                        );

        saveHistory(
                currentUser,
                ActivityAction.UPDATE,
                invoice,
                oldValue,
                snapshot(invoice)
        );

        List<PurchaseOrderItems> items =
                purchaseOrderItemRepository
                        .findByPurchaseOrder_IdOrderByIdAsc(
                                invoice
                                        .getPurchaseOrder()
                                        .getId()
                        );

        return supplierInvoiceMapper
                .toDetailResponse(
                        invoice,
                        items
                );
    }


    // =====================================================
    // GET INVOICE ENTITY
    // =====================================================

    private SupplierInvoices getInvoice(
            Long invoiceId
    ) {

        return supplierInvoiceRepository
                .findDetailedById(
                        invoiceId
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Supplier invoice not found"
                        )
                );
    }


    // =====================================================
    // GET ACTIVE SUPPLIER
    // =====================================================

    private Suppliers getActiveSupplier(
            Long supplierId
    ) {

        Suppliers supplier =
                supplierRepository
                        .findById(
                                supplierId
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Supplier not found"
                                )
                        );

        if (
                supplier.getStatus()
                        != CommonStatus.ACTIVE
        ) {

            throw new BadRequest(
                    "Supplier must be ACTIVE"
            );
        }

        return supplier;
    }


    // =====================================================
    // GET ELIGIBLE PURCHASE ORDER
    // =====================================================

    private PurchaseOrders
    getEligiblePurchaseOrder(
            Long purchaseOrderId,
            Long supplierId
    ) {

        return purchaseOrderRepository
                .findInvoiceEligibleById(
                        purchaseOrderId,
                        supplierId,
                        INVOICE_ELIGIBLE_PO_STATUSES
                )
                .orElseThrow(() ->
                        new BadRequest(
                                "Purchase order must belong to the selected supplier "
                                        + "and have status ORDERED, "
                                        + "PARTIALLY_RECEIVED, or COMPLETED"
                        )
                );
    }


    // =====================================================
    // EDITABLE STATUS
    // =====================================================

    private void assertEditable(
            SupplierInvoices invoice
    ) {

        SupplierInvoiceStatus status =
                invoice.getStatus();

        if (
                status != SupplierInvoiceStatus.DRAFT
                        &&
                        status != SupplierInvoiceStatus
                                .PENDING_VERIFICATION
        ) {

            throw new BadRequest(
                    "Only DRAFT or PENDING_VERIFICATION "
                            + "supplier invoices can be changed"
            );
        }
    }


    // =====================================================
    // VALIDATE DATE
    // =====================================================

    private void validateDates(
            LocalDate invoiceDate,
            LocalDate dueDate
    ) {

        if (invoiceDate == null) {

            throw new BadRequest(
                    "Invoice date is required"
            );
        }

        if (dueDate == null) {

            throw new BadRequest(
                    "Due date is required"
            );
        }

        if (
                dueDate.isBefore(
                        invoiceDate
                )
        ) {

            throw new BadRequest(
                    "Due date cannot be before invoice date"
            );
        }
    }


    // =====================================================
    // VALIDATE MONEY
    // =====================================================

    private void validateAmounts(
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal totalAmount
    ) {

        if (
                subtotal == null
                        ||
                        taxAmount == null
                        ||
                        totalAmount == null
        ) {

            throw new BadRequest(
                    "Subtotal, tax amount and total amount are required"
            );
        }

        if (
                subtotal.compareTo(
                        BigDecimal.ZERO
                ) < 0
        ) {

            throw new BadRequest(
                    "Subtotal must be greater than or equal to 0"
            );
        }

        if (
                taxAmount.compareTo(
                        BigDecimal.ZERO
                ) < 0
        ) {

            throw new BadRequest(
                    "Tax amount must be greater than or equal to 0"
            );
        }

        if (
                totalAmount.compareTo(
                        BigDecimal.ZERO
                ) <= 0
        ) {

            throw new BadRequest(
                    "Total amount must be greater than 0"
            );
        }

        BigDecimal calculatedTotal =
                subtotal.add(
                        taxAmount
                );

        if (
                calculatedTotal.compareTo(
                        totalAmount
                ) != 0
        ) {

            throw new BadRequest(
                    "Total amount must equal subtotal plus tax amount"
            );
        }
    }


    // =====================================================
    // VALIDATE ATTACHMENT
    // =====================================================

    private void validateAttachment(
            MultipartFile file
    ) {

        if (
                file == null
                        ||
                        file.isEmpty()
        ) {

            throw new BadRequest(
                    "Supplier invoice attachment is required"
            );
        }
    }


    // =====================================================
    // DETECT ATTACHMENT TYPE
    // =====================================================

    private String detectAttachmentType(
            MultipartFile file
    ) {

        String contentType =
                file.getContentType();

        String fileName =
                file.getOriginalFilename();

        String normalizedContentType =
                contentType != null
                        ? contentType.toLowerCase()
                        : "";

        String normalizedFileName =
                fileName != null
                        ? fileName.toLowerCase()
                        : "";

        // =================================================
        // IMAGE
        // =================================================

        if (
                normalizedContentType
                        .startsWith("image/")
                        ||
                        normalizedFileName
                                .endsWith(".jpg")
                        ||
                        normalizedFileName
                                .endsWith(".jpeg")
                        ||
                        normalizedFileName
                                .endsWith(".png")
                        ||
                        normalizedFileName
                                .endsWith(".webp")
        ) {

            return "IMAGE";
        }

        // =================================================
        // PDF
        // =================================================

        if (
                normalizedContentType
                        .equals("application/pdf")
                        ||
                        normalizedFileName
                                .endsWith(".pdf")
        ) {

            return "PDF";
        }

        // =================================================
        // XML
        // =================================================

        if (
                normalizedContentType
                        .equals("application/xml")
                        ||
                        normalizedContentType
                                .equals("text/xml")
                        ||
                        normalizedFileName
                                .endsWith(".xml")
        ) {

            return "XML";
        }

        throw new BadRequest(
                "Only image, PDF, or XML supplier invoice files are allowed"
        );
    }


    // =====================================================
    // LOAD PURCHASE ORDER ITEMS
    // =====================================================

    private Map<Long, List<PurchaseOrderItems>>
    loadItemsByPurchaseOrderId(
            List<Long> purchaseOrderIds
    ) {

        if (
                purchaseOrderIds == null
                        ||
                        purchaseOrderIds.isEmpty()
        ) {

            return Collections.emptyMap();
        }

        return purchaseOrderItemRepository
                .findAllByPurchaseOrderIds(
                        purchaseOrderIds
                )
                .stream()
                .collect(
                        Collectors.groupingBy(
                                item ->
                                        item
                                                .getPurchaseOrder()
                                                .getId()
                        )
                );
    }


    // =====================================================
    // CURRENT USER
    // =====================================================

    private Users getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (
                authentication == null
                        ||
                        !authentication
                                .isAuthenticated()
                        ||
                        "anonymousUser".equals(
                                authentication
                                        .getPrincipal()
                        )
        ) {

            throw new Unauthorized(
                    "User is not authenticated"
            );
        }

        return userRepository
                .findByEmail(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Authenticated user not found"
                        )
                );
    }


    // =====================================================
    // ACTIVITY LOG
    // =====================================================

    private void saveHistory(
            Users user,
            ActivityAction action,
            SupplierInvoices invoice,
            String oldValue,
            String newValue
    ) {

        ActivityLogs activityLog =
                ActivityLogs.builder()
                        .user(
                                user
                        )
                        .action(
                                action
                        )
                        .entityType(
                                ActivityEntityType
                                        .SUPPLIER_INVOICE
                        )
                        .entityId(
                                invoice.getId()
                        )
                        .oldValue(
                                oldValue
                        )
                        .newValue(
                                newValue
                        )
                        .build();

        activityLogRepository.save(
                activityLog
        );
    }


    // =====================================================
    // AUDIT SNAPSHOT
    // =====================================================

    private String snapshot(
            SupplierInvoices invoice
    ) {

        return "invoiceCode="
                + invoice.getInvoiceCode()

                + ", invoiceNumber="
                + invoice.getInvoiceNumber()

                + ", supplierId="
                + (
                invoice.getSupplier() != null
                        ? invoice
                        .getSupplier()
                        .getId()
                        : null
        )

                + ", purchaseOrderId="
                + (
                invoice.getPurchaseOrder() != null
                        ? invoice
                        .getPurchaseOrder()
                        .getId()
                        : null
        )

                + ", invoiceDate="
                + invoice.getInvoiceDate()

                + ", dueDate="
                + invoice.getDueDate()

                + ", subtotal="
                + invoice.getSubtotal()

                + ", taxAmount="
                + invoice.getTaxAmount()

                + ", totalAmount="
                + invoice.getTotalAmount()

                + ", status="
                + invoice.getStatus()

                + ", verificationNote="
                + invoice.getVerificationNote()

                + ", xmlFileUrl="
                + invoice.getXmlFileUrl()

                + ", pdfFileUrl="
                + invoice.getPdfFileUrl()

                + ", imageFileUrl="
                + invoice.getImageFileUrl();
    }


    // =====================================================
    // GENERATE INVOICE CODE
    // =====================================================

    private String generateInvoiceCode() {

        String code;

        do {

            code =
                    "SINV-"

                            + LocalDate.now()
                            .format(
                                    DateTimeFormatter
                                            .BASIC_ISO_DATE
                            )

                            + "-"

                            + UUID.randomUUID()
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
                supplierInvoiceRepository
                        .existsByInvoiceCode(
                                code
                        )
        );

        return code;
    }


    // =====================================================
    // NORMALIZE KEYWORD
    // =====================================================

    private String normalizeKeyword(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }


    // =====================================================
    // NORMALIZE REQUIRED TEXT
    // =====================================================

    private String normalizeRequiredText(
            String value,
            String message
    ) {

        if (
                value == null
                        ||
                        value.trim().isEmpty()
        ) {

            throw new BadRequest(
                    message
            );
        }

        return value.trim();
    }
}