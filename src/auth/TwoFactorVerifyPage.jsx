
import { useState } from "react";
import { authApi } from "../api/authApi";
import "../css/AuthPage.css";

export default function TwoFactorVerifyPage({
  onAuthenticated,
  onBackToLogin,
}) {
  const [otp, setOtp] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Xu ly ma OTP chi gom 6 chu so
  const handleOtpChange = (event) => {
    const value = event.target.value
      .replace(/\D/g, "")
      .slice(0, 6);

    setOtp(value);
    setError("");
  };

  // Xac minh OTP voi Backend
  const handleVerify = async (event) => {
    event.preventDefault();

    if (loading) return;

    setError("");

    if (!/^\d{6}$/.test(otp)) {
      setError("Please enter a valid 6-digit code.");
      return;
    }

    if (typeof onAuthenticated !== "function") {
      setError("Authentication flow is not connected yet.");
      return;
    }

    setLoading(true);

    try {
      const response = await authApi.verify2FA(otp);

      if (response?.loginStatus !== "AUTHENTICATED") {
        throw new Error(
          "Two-factor authentication was not completed."
        );
      }

      // Khong luu OTP trong localStorage
      setOtp("");

      // Chuyen ket qua cho component cha
      onAuthenticated(response);
    } catch (err) {
      // Xoa OTP cu de tranh gui lai ma het han
      setOtp("");

      if (err.status === 401) {
        setError(
          "Your authentication session has expired. Please sign in again."
        );
      } else {
        setError(
          err.message ||
          "Invalid verification code. Please try again."
        );
      }
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
          <h2>Two-Factor Authentication</h2>
          <p>
            Open Google Authenticator and enter
            the 6-digit verification code.
          </p>
        </div>

        <form onSubmit={handleVerify}>
          <div className="auth-field">
            <label htmlFor="auth-verify-otp">
              Verification Code
            </label>

            <input
              id="auth-verify-otp"
              className="auth-otp-input"
              type="text"
              inputMode="numeric"
              pattern="[0-9]{6}"
              maxLength={6}
              value={otp}
              onChange={handleOtpChange}
              placeholder="Enter 6-digit code"
              autoComplete="one-time-code"
              autoFocus
              required
              disabled={loading}
            />
          </div>

          {error && (
            <div
              className="auth-error"
              role="alert"
            >
              {error}
            </div>
          )}

          <button
            type="submit"
            className="auth-submit"
            disabled={loading || otp.length !== 6}
          >
            {loading
              ? "Verifying..."
              : "Verify and Continue"}
          </button>
        </form>

        {typeof onBackToLogin === "function" && (
          <button
            type="button"
            className="auth-back-button"
            onClick={onBackToLogin}
            disabled={loading}
          >
            Back to Login
          </button>
        )}

        <div className="auth-footer">
          Your verification code changes every 30 seconds.
          Never share your code with anyone.
        </div>

      </div>
    </div>
  );
}
