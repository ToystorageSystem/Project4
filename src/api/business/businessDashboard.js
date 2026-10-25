import api from "../api";

// Read-only integration checks; no INSERT/UPDATE/DELETE requests.
export async function getBusinessStaffSuppliers() {
    const response = await api.get("/business-staff/suppliers");
    return response.data;
}

export async function getBusinessManagerInventory() {
    const response = await api.get("/business-manager/reports/inventory");
    return response.data;
}
