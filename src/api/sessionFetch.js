import api from "./api";

// Fetch-based modules share the same session, CSRF policy and expiry handling as Axios modules.
// Keep the native Response so CSV downloads and source API error parsers keep working.
export async function sessionFetch(input, options = {}) {
  const url = String(input);
  if (!url.startsWith("/api/")) throw new Error("Session requests must use the same-origin /api path.");
  const headers = new Headers(options.headers);
  const method = (options.method || "GET").toUpperCase();
  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    const { data } = await api.get("/auth/csrf", { signal: options.signal });
    headers.set(data.headerName || "X-CSRF-TOKEN", data.token);
  }
  const response = await globalThis.fetch(input, { ...options, headers, credentials: "include" });
  if (response.status === 401 && !url.startsWith("/api/auth/")) window.dispatchEvent(new Event("session-expired"));
  return response;
}
