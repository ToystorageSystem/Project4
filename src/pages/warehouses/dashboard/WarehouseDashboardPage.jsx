import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../../api/api";
import MainLayout from "../../../components/layout/MainLayout";
import { useAuth } from "../../../auth/AuthContext";
import {
  isManager,
  isStaff,
  isDelivery,
  roleLabel,
} from "../../../auth/access";
import { menuFor } from "../../../components/layout/navigation";
import { number, Status } from "../../../components/operations/WorkspaceUI";
import WorkspaceIcon, { navigationIcon } from "../../../components/ui/WorkspaceIcon";
import { displayName, useWorkspaceClock, useWorkspacePreferences } from "../../../hooks/useWorkspacePersonalization";

const managerSources = [
  {
    label: "Receiving in progress",
    hint: "Receipts being inspected",
    path: "/goods-receipts/monitoring?size=5",
    to: "/warehouse/receiving/in-progress",
    type: "page",
    mark: "↓",
  },
  {
    label: "Awaiting your review",
    hint: "Receipts awaiting a decision",
    path: "/goods-receipts/waiting-review?size=5",
    to: "/warehouse/receiving/waiting-review",
    type: "page",
    mark: "◎",
  },
  {
    label: "Open putaway tasks",
    hint: "Tasks still in progress",
    path: "/putaway-plans",
    to: "/warehouse/putaway",
    filter: (r) => !["COMPLETED", "CANCELLED"].includes(r.status),
    mark: "▤",
  },
  {
    label: "Packing review queue",
    hint: "Packed transfers",
    path: "/stock-transfers/packing-confirmations",
    to: "/warehouse/packing",
    type: "page",
    mark: "↗",
  },
];
const staffSources = [
  {
    label: "Ready to receive",
    hint: "Available receipts",
    path: "/warehouse/goods-receipts/receiving",
    to: "/warehouse/receiving/process",
    mark: "↓",
  },
  {
    label: "Picking",
    hint: "Transfers ready for picking",
    path: "/warehouse/picking/transfers",
    to: "/warehouse/picking",
    mark: "↗",
  },
  {
    label: "Putaway",
    hint: "Warehouse tasks",
    path: "/warehouse/putaway/my-tasks",
    to: "/warehouse/staff-putaway",
    filter: (r) => !["COMPLETED", "CANCELLED"].includes(r.status),
    mark: "▤",
  },
  {
    label: "Stock counts",
    hint: "Stock counting tasks",
    path: "/warehouse/staff/stock-counts",
    to: "/warehouse/staff-counts",
    mark: "≡",
  },
];
function parse(data, source) {
  if (source.type === "page") {
    if (!Array.isArray(data?.content) || !Number.isFinite(data.totalElements))
      throw Error("Unexpected response");
    return { count: data.totalElements, rows: data.content };
  }
  if (!Array.isArray(data)) throw Error("Unexpected response");
  const rows = source.filter ? data.filter(source.filter) : data;
  return { count: rows.length, rows };
}
function recordCode(row) {
  return (
    row.receiptCode ||
    row.transferCode ||
    row.putawayCode ||
    row.taskCode ||
    row.countCode ||
    row.shipmentCode ||
    "—"
  );
}
export default function WarehouseDashboardPage() {
  const { user } = useAuth();
  const clock = useWorkspaceClock();
  const { pins, update } = useWorkspacePreferences(user);
  const manager = isManager(user),
    staff = isStaff(user),
    delivery = isDelivery(user);
  const sources = useMemo(
    () =>
      manager
        ? managerSources
        : staff
          ? staffSources
          : delivery
            ? [
                {
                  label: "Active deliveries",
                  hint: "Your delivery schedule",
                  path: "/delivery-staff/trips",
                  to: "/warehouse/trips",
                  mark: "↗",
                },
              ]
            : [],
    [manager, staff, delivery],
  );
  const [data, setData] = useState([]),
    [loading, setLoading] = useState(true),
    [refresh, setRefresh] = useState(0),
    [updated, setUpdated] = useState(null);
  useEffect(() => {
    let active = true;
    setLoading(true);
    setData([]);
    Promise.allSettled(
      sources.map((s) => api.get(s.path).then((r) => parse(r.data, s))),
    ).then((results) => {
      if (!active) return;
      setData(
        results.map((r) =>
          r.status === "fulfilled" ? r.value : { error: true },
        ),
      );
      setUpdated(new Date());
      setLoading(false);
    });
    return () => {
      active = false;
    };
  }, [sources, refresh]);
  const failed = data.some((d) => d.error),
    available = menuFor(user).filter((i) => i.key !== "dashboard");
  const activity = (data[0]?.rows || []).slice(0, 5);
  const pinned = pins === null ? available.slice(0, 4) : pins.map(key => available.find(i => i.key === key)).filter(Boolean);
  const attentionIndex = sources[1] ? 1 : 0;
  const attention = data[attentionIndex];
  const attentionTitle = !sources.length ? "Your workspace, your way."
    : loading ? "Checking your next step…"
    : attention?.error ? "Queue temporarily unavailable."
    : attention?.count === 0 ? "You're up to date."
    : manager ? "Receipts need a decision."
    : staff ? "Transfers ready for picking."
    : "Keep deliveries moving.";
  const attentionDescription = !sources.length ? "Pin the pages you use most below, so they are always close at hand."
    : loading ? "Getting the latest queue for your account."
    : attention?.error ? "Refresh to try again, or open the queue to check its current state."
    : attention?.count === 0 ? "Nothing is waiting in this queue. You can check your other tasks or open the list."
    : manager ? "Review inspection results and resolve discrepancies before confirming."
    : staff ? "Verify each product, location and quantity before submitting results."
    : "Open your deliveries to check destinations and package conditions.";
  function togglePin(key) {
    const keys = pinned.map(i => i.key);
    update({ pins: keys.includes(key) ? keys.filter(k => k !== key) : [...keys, key].slice(0, 6) });
  }
  return (
    <MainLayout>
      <div className={`personal-welcome period-${clock.key}`}>
        <div>
          <h1>{clock.greeting}, <span>{displayName(user)}.</span></h1>
          <p>{clock.note}</p>
          <div className="welcome-context"><span>{roleLabel(user)}</span><span>Overview of your assigned workspace</span></div>
        </div>
        <div className="day-clock">
          <WorkspaceIcon name={clock.icon} size={34} />
          <div><time dateTime={clock.now.toISOString()}>{clock.now.toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" })}</time><span>{clock.now.toLocaleDateString("en-GB", { weekday: "short", day: "numeric", month: "long" })}</span><small>Local time</small></div>
        </div>
      </div>
      <div className="workload-heading">
        <h2>Your workload</h2>
        <div className="workload-update"><span aria-live="polite">
          {loading
            ? "Fetching data"
            : `Last refreshed ${updated?.toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" })}`}{" "}
          · All time
        </span><button className="refresh-control" disabled={loading} onClick={() => setRefresh(v => v + 1)}><WorkspaceIcon name="refresh" size={16} />{loading ? "Updating…" : "Refresh"}</button></div>
      </div>
      {failed && (
        <p role="alert" className="ops-alert">
          Some data could not be loaded. Check your connection or access
          permissions, then refresh.
        </p>
      )}
      <section className="workload-list" aria-label="Warehouse workload" aria-busy={loading}>
        {sources.map((s, n) => (
          <Link
            key={s.path}
            to={s.to}
            className={`workload-item ${n === 1 ? "workload-priority" : ""}`}
          >
            <span className="workload-icon"><WorkspaceIcon name={n === 0 ? "inbound" : n === 1 ? "check" : n === 2 ? "inventory" : "box"} size={20} /></span>
            <div className="workload-copy"><h3>{s.label}</h3><p>{data[n]?.error ? "Data unavailable" : s.hint}</p></div>
            <strong className={loading ? "workload-loading" : ""}>
              {loading || data[n]?.error ? "—" : number(data[n]?.count)}
            </strong>
            <WorkspaceIcon name="arrow" size={17} />
          </Link>
        ))}
      </section>
      <div className="dashboard-grid">
        <section className="ops-panel activity-panel">
          <div className="panel-heading">
            <div>
              <h2>
                {manager
                  ? "Receiving activity"
                  : staff
                    ? "Receipts awaiting inspection"
                    : delivery ? "Delivery schedule" : "Your activity"}
              </h2>
            </div>
            {sources[0] && (
              <Link to={sources[0].to} className="text-link">
                View all <WorkspaceIcon name="arrow" size={15} />
              </Link>
            )}
          </div>
          {loading ? (
            <div className="ops-empty" role="status">
              Loading activity…
            </div>
          ) : data[0]?.error ? (
            <div className="ops-empty">
              <p>The list could not be loaded.</p>
              <button
                className="text-link"
                onClick={() => setRefresh((v) => v + 1)}
              >
                Try again ↻
              </button>
            </div>
          ) : !activity.length ? (
            <div className="dashboard-empty">
              <span><WorkspaceIcon name="check" size={26} /></span>
              <h3>
                {sources.length
                  ? "No work in this queue yet."
                  : "Your account is ready."}
              </h3>
              <p>
                {sources.length
                  ? "New tasks will appear here when they are created."
                  : "Your available tools are listed in the navigation."}
              </p>
            </div>
          ) : (
            <div className="ops-table-wrap">
              <table className="ops-table">
                <thead>
                  <tr>
                    <th>Reference</th>
                    <th>{delivery ? "Destination" : "Staff / Source"}</th>
                    <th>{manager ? "Progress" : "Status"}</th>
                    <th>{manager ? "Items with issues" : "Open"}</th>
                  </tr>
                </thead>
                <tbody>
                  {activity.map((r, n) => (
                    <tr key={r.receiptId || r.deliveryId || n}>
                      <td>
                        <Link className="record-code" to={sources[0].to}>
                          {recordCode(r)}
                        </Link>
                      </td>
                      <td>
                        {r.staffName ||
                          r.supplierName ||
                          r.toLocationName ||
                          "Unassigned"}
                      </td>
                      <td>
                        {manager ? (
                          <div className="progress-cell">
                            <progress
                              aria-label={`Inspection progress for ${recordCode(r)}`}
                              max="100"
                              value={r.progressPercent ?? 0}
                            />
                            <span>
                              {r.progressPercent == null
                                ? "—"
                                : `${Math.round(r.progressPercent)}%`}
                            </span>
                          </div>
                        ) : (
                          <Status value={r.status} />
                        )}
                      </td>
                      <td>
                        {manager ? (
                          number(r.issueProducts)
                        ) : (
                          <Link className="text-link" to={sources[0].to}>
                            <WorkspaceIcon name="arrow" size={17} /><span className="sr-only">Open {recordCode(r)}</span>
                          </Link>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <div className="panel-foot">
            {manager
              ? "Showing up to 5 receipts currently being inspected"
              : "Tasks within your account scope"}
            <span>Warehouse activity</span>
          </div>
        </section>
        <aside className="attention-panel">
          <div className="attention-title"><WorkspaceIcon name="pin" size={19} /><span>Your next step</span></div>
          <h2>{attentionTitle}</h2>
          <p>{attentionDescription}</p>
          {sources.length > 0 && <div className="attention-count"><strong>{loading || data[attentionIndex]?.error ? "—" : number(data[attentionIndex]?.count)}</strong><span>{data[attentionIndex]?.error ? "Queue unavailable. Try refreshing." : sources[attentionIndex].label}</span></div>}
          {sources[1] || sources[0] ? (
            <Link to={(sources[1] || sources[0]).to} className="ops-button">
              {manager ? "Open review queue" : "Open list"} <WorkspaceIcon name="arrow" size={17} />
            </Link>
          ) : null}
          <div className="attention-foot">One checked detail. One smoother handover.</div>
        </aside>
      </div>
      <section className="personal-shortcuts">
        <div className="shortcuts-heading"><div><h2>Make yourself at home.</h2><p>Your most-used pages, right where you need them.</p></div></div>
        <details className="shortcut-editor"><summary><WorkspaceIcon name="pin" size={16} />Edit shortcuts</summary><div className="shortcut-options"><p>Choose up to 6 pages. Saved on this browser for your account.</p>{available.map(item => <label key={item.key}><input type="checkbox" checked={pinned.some(i => i.key === item.key)} disabled={pinned.length >= 6 && !pinned.some(i => i.key === item.key)} onChange={() => togglePin(item.key)} />{item.label}</label>)}{!available.length && <p>No additional pages are available for your role.</p>}<button type="button" className="text-link" onClick={() => update({ pins: null })}>Restore defaults</button></div></details>
        <div className="pinned-links">{pinned.map(item => <Link to={item.path} key={item.key}><WorkspaceIcon name={navigationIcon(item.key)} /><span>{item.label}</span><WorkspaceIcon name="arrow" size={17} /></Link>)}</div>
        {!pinned.length && <p className="shortcuts-empty">Choose “Edit shortcuts” to pin a page here.</p>}
      </section>
    </MainLayout>
  );
}
