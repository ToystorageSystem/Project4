
const BASE_URL = "/auth2fa";

// Goi API bang session cookie
async function authRequest(path, method = "GET", body = null) {
  const options = {
    method,
    credentials: "include",
    cache: "no-store",
    headers: {
      "Content-Type": "application/json",
    },
  };

  if (body !== null) {
    options.body = JSON.stringify(body);
  }

  let response;

  try {
    response = await fetch(`${BASE_URL}${path}`, options);
  } catch {
    throw new Error(
      "Unable to connect to the authentication server."
    );
  }

  const contentType =
    response.headers.get("content-type") || "";

  let data = null;

  if (contentType.includes("application/json")) {
    data = await response.json();
  }

  if (!response.ok) {
    const message =
      data?.message ||
      data?.detail ||
      (typeof data?.error === "string" && data.error) ||
      `Request failed (${response.status}).`;

    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  return data;
}

export const authApi = {
  // 1. Dang nhap bang email va mat khau
  login(email, password) {
    return authRequest("/login", "POST", {
      email,
      password,
    });
  },

  // 2. Doi mat khau bat buoc lan dau
  changePassword(oldPassword, newPassword) {
    return authRequest("/change-password", "PATCH", {
      oldPassword,
      newPassword,
    });
  },

  // 3. Thiet lap Google Authenticator
  setup2FA() {
    return authRequest("/2fa/setup", "POST");
  },

  // 4. Xac nhan OTP khi thiet lap lan dau
  confirm2FA(code) {
    return authRequest("/2fa/confirm", "POST", {
      code,
    });
  },

  // 5. Xac minh OTP khi dang nhap lan sau
  verify2FA(code) {
    return authRequest("/2fa/verify", "POST", {
      code,
    });
  },

  // 6. Dang xuat
  logout() {
    return authRequest("/logout", "POST");
  },
};
