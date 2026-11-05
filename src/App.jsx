
import { useEffect, useState } from "react";

import {
  Activity,
  AlertTriangle,
  ArrowUpRight,
  ChevronRight,
  ClipboardList,
  Database,
  LayoutDashboard,
  Menu,
  Package2,
  RefreshCw,
  ShieldCheck,
  Users,
  Warehouse,
} from "lucide-react";

// =====================================================
// PAGE IMPORTS
// =====================================================

import UserAccountsPage from "./UserAccountsPage";

import RolesPermissionsPage, {
  createDemoRoles,
} from "./RolesPermissionsPage";

import WarehousesPage from "./WarehousesPage";
import ActivityLogsPage from "./ActivityLogsPage";
import SecurityAlertsPage from "./SecurityAlertsPage";

// =====================================================
// LIVE API IMPORTS
// =====================================================

import {
  getAdminUsers,
} from "./api/admin/userAccountsApi";

import {
  getRoles,
} from "./api/admin/rolesPermissionsApi";

import {
  getSecurityAlerts,
} from "./api/admin/securityAlertsApi";

import "./css/App.css";

// =====================================================
// NAVIGATION
// =====================================================

const NAV_ITEMS = [
  {
    id: "dashboard",
    label: "Dashboard",
    issue: "",
    icon: LayoutDashboard,
  },
  {
    id: "users",
    label: "User Accounts",
    issue: "#23",
    icon: Users,
  },
  {
    id: "roles",
    label: "Roles & Permissions",
    issue: "#24",
    icon: ShieldCheck,
  },
  {
    id: "warehouses",
    label: "Warehouses & Stores",
    issue: "#25",
    icon: Warehouse,
  },
  {
    id: "logs",
    label: "Activity Logs",
    issue: "#26",
    icon: ClipboardList,
  },
  {
    id: "alerts",
    label: "Security Alerts",
    issue: "#27",
    icon: AlertTriangle,
  },
];

// =====================================================
// LEGACY PAGE STATE
//
// Giu du lieu tuong thich voi cac trang
// User Accounts, Roles va Warehouses hien tai.
//
// Du lieu nay khong duoc su dung cho Dashboard.
// Se bo khi hoan tat viec xoa Demo Mode trong
// tung component rieng.
// =====================================================

const INITIAL_USERS = [
  {
    code: "USR-001",
    name: "Alex Nguyen",
    email: "alex@example.com",
    role: "ADMIN",
    location: "Head Office",
    status: "ACTIVE",
  },
  {
    code: "USR-002",
    name: "Taylor Tran",
    email: "taylor@example.com",
    role: "BIZ_MANAGER",
    location: "Head Office",
    status: "ACTIVE",
  },
  {
    code: "USR-003",
    name: "Jamie Le",
    email: "jamie@example.com",
    role: "WH_MANAGER",
    location: "Main Warehouse HCM",
    status: "ACTIVE",
  },
  {
    code: "USR-004",
    name: "Morgan Pham",
    email: "morgan@example.com",
    role: "STORE_MANAGER",
    location: "District 1 Store",
    status: "LOCKED",
  },
  {
    code: "USR-005",
    name: "Riley Do",
    email: "riley@example.com",
    role: "BIZ_STAFF",
    location: "Head Office",
    status: "INACTIVE",
  },
];

