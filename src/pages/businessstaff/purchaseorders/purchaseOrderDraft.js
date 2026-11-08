// Issue #9: pure FE validation. Backend is authoritative for all business rules.
const isPositiveId = (value) => Number.isSafeInteger(Number(value)) && Number(value) > 0;
const isQuantity = (value) => /^[1-9]\d*$/.test(String(value)) && Number(value) <= 2147483647;

function isDate(value) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(String(value || ""))) return false;
    const date = new Date(`${value}T00:00:00Z`);
    return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value;
}

export function todayVietnamIso() {
    const parts = new Intl.DateTimeFormat("en-GB", {
        timeZone: "Asia/Ho_Chi_Minh",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
    }).formatToParts(new Date());
    const value = (type) => parts.find((part) => part.type === type)?.value;
    return `${value("year")}-${value("month")}-${value("day")}`;
}

export function validatePurchaseOrderDraft(form, productOptions) {
    const problems = [];
    if (!isPositiveId(form.supplierId)) problems.push("Select a valid supplier.");
    if (!isPositiveId(form.warehouseId)) problems.push("Select a valid receiving main warehouse.");
    if (form.expectedDeliveryDate && !isDate(form.expectedDeliveryDate)) {
        problems.push("Requested delivery date is invalid.");
    } else if (form.expectedDeliveryDate && form.expectedDeliveryDate < todayVietnamIso()) {
        problems.push("Expected delivery date cannot be in the past.");
    }
    if (!Array.isArray(form.items) || form.items.length === 0) {
        problems.push("Purchase order must contain at least one product.");
    }

    const seen = new Set();
    (form.items || []).forEach((row, index) => {
        const label = `Row ${index + 1}`;
        const productId = Number(row.productId);
        if (!isPositiveId(row.productId)) {
            problems.push(`${label}: Select a product.`);
            return;
        }
        if (seen.has(productId)) problems.push(`${label}: Duplicate product on the order.`);
        seen.add(productId);

        const option = (productOptions || []).find((item) => Number(item.productId) === productId);
        if (!option) {
            problems.push(`${label}: This supplier can no longer supply this product.`);
            return;
        }
        if (!isQuantity(row.orderedQuantity)) {
            problems.push(`${label}: Quantity must be an integer between 1 and 2,147,483,647.`);
        } else if (Number(row.orderedQuantity) < Math.max(1, Number(option.minimumOrderQuantity || 1))) {
            problems.push(`${label}: Minimum order quantity is ${option.minimumOrderQuantity || 1}.`);
        }
        if (!Number.isFinite(Number(option.purchasePrice)) || Number(option.purchasePrice) <= 0) {
            problems.push(`${label}: Invalid supplier purchase price.`);
        }
    });
    return problems;
}

export function toPurchaseOrderPayload(form) {
    return {
        supplierId: Number(form.supplierId),
        warehouseId: Number(form.warehouseId),
        expectedDeliveryDate: form.expectedDeliveryDate || null,
        note: form.note.trim() || null,
        items: form.items.map((row) => ({
            productId: Number(row.productId),
            orderedQuantity: Number(row.orderedQuantity),
            // Deliberately OMIT unitPrice: BE uses active supplier_products.purchase_price.
        })),
    };
}

