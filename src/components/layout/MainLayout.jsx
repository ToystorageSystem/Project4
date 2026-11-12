import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import Sidebar from "./Sidebar";
import { menuFor, navigation } from "./navigation";
import { logout } from "../../api/auth/auth";
import { errorMessage } from "../../api/api";
import { useAuth } from "../../auth/AuthContext";
import { roleLabel } from "../../auth/access";
import WorkspaceIcon from "../ui/WorkspaceIcon";
export default function MainLayout({ children }) {
  const { user, clearUser } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const items = menuFor(user);
  const current = navigation
    .filter((i) => pathname === i.path || pathname.startsWith(i.path + "/"))
    .sort((a, b) => b.path.length - a.path.length)[0];
  async function signOut() {
    setBusy(true);
    setError("");
    try {
      await logout();
      clearUser();
      navigate("/login", { replace: true });
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="workspace-shell">
      <a className="skip-link" href="#main-content">
        Skip to content
      </a>
      <Sidebar items={items} user={user} />
      <div className="workspace-body">
        <header className="workspace-topbar">
          <div className="breadcrumb">
            My workspace <span>/</span>
            <strong>{current?.label || "Details"}</strong>
          </div>
          <div className="topbar-user">
            <div className="user-info">
              <strong>{user?.name || user?.email}</strong>
              <small>{roleLabel(user)}</small>
            </div>
            <span className="user-avatar">
              {(user?.name || "T").charAt(0).toUpperCase()}
            </span>
            <button disabled={busy} className="sign-out" onClick={signOut}>
              <WorkspaceIcon name="logout" size={16} />{busy ? "Signing out…" : "Sign out"}
            </button>
          </div>
        </header>
        {error && (
          <p role="alert" className="ops-alert">
            {error}
          </p>
        )}
        <main id="main-content" tabIndex={-1} className="workspace-content">
          {children}
        </main>
      </div>
    </div>
  );
}
