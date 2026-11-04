import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Sidebar from "./Sidebar";
import { warehouseMenuItems } from "../icons/warehouseMenuItems.jsx";
import { logout } from "../../api/auth/auth.js";
import { useAuth } from "../../auth/AuthContext.jsx";

function formatRole(role) {
  if (typeof role === "object" && role)
    role = role.roleName ?? role.name ?? role.roleCode;
  return String(role || "Warehouse")
    .replaceAll("_", " ")
    .toLowerCase();
}

export default function MainLayout({ children }) {
  const { user, clearUser } = useAuth();
  const navigate = useNavigate();
  const [loggingOut, setLoggingOut] = useState(false);
  const name = user?.name ?? user?.fullName ?? user?.email ?? "User";
  const role = formatRole(
    user?.roleName ??
      user?.role ??
      user?.roleCode ??
      user?.roles?.[0] ??
      user?.userRoles?.[0]?.role,
  );
  async function handleLogout() {
    if (loggingOut) return;
    setLoggingOut(true);
    try {
      await logout();
    } catch (error) {
      console.error("Logout error:", error);
    } finally {
      clearUser();
      navigate("/login", { replace: true });
    }
  }
  return (
    <div className="workspace-shell min-h-screen bg-slate-50 md:flex">
      <a href="#main-content" className="skip-link">
        Skip to content
      </a>
      <Sidebar title="ToyStorage" items={warehouseMenuItems} />
      <div className="min-w-0 flex-1">
        <header className="flex h-16 items-center justify-between gap-3 border-b border-slate-200 bg-white px-4 md:px-7 print:hidden">
          <div className="text-sm font-medium text-slate-500">
            Workspace <span className="mx-2 text-slate-300">/</span>
            <span className="text-slate-800">Warehouse</span>
          </div>
          <div className="flex min-w-0 items-center gap-3">
            <div className="hidden min-w-0 text-right sm:block">
              <p className="max-w-48 truncate text-sm font-semibold">{name}</p>
              <p className="text-xs capitalize text-slate-500">{role}</p>
            </div>
            <span
              aria-hidden="true"
              className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-orange-50 text-sm font-bold text-orange-700"
            >
              {String(name).charAt(0).toUpperCase()}
            </span>
            <button
              type="button"
              disabled={loggingOut}
              onClick={handleLogout}
              className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 disabled:opacity-50"
            >
              {loggingOut ? "Signing out…" : "Sign out"}
            </button>
          </div>
        </header>
        <main
          id="main-content"
          tabIndex={-1}
          className="mx-auto flex w-full max-w-[1600px] flex-col p-4 md:p-7 print:p-0"
        >
          {children}
        </main>
      </div>
    </div>
  );
}