const INITIAL_WAREHOUSES = [
  {
    id: 1,
    code: "WH-HCM",
    name: "Main Warehouse HCM",
    type: "MAIN_WAREHOUSE",
    address: "Ho Chi Minh City, Vietnam",
    phone: "0900000001",
    status: "ACTIVE",
    managerId: 101,
    pending: {
      purchaseOrders: 2,
      goodsReceipts: 0,
      stockTransfers: 1,
      shipments: 0,
    },
  },
  {
    id: 2,
    code: "WH-HN",
    name: "Main Warehouse Hanoi",
    type: "MAIN_WAREHOUSE",
    address: "Hanoi, Vietnam",
    phone: "0900000002",
    status: "ACTIVE",
    managerId: 103,
    pending: {
      purchaseOrders: 0,
      goodsReceipts: 0,
      stockTransfers: 0,
      shipments: 0,
    },
  },
  {
    id: 3,
    code: "ST-D1",
    name: "District 1 Store",
    type: "STORE",
    address: "District 1, Ho Chi Minh City",
    phone: "0900000003",
    status: "ACTIVE",
    managerId: 105,
    pending: {
      purchaseOrders: 0,
      goodsReceipts: 0,
      stockTransfers: 0,
      shipments: 1,
    },
  },
  {
    id: 4,
    code: "ST-D7",
    name: "District 7 Store",
    type: "STORE",
    address: "District 7, Ho Chi Minh City",
    phone: "0900000004",
    status: "INACTIVE",
    managerId: null,
    pending: {
      purchaseOrders: 0,
      goodsReceipts: 0,
      stockTransfers: 0,
      shipments: 0,
    },
  },
];

const INITIAL_EMPLOYEES = [
  {
    id: 101,
    name: "Jamie Le",
    role: "WH_MANAGER",
    warehouseId: 1,
  },
  {
    id: 102,
    name: "Taylor Pham",
    role: "WH_STAFF",
    warehouseId: 1,
  },
  {
    id: 103,
    name: "Casey Vo",
    role: "WH_MANAGER",
    warehouseId: 2,
  },
  {
    id: 104,
    name: "Minh Ho",
    role: "WH_STAFF",
    warehouseId: 2,
  },
  {
    id: 105,
    name: "Morgan Pham",
    role: "STORE_MANAGER",
    warehouseId: 3,
  },
  {
    id: 106,
    name: "Jordan Nguyen",
    role: "STORE_STAFF",
    warehouseId: 3,
  },
  {
    id: 107,
    name: "Robin Le",
    role: "STORE_MANAGER",
    warehouseId: null,
  },
  {
    id: 108,
    name: "Alex Bui",
    role: "WH_MANAGER",
    warehouseId: null,
  },
  {
    id: 109,
    name: "Linh Do",
    role: "WH_STAFF",
    warehouseId: null,
  },
  {
    id: 110,
    name: "Hoa Tran",
    role: "STORE_STAFF",
    warehouseId: null,
  },
];

// =====================================================
// LEGACY HELPERS
// =====================================================

function createDemoUsers() {
  return INITIAL_USERS.map((user) => ({
    ...user,
  }));
}

function createDemoWarehouses() {
  return INITIAL_WAREHOUSES.map((location) => ({
    ...location,
    pending: {
      ...location.pending,
    },
  }));
}

function createDemoEmployees() {
  return INITIAL_EMPLOYEES.map((employee) => ({
    ...employee,
  }));
}

// =====================================================
// ACTIVITY LOGS API
//
// Su dung API read-only da test trong Task #26.
// GET /api/admin/activity-logs?page=0&size=4
// =====================================================

async function fetchActivityLogSummary() {
  const response = await fetch(
    "/api/admin/activity-logs?page=0&size=4",
    {
      method: "GET",
      credentials: "include",
      cache: "no-store",
      headers: {
        Accept: "application/json",
      },
    }
  );

  if (!response.ok) {
    throw new Error(
      `Activity Logs request failed (${response.status}).`
    );
  }

  const data = await response.json();

  if (!Array.isArray(data?.content)) {
    throw new Error(
      "Unexpected Activity Logs API response."
    );
  }

  return data;
}

// =====================================================
// FORMAT HELPERS
// =====================================================

function formatLabel(value) {
  return String(value ?? "Unknown").replaceAll(
    "_",
    " "
  );
}

function formatTime(value) {
  if (!value) {
    return "N/A";
  }

  return String(value)
    .replace("T", " ")
    .slice(0, 19);
}

// =====================================================
// DASHBOARD METRIC CARD
// =====================================================

