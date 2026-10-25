import api from "../../api";

export async function getPendingWarehouseGoodsReceipts(signal) {
    const response = await api.get("/goods-receipts/warehouse", {
        params: { status: "CREATED" },
        signal,
    });
    return response.data;
}
