
import { useState } from "react";
import "../css/AuthPage.css";
import { authApi } from "../api/authApi";

// Trang dang nhap cho ToyStorageSystem.
// Viec chuyen sang Change Password hoac OTP
// se do component cha xu ly.

export default function LoginPage({ onLoginResult }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleLogin = async (event) => {
    event.preventDefault();

    if (loading) return;

    setError("");

    if (!email.trim() || !password) {
      setError("Please enter your email and password.");
      return;
    }

    setLoading(true);

    try {
      const response = await authApi.login(
        email.trim(),
        password
      );

      const validStatuses = [
        "REQUIRE_CHANGE_PASSWORD",
        "REQUIRE_2FA_SETUP",
        "REQUIRE_2FA_VERIFICATION",
        "AUTHENTICATED",
      ];

      if (!validStatuses.includes(response?.loginStatus)) {
        throw new Error(
          "Unexpected authentication response."
        );
      }

      // Khong giu lai mat khau sau khi dang nhap.
      setPassword("");

      // Component cha quyet dinh buoc tiep theo.
      if (typeof onLoginResult !== "function") {
        throw new Error(
          "Authentication flow is not connected yet."
        );
      }

      onLoginResult(response);
    } catch (err) {
      setError(
        err.message || "Login failed. Please try again."
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
          <h2>Welcome Back</h2>
          <p>Sign in to your account to continue.</p>
        </div>

        <form onSubmit={handleLogin}>
          <div className="auth-field">
            <label htmlFor="auth-email">
              Email Address
            </label>

            <input
              id="auth-email"
              type="email"
              value={email}
              onChange={(event) =>
                setEmail(event.target.value)
              }
              placeholder="Enter your email"
              autoComplete="username"
              required
              disabled={loading}
            />
          </div>

          <div className="auth-field">
            <label htmlFor="auth-password">
              Password
            </label>

            <input
              id="auth-password"
              type={showPassword ? "text" : "password"}
              value={password}
              onChange={(event) =>
                setPassword(event.target.value)
              }
              placeholder="Enter your password"
              autoComplete="current-password"
              required
              disabled={loading}
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
              Show password
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
            {loading ? "Signing in..." : "Sign In"}
          </button>
        </form>

        <div className="auth-footer">
          Protected by two-factor authentication
        </div>
      </div>
    </div>
  );
}
