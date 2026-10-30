import api from "../api";

// Issue #4 - Business Staff: rejected product change requests.
// The backend response uses `items`, not `content`.
export async function listRejectedProductRequests({ page = 0, size = 10 } = {}, signal) {
    const response = await api.get("/products/rejected", {
        params: { page, size },
        signal,
    });
    return response.data;
}

export async function getRejectedProductRequest(requestId, signal) {
    const response = await api.get(
        `/products/rejected/${encodeURIComponent(requestId)}`,
        { signal }
    );
    return response.data;
}

export async function resubmitRejectedProductRequest(requestId, payload) {
    const response = await api.post(
        `/products/rejected/${encodeURIComponent(requestId)}/resubmit`,
        payload
    );
    return response.data;
}
