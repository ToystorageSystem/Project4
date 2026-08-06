package com.toystorage.backend.entity.suppliers;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.receipts.PurchaseOrders;
import com.toystorage.backend.enums.suppliers.SupplierInvoiceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "supplier_invoices",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_supplier_invoice_number",
                        columnNames = {"supplier_id", "invoice_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierInvoices {

    /*
     * Khóa chính của hóa đơn nhà cung cấp.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Mã hóa đơn nội bộ do hệ thống sinh.
     *
     * Ví dụ:
     * SINV2026080001
     */
    @Column(
            name = "invoice_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String invoiceCode;

    /*
     * Số hóa đơn do nhà cung cấp phát hành.
     *
     * Số hóa đơn có thể trùng giữa hai nhà cung cấp khác nhau,
     * nhưng không được trùng trong cùng một nhà cung cấp.
     */
    @Column(
            name = "invoice_number",
            nullable = false,
            length = 100
    )
    private String invoiceNumber;

    /*
     * Ký hiệu hóa đơn.
     *
     * Ví dụ:
     * 1C26TAA
     */
    @Column(
            name = "invoice_series",
            length = 50
    )
    private String invoiceSeries;

    /*
     * Nhà cung cấp phát hành hóa đơn.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "supplier_id",
            nullable = false
    )
    private Suppliers supplier;

    /*
     * Đơn mua hàng liên quan đến hóa đơn.
     *
     * Một đơn mua hàng có thể có nhiều hóa đơn,
     * ví dụ khi nhà cung cấp giao hàng thành nhiều đợt.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "purchase_order_id",
            nullable = false
    )
    private PurchaseOrders purchaseOrder;

    /*
     * Ngày phát hành hóa đơn.
     */
    @Column(
            name = "invoice_date",
            nullable = false
    )
    private LocalDate invoiceDate;

    /*
     * Giá trị hàng hóa trước thuế.
     */
    @Column(
            name = "subtotal",
            precision = 18,
            scale = 2
    )
    private BigDecimal subtotal;

    /*
     * Tổng tiền thuế.
     */
    @Column(
            name = "tax_amount",
            precision = 18,
            scale = 2
    )
    private BigDecimal taxAmount;

    /*
     * Tổng giá trị hóa đơn sau thuế.
     */
    @Column(
            name = "total_amount",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal totalAmount;

    /*
     * Mã số thuế của nhà cung cấp trên hóa đơn.
     */
    @Column(
            name = "supplier_tax_code",
            length = 50
    )
    private String supplierTaxCode;

    /*
     * Mã tra cứu hóa đơn điện tử.
     */
    @Column(
            name = "lookup_code",
            length = 255
    )
    private String lookupCode;

    /*
     * Đường dẫn file XML của hóa đơn điện tử.
     *
     * Database chỉ lưu đường dẫn file,
     * không lưu trực tiếp nội dung file.
     */
    @Column(
            name = "xml_file_url",
            length = 500
    )
    private String xmlFileUrl;

    /*
     * Đường dẫn file PDF thể hiện hóa đơn.
     */
    @Column(
            name = "pdf_file_url",
            length = 500
    )
    private String pdfFileUrl;

    /*
     * Đường dẫn ảnh hóa đơn nếu người dùng tải ảnh lên.
     */
    @Column(
            name = "image_file_url",
            length = 500
    )
    private String imageFileUrl;

    /*
     * Trạng thái kiểm tra hóa đơn.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private SupplierInvoiceStatus status =
            SupplierInvoiceStatus.DRAFT;

    /*
     * Business Staff tải hóa đơn lên hệ thống.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "uploaded_by",
            nullable = false
    )
    private Users uploadedBy;

    /*
     * Ghi chú khi kiểm tra hóa đơn.
     *
     * Ví dụ:
     * - Sai số lượng.
     * - Sai đơn giá.
     * - Thiếu mã số thuế.
     */
    @Lob
    @Column(
            name = "verification_note",
            columnDefinition = "TEXT"
    )
    private String verificationNote;

    /*
     * Thời điểm tải hóa đơn lên.
     */
    @Column(
            name = "uploaded_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime uploadedAt;

    /*
     * Thời điểm cập nhật gần nhất.
     */
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (uploadedAt == null) {
            uploadedAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (status == null) {
            status = SupplierInvoiceStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}