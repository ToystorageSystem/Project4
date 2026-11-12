/**
 * Gom ton kho theo cap (san pham, kho/cua hang), khong hien thi vi tri ke.
 * Available duoc cong tu du lieu backend: khong tu tinh lai bang physical - reserved,
 * vi hang dang cach ly/cho nhap co the chua san sang de cap phat.
 */
export function aggregateWarehouseProducts(records, { physicalKey = "onHandQuantity" } = {}) {
    const groups = new Map();
    for (const item of records || []) {
        if (!item) continue;
        const warehouseKey = String(item.warehouseId ?? item.warehouseCode ?? item.warehouseName ?? "unknown");
        const productKey = String(item.productId ?? item.productCode ?? "unknown");
        const key = JSON.stringify([warehouseKey, productKey]);
        let group = groups.get(key);
        if (!group) {
            group = {
                ...item,
                inventoryBalanceId: key,
                quantity: 0,
                onHandQuantity: 0,
                reservedQuantity: 0,
                availableQuantity: 0,
                stockStatus: "OUT_OF_STOCK",
            };
            delete group.locationId;
            delete group.locationCode;
            delete group.locationName;
            delete group.locationType;
            delete group.zone;
            delete group.shelf;
            delete group.minimumStockLevel;
            groups.set(key, group);
        }
        const n = (field) => Number.isFinite(Number(item[field])) ? Number(item[field]) : 0;
        group.quantity += n(physicalKey);
        group.onHandQuantity += n(physicalKey);
        group.reservedQuantity += n("reservedQuantity");
        group.availableQuantity += n("availableQuantity");
        if (item.lastUpdatedAt && (!group.lastUpdatedAt || item.lastUpdatedAt > group.lastUpdatedAt)) {
            group.lastUpdatedAt = item.lastUpdatedAt;
        }
    }
    for (const group of groups.values()) {
        group.stockStatus = group.availableQuantity > 0 ? "IN_STOCK" : "OUT_OF_STOCK";
    }
    return [...groups.values()].sort((a, b) =>
        String(a.warehouseName ?? "").localeCompare(String(b.warehouseName ?? ""))
        || String(a.productName ?? "").localeCompare(String(b.productName ?? "")));
}
