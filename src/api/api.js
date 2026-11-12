import axios from "axios";

const api = axios.create({
  baseURL: "/api",
  withCredentials: true,
  timeout: 30000,
});
// Ask for a fresh session token before each mutation. Login and MFA rotate it.
// Never retry mutations automatically: a response can be lost after a successful write.
api.interceptors.request.use(async (config) => {
  if (
    !["get", "head", "options"].includes((config.method || "get").toLowerCase())
  ) {
    const { data } = await api.get("/auth/csrf");
    config.headers.set(data.headerName || "X-CSRF-TOKEN", data.token);
  }
  return config;
});
api.interceptors.response.use(
  (r) => r,
  (error) => {
    if (
      error.response?.status === 401 &&
      !error.config?.url?.startsWith("/auth/")
    ) {
      window.dispatchEvent(new Event("session-expired"));
    }
    return Promise.reject(error);
  },
);
export function errorMessage(error) {
  const data = error?.response?.data;
  const message =
    typeof data === "string" && !data.startsWith("<") ? data : data?.message;
  const known = {
    INVALID_VERIFICATION:
      "The verification code is invalid or has already been used. Wait for a new code.",
    OTP_LOCKED_5_MINUTES: "Too many incorrect attempts. Please wait 5 minutes.",
    NEW_PASSWORD_INVALID:
      "Your new password must differ from your current password and be at most 72 bytes.",
    SECOND_FACTOR_REQUIRED: "Enter your Google Authenticator code or a recovery code.",
    WRONG_AUTH_STAGE: "Your authentication step has changed. Reload this page.",
    LOGIN_REQUIRED: "Your session has expired. Please sign in again.",
  };
  return (
    known[message] ||
    message ||
    {
      401: "Invalid credentials or expired session.",
      403: "You do not have permission to perform this action.",
      409: "The data has changed. Reload before continuing.",
      429: "Too many requests. Please try again later.",
    }[error?.response?.status] ||
    (error?.response
      ? "Unable to complete the request. Please try again."
      : "Unable to reach the server. Check your connection and try again.")
  );
}
export default api;
