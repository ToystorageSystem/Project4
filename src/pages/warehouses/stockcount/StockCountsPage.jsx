import { useState } from "react";
import usePager from "../../../hooks/usePager";
import Pager from "../../../components/operations/Pager";
import api from "../../../api/api";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Modal from "../../../components/ui/Modal";
import useResource from "../../../hooks/useResource";
import {
  Field,
  Notice,
  ResourceState,
  Status,
  Table,
  useAction,
  number,
} from "../../../components/operations/WorkspaceUI";
import { useAuth } from "../../../auth/AuthContext";
import "../../../manager-ops.css";
const BASE = "/warehouse/manager/stock-counts";
// First week of the month (first 5 working days) and mid-month (week containing the 15th) count periods.
function PeriodicSchedule({ overview }) {
  const windows = overview.data?.countWindows || [];
  if (overview.loading || overview.error || !windows.length) return null;
  return (
    <section className="ops-panel" style={{ marginBottom: 16 }}>
      <div className="panel-heading">
        <h2>Periodic count schedule</h2>
        <span>First week & mid-month week · Mon–Fri only</span>
      </div>
      <div className="detail-ribbon" style={{ margin: 0, border: 0 }}>
        {windows.map((w) => (
          <div key={w.type + w.start}>
            <span>
              {w.type === "MID_MONTH" ? "Mid-month" : w.type === "BEGINNING_OF_MONTH" ? "First week" : "End of month"}
              {w.active ? " · this week" : w.upcoming ? " · upcoming" : " · past"}
            </span>
            <strong>
              {w.start} → {w.end}
            </strong>
            <small>
              {w.plans.length
                ? w.plans.map((p) => `${p.code} (${p.status.replaceAll("_", " ")}${p.assignedTo ? ", " + p.assignedTo : ""})`).join(" · ")
                : "Plan will be created automatically"}
            </small>
          </div>
        ))}
      </div>
    </section>
  );
}
export default function StockCountsPage() {
  const { user } = useAuth();
  const list = useResource(BASE),
    staff = useResource(BASE + "/staff"),
    locations = useResource(BASE + "/locations");
  const [id, setId] = useState(null),
    [mode, setMode] = useState(null),
    [filter, setFilter] = useState("ALL"),
    [search, setSearch] = useState("");
  const detail = useResource(id ? BASE + "/" + id : null);
  // Count windows (first week / mid-month) and "count week has started" notifications.
  const overview = useResource("/warehouse/manager/operations/overview");
  const notices = useResource(BASE + "/notifications");
  const windowByPlan = new Map();
  (overview.data?.countWindows || []).forEach((w) => w.plans.forEach((p) => windowByPlan.set(p.id, w)));
  async function readNotice(n, openPlan) {
    try {
      await api.patch(`${BASE}/notifications/${n.id}/read`);
    } catch {
      /* the notification list is refreshed below either way */
    }
    notices.reload();
    if (openPlan) {
      setId(n.stockCountId);
      setReviewed({});
      setQuantities({});
      action.clear();
    }
  }
  const [scope, setScope] = useState("ALL"),
    [quantities, setQuantities] = useState({}),
    [reviewed, setReviewed] = useState({});
  const action = useAction(async () => {
    setReviewed({});
    await Promise.all([list.reload(), detail.reload()]);
  });
  // Plans appear only when their count week (first week / mid-month) has opened.
  const notOpenYet = (r) => r.status === "PLANNED" && windowByPlan.get(r.id)?.upcoming;
  const rows = (list.data || []).filter(
    (r) =>
      !notOpenYet(r) &&
      (filter === "ALL" || r.status === filter) &&
      [r.code, r.assignedTo, r.warehouse]
        .join(" ")
        .toLowerCase()
        .includes(search.toLowerCase()),
  );
  const plan = detail.data?.plan,
    // Count sheet grouped by zone, then shelf and location.
    items = [...(detail.data?.items || [])].sort((a, b) =>
      [a.zone || "~", a.shelf || "", a.location || "", a.name || ""].join("|").localeCompare(
        [b.zone || "~", b.shelf || "", b.location || "", b.name || ""].join("|"),
      ),
    );
  // A quantity counts as entered when it is a whole number ≥ 0; untouched lines keep the system quantity.
  const entered = (i) =>
    quantities[i.id] != null && quantities[i.id] !== "" && Number.isInteger(Number(quantities[i.id])) && Number(quantities[i.id]) >= 0;
  // Damaged units found during the count (moved to the damaged area with the count's lot code).
  const [damagedQty, setDamagedQty] = useState({});
  const damagedOf = (i) =>
    damagedQty[i.id] != null && damagedQty[i.id] !== "" && Number.isInteger(Number(damagedQty[i.id])) && Number(damagedQty[i.id]) >= 0
      ? Number(damagedQty[i.id])
      : 0;
  const invalidDamaged = items.some((i) => damagedQty[i.id] != null && damagedQty[i.id] !== "" && !(Number.isInteger(Number(damagedQty[i.id])) && Number(damagedQty[i.id]) >= 0));
  const changed = (i) => entered(i) || damagedOf(i) > 0;
  const goodOf = (i) => (entered(i) ? Number(quantities[i.id]) : Math.max(0, Number(i.system ?? 0) - damagedOf(i)));
  const invalidEntry = invalidDamaged || items.some((i) => quantities[i.id] != null && quantities[i.id] !== "" && !entered(i));
  const selectedItem = mode?.item;
  function open(value) {
    action.clear();
    setScope(value === "assign" ? plan.scopeType : "ALL");
    setMode(value);
  }
  async function submit(e) {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(e.currentTarget));
    let task;
    if (mode === "create")
      task = () =>
        api.post(BASE, {
          scopeType: scope,
          scopeValue: scope === "ALL" ? null : f.scopeValue,
          staffId: f.staffId ? Number(f.staffId) : null,
        });
    if (mode === "assign")
      task = () =>
        api.patch(`${BASE}/${id}/assign`, {
          scopeType: scope,
          scopeValue: scope === "ALL" ? null : f.scopeValue,
          staffId: Number(f.staffId),
        });
    if (mode === "start")
      task = () =>
        api.post(`${BASE}/${id}/start-manager`, {
          fallbackLocationId: f.fallbackLocationId
            ? Number(f.fallbackLocationId)
            : null,
        });
    if (mode === "recount")
      task = () =>
        api.post(`${BASE}/${id}/recount`, {
          staffId: Number(f.staffId),
          reason: f.reason,
          itemIds: items.filter((i) => i.difference !== 0).map((i) => i.id),
        });
    if (mode?.type === "correct")
      task = () =>
        api.patch(`${BASE}/${id}/items/${selectedItem.id}/correct`, {
          quantity: Number(f.quantity),
          reason: f.reason,
        });
    if (mode === "cancel")
      task = () => api.post(`${BASE}/${id}/cancel`, { reason: f.reason });
    if (task && (await action.run(task))) {
      setMode(null);
      setReviewed({});
    }
  }
  function staffSelect(required = true) {
    return (
      <Field label="Assigned staff">
        <select
          name="staffId"
          required={required}
          defaultValue={plan?.assignedToId || ""}
        >
          <option value="">
            {required ? "Select a staff member" : "Unassigned"}
          </option>
          {(staff.data || []).map((s) => (
            <option key={s.id} value={s.id}>
              {s.name} · {s.email}
            </option>
          ))}
        </select>
      </Field>
    );
  }
  const countPager = usePager(rows, 20, [search, filter]);
  return (
    <MainLayout>
      <PageHeader
        title={id ? plan?.code || "Stock count details" : "Stock counts"}
        description="Plan, assign, reconcile and confirm stock counts."
        actions={
          <>
            {id ? (
              <button
                className="ops-button secondary"
                onClick={() => {
                  setId(null);
                  setReviewed({});
                  setQuantities({});
                  action.clear();
                }}
              >
                ← Back to list
              </button>
            ) : null}
            <button
              className="ops-button secondary"
              disabled={list.loading || detail.loading}
              onClick={() => {
                setReviewed({});
                list.reload();
                detail.reload();
              }}
            >
              Refresh ↻
            </button>
          </>
        }
      />
      <Notice error={action.error}>{action.success}</Notice>
      {!id ? (
        <>
          {(notices.data || []).length > 0 && (
            <section className="ops-panel" role="status" style={{ marginBottom: 16, borderColor: "var(--brand)" }}>
              <div className="panel-heading">
                <h2>Stock count week has started</h2>
                <span>Physical stock counts are open now. Assign staff or start a direct count.</span>
              </div>
              <div className="detail-ribbon" style={{ margin: 0, border: 0 }}>
                {notices.data.map((n) => (
                  <div key={n.id}>
                    <span>{n.type === "MID_MONTH" ? "Mid-month count" : n.type === "BEGINNING_OF_MONTH" ? "First-week count" : n.type?.replaceAll("_", " ")}</span>
                    <strong>{n.code}</strong>
                    <small>
                      <button className="text-link" onClick={() => readNotice(n, true)}>Open plan ↗</button>{" "}
                      <button className="text-link" onClick={() => readNotice(n, false)}>Dismiss</button>
                    </small>
                  </div>
                ))}
              </div>
            </section>
          )}
          <PeriodicSchedule overview={overview} />
          <div className="ops-toolbar">
            <input
              aria-label="Search stock counts"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search count code or staff member…"
            />
            <select
              aria-label="Status"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
            >
              {[
                "ALL",
                "PLANNED",
                "COUNTING",
                "RECOUNTING",
                "PENDING_CONFIRMATION",
                "COMPLETED",
                "CANCELLED",
              ].map((s) => (
                <option key={s}>{s}</option>
              ))}
            </select>
          </div>
          <ResourceState resource={list} empty={!rows.length}>
            <Table
              head={[
                "Plan",
                "Date / Scope",
                "Assignment",
                "Progress",
                "Discrepancies",
                "Status",
                "",
              ]}
            >
              {countPager.rows.map((r) => (
                <tr key={r.id}>
                  <td>
                    <strong>{r.code}</strong>
                    <small>{r.type}</small>
                  </td>
                  <td>
                    {r.date}
                    <small>
                      {r.scopeType} {r.scopeValue}
                    </small>
                  </td>
                  <td>{r.assignedTo || "Unassigned"}</td>
                  <td>
                    {r.counted} / {r.total}
                  </td>
                  <td>{r.differences}</td>
                  <td>
                    <Status value={r.status} />
                    {r.status === "PLANNED" && windowByPlan.get(r.id) && (
                      <small>
                        {windowByPlan.get(r.id).active
                          ? "Count week is open – you can count now"
                          : windowByPlan.get(r.id).upcoming
                            ? `Opens ${windowByPlan.get(r.id).start}`
                            : "Count week has ended"}
                      </small>
                    )}
                  </td>
                  <td>
                    <button
                      className={r.status === "PLANNED" && windowByPlan.get(r.id)?.active ? "ops-button" : "text-link"}
                      disabled={action.busy}
                      onClick={async () => {
                        setReviewed({});
                        setQuantities({});
                        action.clear();
                        // Count week is open: one click starts the count (stock is snapshotted on the server).
                        if (r.status === "PLANNED" && windowByPlan.get(r.id)?.active) {
                          const started = await action.run(
                            () => api.post(`${BASE}/${r.id}/start-manager`, {}),
                            "Count started. Enter only the quantities that differ, then confirm.",
                          );
                          if (!started) return;
                        }
                        setId(r.id);
                      }}
                    >
                      {r.status === "PLANNED" && windowByPlan.get(r.id)?.active ? "Open & count" : "Open ↗"}
                    </button>
                  </td>
                </tr>
              ))}
            </Table>
            <Pager pager={countPager} label="stock counts" />
          </ResourceState>
        </>
      ) : (
        <ResourceState resource={detail}>
          {plan && (
            <>
              <div className="detail-ribbon">
                <div>
                  <span>Status</span>
                  <Status value={plan.status} />
                </div>
                <div>
                  <span>Scope</span>
                  <strong>
                    {plan.scopeType} {plan.scopeValue}
                  </strong>
                </div>
                <div>
                  <span>Assignee</span>
                  <strong>{plan.assignedTo || "Unassigned"}</strong>
                </div>
                <div>
                  <span>Count date</span>
                  <strong>{plan.date}</strong>
                </div>
                <div>
                  <span>SKU coverage</span>
                  <strong>
                    {detail.data.coveredSkus} / {detail.data.catalogTotal}
                  </strong>
                </div>
              </div>
              {!!detail.data.missingSkus?.length && (
                <Notice
                  error={`Missing from the stock snapshot: ${detail.data.missingSkus.join(", ")}. The server will validate coverage on confirmation.`}
                />
              )}
              <div className="ops-toolbar">
                {plan.status === "PLANNED" && (
                  <>
                    <button
                      className="ops-button"
                      disabled={action.busy}
                      onClick={() => open("start")}
                    >
                      Start direct count
                    </button>
                    <button
                      className="ops-button secondary"
                      disabled={action.busy || staff.loading || !!staff.error}
                      onClick={() => open("assign")}
                    >
                      Assign staff
                    </button>
                  </>
                )}
                {plan.status === "PENDING_CONFIRMATION" && (
                  <>
                    <button
                      className="ops-button"
                      disabled={
                        action.busy ||
                        !items.length ||
                        !items.some((i) => reviewed[i.id]) ||
                        items.some((i) => i.finalQuantity == null)
                      }
                      onClick={async () => {
                        if (
                          window.confirm(
                            "Confirm the count and update inventory? Lines you did not tick are accepted as counted.",
                          )
                        ) {
                          await action.run(() =>
                            api.post(`${BASE}/${id}/confirm`, {
                              items: items.map((i) => ({
                                itemId: i.id,
                                finalQuantity: i.finalQuantity,
                              })),
                            }),
                          );
                          setReviewed({});
                        }
                      }}
                    >
                      Confirm count ({items.filter((i) => reviewed[i.id]).length}/{items.length} checked)
                    </button>
                    <button
                      className="ops-button secondary"
                      disabled={
                        action.busy || !items.some((i) => i.difference !== 0)
                      }
                      onClick={() => open("recount")}
                    >
                      Recount discrepancy lines
                    </button>
                  </>
                )}
                {!["COMPLETED", "CANCELLED"].includes(plan.status) && (
                  <button
                    className="ops-button danger"
                    disabled={action.busy}
                    onClick={() => open("cancel")}
                  >
                    Cancel plan
                  </button>
                )}
              </div>
              <></>
              <Table
                head={[
                  "Product",
                  "Location",
                  "System",
                  "First count",
                  "Second count",
                  "Good (on shelf)",
                  "Damaged",
                  "Missing",
                  "Difference",
                  "Actions",
                ]}
              >
                {items.flatMap((i, n) => [
                  ...(n === 0 || (items[n - 1].zone || "") !== (i.zone || "")
                    ? [
                        <tr key={`zone-${i.zone || "none"}-${n}`} className="zone-row">
                          <td colSpan={10}>
                            <strong>Zone {i.zone || "—"}</strong>{" "}
                            <small>{items.filter((x) => (x.zone || "") === (i.zone || "")).length} lines</small>
                          </td>
                        </tr>,
                      ]
                    : []),
                  <tr key={i.id}>
                    <td>
                      <strong>{i.name}</strong>
                      <small>
                        {i.sku} · {i.barcode}
                      </small>
                    </td>
                    <td>
                      {i.location}
                      <small>
                        {i.zone} / {i.shelf}
                      </small>
                    </td>
                    <td>{number(i.system)}</td>
                    <td>{number(i.first)}</td>
                    <td>{number(i.second)}</td>
                    <td>
                      {plan.status === "COUNTING" &&
                      plan.assignedToId === user.id ? (
                        <input
                          aria-label={`Counted quantity ${i.sku} ${i.location}`}
                          className="quantity-input"
                          type="number"
                          min="0"
                          step="1"
                          placeholder={String(i.system ?? 0)}
                          value={quantities[i.id] ?? ""}
                          onChange={(e) =>
                            setQuantities({
                              ...quantities,
                              [i.id]: e.target.value,
                            })
                          }
                        />
                      ) : (
                        number(i.finalQuantity)
                      )}
                    </td>
                    <td>
                      {plan.status === "COUNTING" && plan.assignedToId === user.id ? (
                        <input
                          aria-label={`Damaged quantity ${i.sku} ${i.location}`}
                          className="quantity-input"
                          type="number"
                          min="0"
                          step="1"
                          placeholder="0"
                          value={damagedQty[i.id] ?? ""}
                          onChange={(e) => setDamagedQty({ ...damagedQty, [i.id]: e.target.value })}
                        />
                      ) : (
                        number(i.damaged ?? 0)
                      )}
                    </td>
                    <td className="difference-value">
                      {(() => {
                        const counting = plan.status === "COUNTING" && plan.assignedToId === user.id;
                        const good = counting ? goodOf(i) : i.finalQuantity;
                        const damaged = counting ? damagedOf(i) : i.damaged ?? 0;
                        if (good == null) return "—";
                        const missing = Number(i.system ?? 0) - good - damaged;
                        return missing > 0 ? number(missing) : "—";
                      })()}
                    </td>
                    <td className={i.difference ? "difference-value" : ""}>
                      {number(i.difference)}
                    </td>
                    <td>
                      {plan.status === "PENDING_CONFIRMATION" && (
                        <>
                          <label className="review-check">
                            <input
                              type="checkbox"
                              checked={!!reviewed[i.id]}
                              onChange={(e) =>
                                setReviewed({
                                  ...reviewed,
                                  [i.id]: e.target.checked,
                                })
                              }
                            />
                            Reviewed
                          </label>
                          <button
                            className="text-link"
                            disabled={action.busy}
                            onClick={() => open({ type: "correct", item: i })}
                          >
                            Correct
                          </button>
                        </>
                      )}
                    </td>
                  </tr>,
                ])}
              </Table>
              {plan.status === "COUNTING" && plan.assignedToId === user.id && (
                <button
                  className="ops-button mt-4"
                  disabled={action.busy || !items.length || invalidEntry || !items.some(changed)}
                  onClick={() => {
                    if (
                      window.confirm(
                        `Confirm the count? ${items.filter(changed).length} line(s) changed; the other lines keep the system quantity. Damaged goods go to the damaged area and shortages are listed in Review receiving for your decision.`,
                      )
                    )
                      action.run(() =>
                        api.post(`${BASE}/${id}/confirm-manager`, {
                          items: items.map((i) => ({
                            itemId: i.id,
                            quantity: goodOf(i),
                            damagedQuantity: damagedOf(i),
                          })),
                        }),
                      );
                  }}
                >
                  Confirm count ({items.filter(changed).length} changed)
                </button>
              )}
              <section className="ops-panel mt-6">
                <div className="panel-heading">
                  <h2>Count audit trail</h2>
                  <span>{detail.data.audit?.length || 0} actions</span>
                </div>
                <Table
                  head={[
                    "Time",
                    "Performed by",
                    "Actions",
                    "Before → After",
                    "Reason",
                  ]}
                >
                  {(detail.data.audit || []).map((a, n) => (
                    <tr key={n}>
                      <td>{a.at?.replace("T", " ")}</td>
                      <td>{a.actor}</td>
                      <td>{a.action}</td>
                      <td>
                        {number(a.before)} → {number(a.after)}
                      </td>
                      <td>{a.reason || "—"}</td>
                    </tr>
                  ))}
                </Table>
              </section>
            </>
          )}
        </ResourceState>
      )}
      <Modal
        open={!!mode}
        title={
          mode?.type === "correct"
            ? "Correct count"
            : {
                create: "Create stock count",
                assign: "Assign stock count",
                start: "Start direct count",
                recount: "Request recount",
                cancel: "Cancel plan",
              }[mode] || "Stock counts"
        }
        onClose={() => {
          if (!action.busy) setMode(null);
        }}
      >
        <form
          className="ops-form"
          onSubmit={submit}
          key={typeof mode === "string" ? mode : mode?.item?.id}
        >
          <Notice error={action.error} />
          {["create", "assign"].includes(mode) && (
            <>
              <Field label="Scope">
                <select
                  value={scope}
                  onChange={(e) => setScope(e.target.value)}
                >
                  {["ALL", "ZONE", "LOCATION", "PRODUCT"].map((s) => (
                    <option key={s}>{s}</option>
                  ))}
                </select>
              </Field>
              {scope !== "ALL" &&
                (scope === "LOCATION" ? (
                  <Field label="Location">
                    <select
                      name="scopeValue"
                      required
                      defaultValue={plan?.scopeValue || ""}
                    >
                      <option value="">Select a location</option>
                      {(locations.data || []).map((l) => (
                        <option value={l.id} key={l.id}>
                          {l.code} · {l.name}
                        </option>
                      ))}
                    </select>
                  </Field>
                ) : (
                  <Field
                    label={
                      scope === "PRODUCT" ? "Product ID" : "Zone code"
                    }
                    name="scopeValue"
                    required
                    defaultValue={plan?.scopeValue || ""}
                  />
                ))}
              {staffSelect(mode === "assign")}
            </>
          )}
          {mode === "start" && (
            <>
              <p>
                The server snapshots stock when the count starts. Select a location for
                products with no stock balance.
              </p>
              <Field label="Location for SKUs with no stock">
                <select name="fallbackLocationId">
                  <option value="">None selected</option>
                  {(locations.data || []).map((l) => (
                    <option value={l.id} key={l.id}>
                      {l.code} · {l.name}
                    </option>
                  ))}
                </select>
              </Field>
            </>
          )}
          {mode === "recount" && staffSelect()}
          {mode?.type === "correct" && (
            <Field
              label="Corrected quantity"
              name="quantity"
              type="number"
              min="0"
              step="1"
              required
              defaultValue={selectedItem?.finalQuantity ?? ""}
            />
          )}
          {(mode === "recount" ||
            mode === "cancel" ||
            mode?.type === "correct") && (
            <Field
              label="Reason"
              name="reason"
              required
              minLength={3}
              maxLength={1000}
            />
          )}
          <button
            className="ops-button"
            disabled={
              action.busy ||
              ((mode === "create" || mode === "assign" || mode === "recount") &&
                (staff.loading || !!staff.error))
            }
          >
            {action.busy ? "Processing…" : "Confirm"}
          </button>
        </form>
      </Modal>
    </MainLayout>
  );
}
