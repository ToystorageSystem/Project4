
import { useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import { authApi } from "../api/authApi";
import "../css/AuthPage.css";

export default function TwoFactorSetupPage({
  onAuthenticated,
}) {
  const [setupData, setSetupData] = useState(null);
  const [otp, setOtp] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Tao ma QR de thiet lap Google Authenticator
  const handleSetup = async () => {
    if (loading || setupData) return;

    setLoading(true);
    setError("");

    try {
      const response = await authApi.setup2FA();

      if (
        !response?.otpAuthUri ||
        !response?.manualEntryKey
      ) {
        throw new Error(
          "Invalid authenticator setup response."
        );
      }

      setSetupData(response);
    } catch (err) {
      setError(
        err.message ||
        "Unable to set up two-factor authentication."
      );
    } finally {
      setLoading(false);
    }
  };

  // Xac nhan OTP sau khi quet QR
  const handleConfirm = async (event) => {
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
      const response = await authApi.confirm2FA(otp);

      if (response?.loginStatus !== "AUTHENTICATED") {
        throw new Error(
          "Two-factor authentication was not completed."
        );
      }

      setOtp("");
      setSetupData(null);

      // Bao cho component cha biet da dang nhap thanh cong
      onAuthenticated(response);
    } catch (err) {
      setError(
        err.message ||
        "Invalid verification code. Please try again."
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
          <h2>Set Up Two-Factor Authentication</h2>
          <p>
            Protect your account with Google Authenticator.
            You only need to complete this setup once.
          </p>
        </div>

        {!setupData ? (
          <button
            type="button"
            className="auth-submit"
            onClick={handleSetup}
            disabled={loading}
          >
            {loading
              ? "Preparing Authenticator..."
              : "Set Up Authenticator"}
          </button>
        ) : (
          <>
            <div className="auth-brand">
              <QRCodeSVG
                value={setupData.otpAuthUri}
                size={190}
                level="M"
                includeMargin
              />
            </div>

            <div className="auth-heading">
              <p>
                Open Google Authenticator, tap the plus
                button, and scan the QR code above.
              </p>
            </div>

            <div className="auth-field">
              <label htmlFor="auth-manual-key">
                Manual Setup Key
              </label>

              <input
                id="auth-manual-key"
                type="text"
                value={setupData.manualEntryKey}
                readOnly
                autoComplete="off"
              />
            </div>

            <div className="auth-heading">
              <p>
                Keep your setup key private. Never share
                your QR code or setup key with anyone.
              </p>
            </div>

            <form onSubmit={handleConfirm}>
              <div className="auth-field">
                <label htmlFor="auth-setup-otp">
                  Verification Code
                </label>

                <input
                  id="auth-setup-otp"
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]{6}"
                  maxLength={6}
                  value={otp}
                  onChange={(event) =>
                    setOtp(
                      event.target.value
                        .replace(/\D/g, "")
                        .slice(0, 6)
                    )
                  }
                  placeholder="Enter 6-digit code"
                  autoComplete="one-time-code"
                  disabled={loading}
                  required
                />
              </div>

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
          </>
        )}

        {error && (
          <div className="auth-error" role="alert">
            {error}
          </div>
        )}

        <div className="auth-footer">
          Secure your account with two-factor authentication.
        </div>
      </div>
    </div>
  );
}
