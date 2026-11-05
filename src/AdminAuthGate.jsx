
import { useCallback, useEffect, useState } from "react";

import {
  KeyRound,
  LogOut,
  RefreshCw,
  ShieldCheck,
} from "lucide-react";

import App from "./App.jsx";
import "./css/AdminAuthGate.css";

const AUTH_BASE = "/api/auth";

// ======================================================
// AUTH API
// ======================================================

async function authRequest(path, { body, ...options } = {}) {
  const response = await fetch(`${AUTH_BASE}${path}`, {
    ...options,
    credentials: "include",
    cache: "no-store",
    headers: {
      Accept: "application/json",
      ...(body !== undefined
        ? { "Content-Type": "application/json" }
        : {}),
      ...options.headers,
    },
    ...(body !== undefined
      ? { body: JSON.stringify(body) }
      : {}),
  });

  const raw = await response.text();
  let data = null;

  if (raw) {
    try {
      data = JSON.parse(raw);
    } catch {
      data = null;
    }
  }

  if (!response.ok) {
    const error = new Error(
      typeof data?.message === "string"
        ? data.message
        : `Request failed (${response.status}).`
    );

    error.status = response.status;
    error.code = data?.code;

    throw error;
  }

  return data;
}

function getSession() {
  return authRequest("/me", {
    method: "GET",
  });
}

function login(email, password) {
  return authRequest("/login", {
    method: "POST",
    body: { email, password },
  });
}

function logout() {
  return authRequest("/logout", {
    method: "POST",
  });
}

function changePassword(oldPassword, newPassword) {
  return authRequest("/change-password", {
    method: "PATCH",
    body: { oldPassword, newPassword },
  });
}

function isAdmin(data) {
  return (
    Array.isArray(data?.authorities) &&
    data.authorities.includes("ROLE_ADMIN")
  );
}

// ======================================================
// SHARED ADMIN AUTHENTICATION
// ======================================================

