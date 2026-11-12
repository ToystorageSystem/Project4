import { Fragment, useCallback, useEffect, useState } from "react";
import usePager, { searchRows } from "../../../hooks/usePager";
import Pager, { SearchBox } from "../../../components/operations/Pager";
import { errorMessage } from "../../../api/api";
import {
  TASK_TYPE_LABEL,
  addReview,
  firstOfMonthIso,
  fmtDateTime,
  getPerformance,
  getStaffPerformance,
  todayIso,
} from "../../../api/warehouses/manager/managerOps";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Modal from "../../../components/ui/Modal";
import { Field, Notice, Status, Table, number } from "../../../components/operations/WorkspaceUI";
import "../../../manager-ops.css";

const pct = (v) => `${Number(v || 0).toFixed(0)}%`;
const GRADE_LABEL = { A: "Excellent", B: "Good", C: "Fair", D: "Needs improvement", "N/A": "No data" };

function Grade({ value }) {
  return <span className={`mo-grade ${value}`} title={GRADE_LABEL[value]}>{value === "N/A" ? "–" : value}</span>;
}

function Stars({ value, onChange }) {
  return (
    <div className="mo-stars" role="radiogroup" aria-label="Rating">
      {[1, 2, 3, 4, 5].map((n) => (
        <button type="button" key={n} className={n <= value ? "on" : ""} aria-label={`${n} star`} aria-checked={n === value} role="radio" onClick={() => onChange(n)}>
          ★
        </button>
      ))}
    </div>
  );
}

