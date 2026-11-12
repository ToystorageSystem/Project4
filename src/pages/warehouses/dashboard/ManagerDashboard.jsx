import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../../api/api";
import { useAuth } from "../../../auth/AuthContext";
import { AVAILABILITY, DELIVERY_STATUS, WORK_ROLE_LABEL, countWindowLabel, todayIso } from "../../../api/warehouses/manager/managerOps";
import MainLayout from "../../../components/layout/MainLayout";
import { Status, number } from "../../../components/operations/WorkspaceUI";
import WorkspaceIcon from "../../../components/ui/WorkspaceIcon";
import { displayName, useWorkspaceClock } from "../../../hooks/useWorkspacePersonalization";
import "../../../manager-ops.css";

// Warehouse Manager dashboard: a read-only report of the warehouse today. Work is assigned in Daily task assignment.
const sum = (map, keys) => keys.reduce((s, k) => s + (map?.[k] || 0), 0);
const sources = (today) => ({
  overview: "/warehouse/manager/operations/overview",
  board: "/warehouse/manager/daily-tasks",
  deliveries: `/warehouse/manager/inbound-schedule?from=${today}&to=${today}`,
  availability: "/warehouse/manager/daily-tasks/availability",
});

function Tile({ label, value, hint, alert, failed }) {
  return (
    <div className={`mo-tile ${alert ? "alert" : ""}`}>
      <span>{label}</span>
      <strong>{failed ? "—" : value}</strong>
      <small>{failed ? "Data unavailable" : hint}</small>
    </div>
  );
}
function Chip({ map, value }) {
  const [label, tone] = map[value] || [value, ""];
  return <span className={`mo-chip ${tone}`}>{label}</span>;
}

