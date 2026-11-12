import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { fmtDateTime } from "../../../api/warehouses/manager/managerOps";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import useResource from "../../../hooks/useResource";
import { ResourceState, Status, Table, number } from "../../../components/operations/WorkspaceUI";
import "../../../manager-ops.css";

const sum = (map, keys) => keys.reduce((s, k) => s + (map?.[k] || 0), 0);
const DIRECTION = {
  INBOUND_RECEIPT: ["Inbound · supplier", "/warehouse/receiving/in-progress"],
  INBOUND_TRANSFER: ["Inbound · transfer", "/warehouse/receiving/in-progress"],
  OUTBOUND_TRANSFER: ["Outbound · transfer", "/warehouse/packing"],
};

function StatusList({ map }) {
  const entries = Object.entries(map || {});
  if (!entries.length) return <p className="mo-muted">No records.</p>;
  return (
    <div className="mo-kv">
      {entries.map(([k, v]) => (
        <span key={k} style={{ display: "contents" }}>
          <span>{k.replaceAll("_", " ")}</span>
          <span>{number(v)}</span>
        </span>
      ))}
    </div>
  );
}

function Card({ title, hint, value, links, children }) {
  return (
    <section className="mo-card">
      <header>
        <div>
          <h3>{title}</h3>
          <small>{hint}</small>
        </div>
        {value != null && <strong style={{ fontSize: 26 }}>{number(value)}</strong>}
      </header>
      <div className="mo-card-body">
        {children}
        {links && (
          <div className="mo-task-actions">
            {links.map(([label, to]) => (
              <Link key={to + label} className="text-link" to={to}>{label} ↗</Link>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

export default function OperationsOverviewPage() {
  const resource = useResource("/warehouse/manager/operations/overview");
  const [direction, setDirection] = useState("ALL");
  const o = resource.data;
  const movements = useMemo(
    () => (o?.recentMovements || []).filter((m) => direction === "ALL" || m.direction.startsWith(direction)),
    [o, direction],
  );

  return (
    <MainLayout>
      <PageHeader
        title="Warehouse operations"
        actions={<button className="ops-button secondary" disabled={resource.loading} onClick={resource.reload}>Refresh ↻</button>}
      />
      <p className="mo-muted">Inbound, outbound and transfers, goods arrangement, damaged / missing goods and the mid-month & end-of-month stock counts in one place.</p>
      <ResourceState resource={resource}>
        {o && (
          <>
            <div className="metric-strip">
              <div><span>Inbound open</span><strong>{number(sum(o.goodsReceipts, ["CREATED", "RECEIVING", "INSPECTED", "CONFIRMED"]) + sum(o.inboundTransfers, ["SHIPPED", "RECEIVING"]))}</strong></div>
              <div><span>Outbound open</span><strong>{number(sum(o.outboundTransfers, ["CREATED", "PICKING", "PACKING", "PACKED"]))}</strong></div>
              <div><span>Putaway open</span><strong>{number(sum(o.putawayTasks, ["PENDING", "AVAILABLE", "IN_PROGRESS"]))}</strong></div>
              <div><span>Damaged / shortage open</span><strong>{number(sum(o.damagedGoods, ["REPORTED", "INSPECTING", "APPROVED"]) + sum(o.discrepancies, ["OPEN", "INVESTIGATING"]))}</strong></div>
              <div><span>Low-stock products</span><strong>{number(o.lowStockProducts)}</strong></div>
            </div>

            <div className="mo-grid">
              <Card title="Inbound" hint="Supplier goods receipts"
                links={[["Receiving monitor", "/warehouse/receiving/in-progress"], ["Review receipts", "/warehouse/receiving/waiting-review"], ["Incident reports", "/warehouse/receiving/incident-reports"]]}>
                <StatusList map={o.goodsReceipts} />
              </Card>
              <Card title="Outbound" hint="Transfers leaving this warehouse"
                links={[["Review packing", "/warehouse/packing"], ["Dispatch handover", "/warehouse/dispatch"], ["Shipment history", "/warehouse/shipment-history"]]}>
                <StatusList map={o.outboundTransfers} />
              </Card>
              <Card title="Incoming transfers" hint="Transfers sent to this warehouse"
                links={[["Store returns", "/warehouse/store-returns/review"], ["Receiving history", "/warehouse/receiving/completed"]]}>
                <StatusList map={o.inboundTransfers} />
              </Card>
              <Card title="Goods arrangement" hint="Putaway tasks and storage locations"
                links={[["Putaway monitor", "/warehouse/putaway"], ["Storage locations", "/warehouse/locations"], ["Inventory", "/warehouse/inventory"]]}>
                <StatusList map={o.putawayTasks} />
              </Card>
              <Card title="Damaged goods" hint="Damage reports in this warehouse"
                links={[["Handle damaged goods", "/warehouse/damaged-goods"]]}>
                <StatusList map={o.damagedGoods} />
              </Card>
              <Card title="Shortages & discrepancies" hint="Receiving / transfer discrepancies"
                links={[["Handle discrepancies", "/warehouse/discrepancies"], ["Incident reports", "/warehouse/receiving/incident-reports"]]}>
                <StatusList map={o.discrepancies} />
              </Card>
            </div>

            <section className="ops-panel">
              <div className="panel-heading">
                <h2>Stock count schedule · mid-month & end-of-month</h2>
                <Link className="text-link" to="/warehouse/stock-counts">Manage stock counts ↗</Link>
              </div>
              <div className="mo-card-body">
                <p className="mo-muted">Counts are only allowed Monday–Friday in the week containing the 15th and in the last working week of the month. Plans are created automatically for each period.</p>
                <div className="mo-windows">
                  {o.countWindows.map((w) => (
                    <div key={w.type + w.start} className={`mo-window ${w.active ? "active" : ""}`}>
                      <div className="mo-task-head">
                        <strong>{w.type === "MID_MONTH" ? "Mid-month count" : "End-of-month count"} · {w.start} → {w.end}</strong>
                        {w.active ? <span className="mo-chip danger">This week</span> : w.upcoming ? <span className="mo-chip">Upcoming</span> : <span className="mo-chip good">Past</span>}
                      </div>
                      {!w.plans.length ? (
                        <p className="mo-muted">No plan yet. {w.active || w.upcoming ? "It will be created automatically, or create one from Stock counts." : ""}</p>
                      ) : (
                        <div className="mo-task-meta" style={{ marginTop: 6 }}>
                          {w.plans.map((p) => (
                            <span key={p.id}>
                              <strong>{p.code}</strong> · {p.type} · <Status value={p.status} /> · {p.assignedTo || "Unassigned"}
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </section>

            <section className="ops-panel mt-6">
              <div className="panel-heading">
                <h2>Recent inbound, outbound & transfers</h2>
                <select aria-label="Direction" value={direction} onChange={(e) => setDirection(e.target.value)}>
                  <option value="ALL">All movements</option>
                  <option value="INBOUND">Inbound</option>
                  <option value="OUTBOUND">Outbound</option>
                </select>
              </div>
              <Table head={["Document", "Type", "Counterpart", "Expected", "Created", "Status", ""]}>
                {movements.map((m) => (
                  <tr key={m.direction + m.id}>
                    <td><strong>{m.code}</strong><small>{m.transferType}</small></td>
                    <td>{DIRECTION[m.direction]?.[0] || m.direction}</td>
                    <td>{m.counterpart}</td>
                    <td>{m.expectedDate || "—"}</td>
                    <td>{fmtDateTime(m.createdAt)}</td>
                    <td><Status value={m.status} /></td>
                    <td>
                      <Link className="text-link" to={DIRECTION[m.direction]?.[1] || "/warehouse/dashboard"}>Open ↗</Link>
                    </td>
                  </tr>
                ))}
              </Table>
            </section>
          </>
        )}
      </ResourceState>
    </MainLayout>
  );
}
