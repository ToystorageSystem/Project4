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

    if (!isPositiveId(supplierId)) issues.push("Please select an active supplier.");
    if (!isPositiveId(purchaseOrderId)) {
        issues.push("Select a valid purchase order for this supplier.");
    } else if (!purchaseOrders.some((po) => String(po.id) === purchaseOrderId)) {
        issues.push("This purchase order is not eligible for invoicing. Select another order.");
    }
    if (!invoiceNumber) issues.push("Invoice number is required.");
    else if (invoiceNumber.length > 100) issues.push("Invoice number cannot exceed 100 characters.");
    if (!validLocalDate(form.invoiceDate)) {
    issues.push("Invalid invoice issue date.");
} else {
    const now = new Date();

    const today =
        `${now.getFullYear()}-` +
        `${String(now.getMonth() + 1).padStart(2, "0")}-` +
        `${String(now.getDate()).padStart(2, "0")}`;

    if (form.invoiceDate > today) {
        issues.push(
            "Invoice issue date cannot be in the future."
        );
    }
}
    if (!validLocalDate(form.dueDate)) issues.push("Invalid payment due date.");
    if (validLocalDate(form.invoiceDate) && validLocalDate(form.dueDate) && form.dueDate < form.invoiceDate) {
        issues.push("Payment due date cannot be before the invoice issue date.");
    }
    if (subtotal === null) issues.push("Subtotal must be non-negative, with at most two decimal places.");
    if (taxAmount === null) issues.push("Tax amount must be non-negative, with at most two decimal places.");
    if (total !== null && (!Number.isSafeInteger(total) || total <= 0)) {
        issues.push("Invoice total must be greater than zero and within supported limits.");
    }
    return issues;
}

export function supplierInvoiceDraftPayload(form) {
    const subtotalCents = parseInvoiceCents(form.subtotal);
    const taxCents = parseInvoiceCents(form.taxAmount);
    if (subtotalCents === null || taxCents === null) throw new Error("Invalid invoice amount.");
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
