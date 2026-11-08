import { sessionFetch as fetch } from "../../api/sessionFetch";

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
const NAV_ITEMS = [
  {id:"dashboard",label:"Dashboard",icon:LayoutDashboard},
  {id:"users",label:"User Accounts",icon:Users},
  {id:"roles",label:"Roles & Permissions",icon:ShieldCheck},
  {id:"warehouses",label:"Warehouses & Stores",icon:Warehouse},
  {id:"logs",label:"Activity Logs",icon:ClipboardList},
  {id:"alerts",label:"Security Alerts",icon:AlertTriangle},
];

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
// OPERATIONS DASHBOARD - ACCOUNT DATA
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


export default OverviewPage;

