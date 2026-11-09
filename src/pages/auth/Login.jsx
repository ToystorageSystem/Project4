import { useEffect, useState } from "react";
import {
  Link,
  useLocation,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import * as auth from "../../api/auth/auth";
import { errorMessage } from "../../api/api";
import { useAuth } from "../../auth/AuthContext";
import { authPath } from "../../auth/access";

const titles = {
  "/login": "Sign in",
  "/change-password": "Change password",
  "/2fa/setup": "Set up Authenticator",
  "/2fa": "Verify your account",
  "/forgot-password": "Forgot password",
  "/reset-password": "Reset password",
};
export default function Login() {
  const { user, setUser, loading, error: sessionError, refresh } = useAuth();
  const { pathname, hash } = useLocation();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const resetToken = new URLSearchParams(hash.slice(1)).get("token") || params.get("token") || "";
  const [form, setForm] = useState({
    email: "",
    password: "",
    currentPassword: "",
    newPassword: "",
    confirm: "",
    code: "",
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [setup, setSetup] = useState(null);
  const [setupRetry, setSetupRetry] = useState(0);
  const [recovery, setRecovery] = useState(false);
  const [codes, setCodes] = useState(null);
  const protectedStep = ["/change-password", "/2fa/setup", "/2fa"].includes(
    pathname,
  );
  useEffect(() => {
    setError("");
    setMessage("");
    setRecovery(false);
    setForm((f) => ({
      ...f,
      password: "",
      currentPassword: "",
      newPassword: "",
      confirm: "",
      code: "",
    }));
  }, [pathname]);
  useEffect(() => {
    if (loading || codes || sessionError) return;
    if (
      user &&
      !["/forgot-password", "/reset-password"].includes(pathname) &&
      authPath(user) !== pathname
    )
      navigate(authPath(user), { replace: true });
    if (!user && protectedStep) navigate("/login", { replace: true });
  }, [user, loading, pathname, navigate, codes, protectedStep, sessionError]);
  useEffect(() => {
    if (pathname !== "/2fa/setup" || user?.stage !== "SETUP_2FA") return;
    let active = true;
    setSetup(null);
    auth
      .getTwoFactorSetup()
      .then((data) => {
        if (active) setSetup(data);
      })
      .catch((e) => {
        if (active) setError(errorMessage(e));
      });
    return () => {
      active = false;
    };
  }, [pathname, user?.stage, setupRetry]);
  function field(name, label, type = "text", extra = {}) {
    return (
      <label className="auth-field">
        <span>{label}</span>
        <input
          name={name}
          type={type}
          value={form[name]}
          onChange={(e) => setForm({ ...form, [name]: e.target.value })}
          required
          {...extra}
        />
      </label>
    );
  }
  async function submit(event) {
    event.preventDefault();
    setError("");
    setMessage("");
    if (["/change-password", "/reset-password"].includes(pathname)) {
      if (form.newPassword !== form.confirm) {
        setError("Your new passwords do not match.");
        return;
      }
      if (form.newPassword.length < 6) {
        setError("Your password must be at least 6 characters.");
        return;
      }
      if (new TextEncoder().encode(form.newPassword).length > 72) {
        setError("Your password must be at most 72 bytes.");
        return;
      }
    }
    setBusy(true);
    try {
      let data;
      if (pathname === "/login")
        data = await auth.login({
          email: form.email.trim(),
          password: form.password,
        });
      else if (pathname === "/change-password")
        data = await auth.changePassword({
          currentPassword: form.currentPassword,
          newPassword: form.newPassword,
        });
      else if (pathname === "/2fa/setup")
        data = await auth.confirmTwoFactor(form.code);
      else if (pathname === "/2fa")
        data = await (recovery ? auth.recoverTwoFactor : auth.verifyTwoFactor)(
          form.code.trim(),
        );
      else if (pathname === "/forgot-password") {
        await auth.forgotPassword(form.email.trim());
        setMessage(
          "If this email is registered, a temporary password will be sent to your inbox.",
        );
        return;
      } else {
        data = await auth.resetPassword({
          token: resetToken,
          newPassword: form.newPassword,
          ...(form.code.trim()
            ? { [recovery ? "recoveryCode" : "otp"]: form.code.trim() }
            : {}),
        });
      }
      // Recovery codes are shown once and never stored in browser storage.
      if (data.recoveryCodes?.length) {
        setCodes(data.recoveryCodes);
        setUser(data);
      } else {
        setUser(data);
        navigate(authPath(data), { replace: true });
      }
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
      setForm((f) => ({ ...f, password: "", currentPassword: "", code: "" }));
    }
  }
  const title = titles[pathname] || titles["/login"];
  return (
    <div className="auth-shell">
      <main className="auth-main">
        <div className="auth-form-wrap">
          <Link className="auth-brand" to="/login"><img src="/logo.png" alt="" width="44" height="44" /><span>ToyStorage</span></Link>
          <h1>{codes ? "Recovery codes" : title}</h1>
          {loading ? (
            <p role="status">Checking your session…</p>
          ) : sessionError ? (
            <>
              <p role="alert">{errorMessage(sessionError)}</p>
              <button className="ops-button" onClick={refresh}>
                Reconnect
              </button>
            </>
          ) : codes ? (
            <>
              <p>
                Each code can be used once if you lose access to Authenticator.
                Keep them somewhere safe before continuing.
              </p>
              <div className="recovery-codes">
                {codes.map((code) => (
                  <code key={code}>{code}</code>
                ))}
              </div>
              <button
                className="ops-button"
                onClick={() => {
                  setCodes(null);
                  navigate(authPath(user), { replace: true });
                }}
              >
                I've saved my codes. Continue
              </button>
            </>
          ) : (
            <form onSubmit={submit} className="auth-form">
              {error && (
                <p className="ops-alert" role="alert">
                  {error}
                </p>
              )}
              {message && (
                <p className="ops-notice" role="status">
                  {message}
                </p>
              )}
              {["/login", "/forgot-password"].includes(pathname) &&
                field("email", "Email", "email", {
                  autoComplete: "username",
                  maxLength: 150,
                })}
              {pathname === "/login" &&
                field("password", "Password", "password", {
                  autoComplete: "current-password",
                })}
              {pathname === "/change-password" &&
                field("currentPassword", "Current password", "password", {
                  autoComplete: "current-password",
                })}
              {["/change-password", "/reset-password"].includes(pathname) && (
                <>
                  {field(
                    "newPassword",
                    "New password",
                    "password",
                    {
                      autoComplete: "new-password",
                      minLength: 6,
                      maxLength: 72,
                    },
                  )}
                  {field("confirm", "Confirm new password", "password", {
                    autoComplete: "new-password",
                    minLength: 6,
                    maxLength: 72,
                  })}
                </>
              )}
              {pathname === "/2fa/setup" && (
                <>
                  {setup ? (
                    <div className="auth-enrollment">
                      <img
                        src={setup.qrDataUrl}
                        width="180"
                        height="180"
                        alt="Google Authenticator setup QR code"
                      />
                      <p>
                        Open Google Authenticator → Add account → Scan QR code.
                      </p>
                      <details>
                        <summary>Enter the key manually</summary>
                        <code>{setup.manualKey}</code>
                      </details>
                    </div>
                  ) : (
                    <p role="status">
                      Loading your QR code…{" "}
                      <button
                        type="button"
                        onClick={() => setSetupRetry((v) => v + 1)}
                      >
                        Try again
                      </button>
                    </p>
                  )}
                </>
              )}
              {["/2fa", "/2fa/setup", "/reset-password"].includes(pathname) && (
                <>
                  {field(
                    "code",
                    recovery
                      ? "Recovery code"
                      : "Google Authenticator code (6 digits)",
                    "text",
                    recovery
                      ? {
                          autoComplete: "off",
                          maxLength: 64,
                          required: pathname !== "/reset-password",
                        }
                      : {
                          inputMode: "numeric",
                          pattern: "[0-9]{6}",
                          maxLength: 6,
                          autoComplete: "one-time-code",
                          required: pathname !== "/reset-password",
                        },
                  )}
                  {pathname !== "/2fa/setup" && (
                    <button
                      type="button"
                      className="text-link"
                      onClick={() => {
                        setRecovery(!recovery);
                        setForm({ ...form, code: "" });
                      }}
                    >
                      {recovery
                        ? "Use an Authenticator code"
                        : "Use a recovery code"}
                    </button>
                  )}
                </>
              )}
              <button
                className="ops-button"
                disabled={
                  busy ||
                  (pathname === "/2fa/setup" && !setup) ||
                  (pathname === "/reset-password" && !resetToken)
                }
              >
                {busy
                  ? "Processing…"
                  : pathname === "/forgot-password"
                    ? "Send temporary password"
                    : pathname === "/login"
                      ? "Sign in"
                      : "Continue"}
              </button>
              {pathname === "/login" ? (
                <Link className="text-link" to="/forgot-password">
                  Forgot your password?
                </Link>
              ) : (
                <Link
                  className="text-link"
                  to="/login"
                  onClick={async (e) => {
                    if (user) {
                      e.preventDefault();
                      try {
                        await auth.logout();
                        setUser(null);
                        navigate("/login");
                      } catch (err) {
                        setError(errorMessage(err));
                      }
                    }
                  }}
                >
                  Back to sign in
                </Link>
              )}
            </form>
          )}
        </div>
      </main>
    </div>
  );
}
