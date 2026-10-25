import { receiptText as T } from "./receiptText.js";

const MAX_INT = 2147483647;
export const newRow = (key) => ({ key, productId: "", expectedQuantity: "1", unitPrice: "" });

export function normalizePrice(value) {
    return String(value ?? "").trim().replace(",", ".");
}

export function isPositivePrice(value) {
    const text = normalizePrice(value);
    // DECIMAL(18,2) is the purchase_order_items.unit_price schema.
    return /^(?:0|[1-9]\d{0,15})(?:\.\d{1,2})?$/.test(text) && Number(text) > 0;
}

export function calculateLineTotal(item) {
    const quantity = Number(item.expectedQuantity);
    const price = Number(normalizePrice(item.unitPrice));
    return Number.isSafeInteger(quantity) && quantity > 0 &&
        Number.isFinite(price) && price > 0 ? quantity * price : 0;
}

export function validateForm(form, suppliers, warehouses, products) {
    const errors = { items: {} };
    if (!suppliers.some((s) => String(s.id) === form.supplierId)) errors.supplierId = T.invalidSupplier;
    if (!warehouses.some((w) => String(w.id) === form.warehouseId)) errors.warehouseId = T.invalidWarehouse;
    if (form.note.length > 2000) errors.note = T.invalidNote;
    if (form.items.length === 0) errors.general = T.invalidProduct;

    const usedIds = new Set();
    form.items.forEach((item) => {
        const itemErrors = {};
        if (!products.some((p) => String(p.id) === item.productId && p.status === "ACTIVE")) {
            itemErrors.productId = T.invalidProduct;
        } else if (usedIds.has(item.productId)) {
            itemErrors.productId = T.duplicateProduct;
        }
        if (item.productId) usedIds.add(item.productId);
        if (!/^[1-9]\d*$/.test(item.expectedQuantity) || Number(item.expectedQuantity) > MAX_INT) {
            itemErrors.expectedQuantity = T.invalidQuantity;
        }
        if (!isPositivePrice(item.unitPrice)) itemErrors.unitPrice = T.invalidPrice;
        if (Object.keys(itemErrors).length) errors.items[item.key] = itemErrors;
    });
    return errors;
}

export function hasFormErrors(errors) {
    return Boolean(errors.general || errors.supplierId || errors.warehouseId || errors.note || Object.keys(errors.items || {}).length);
}
