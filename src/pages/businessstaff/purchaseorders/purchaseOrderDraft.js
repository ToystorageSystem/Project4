// Issue #9: pure FE validation. Backend is authoritative for all business rules.
const isPositiveId = (value) => Number.isSafeInteger(Number(value)) && Number(value) > 0;
const isQuantity = (value) => /^[1-9]\d*$/.test(String(value)) && Number(value) <= 2147483647;

function isDate(value) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(String(value || ""))) return false;
    const date = new Date(`${value}T00:00:00Z`);
    return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value;
}

export function validatePurchaseOrderDraft(form, productOptions) {
    const problems = [];
    if (!isPositiveId(form.supplierId)) problems.push("Chọn nhà cung cấp hợp lệ.");
    if (!isPositiveId(form.warehouseId)) problems.push("Chọn kho tổng nhận hàng hợp lệ.");
    if (!isDate(form.expectedDeliveryDate)) problems.push("Nhập ngày dự kiến giao hàng hợp lệ.");
    if (!Array.isArray(form.items) || form.items.length === 0) {
        problems.push("Đơn mua hàng phải có ít nhất một sản phẩm.");
    }

    const seen = new Set();
    (form.items || []).forEach((row, index) => {
        const label = `Dòng ${index + 1}`;
        const productId = Number(row.productId);
        if (!isPositiveId(row.productId)) {
            problems.push(`${label}: Chọn sản phẩm.`);
            return;
        }
        if (seen.has(productId)) problems.push(`${label}: Sản phẩm bị trùng trong đơn.`);
        seen.add(productId);

        const option = (productOptions || []).find((item) => Number(item.productId) === productId);
        if (!option) {
            problems.push(`${label}: Sản phẩm không còn được phép mua từ nhà cung cấp này.`);
            return;
        }
        if (!isQuantity(row.orderedQuantity)) {
            problems.push(`${label}: Số lượng phải là số nguyên từ 1 đến 2.147.483.647.`);
        } else if (Number(row.orderedQuantity) < Math.max(1, Number(option.minimumOrderQuantity || 1))) {
            problems.push(`${label}: Số lượng phải đạt MOQ ${option.minimumOrderQuantity || 1}.`);
        }
        if (!Number.isFinite(Number(option.purchasePrice)) || Number(option.purchasePrice) <= 0) {
            problems.push(`${label}: Giá nhập từ liên kết NCC không hợp lệ.`);
        }
    });
    return problems;
}

export function toPurchaseOrderPayload(form) {
    return {
        supplierId: Number(form.supplierId),
        warehouseId: Number(form.warehouseId),
        expectedDeliveryDate: form.expectedDeliveryDate,
        note: form.note.trim() || null,
        items: form.items.map((row) => ({
            productId: Number(row.productId),
            orderedQuantity: Number(row.orderedQuantity),
            // Deliberately OMIT unitPrice: BE uses active supplier_products.purchase_price.
        })),
    };
}
