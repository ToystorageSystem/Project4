import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import Sidebar from "./Sidebar";
import { menuFor, navigation } from "./navigation";
import { logout } from "../../api/auth/auth";
import { errorMessage } from "../../api/api";
import { useAuth } from "../../auth/AuthContext";
import { roleLabel } from "../../auth/access";
import WorkspaceIcon from "../ui/WorkspaceIcon";
import { useWorkspacePreferences } from "../../hooks/useWorkspacePersonalization";
export default function MainLayout({ children }) {
  const { user, clearUser } = useAuth();
  const { compact, update } = useWorkspacePreferences(user);
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
    <div className={`workspace-shell ${compact ? "workspace-compact" : ""}`}>
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
            <label className="density-control"><input type="checkbox" checked={compact} onChange={e => update({ compact: e.target.checked })} />Compact view</label>
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
        <footer className="workspace-footer">
          <span>ToyStorage</span><span>A place for every little thing.</span>
          <span>Your warehouse, in order.</span>
        </footer>
      </div>
    </div>
  );
}