export default function AdminAuthGate() {
  const [status, setStatus] = useState("checking");
  const [session, setSession] = useState(null);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  // ====================================================
  // CHECK EXISTING SESSION
  // ====================================================

  const verifySession = useCallback(async () => {
    setStatus("checking");
    setError("");

    try {
      const current = await getSession();

      setSession(current);

      setStatus(
        isAdmin(current)
          ? "authenticated"
          : "forbidden"
      );
    } catch (err) {
      setSession(null);

      if (err.status === 401) {
        setStatus("signedOut");
      } else if (
        err.code === "PASSWORD_CHANGE_REQUIRED"
      ) {
        setStatus("changePassword");
      } else {
        setError(
          err.message || "Unable to verify the session."
        );
        setStatus("unavailable");
      }
    }
  }, []);

  useEffect(() => {
    void verifySession();
  }, [verifySession]);

  // ====================================================
  // LOGIN
  // ====================================================

  async function handleLogin(event) {
    event.preventDefault();

    if (busy) return;

    setBusy(true);
    setError("");
    setNotice("");

    try {
      const result = await login(
        email.trim(),
        password
      );

      setPassword("");

      // Khong cho tai khoan thieu role ADMIN
      // truy cap Admin Workspace.
      if (!isAdmin(result)) {
        setSession(result);
        setStatus("forbidden");
        return;
      }

      // Tai khoan bat buoc doi mat khau.
      if (result.mustChangePassword) {
        setStatus("changePassword");
        return;
      }

      // Kiem tra lai session sau login.
      const current = await getSession();

      setSession(current);

      setStatus(
        isAdmin(current)
          ? "authenticated"
          : "forbidden"
      );
    } catch (err) {
      setPassword("");

      if (
        err.code === "PASSWORD_CHANGE_REQUIRED"
      ) {
        setStatus("changePassword");
      } else {
        setError(
          err.message || "Sign in failed."
        );
      }
    } finally {
      setBusy(false);
    }
  }

  // ====================================================
  // LOGOUT
  // ====================================================

  async function handleLogout() {
    if (busy) return;

    setBusy(true);
    setError("");

    try {
      // POST /api/auth/logout
      // Huy HTTP Session tai Backend.
      await logout();

      setSession(null);
      setEmail("");
      setPassword("");
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");

      setNotice("Signed out successfully.");
      setStatus("signedOut");
    } catch (err) {
      setError(
        err.message ||
          "Could not sign out. Please try again."
      );
    } finally {
      setBusy(false);
    }
  }

  // ====================================================
  // CHANGE REQUIRED PASSWORD
  // ====================================================

  async function handlePasswordChange(event) {
    event.preventDefault();

    if (busy) return;

    if (newPassword !== confirmPassword) {
      setError("The new passwords do not match.");
      return;
    }

    if (newPassword.length < 6) {
      setError(
        "The new password must contain at least 6 characters."
      );
      return;
    }

    setBusy(true);
    setError("");

    try {
      await changePassword(
        oldPassword,
        newPassword
      );

      // Dang xuat sau khi doi mat khau.
      // Yeu cau user dang nhap lai.
      await logout();

      setSession(null);
      setPassword("");
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");

      setNotice(
        "Password updated. Please sign in again."
      );

      setStatus("signedOut");
    } catch (err) {
      setError(
        err.message || "Unable to change password."
      );
    } finally {
      setBusy(false);
    }
  }

  // ====================================================
  // AUTHENTICATED -> RENDER ADMIN APP
  // ====================================================

  if (status === "authenticated") {
    return (
      <>
        <App />

        <div className="admin-auth-session-bar">
          <span
            className="admin-auth-session-name"
            title={session?.username || "Admin"}
          >
            {session?.username || "Admin"}
          </span>

          <button
            type="button"
            onClick={handleLogout}
            disabled={busy}
          >
            <LogOut size={15} />

            {busy
              ? "Signing out..."
              : "Log out"}
          </button>

          {error && (
            <span
              className="admin-auth-session-error"
              role="alert"
            >
              {error}
            </span>
          )}
        </div>
      </>
    );
  }

  // ====================================================
  // LOGIN / AUTHENTICATION SCREEN
  // ====================================================

  return (
    <main className="admin-auth-screen">
      <section className="admin-auth-card">
        <div className="admin-auth-logo">
          <ShieldCheck size={29} />
        </div>

        <div className="admin-auth-eyebrow">
          TOYSTORAGE · ADMINISTRATION
        </div>

        <h1>
          {status === "checking"
            ? "Checking your session"
            : status === "forbidden"
              ? "Admin access required"
              : status === "unavailable"
                ? "Unable to connect"
                : status === "changePassword"
                  ? "Change your password"
                  : "Admin Sign In"}
        </h1>

        <p className="admin-auth-subtitle">
          {status === "checking"
            ? "Verifying your existing sign-in..."
            : status === "forbidden"
              ? "This account does not have the ROLE_ADMIN permission."
              : status === "unavailable"
                ? "The server could not verify your session."
                : status === "changePassword"
                  ? "Your account must change its password before using Admin features."
                  : "Sign in once to access all Admin tasks."}
        </p>

        {notice && (
          <p
            className="admin-auth-notice"
            role="status"
          >
            {notice}
          </p>
        )}

        {error && (
          <p
            className="admin-auth-error"
            role="alert"
          >
            {error}
          </p>
        )}

        {/* CHECKING */}
        {status === "checking" && (
          <div className="admin-auth-loading">
            Connecting...
          </div>
        )}

        {/* LOGIN */}
        {status === "signedOut" && (
          <form
            className="admin-auth-form"
            onSubmit={handleLogin}
          >
            <label>
              Email

              <input
                type="email"
                required
                autoComplete="username"
                value={email}
                onChange={(event) =>
                  setEmail(event.target.value)
                }
                placeholder="Enter your email"
                disabled={busy}
              />
            </label>

            <label>
              Password

              <input
                type="password"
                required
                autoComplete="current-password"
                value={password}
                onChange={(event) =>
                  setPassword(event.target.value)
                }
                placeholder="Enter your password"
                disabled={busy}
              />
            </label>

            <button
              className="admin-auth-primary"
              disabled={busy}
              type="submit"
            >
              <KeyRound size={17} />

              {busy
                ? "Signing in..."
                : "Sign In"}
            </button>
          </form>
        )}

        {/* CHANGE PASSWORD */}
        {status === "changePassword" && (
          <form
            className="admin-auth-form"
            onSubmit={handlePasswordChange}
          >
            <label>
              Current password

              <input
                type="password"
                required
                autoComplete="current-password"
                value={oldPassword}
                disabled={busy}
                onChange={(event) =>
                  setOldPassword(event.target.value)
                }
              />
            </label>

            <label>
              New password

              <input
                type="password"
                required
                minLength={6}
                autoComplete="new-password"
                value={newPassword}
                disabled={busy}
                onChange={(event) =>
                  setNewPassword(event.target.value)
                }
              />
            </label>

            <label>
              Confirm new password

              <input
                type="password"
                required
                minLength={6}
                autoComplete="new-password"
                value={confirmPassword}
                disabled={busy}
                onChange={(event) =>
                  setConfirmPassword(event.target.value)
                }
              />
            </label>

            <button
              className="admin-auth-primary"
              type="submit"
              disabled={busy}
            >
              {busy
                ? "Updating..."
                : "Update Password"}
            </button>

            <button
              className="admin-auth-secondary"
              type="button"
              disabled={busy}
              onClick={handleLogout}
            >
              Sign out instead
            </button>
          </form>
        )}

        {/* ACCOUNT WITHOUT ADMIN ROLE */}
        {status === "forbidden" && (
          <button
            className="admin-auth-primary"
            type="button"
            onClick={handleLogout}
            disabled={busy}
          >
            <LogOut size={17} />

            {busy
              ? "Signing out..."
              : "Sign out and switch account"}
          </button>
        )}

        {/* CONNECTION ERROR */}
        {status === "unavailable" && (
          <button
            className="admin-auth-primary"
            type="button"
            onClick={() => void verifySession()}
          >
            <RefreshCw size={17} />
            Retry connection
          </button>
        )}

        <p className="admin-auth-footnote">
          Your session is managed securely by Spring Boot.
        </p>
      </section>
    </main>
  );
}