export default function StaffPerformancePage() {
  const [from, setFrom] = useState(firstOfMonthIso());
  const [to, setTo] = useState(todayIso());
  const [board, setBoard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [reviewing, setReviewing] = useState(null);
  const [rating, setRating] = useState(4);
  const [busy, setBusy] = useState(false);
  const [modalError, setModalError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setBoard(await getPerformance(from, to));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [from, to]);
  useEffect(() => {
    load();
  }, [load]);

  async function openDetail(staffId) {
    setDetailLoading(true);
    setError("");
    try {
      setDetail(await getStaffPerformance(staffId, from, to));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setDetailLoading(false);
    }
  }

  async function submitReview(e) {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(e.currentTarget));
    setBusy(true);
    setModalError("");
    try {
      await addReview(reviewing.staffId, { periodFrom: from, periodTo: to, rating, strengths: f.strengths, improvements: f.improvements, comment: f.comment });
      setReviewing(null);
      setNotice(`Review saved for ${reviewing.name}.`);
      await load();
      if (detail?.summary.staffId === reviewing.staffId) await openDetail(reviewing.staffId);
    } catch (err) {
      setModalError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  function setPreset(kind) {
    const today = todayIso();
    if (kind === "month") { setFrom(firstOfMonthIso()); setTo(today); }
    if (kind === "7") { const d = new Date(); d.setDate(d.getDate() - 6); setFrom(d.toLocaleDateString("en-CA")); setTo(today); }
    if (kind === "last") {
      const d = new Date(); d.setDate(1); d.setDate(0);
      setTo(d.toLocaleDateString("en-CA")); d.setDate(1); setFrom(d.toLocaleDateString("en-CA"));
    }
  }

  const s = detail?.summary;
  const [staffSearch, setStaffSearch] = useState("");
  const staffRows = searchRows(board?.staff, staffSearch, (x) => [x.name, x.email, x.code, x.grade, x.remark]);
  const staffPager = usePager(staffRows, 20, [staffSearch, from, to]);
  const maxDaily = Math.max(1, ...(detail?.daily || []).map((d) => d.assigned));

  return (
    <MainLayout>
      <PageHeader
        title={s ? `Performance · ${s.name}` : "Staff performance"}
        actions={
          <>
            {s && <button className="ops-button secondary" onClick={() => setDetail(null)}>← All staff</button>}
            <input type="date" aria-label="From" value={from} max={to} onChange={(e) => e.target.value && setFrom(e.target.value)} />
            <input type="date" aria-label="To" value={to} min={from} onChange={(e) => e.target.value && setTo(e.target.value)} />
            <select aria-label="Period preset" defaultValue="" onChange={(e) => { setPreset(e.target.value); e.target.value = ""; }}>
              <option value="" disabled>Quick period…</option>
              <option value="7">Last 7 days</option>
              <option value="month">This month</option>
              <option value="last">Last month</option>
            </select>
            <button className="ops-button secondary" disabled={loading} onClick={() => (s ? openDetail(s.staffId) : load())}>Refresh ↻</button>
          </>
        }
      />
      <p className="mo-muted">
        Score = 45% task completion + 25% on-time rate + 15% system activity (receiving, putaway, counting, packing, damage reports) + 15% manager rating.
        Grade A ≥ 85, B ≥ 70, C ≥ 55, D &lt; 55.
      </p>
      <Notice error={error}>{notice}</Notice>

      {!s ? (
        loading && !board ? (
          <div className="ops-empty" role="status">Loading performance…</div>
        ) : board ? (
          <>
            <div className="metric-strip">
              <div><span>Staff</span><strong>{board.staffCount}</strong></div>
              <div><span>Average score</span><strong>{Number(board.averageScore).toFixed(1)}</strong></div>
              <div><span>Grade A</span><strong>{board.staff.filter((x) => x.grade === "A").length}</strong></div>
              <div><span>Needs attention (C/D)</span><strong>{board.staff.filter((x) => ["C", "D"].includes(x.grade)).length}</strong></div>
            </div>
            <div className="ops-toolbar">
              <SearchBox value={staffSearch} onChange={setStaffSearch} placeholder="Search staff name, email, grade…" />
              <span>{staffRows.length} staff</span>
            </div>
            <Table head={["#", "Staff", "Grade", "Score", "Tasks done", "On time", "Overdue", "System activity", "Rating", "Remark", ""]}>
              {staffPager.rows.map((x, idx) => {
                const i = staffPager.from - 1 + idx;
                const m = x.metrics;
                return (
                  <tr key={x.staffId}>
                    <td>{i + 1}</td>
                    <td><strong>{x.name}</strong><small>{x.email}</small></td>
                    <td><Grade value={x.grade} /></td>
                    <td><strong>{Number(x.score).toFixed(1)}</strong></td>
                    <td>{m.tasksCompleted}/{m.tasksAssigned}<small>{pct(m.completionRate)}</small></td>
                    <td>{pct(m.onTimeRate)}</td>
                    <td className={m.tasksOverdue ? "difference-value" : ""}>{m.tasksOverdue}</td>
                    <td>
                      {number(m.receiptsHandled + m.putawayCompleted + m.stockCountsCompleted + m.packingCompleted + m.damageReports + m.claimedWarehouseTasks)}
                      <small>GR {m.receiptsHandled} · PA {m.putawayCompleted} · SC {m.stockCountsCompleted} · PK {m.packingCompleted}</small>
                    </td>
                    <td>{x.averageRating != null ? `${x.averageRating} ★` : "—"}</td>
                    <td><small>{x.remark}</small></td>
                    <td>
                      <button className="text-link" disabled={detailLoading} onClick={() => openDetail(x.staffId)}>Details ↗</button>{" "}
                      <button className="text-link" onClick={() => { setRating(4); setModalError(""); setReviewing(x); }}>Review</button>
                    </td>
                  </tr>
                );
              })}
            </Table>
            <Pager pager={staffPager} label="staff" />
          </>
        ) : null
      ) : (
        <>
          <div className="detail-ribbon">
            <div><span>Grade</span><Grade value={s.grade} /></div>
            <div><span>Score</span><strong>{Number(s.score).toFixed(1)} / 100</strong></div>
            <div><span>Tasks</span><strong>{s.metrics.tasksCompleted}/{s.metrics.tasksAssigned} completed</strong><small>{pct(s.metrics.completionRate)}</small></div>
            <div><span>On time</span><strong>{pct(s.metrics.onTimeRate)}</strong><small>{s.metrics.tasksOverdue} overdue</small></div>
            <div><span>Avg. task duration</span><strong>{s.metrics.avgCompletionMinutes} min</strong></div>
            <div><span>Manager rating</span><strong>{s.averageRating != null ? `${s.averageRating} ★` : "Not rated"}</strong></div>
            <button className="ops-button" onClick={() => { setRating(4); setModalError(""); setReviewing(s); }}>+ Add review</button>
          </div>
          <p className="mo-muted">{s.remark}</p>
          <div className="mo-grid two">
            <section className="mo-card">
              <header><h3>Daily tasks (assigned vs completed)</h3></header>
              <div className="mo-card-body">
                <div className="mo-bars" aria-label="Daily tasks chart">
                  {detail.daily.map((d) => (
                    <div key={d.date} title={`${d.date}: ${d.completed}/${d.assigned}`}>
                      <span style={{ height: `${((d.assigned - d.completed) / maxDaily) * 100}%` }} />
                      <span className="done" style={{ height: `${(d.completed / maxDaily) * 100}%` }} />
                    </div>
                  ))}
                </div>
                <small className="mo-muted">Orange = completed, beige = not completed · {detail.daily.length} days</small>
              </div>
            </section>
            <section className="mo-card">
              <header><h3>Warehouse activity</h3></header>
              <div className="mo-card-body mo-kv">
                <span>Goods receipts received</span><span>{s.metrics.receiptsHandled}</span>
                <span>Putaway tasks completed</span><span>{s.metrics.putawayCompleted}</span>
                <span>Stock counts completed</span><span>{s.metrics.stockCountsCompleted}</span>
                <span>Transfers packed</span><span>{s.metrics.packingCompleted}</span>
                <span>Damage reports filed</span><span>{s.metrics.damageReports}</span>
                <span>Warehouse tasks claimed</span><span>{s.metrics.claimedWarehouseTasks}</span>
                {Object.entries(s.metrics.tasksByType || {}).map(([k, v]) => (
                  <Fragment key={k}><span>Daily tasks · {TASK_TYPE_LABEL[k] || k}</span><span>{v}</span></Fragment>
                ))}
              </div>
            </section>
          </div>
          <section className="ops-panel">
            <div className="panel-heading"><h2>Reviews</h2><span>{detail.reviews.length} reviews</span></div>
            <Table head={["Date", "Period", "Rating", "System score", "Strengths", "To improve", "Comment", "Reviewer"]}>
              {detail.reviews.map((r) => (
                <tr key={r.id}>
                  <td>{fmtDateTime(r.createdAt)}</td>
                  <td>{r.periodFrom} → {r.periodTo}</td>
                  <td>{"★".repeat(r.rating)}</td>
                  <td>{r.systemScore ?? "—"} {r.grade && <small>Grade {r.grade}</small>}</td>
                  <td>{r.strengths || "—"}</td>
                  <td>{r.improvements || "—"}</td>
                  <td>{r.comment || "—"}</td>
                  <td>{r.reviewer}</td>
                </tr>
              ))}
            </Table>
          </section>
          <section className="ops-panel mt-6">
            <div className="panel-heading"><h2>Recent tasks</h2><span>{detail.recentTasks.length} tasks</span></div>
            <Table head={["Date", "Task", "Type", "Priority", "Status", "Completed", "On time"]}>
              {detail.recentTasks.map((t) => (
                <tr key={t.id}>
                  <td>{t.taskDate}</td>
                  <td><strong>{t.title}</strong><small>{t.code}</small></td>
                  <td>{TASK_TYPE_LABEL[t.taskType] || t.taskType}</td>
                  <td>{t.priority}</td>
                  <td><Status value={t.status} /></td>
                  <td>{fmtDateTime(t.completedAt)}</td>
                  <td>{t.status === "COMPLETED" ? (t.onTime ? "Yes" : "Late") : t.overdue ? "Overdue" : "—"}</td>
                </tr>
              ))}
            </Table>
          </section>
        </>
      )}

      <Modal open={!!reviewing} title={`Review · ${reviewing?.name || ""}`} onClose={() => !busy && setReviewing(null)}>
        <form className="ops-form" onSubmit={submitReview} key={reviewing?.staffId || "none"}>
          <Notice error={modalError} />
          <p className="mo-muted">Period {from} → {to} · System score {reviewing ? Number(reviewing.score).toFixed(1) : "—"} (grade {reviewing?.grade})</p>
          <Field label="Rating"><Stars value={rating} onChange={setRating} /></Field>
          <Field label="Strengths"><textarea name="strengths" rows={2} maxLength={1000} /></Field>
          <Field label="Areas to improve"><textarea name="improvements" rows={2} maxLength={1000} /></Field>
          <Field label="Comment"><textarea name="comment" rows={2} maxLength={1000} /></Field>
          <button className="ops-button" disabled={busy}>{busy ? "Saving…" : "Save review"}</button>
        </form>
      </Modal>
    </MainLayout>
  );
}
