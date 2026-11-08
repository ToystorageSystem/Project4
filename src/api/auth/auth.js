import api from "../api";
export const login = async (data) => (await api.post("/auth/login", data)).data;
export const logout = async () => {
  await api.post("/auth/logout");
};
export const getAuthStatus = async () => (await api.get("/auth/status")).data;
export const changePassword = async (data) =>
  (await api.post("/auth/change-password", data)).data;
export const getTwoFactorSetup = async () =>
  (await api.get("/auth/2fa/setup")).data;
export const confirmTwoFactor = async (code) =>
  (await api.post("/auth/2fa/confirm", { code })).data;
export const verifyTwoFactor = async (code) =>
  (await api.post("/auth/2fa/verify", { code })).data;
export const recoverTwoFactor = async (code) =>
  (await api.post("/auth/2fa/recover", { code })).data;
export const forgotPassword = async (email) =>
  (await api.post("/auth/forgot-password", { email })).data;
export const resetPassword = async (data) =>
  (await api.post("/auth/reset-password", data)).data;