export default function ManagerDashboard() {
  const { user } = useAuth();
  const clock = useWorkspaceClock();
  const [data, setData] = useState({});
  const [loading, setLoading] = useState(true);
  const [updated, setUpdated] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    const map = sources(todayIso());
    const keys = Object.keys(map);
    const results = await Promise.allSettled(keys.map((k) => api.get(map[k]).then((r) => r.data)));
    setData(Object.fromEntries(keys.map((k, i) => [k, results[i].status === "fulfilled" ? results[i].value : { error: true }])));
    setUpdated(new Date());
    setLoading(false);
  }, []);
  useEffect(() => {
    load();
  }, [load]);

  // Accept only well-formed responses; anything else is shown as unavailable, never as zero.
  const o = Array.isArray(data.overview?.countWindows) ? data.overview : null;
  const board = Array.isArray(data.board?.staff) ? data.board : null;
  // Receipts no longer wait for approval; what needs the manager is the open shortage / damage / surplus issues.
  const issuesToDecide = o ? (o.discrepancies?.OPEN || 0) : null;
  const deliveries = Array.isArray(data.deliveries) ? data.deliveries : null;
  const staff = Array.isArray(data.availability) ? data.availability : null;
  const failed = !loading && (!o || !board || !deliveries || !staff);
  const dash = (v) => (loading || v == null ? "—" : number(v));
  const countWindow = o?.countWindows?.find((w) => w.active) || o?.countWindows?.find((w) => w.upcoming);
  const openIssues = o ? sum(o.damagedGoods, ["REPORTED", "INSPECTING", "APPROVED"]) + sum(o.discrepancies, ["OPEN", "INVESTIGATING"]) : null;
  const arrived = deliveries?.filter((d) => ["ARRIVED", "RECEIVING", "RECEIVED"].includes(d.status)).length;
  const taskById = new Map((board?.staff || []).map((s) => [s.staffId, s]));

  return (
    <MainLayout>
      <div className={`personal-welcome period-${clock.key}`}>
        <div>
          <h1>{clock.greeting}, <span>{displayName(user)}.</span></h1>
          <div className="welcome-context"><span>Warehouse Manager · daily report</span></div>
        </div>
        <div className="day-clock">
          <WorkspaceIcon name={clock.icon} size={34} />
          <div>
            <time dateTime={clock.now.toISOString()}>{clock.now.toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" })}</time>
            <span>{clock.now.toLocaleDateString("en-GB", { weekday: "short", day: "numeric", month: "long" })}</span>
          </div>
        </div>
      </div>

      <div className="workload-heading">
        <h2>Today's report</h2>
        <div className="workload-update">
          <span aria-live="polite">{loading ? "Fetching data" : `Updated ${updated?.toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" })}`}</span>
          <button className="refresh-control" disabled={loading} onClick={load}><WorkspaceIcon name="refresh" size={16} />{loading ? "Updating…" : "Refresh"}</button>
        </div>
      </div>
      {failed && <p role="alert" className="ops-alert">Some data could not be loaded. Check your connection or access permissions, then refresh.</p>}

      <section className="mo-tiles" aria-label="Warehouse today" aria-busy={loading}>
        <Tile label="Tasks done" failed={!loading && !board} value={loading || !board ? "—" : `${board.completed}/${board.total}`}
          hint={board?.overdue ? `${board.overdue} overdue` : "Assigned today"} alert={board?.overdue > 0} />
        <Tile label="Deliveries arrived" failed={!loading && !deliveries} value={loading || !deliveries ? "—" : `${arrived}/${deliveries.length}`}
          hint="Purchase orders & transfers due today" alert={deliveries?.some((d) => d.status === "LATE")} />
        <Tile label="Issues to decide" failed={!loading && !o} value={dash(issuesToDecide)} hint="Keep internal or send to Business" alert={issuesToDecide > 0} />
        <Tile label="Low-stock products" failed={!loading && !o} value={dash(o?.lowStockProducts)} hint="At or below minimum" alert={o?.lowStockProducts > 0} />
        <Tile label="Damaged & shortages" failed={!loading && !o} value={dash(openIssues)} hint="Open cases (incl. sent to Business)" alert={openIssues > 0} />
        <Tile label="Staff free / total" failed={!loading && !staff} value={loading || !staff ? "—" : `${staff.filter((s) => s.status === "FREE").length}/${staff.length}`}
          hint={staff ? `${staff.filter((s) => s.status === "BUSY").length} busy now` : ""} />
      </section>

      <section className="ops-panel">
        <div className="panel-heading"><h2>Deliveries today</h2><span>Purchase orders and incoming transfers due today</span></div>
        {loading ? <div className="ops-empty" role="status">Loading…</div>
          : !deliveries ? <div className="ops-empty"><p>Data unavailable.</p></div>
          : !deliveries.length ? <div className="ops-empty"><p>No deliveries expected today.</p></div>
          : (
            <div className="ops-table-wrap">
              <table className="ops-table">
                <thead><tr><th>From</th><th>Order</th><th>Expected</th><th>Units</th><th>Status</th><th>Receiving staff</th></tr></thead>
                <tbody>
                  {deliveries.map((d) => (
                    <tr key={d.type + (d.purchaseOrderId || d.transferId)}>
                      <td>{d.supplierName}</td>
                      <td>{d.orderCode}</td>
                      <td>{d.deliveryDate}</td>
                      <td>{number(d.remainingQuantity)}</td>
                      <td><Chip map={DELIVERY_STATUS} value={d.status} /></td>
                      <td>{d.receivingStaff || "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
      </section>

      <div className="mo-grid two">
        <section className="mo-card">
          <header><div><h3>Staff today</h3><small>Availability and tasks completed</small></div></header>
          <div className="mo-card-body">
            {loading ? <p className="mo-muted">Loading…</p>
              : !staff ? <p className="mo-muted">Data unavailable.</p>
              : !staff.length ? <p className="mo-muted">No warehouse staff assigned to this warehouse.</p>
              : staff.map((s) => {
                const day = taskById.get(s.staffId);
                const total = day?.total || 0, done = day?.completed || 0;
                const pct = total ? Math.round((done * 100) / total) : 0;
                return (
                  <div key={s.staffId}>
                    <div className="mo-task-head">
                      <strong>{s.name} <Chip map={AVAILABILITY} value={s.status} /></strong>
                      <small className="mo-muted">{total ? `${done}/${total} tasks` : "No tasks"}</small>
                    </div>
                    <small className="mo-muted">{s.roles?.length ? s.roles.map((r) => WORK_ROLE_LABEL[r] || r).join(" · ") : "No role today"}</small>
                    {s.currentWork.length > 0 && <small className="mo-muted"> · Now: {s.currentWork.join(" · ")}</small>}
                    <div className={`mo-progress ${pct === 100 ? "good" : ""}`}><span style={{ width: `${pct}%` }} /></div>
                  </div>
                );
              })}
          </div>
        </section>

        <section className="mo-card">
          <header><div><h3>Stock flow</h3><small>Open documents in this warehouse</small></div></header>
          <div className="mo-card-body mo-kv">
            <span>Inbound from suppliers</span><span>{dash(o && sum(o.goodsReceipts, ["CREATED", "RECEIVING", "INSPECTED", "CONFIRMED"]))}</span>
            <span>Putaway & arrangement</span><span>{dash(o && sum(o.putawayTasks, ["PENDING", "AVAILABLE", "IN_PROGRESS"]))}</span>
            <span>Outbound transfers</span><span>{dash(o && sum(o.outboundTransfers, ["CREATED", "PICKING", "PACKING", "PACKED"]))}</span>
            <span>Ready to dispatch</span><span>{dash(o && sum(o.outboundTransfers, ["PACKED"]))}</span>
            <span>Incoming transfers & returns</span><span>{dash(o && sum(o.inboundTransfers, ["SHIPPED", "RECEIVING"]))}</span>
          </div>
        </section>

        <section className="mo-card">
          <header><div><h3>Stock count</h3><small>Physical count: first week & mid-month week (Mon–Fri)</small></div></header>
          <div className="mo-card-body">
            {loading ? <p className="mo-muted">Loading…</p>
              : !countWindow ? <p className="mo-muted">No count period scheduled.</p>
              : (
                <div className={`mo-window ${countWindow.active ? "active" : ""}`}>
                  <div className="mo-task-head">
                    <strong>{countWindowLabel(countWindow.type)}</strong>
                    {countWindow.active ? <span className="mo-chip danger">This week</span> : <span className="mo-chip">Upcoming</span>}
                  </div>
                  <p className="mo-muted">{countWindow.start} → {countWindow.end}</p>
                  {countWindow.plans.length
                    ? countWindow.plans.map((p) => <p key={p.id}><strong>{p.code}</strong> <Status value={p.status} /> <small className="mo-muted">{p.assignedTo || "Unassigned"}</small></p>)
                    : <p className="mo-muted">Plan will be created automatically.</p>}
                </div>
              )}
          </div>
        </section>

        <section className="mo-card">
          <header><div><h3>Records</h3><small>History and documents</small></div></header>
          <div className="mo-card-body mo-kv">
            <span><Link className="text-link" to="/warehouse/receiving/completed">Receiving history ↗</Link></span><span />
            <span><Link className="text-link" to="/warehouse/putaway">Putaway progress ↗</Link></span><span />
            <span><Link className="text-link" to="/warehouse/receiving/in-progress">Receiving progress ↗</Link></span><span />
          </div>
        </section>
      </div>
    </MainLayout>
  );
}
