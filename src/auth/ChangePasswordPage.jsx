
import { useState } from "react";
import { authApi } from "../api/authApi";
import "../css/AuthPage.css";

export default function ChangePasswordPage({
  onPasswordChanged,
}) {
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Xu ly doi mat khau bat buoc
  const handleSubmit = async (event) => {
    event.preventDefault();

    if (loading) return;

    setError("");

    // Kiem tra cac truong bat buoc
    if (!oldPassword || !newPassword || !confirmPassword) {
      setError("Please complete all password fields.");
      return;
    }

    // Mat khau moi toi thieu 6 ky tu
    if (newPassword.length < 6) {
      setError(
        "New password must contain at least 6 characters."
      );
      return;
    }

    // Khong cho phep mat khau moi trung mat khau cu
    if (newPassword === oldPassword) {
      setError(
        "New password must differ from the current password."
      );
      return;
    }

    // Xac nhan mat khau moi
    if (newPassword !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    if (typeof onPasswordChanged !== "function") {
      setError("Authentication flow is not connected yet.");
      return;
    }

    setLoading(true);

    try {
      // Goi Backend doi mat khau
      const response = await authApi.changePassword(
        oldPassword,
        newPassword
      );

      const nextStatus = response?.loginStatus;

      // Sau khi doi mat khau van phai xac thuc 2FA
      if (
        nextStatus !== "REQUIRE_2FA_SETUP" &&
        nextStatus !== "REQUIRE_2FA_VERIFICATION"
      ) {
        throw new Error(
          "Unexpected password change response."
        );
      }

      // Xoa mat khau khoi state sau khi thanh cong
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");

      // Chuyen sang buoc xac thuc 2FA
      onPasswordChanged(response);

    } catch (err) {
      setError(
        err.message || "Unable to change password."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">

        <div className="auth-brand">
          <h1>ToyStorageSystem</h1>
          <p>Inventory Management System</p>
        </div>

        <div className="auth-heading">
          <h2>Change Your Password</h2>
          <p>
            You must change your temporary password
            before accessing your account.
          </p>
        </div>

        <form onSubmit={handleSubmit}>

          <div className="auth-field">
            <label htmlFor="auth-old-password">
              Current Password
            </label>

            <input
              id="auth-old-password"
              type={showPassword ? "text" : "password"}
              value={oldPassword}
              onChange={(event) =>
                setOldPassword(event.target.value)
              }
              placeholder="Enter current password"
              autoComplete="current-password"
              disabled={loading}
              required
            />
          </div>

          <div className="auth-field">
            <label htmlFor="auth-new-password">
              New Password
            </label>

            <input
              id="auth-new-password"
              type={showPassword ? "text" : "password"}
              value={newPassword}
              onChange={(event) =>
                setNewPassword(event.target.value)
              }
              placeholder="Enter new password"
              autoComplete="new-password"
              minLength={6}
              disabled={loading}
              required
            />
          </div>

          <div className="auth-field">
            <label htmlFor="auth-confirm-password">
              Confirm New Password
            </label>

            <input
              id="auth-confirm-password"
              type={showPassword ? "text" : "password"}
              value={confirmPassword}
              onChange={(event) =>
                setConfirmPassword(event.target.value)
              }
              placeholder="Confirm new password"
              autoComplete="new-password"
              minLength={6}
              disabled={loading}
              required
            />

            <label className="auth-show-password">
              <input
                type="checkbox"
                checked={showPassword}
                onChange={(event) =>
                  setShowPassword(event.target.checked)
                }
                disabled={loading}
              />
              Show passwords
            </label>
          </div>

          {error && (
            <div className="auth-error" role="alert">
              {error}
            </div>
          )}

          <button
            type="submit"
            className="auth-submit"
            disabled={loading}
          >
            {loading
              ? "Changing Password..."
              : "Change Password"}
          </button>
        </form>

        <div className="auth-footer">
          Your password must contain at least 6 characters.
        </div>

      </div>
    </div>
  );
}
