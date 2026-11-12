// Issue #11 - Form rules match CreateStockTransferRequest and TransferReasonCode.
export const TRANSFER_REASONS = [
    { value: "REBALANCE", label: "Inventory Rebalancing" },
    { value: "SLOW_MOVING", label: "Slow-Moving Stock" },
    { value: "DAMAGED", label: "Damaged or Defective Stock" },
    { value: "SURPLUS", label: "Excess Store Stock" },
    { value: "RECALL", label: "Product Recall" },
    { value: "STORE_CLOSURE", label: "Store Closure or Relocation" },
    { value: "WRONG_TRANSFER", label: "Incorrect Transfer" },
    { value: "OTHER", label: "Other Reason" },
];

// The Backend permits editing until the warehouse starts picking.
// The issuing warehouse cannot be changed; the destination can change in DRAFT only.
export const TRANSFER_EDITABLE_STATUSES = ["DRAFT", "CREATED", "PENDING_SOURCE_CONFIRMATION"];

export function canEditStockTransfer(status) {
    return TRANSFER_EDITABLE_STATUSES.includes(status);
}

// The source products API returns current available stock (not stock already
// reserved for this transfer). During a submitted transfer update, the Backend
// releases this transfer's reservations, validates, then reserves the new items
// in one transaction. Include this transfer's approved quantity in the form's
// *estimated* editable capacity so an unchanged item is not falsely rejected.
// Server-side validation is authoritative, especially if stock/status changes.
export function productsWithCurrentReservation(sourceProducts, detail) {
    const products = new Map((Array.isArray(sourceProducts) ? sourceProducts : [])
        .map((product) => [String(product.productId), { ...product }]));
    if (!["CREATED", "PENDING_SOURCE_CONFIRMATION"].includes(detail?.status)) {
        return Array.from(products.values());
    }
    for (const item of detail.items || []) {
        if (item.productId == null) continue;
        const key = String(item.productId);
        const existing = products.get(key);
        const available = Number(existing?.availableQuantity);
        const approved = Number(item.approvedQuantity);
        const free = Number.isSafeInteger(available) && available > 0 ? available : 0;
        const ownReservation = Number.isSafeInteger(approved) && approved > 0 ? approved : 0;
        products.set(key, {
            ...existing,
            productId: item.productId,
            productCode: existing?.productCode || item.productCode,
            productName: existing?.productName || item.productName,
            barcode: existing?.barcode || item.barcode,
            baseUnit: existing?.baseUnit || item.baseUnit,
            availableQuantity: free + ownReservation,
            ownReservation,
        });
    }
    return Array.from(products.values());
}

export const blankTransferRow = (clientKey) => ({ clientKey, productId: "", requestedQuantity: "" });

export function blankStockTransferForm() {
    return {
        fromWarehouseId: "", toWarehouseId: "", expectedShipmentDate: "",
        expectedReceiptDate: "", reasonCode: "", reasonNote: "", notes: "",
        items: [blankTransferRow(1)],
    };
}

export function todayLocalIso(date = new Date()) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

function positiveId(value) {
    const number = Number(value);
    return Number.isSafeInteger(number) && number > 0;
}

function validQuantity(value) {
    const string = String(value ?? "");
    if (!/^[1-9]\d*$/.test(string)) return false;
    const quantity = Number(string);
    return Number.isSafeInteger(quantity) && quantity <= 2147483647;
}