function MetricCard({
  icon: Icon,
  label,
  value,
  note,
  onClick,
  color,
}) {
  return (
    <button
      type="button"
      className="metric-card"
      onClick={onClick}
    >
      <span className={`metric-icon ${color}`}>
        <Icon size={22} />
      </span>

      <span className="metric-content">
        <span className="metric-label">
          {label}
        </span>

        <strong>{value}</strong>

        <small>{note}</small>
      </span>

      <ArrowUpRight
        className="metric-arrow"
        size={18}
      />
    </button>
  );
}

// =====================================================
// OPERATIONS DASHBOARD - LIVE MYSQL
//
// Dashboard khong con su dung:
// - Fake Security Alerts
// - Fake Activity Logs
// - Fake Weekly Chart
// =====================================================

function OverviewPage({ navigate }) {
  const [data, setData] = useState({
    users: null,
    roles: null,
    alerts: null,
    logs: null,
    recentLogs: [],
  });

  const [loading, setLoading] =
    useState(true);

  const [warning, setWarning] =
    useState("");

  const [reloadKey, setReloadKey] =
    useState(0);

  // ===================================================
  // LOAD DASHBOARD DATA
  // ===================================================

  useEffect(() => {
    let active = true;

    setLoading(true);
    setWarning("");

    Promise.allSettled([
      getAdminUsers(0, 1),
      getRoles(),
      getSecurityAlerts({
        page: 0,
        size: 1,
      }),
      fetchActivityLogSummary(),
    ]).then((results) => {
      if (!active) {
        return;
      }

      const [
        usersResult,
        rolesResult,
        alertsResult,
        logsResult,
      ] = results;

      const users =
        usersResult.status === "fulfilled"
          ? usersResult.value?.totalElements ?? null
          : null;

      const roles =
        rolesResult.status === "fulfilled" &&
        Array.isArray(rolesResult.value)
          ? rolesResult.value.length
          : null;

      const alerts =
        alertsResult.status === "fulfilled"
          ? alertsResult.value?.totalElements ?? null
          : null;

      const logs =
        logsResult.status === "fulfilled"
          ? logsResult.value?.totalElements ?? null
          : null;

      const recentLogs =
        logsResult.status === "fulfilled" &&
        Array.isArray(logsResult.value?.content)
          ? logsResult.value.content
          : [];

      setData({
        users,
        roles,
        alerts,
        logs,
        recentLogs,
      });

      if (
        results.some(
          (result) =>
            result.status === "rejected"
        )
      ) {
        setWarning(
          "Some metrics are unavailable. Check your Admin session and permissions."
        );
      }

      setLoading(false);
    });

    return () => {
      active = false;
    };
  }, [reloadKey]);

  // ===================================================
  // REFRESH
  // ===================================================

  function refreshDashboard() {
    setReloadKey((key) => key + 1);
  }

  function metricValue(value) {
    if (loading) {
      return "...";
    }

    return value ?? "—";
  }

  // ===================================================
  // RENDER
  // ===================================================

  return (
    <div className="page-stack">

      {/* DASHBOARD HEADER */}
      <div className="page-heading">
        <div>
          <span className="eyebrow">
            ADMINISTRATION / OVERVIEW
          </span>

          <h1>Operations Dashboard</h1>

          <p>
            Live overview of user accounts,
            roles, security alerts and audit logs.
          </p>
        </div>

        <button
          type="button"
          className="outline-tag"
          onClick={refreshDashboard}
          disabled={loading}
        >
          <RefreshCw size={14} />

          {loading
            ? "Refreshing..."
            : "Refresh"}
        </button>
      </div>

      {/* LOADING WARNING */}
      {warning && (
        <p
          className="table-footer"
          role="alert"
        >
          {warning}
        </p>
      )}

      {/* LIVE METRICS */}
      <div className="metric-grid">

        <MetricCard
          icon={Users}
          label="User Accounts"
          value={metricValue(data.users)}
          note="Live account records"
          color="blue"
          onClick={() => navigate("users")}
        />

        <MetricCard
          icon={ShieldCheck}
          label="System Roles"
          value={metricValue(data.roles)}
          note="Live role records"
          color="purple"
          onClick={() => navigate("roles")}
        />

        <MetricCard
          icon={AlertTriangle}
          label="Security Alerts"
          value={metricValue(data.alerts)}
          note="Live security alerts"
          color="orange"
          onClick={() => navigate("alerts")}
        />

        <MetricCard
          icon={Activity}
          label="Activity Logs"
          value={metricValue(data.logs)}
          note="Live audit events"
          color="green"
          onClick={() => navigate("logs")}
        />

      </div>

      {/* DASHBOARD WIDGETS */}
      <div className="dashboard-grid">

        {/* RECENT ACTIVITY */}
        <section className="panel recent-panel">

          <div className="panel-head">
            <div>
              <h2>Recent Activity</h2>

              <p>
                Latest audit entries from Backend
              </p>
            </div>

            <button
              type="button"
              className="text-link"
              onClick={() => navigate("logs")}
            >
              View all

              <ChevronRight size={15} />
            </button>
          </div>

          <div className="recent-list">
            {loading ? (
              <p className="table-footer">
                Loading recent activity...
              </p>
            ) : data.recentLogs.length === 0 ? (
              <p className="table-footer">
                {data.logs === null
                  ? "Recent activity is unavailable."
                  : "No activity logs found."}
              </p>
            ) : (
              data.recentLogs.map((entry) => (
                <div
                  className="recent-row"
                  key={entry.id}
                >
                  <span className="recent-icon">
                    <Activity size={17} />
                  </span>

                  <div className="recent-text">
                    <strong>
                      {formatLabel(entry.action)}
                      {" / "}
                      {formatLabel(entry.entityType)}
                    </strong>

                    <span>
                      {entry.actorName ||
                        "System / Unknown"}
                      {" · "}
                      Log #{entry.id}
                    </span>
                  </div>

                  <time>
                    {formatTime(entry.createdAt)}
                  </time>
                </div>
              ))
            )}
          </div>
        </section>

        {/* QUICK ACCESS */}
        <section className="panel quick-panel">

          <div className="panel-head">
            <div>
              <h2>Quick Access</h2>

              <p>
                Open an administration module
              </p>
            </div>
          </div>

          {NAV_ITEMS.filter(
            (item) => item.id !== "dashboard"
          ).map(
            ({
              id,
              icon: Icon,
              label,
              issue,
            }) => (
              <button
                className="quick-link"
                key={id}
                type="button"
                onClick={() => navigate(id)}
              >
                <span className="quick-link-icon">
                  <Icon size={17} />
                </span>

                <span>{label}</span>

                <small>{issue}</small>

                <ChevronRight size={16} />
              </button>
            )
          )}
        </section>

      </div>
    </div>
  );
}

