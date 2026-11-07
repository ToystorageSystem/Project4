
import { useCallback, useEffect, useState } from "react";
import { LogOut, RefreshCw, ShieldCheck } from "lucide-react";

import App from "./App.jsx";
import AuthFlow from "./auth/AuthFlow.jsx";

import "./css/AdminAuthGate.css";

// Kiem tra quyen Admin sau khi hoan tat 2FA.
// Ho tro authority hien tai cua Backend va ten chuan.
function isAdmin(session) {
  const authorities = session?.authorities;

  return (
    Array.isArray(authorities) &&
    (
      authorities.includes("ROLE_ROLE-ADMIN") ||
      authorities.includes("ROLE_ADMIN")
    )
  );
}

export default function IntegratedAdminAuthGate() {
  const [status, setStatus] = useState("checking");
  const [session, setSession] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [flowKey, setFlowKey] = useState(0);

  // Kiem tra session thuc te tren Backend.
  // Khong tin vao authorities cua response Login ban dau.
  const verifySession = useCallback(async () => {
    setStatus("checking");
    setError("");

    try {
      const response = await fetch("/api/auth/me", {
        method: "GET",
        credentials: "include",
        cache: "no-store",
        headers: {
          Accept: "application/json",
        },
      });

      if (response.status === 401) {
        setSession(null);
        setStatus("signedOut");
        return;
      }

      if (!response.ok) {
        throw new Error(
          `Unable to verify session (HTTP ${response.status}).`
        );
      }

      const current = await response.json();

      setSession(current);

      if (isAdmin(current)) {
        setStatus("authenticated");
      } else {
        setStatus("forbidden");
      }
    } catch (err) {
      setError(
        err.message || "Unable to connect to the server."
      );
      setStatus("unavailable");
    }
  }, []);

  // Kiem tra session khi mo ung dung.
  useEffect(() => {
    void verifySession();
  }, [verifySession]);

  // Chi duoc goi khi AuthFlow hoan thanh OTP.
  const handleAuthenticated = () => {
    void verifySession();
  };

  // Dang xuat va huy session Backend.
  const handleLogout = async () => {
    if (busy) return;

    setBusy(true);
    setError("");

    try {
      const response = await fetch("/api/auth/logout", {
        method: "POST",
        credentials: "include",
        cache: "no-store",
      });

      if (!response.ok) {
        throw new Error(
          `Unable to sign out (HTTP ${response.status}).`
        );
      }

      setSession(null);
      setStatus("signedOut");
      setFlowKey((current) => current + 1);
    } catch (err) {
      setError(
        err.message || "Unable to sign out."
      );
    } finally {
      setBusy(false);
    }
  };

  // Chua dang nhap: hien thi Login + Google Authenticator.
  if (status === "signedOut") {
    return (
      <AuthFlow
        key={flowKey}
        onAuthenticated={handleAuthenticated}
      />
    );
  }

  // Da xac thuc OTP va co quyen Admin.
  if (status === "authenticated") {
    return (
      <>
        <App />

        <div className="admin-auth-session-bar">
          <span className="admin-auth-session-name">
            {session?.username || "Admin"}
          </span>

          <button
            type="button"
            onClick={handleLogout}
            disabled={busy}
          >
            <LogOut size={15} />
            {busy ? "Signing out..." : "Log out"}
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

  // Dang kiem tra, khong co quyen, hoac loi ket noi.
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
              : "Unable to connect"}
        </h1>

        <p className="admin-auth-subtitle">
          {status === "checking"
            ? "Verifying your authentication session..."
            : status === "forbidden"
              ? "Your account does not have permission to access the Admin Dashboard."
              : "The server could not verify your session."}
        </p>

        {error && (
          <p className="admin-auth-error" role="alert">
            {error}
          </p>
        )}

        {status === "checking" && (
          <div className="admin-auth-loading">
            Connecting...
          </div>
        )}

        {status === "forbidden" && (
          <button
            type="button"
            className="admin-auth-primary"
            onClick={handleLogout}
            disabled={busy}
          >
            <LogOut size={17} />
            Sign out and switch account
          </button>
        )}

        {status === "unavailable" && (
          <button
            type="button"
            className="admin-auth-primary"
            onClick={() => void verifySession()}
          >
            <RefreshCw size={17} />
            Retry connection
          </button>
        )}
      </section>
    </main>
  );
}