// Keep error messages and field highlighting derived from the SAME checks.
// Each item is keyed by its clientKey so adding/removing rows cannot misplace errors.
export function validateStockTransferDraftDetailed(form, sources, destinations, products, today = todayLocalIso()) {
    const errors = [];
    const fieldErrors = { items: {} };
    const addError = (field, message, clientKey = null) => {
        errors.push(message);
        if (clientKey === null) {
            if (!fieldErrors[field]) fieldErrors[field] = message;
            return;
        }
        const rowErrors = fieldErrors.items[clientKey] ?? (fieldErrors.items[clientKey] = {});
        if (!rowErrors[field]) rowErrors[field] = message;
    };

    const source = sources.find((item) => String(item.id) === form.fromWarehouseId);
    const destination = destinations.find((item) => String(item.id) === form.toWarehouseId);
    if (!source || !["MAIN_WAREHOUSE", "STORE"].includes(source.type)) {
        addError("fromWarehouseId", "Select a valid source main warehouse or store.");
    }
    if (!destination || destination.type !== "STORE") {
        addError("toWarehouseId", "Select a valid destination store.");
    }
    if (source && destination && String(source.id) === String(destination.id)) {
        addError("toWarehouseId", "Source and destination cannot be the same.");
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(form.expectedShipmentDate || "")) {
        addError("expectedShipmentDate", "Select an expected ship date.");
    } else if (form.expectedShipmentDate < today) {
        addError("expectedShipmentDate", "Expected ship date cannot be in the past.");
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(form.expectedReceiptDate || "")) {
        addError("expectedReceiptDate", "Select an expected receipt date.");
    } else if (form.expectedShipmentDate && form.expectedReceiptDate < form.expectedShipmentDate) {
        addError("expectedReceiptDate", "Expected receipt date cannot be before the ship date.");
    }
    if (!TRANSFER_REASONS.some((reason) => reason.value === form.reasonCode)) {
        addError("reasonCode", "Select a valid transfer reason.");
    }
    if (form.reasonCode === "OTHER" && !form.reasonNote.trim()) {
        addError("reasonNote", "Please explain the other transfer reason.");
    }
    if (!Array.isArray(form.items) || !form.items.length) {
        addError("itemList", "Add at least one product.");
    } else {
        const seen = new Set();
        form.items.forEach((row, index) => {
            const label = `Product #${index + 1}`;
            const option = products.find((item) => String(item.productId) === row.productId);
            if (!positiveId(row.productId) || !option) {
                addError("productId", `${label}: Select a product with stock at the source.`, row.clientKey);
            } else if (seen.has(row.productId)) {
                addError("productId", `${label}: Duplicate product lines are not allowed.`, row.clientKey);
            } else {
                seen.add(row.productId);
            }
            if (!validQuantity(row.requestedQuantity)) {
                addError("requestedQuantity", `${label}: Quantity must be an integer between 1 and 2,147,483,647.`, row.clientKey);
            } else if (option) {
                const available = Number(option.availableQuantity);
                if (!Number.isSafeInteger(available) || available < 0) {
                    addError("requestedQuantity", `${label}: Available stock is invalid. Reload the data.`, row.clientKey);
                } else if (Number(row.requestedQuantity) > available) {
                    addError("requestedQuantity", `${label}: Requested ${row.requestedQuantity} exceeds available stock ${available}.`, row.clientKey);
                }
            }
        });
    }
    return { errors, fieldErrors };
}

// Preserve the original function for any other callers that expect a string array.
export function validateStockTransferDraft(form, sources, destinations, products, today = todayLocalIso()) {
    return validateStockTransferDraftDetailed(form, sources, destinations, products, today).errors;
}

// Create DTO for POST /api/stock-transfers.
export function toStockTransferPayload(form) {
    return {
        fromWarehouseId: Number(form.fromWarehouseId),
        toWarehouseId: Number(form.toWarehouseId),
        expectedShipmentDate: form.expectedShipmentDate,
        expectedReceiptDate: form.expectedReceiptDate,
        reasonCode: form.reasonCode,
        reasonNote: form.reasonCode === "OTHER" ? (form.reasonNote.trim() || null) : null,
        notes: form.notes.trim() || null,
        items: form.items.map((row) => ({
            productId: Number(row.productId),
            requestedQuantity: Number(row.requestedQuantity),
        })),
    };
}

// PUT UpdateStockTransferRequest deliberately excludes fromWarehouseId.
export function toUpdateStockTransferPayload(form) {
    const payload = toStockTransferPayload(form);
    return {
        toWarehouseId: payload.toWarehouseId,
        expectedShipmentDate: payload.expectedShipmentDate,
        expectedReceiptDate: payload.expectedReceiptDate,
        reasonCode: payload.reasonCode,
        reasonNote: payload.reasonNote,
        notes: payload.notes,
        items: payload.items,
    };
}

// Fill the form with the authoritative Backend detail, retaining immutable source.
export function stockTransferFormFromDetail(detail) {
    return {
        fromWarehouseId: String(detail.fromWarehouseId ?? ""),
        toWarehouseId: String(detail.toWarehouseId ?? ""),
        expectedShipmentDate: detail.expectedShipmentDate || "",
        expectedReceiptDate: detail.expectedReceiptDate || "",
        reasonCode: detail.reasonCode || "",
        reasonNote: detail.reasonNote || "",
        notes: detail.notes || "",
        items: (detail.items || []).map((item, index) => ({
            clientKey: index + 1,
            productId: String(item.productId ?? ""),
            requestedQuantity: String(item.requestedQuantity ?? ""),
        })),
    };
}