// =====================================================
// MAIN APP
// =====================================================

export default function App() {
  const [activePage, setActivePage] =
    useState("dashboard");

  const [mobileMenuOpen, setMobileMenuOpen] =
    useState(false);

  // ===================================================
  // LEGACY PAGE STATE
  //
  // Giu nguyen props cua trang #23, #24, #25
  // de khong lam hong cac component hien tai.
  // ===================================================

  const [userRows, setUserRows] =
    useState(createDemoUsers);

  const [roleRows, setRoleRows] =
    useState(createDemoRoles);

  const [warehouseRows, setWarehouseRows] =
    useState(createDemoWarehouses);

  const [employeeRows, setEmployeeRows] =
    useState(createDemoEmployees);

  const currentPage = NAV_ITEMS.find(
    (item) => item.id === activePage
  );

  // ===================================================
  // NAVIGATION
  // ===================================================

  function navigate(pageId) {
    setActivePage(pageId);

    setMobileMenuOpen(false);

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  }

  // ===================================================
  // LEGACY RESET
  // ===================================================

  function resetDemoWarehouses() {
    setWarehouseRows(createDemoWarehouses());
    setEmployeeRows(createDemoEmployees());
  }

  // ===================================================
  // APP RENDER
  // ===================================================

  return (
    <div className="app-shell">

      {/* MOBILE OVERLAY */}
      {mobileMenuOpen && (
        <button
          type="button"
          className="menu-overlay"
          aria-label="Close navigation"
          onClick={() =>
            setMobileMenuOpen(false)
          }
        />
      )}

      {/* ===========================================
          SIDEBAR
          =========================================== */}

      <aside
        className={`sidebar ${
          mobileMenuOpen
            ? "sidebar-open"
            : ""
        }`}
      >
        {/* BRAND */}
        <div className="brand-block">
          <span className="brand-mark">
            <Package2 size={24} />
          </span>

          <div>
            <strong>ToyStorage</strong>

            <small>
              OPERATIONS PLATFORM
            </small>
          </div>
        </div>

        {/* NAVIGATION LABEL */}
        <div className="sidebar-section-label">
          WORKSPACE
        </div>

        {/* NAVIGATION */}
        <nav
          className="nav-list"
          aria-label="Main navigation"
        >
          {NAV_ITEMS.map(
            ({
              id,
              label,
              issue,
              icon: Icon,
            }) => (
              <button
                key={id}
                type="button"
                className={`nav-link ${
                  activePage === id
                    ? "selected"
                    : ""
                }`}
                onClick={() => navigate(id)}
              >
                <Icon size={19} />

                <span>{label}</span>

                {issue && (
                  <small>{issue}</small>
                )}
              </button>
            )
          )}
        </nav>

        {/* SIDEBAR FOOTER */}
        <div className="sidebar-footer">

          <span className="sidebar-foot-icon">
            <Database size={18} />
          </span>

          <div>
            <strong>
              Admin Workspace
            </strong>

            <small>
              ToyStorage Operations
            </small>
          </div>
        </div>
      </aside>

      {/* ===========================================
          MAIN COLUMN
          =========================================== */}

      <div className="main-column">

        {/* TOPBAR */}
        <header className="topbar">

          <div className="topbar-left">
            <button
              type="button"
              className="mobile-toggle"
              aria-label="Open navigation"
              onClick={() =>
                setMobileMenuOpen(true)
              }
            >
              <Menu size={23} />
            </button>

            <div className="breadcrumb">
              <span>Workspace</span>

              <ChevronRight size={15} />

              <strong>
                {currentPage?.label}
              </strong>
            </div>
          </div>

          <div className="topbar-right">

            <span className="preview-pill">
              <span className="preview-dot" />

              ADMIN WORKSPACE
            </span>

            <span className="profile-avatar">
              TS
            </span>
          </div>
        </header>

        {/* =========================================
            PAGE CONTENT
            ========================================= */}

        <main className="page-content">

          {/* DASHBOARD */}
          {activePage === "dashboard" ? (
            <OverviewPage navigate={navigate} />

          ) : activePage === "users" ? (

            <UserAccountsPage
              users={userRows}
              setUsers={setUserRows}
              onReset={() =>
                setUserRows(createDemoUsers())
              }
            />

          ) : activePage === "roles" ? (

            <RolesPermissionsPage
              roles={roleRows}
              setRoles={setRoleRows}
              onReset={() =>
                setRoleRows(createDemoRoles())
              }
            />

          ) : activePage === "warehouses" ? (

            <WarehousesPage
              warehouses={warehouseRows}
              setWarehouses={setWarehouseRows}
              employees={employeeRows}
              setEmployees={setEmployeeRows}
              onReset={resetDemoWarehouses}
            />

          ) : activePage === "logs" ? (

            <ActivityLogsPage />

          ) : null}

          {/* =======================================
              ISSUE #27 - SECURITY ALERTS

              Giu component mounted khi chuyen trang.
              ======================================= */}

          <div
            style={{
              display:
                activePage === "alerts"
                  ? "block"
                  : "none",
            }}
          >
            <SecurityAlertsPage />
          </div>

        </main>

        {/* FOOTER */}
        <footer className="main-footer">
          ToyStorageSystem · Administration · 2026
        </footer>

      </div>
    </div>
  );
}
