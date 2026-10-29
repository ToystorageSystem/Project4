// Issue #11 - Form rules match CreateStockTransferRequest and TransferReasonCode.
export const TRANSFER_REASONS = [
    { value: "REBALANCE", label: "Điều phối lại tồn kho" },
    { value: "SLOW_MOVING", label: "Hàng bán chậm" },
    { value: "DAMAGED", label: "Hàng lỗi hoặc hư hỏng" },
    { value: "SURPLUS", label: "Hàng dư tại cửa hàng" },
    { value: "RECALL", label: "Thu hồi sản phẩm" },
    { value: "STORE_CLOSURE", label: "Đóng cửa hoặc chuyển cửa hàng" },
    { value: "WRONG_TRANSFER", label: "Điều chuyển sai" },
    { value: "OTHER", label: "Lý do khác" },
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

export function validateStockTransferDraft(form, sources, destinations, products, today = todayLocalIso()) {
    const errors = [];
    const source = sources.find((item) => String(item.id) === form.fromWarehouseId);
    const destination = destinations.find((item) => String(item.id) === form.toWarehouseId);
    if (!source || !["MAIN_WAREHOUSE", "STORE"].includes(source.type)) {
        errors.push("Chọn kho tổng hoặc cửa hàng xuất hàng hợp lệ.");
    }
    if (!destination || destination.type !== "STORE") {
        errors.push("Chọn cửa hàng nhận hàng hợp lệ.");
    }
    if (source && destination && String(source.id) === String(destination.id)) {
        errors.push("Điểm xuất và điểm nhận không được trùng nhau.");
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(form.expectedShipmentDate || "")) {
        errors.push("Chọn ngày dự kiến xuất hàng.");
    } else if (form.expectedShipmentDate < today) {
        errors.push("Ngày dự kiến xuất không được ở quá khứ.");
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(form.expectedReceiptDate || "")) {
        errors.push("Chọn ngày dự kiến nhận hàng.");
    } else if (form.expectedShipmentDate && form.expectedReceiptDate < form.expectedShipmentDate) {
        errors.push("Ngày dự kiến nhận không được trước ngày xuất.");
    }
    if (!TRANSFER_REASONS.some((reason) => reason.value === form.reasonCode)) {
        errors.push("Chọn lý do điều chuyển hợp lệ.");
    }
    if (form.reasonCode === "OTHER" && !form.reasonNote.trim()) {
        errors.push("Lý do khác phải có nội dung giải thích.");
    }
    if (!Array.isArray(form.items) || !form.items.length) {
        errors.push("Thêm ít nhất một sản phẩm.");
    } else {
        const seen = new Set();
        form.items.forEach((row, index) => {
            const label = `Sản phẩm #${index + 1}`;
            const option = products.find((item) => String(item.productId) === row.productId);
            if (!positiveId(row.productId) || !option) {
                errors.push(`${label}: Chọn sản phẩm có trong tồn kho của điểm xuất.`);
            } else if (seen.has(row.productId)) {
                errors.push(`${label}: Không được thêm trùng sản phẩm.`);
            } else {
                seen.add(row.productId);
            }
            if (!validQuantity(row.requestedQuantity)) {
                errors.push(`${label}: Số lượng phải là số nguyên từ 1 đến 2.147.483.647.`);
            } else if (option) {
                const available = Number(option.availableQuantity);
                if (!Number.isSafeInteger(available) || available < 0) {
                    errors.push(`${label}: Tồn khả dụng không hợp lệ, cần tải lại dữ liệu.`);
                } else if (Number(row.requestedQuantity) > available) {
                    errors.push(`${label}: Yêu cầu ${row.requestedQuantity} vượt tồn khả dụng ${available}.`);
                }
            }
        });
    }
    return errors;
}

// Create DTO for POST /api/stock-transfers.
export function toStockTransferPayload(form) {
    return {
        fromWarehouseId: Number(form.fromWarehouseId),
        toWarehouseId: Number(form.toWarehouseId),
        expectedShipmentDate: form.expectedShipmentDate,
        expectedReceiptDate: form.expectedReceiptDate,
        reasonCode: form.reasonCode,
        reasonNote: form.reasonNote.trim() || null,
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
