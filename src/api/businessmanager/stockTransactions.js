import api from "../api";

const BASE = "/business-manager/stock-transactions";

const cleanParams = (filters) =>
    Object.fromEntries(
        Object.entries(filters).filter(
            ([, value]) => value !== "" && value !== null && value !== undefined
        )
    );

export async function listStockTransactions(type, filters = {}, signal) {
    if (!["imports", "exports"].includes(type)) {
        throw new Error("Invalid stock transaction type");
    }

    const response = await api.get(`${BASE}/${type}`, {
        params: cleanParams(filters),
        signal,
    });

    return response.data;
}

export async function getStockTransactionDetail(type, id, signal) {
    if (!["imports", "exports"].includes(type)) {
        throw new Error("Invalid stock transaction type");
    }

    const response = await api.get(`${BASE}/${type}/${id}`, {
        signal,
    });

    return response.data;
}
