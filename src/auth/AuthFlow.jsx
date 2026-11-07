
import { useState } from "react";

import { authApi } from "../api/authApi";
import LoginPage from "./LoginPage";
import ChangePasswordPage from "./ChangePasswordPage";
import TwoFactorSetupPage from "./TwoFactorSetupPage";
import TwoFactorVerifyPage from "./TwoFactorVerifyPage";

import "../css/AuthPage.css";

export default function AuthFlow({ onAuthenticated }) {
  const [step, setStep] = useState("LOGIN");
  const [flowError, setFlowError] = useState("");

  // Chuyen man hinh theo trang thai Backend
  const handleAuthResult = (response) => {
    const nextStatus = response?.loginStatus;

    setFlowError("");

    switch (nextStatus) {
      case "REQUIRE_CHANGE_PASSWORD":
      case "REQUIRE_2FA_SETUP":
      case "REQUIRE_2FA_VERIFICATION":
        setStep(nextStatus);
        break;

      case "AUTHENTICATED":
        setStep("AUTHENTICATED");

        // App cha se xu ly dashboard sau khi tich hop
        if (typeof onAuthenticated === "function") {
          onAuthenticated(response);
        }
        break;

      default:
        setFlowError(
          "Unexpected authentication response."
        );
    }
  };

  // Quay ve Login va ket thuc session hien tai
  const handleBackToLogin = async () => {
    setFlowError("");

    try {
      await authApi.logout();
      setStep("LOGIN");
    } catch (error) {
      setFlowError(
        error.message ||
        "Unable to end the authentication session."
      );
    }
  };

  // Hien thi loi dieu huong neu co
  if (flowError) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <div className="auth-heading">
            <h2>Authentication Error</h2>
            <p>
              Please review the error and try again.
            </p>
          </div>

          <div className="auth-error" role="alert">
            {flowError}
          </div>

          <button
            type="button"
            className="auth-submit"
            onClick={handleBackToLogin}
            style={{ marginTop: 20 }}
          >
            Retry Sign In
          </button>
        </div>
      </div>
    );
  }

  // Man hinh dang nhap
  if (step === "LOGIN") {
    return (
      <LoginPage
        onLoginResult={handleAuthResult}
      />
    );
  }

  // Doi mat khau bat buoc lan dau
  if (step === "REQUIRE_CHANGE_PASSWORD") {
    return (
      <ChangePasswordPage
        onPasswordChanged={handleAuthResult}
      />
    );
  }

  // Thiet lap Google Authenticator
  if (step === "REQUIRE_2FA_SETUP") {
    return (
      <TwoFactorSetupPage
        onAuthenticated={handleAuthResult}
      />
    );
  }

  // Xac minh OTP khi dang nhap lan sau
  if (step === "REQUIRE_2FA_VERIFICATION") {
    return (
      <TwoFactorVerifyPage
        onAuthenticated={handleAuthResult}
        onBackToLogin={handleBackToLogin}
      />
    );
  }

  // Da hoan thanh xac thuc
  // Chua tu dong vao Dashboard khi chua tich hop App
  if (step === "AUTHENTICATED") {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <div className="auth-brand">
            <h1>ToyStorageSystem</h1>
            <p>Inventory Management System</p>
          </div>

          <div className="auth-heading">
            <h2>Authentication Successful</h2>
            <p>
              Your identity has been verified.
              Dashboard integration is not enabled yet.
            </p>
          </div>

          <button
            type="button"
            className="auth-submit"
            onClick={handleBackToLogin}
          >
            Sign Out
          </button>
        </div>
      </div>
    );
  }

  return null;
}
