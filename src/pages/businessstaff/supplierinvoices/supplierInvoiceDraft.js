// Validations mirror SupplierInvoiceService and the request DTOs.
// Money is handled in integer cents to avoid floating-point addition errors.
const MONEY_PATTERN = /^(?:0|[1-9]\d*)(?:\.(\d{1,2}))?$/;

export function parseInvoiceCents(value) {
    const source = String(value ?? "").trim();
    const match = MONEY_PATTERN.exec(source);
    if (!match) return null;
    const cents = BigInt(source.split(".")[0]) * 100n + BigInt((match[1] || "").padEnd(2, "0") || "0");
    return cents <= BigInt(Number.MAX_SAFE_INTEGER) ? Number(cents) : null;
}

export function centsToInvoiceDecimal(value) {
    if (!Number.isSafeInteger(value) || value < 0) return null;
    return `${Math.floor(value / 100)}.${String(value % 100).padStart(2, "0")}`;
}

const isPositiveId = (value) => Number.isSafeInteger(Number(value)) && Number(value) > 0;

function validLocalDate(value) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value || "")) return false;
    const date = new Date(`${value}T00:00:00Z`);
    return Number.isFinite(date.getTime()) && date.toISOString().slice(0, 10) === value;
}

export function validateSupplierInvoiceDraft(form, purchaseOrders) {
    const issues = [];
    const supplierId = String(form.supplierId || "");
    const purchaseOrderId = String(form.purchaseOrderId || "");
    const invoiceNumber = String(form.invoiceNumber || "").trim();
    const subtotal = parseInvoiceCents(form.subtotal);
    const taxAmount = parseInvoiceCents(form.taxAmount);
    const total = subtotal === null || taxAmount === null ? null : subtotal + taxAmount;

    if (!isPositiveId(supplierId)) issues.push("Vui lòng chọn nhà cung cấp đang hoạt động.");
    if (!isPositiveId(purchaseOrderId)) {
        issues.push("Vui lòng chọn đơn mua hàng hợp lệ của nhà cung cấp.");
    } else if (!purchaseOrders.some((po) => String(po.id) === purchaseOrderId)) {
        issues.push("Đơn mua hàng không nằm trong danh sách PO đủ điều kiện. Hãy chọn PO khác.");
    }
    if (!invoiceNumber) issues.push("Số hóa đơn không được để trống.");
    else if (invoiceNumber.length > 100) issues.push("Số hóa đơn không được vượt quá 100 ký tự.");
    if (!validLocalDate(form.invoiceDate)) issues.push("Ngày phát hành hóa đơn không hợp lệ.");
    if (!validLocalDate(form.dueDate)) issues.push("Hạn thanh toán không hợp lệ.");
    if (validLocalDate(form.invoiceDate) && validLocalDate(form.dueDate) && form.dueDate < form.invoiceDate) {
        issues.push("Hạn thanh toán không được trước ngày phát hành hóa đơn.");
    }
    if (subtotal === null) issues.push("Giá trị trước thuế phải là số không âm, tối đa 2 chữ số thập phân.");
    if (taxAmount === null) issues.push("Tiền thuế phải là số không âm, tối đa 2 chữ số thập phân.");
    if (total !== null && (!Number.isSafeInteger(total) || total <= 0)) {
        issues.push("Tổng giá trị hóa đơn phải lớn hơn 0 và nằm trong giới hạn hỗ trợ.");
    }
    return issues;
}

export function supplierInvoiceDraftPayload(form) {
    const subtotalCents = parseInvoiceCents(form.subtotal);
    const taxCents = parseInvoiceCents(form.taxAmount);
    if (subtotalCents === null || taxCents === null) throw new Error("Giá trị hóa đơn không hợp lệ.");
    return {
        supplierId: Number(form.supplierId),
        purchaseOrderId: Number(form.purchaseOrderId),
        invoiceNumber: form.invoiceNumber.trim(),
        invoiceDate: form.invoiceDate,
        dueDate: form.dueDate,
        subtotal: Number(centsToInvoiceDecimal(subtotalCents)),
        taxAmount: Number(centsToInvoiceDecimal(taxCents)),
        totalAmount: Number(centsToInvoiceDecimal(subtotalCents + taxCents)),
    };
}
