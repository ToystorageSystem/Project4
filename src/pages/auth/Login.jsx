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
import { useWorkspaceClock } from "../../hooks/useWorkspacePersonalization";
import WorkspaceIcon from "../../components/ui/WorkspaceIcon";

const titles = {
  "/login": ["Welcome back,", "let's start your shift."],
  "/change-password": ["First things first,", "set your own password."],
  "/2fa/setup": ["One more layer", "of account security."],
  "/2fa": ["Verify", "to continue."],
  "/forgot-password": ["Forgot your password?", "Let's get you back in."],
  "/reset-password": ["Restore", "your access."],
};
export default function Login() {
  const clock = useWorkspaceClock();
  const { user, setUser, loading, error: sessionError, refresh } = useAuth();
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const [params] = useSearchParams();
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
          "If this email is registered, you will receive a password reset link. Check your spam folder too.",
        );
        return;
      } else {
        data = await auth.resetPassword({
          token: params.get("token") || "",
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
  const [first, second] = titles[pathname] || titles["/login"];
  return (
    <div className="auth-shell">
      <aside className="auth-story">
        <Link className="brand-lockup" to="/login">
          <span className="brand-logo">
            <img src="/logo.png" alt="ToyStorage logo" width="46" height="46" />
          </span>
          <span>
            ToyStorage<span className="brand-caption">A little more organised.</span>
          </span>
        </Link>
        <div className="auth-story-body">
          <div className="auth-time"><WorkspaceIcon name={clock.icon} size={24} /><span>{clock.now.toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "long" })}</span></div>
          <h1>
            A place for every
            <br />
            <span>little thing.</span>
          </h1>
          <p>
            From the first receipt to the last handover.
            <br />
            The details are yours. Keeping them together is ours.
          </p>
          <ul className="auth-workflow"><li><WorkspaceIcon name="inbound" /><div><strong>Receive with care</strong><span>Inspect arrivals and capture the details.</span></div></li><li><WorkspaceIcon name="inventory" /><div><strong>Keep things in order</strong><span>Know what is on hand and where it belongs.</span></div></li><li><WorkspaceIcon name="outbound" /><div><strong>Send it on its way</strong><span>Pick, pack and hand over with confidence.</span></div></li></ul>
        </div>
        <span className="auth-story-foot">
          Your warehouse. Your rhythm.
        </span>
      </aside>
      <main className="auth-main">
        <div className="auth-form-wrap">
          <h2>
            {codes ? (
              "Save your recovery codes."
            ) : (
              <>
                {pathname === "/login" ? `${clock.greeting}.` : first}
                <br />
                {pathname === "/login" ? "Make yourself at home." : second}
              </>
            )}
          </h2>
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
                I've saved my codes. Continue →
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
                field("email", "Work email", "email", {
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
                    "New password · at least 12 characters",
                    "password",
                    {
                      autoComplete: "new-password",
                      minLength: 12,
                      maxLength: 72,
                    },
                  )}
                  {field("confirm", "Confirm new password", "password", {
                    autoComplete: "new-password",
                    minLength: 12,
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
              {pathname === "/reset-password" && (
                <p className="auth-hint">
                  If 2FA is not enabled on your account, leave the code blank.
                  After the reset, you will set up Authenticator again.
                </p>
              )}
              <button
                className="ops-button"
                disabled={
                  busy ||
                  (pathname === "/2fa/setup" && !setup) ||
                  (pathname === "/reset-password" && !params.get("token"))
                }
              >
                {busy
                  ? "Processing…"
                  : pathname === "/forgot-password"
                    ? "Send reset instructions →"
                    : pathname === "/login"
                      ? "Sign in →"
                      : "Continue →"}
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
          <p className="auth-fineprint">
            Accounts are provided by your system administrator.
            <br />
            Your session expires after 15 minutes of inactivity.
          </p>
        </div>
      </main>
    </div>
  );
}
