import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { errorMessage } from "../../../api/api";
import {
  PRIORITIES,
  TASK_TYPES,
  TASK_TYPE_LABEL,
  cancelTask,
  copyTasks,
  createTasksBulk,
  fmtDateTime,
  fmtTime,
  AVAILABILITY,
  WORK_ROLES,
  WORK_ROLE_LABEL,
  copyShifts,
  getAvailability,
  getShifts,
  setShift,
  getJobs,
  getTaskBoard,
  getTaskStaff,
  managerCompleteTask,
  shiftIso,
  todayIso,
  updateTask,
} from "../../../api/warehouses/manager/managerOps";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Modal from "../../../components/ui/Modal";
import { Field, Notice, Status, number } from "../../../components/operations/WorkspaceUI";
import "../../../manager-ops.css";

const EMPTY = { taskType: "RECEIVING", title: "", description: "", priority: "NORMAL", dueTime: "", referenceCode: "", referencePath: "", targetQuantity: "", managerNote: "", includePutaway: true };

export function TaskCard({ task, children }) {
  const closed = ["COMPLETED", "CANCELLED"].includes(task.status);
  return (
    <div className={`mo-task p-${task.priority} ${closed ? "done" : ""} ${task.overdue ? "overdue" : ""}`}>
      <div className="mo-task-head">
        <strong>{task.title}</strong>
        <Status value={task.status} />
      </div>
      <div className="mo-task-meta">
        <span className="mo-chip">{TASK_TYPE_LABEL[task.taskType] || task.taskType}</span>
        {task.priority !== "NORMAL" && (
          <span className={`mo-chip ${task.priority === "URGENT" ? "danger" : task.priority === "HIGH" ? "warn" : ""}`}>{task.priority}</span>
        )}
        {task.overdue && <span className="mo-chip danger">Overdue</span>}
        {task.status === "COMPLETED" && (task.onTime ? <span className="mo-chip good">On time</span> : <span className="mo-chip warn">Late</span>)}
        <span>Due {fmtTime(task.dueTime)}</span>
        {task.referenceCode && <span>Ref {task.referenceCode}</span>}
        {task.targetQuantity != null && (
          <span>
            Qty {number(task.completedQuantity ?? 0)} / {number(task.targetQuantity)}
          </span>
        )}
      </div>
      {task.description && <p className="mo-muted">{task.description}</p>}
      {task.managerNote && <p className="mo-muted">Manager: {task.managerNote}</p>}
      {task.staffNote && <p className="mo-muted">Staff: {task.staffNote}</p>}
      {task.completedAt && <p className="mo-muted">Completed {fmtDateTime(task.completedAt)}</p>}
      {children && <div className="mo-task-actions">{children}</div>}
    </div>
  );
}

const JOB_CATEGORY = { ALL: "All work", INBOUND: "Inbound (receiving & putaway)", OUTBOUND: "Outbound", ISSUES: "Damaged, shortages & shelves", STOCK_COUNT: "Stock count" };
function Availability({ value }) {
  const [label, tone] = AVAILABILITY[value] || [value, ""];
  return <span className={`mo-chip ${tone}`}>{label}</span>;
}

export default function DailyTasksPage() {
  const [date, setDate] = useState(todayIso());
  const [board, setBoard] = useState(null);
  const [jobs, setJobs] = useState([]);
  const [availability, setAvailability] = useState([]);
  const [jobFilter, setJobFilter] = useState("ALL");
  const [shifts, setShifts] = useState([]);
  const [staff, setStaff] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [modal, setModal] = useState(null); // {mode:"create"|"edit"|"complete"|"cancel", task?, form?}
  const [form, setForm] = useState(EMPTY);
  const [selected, setSelected] = useState([]);
  const [modalError, setModalError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [b, s, j, a, sh] = await Promise.all([getTaskBoard(date), getTaskStaff(), getJobs(date), getAvailability(date), getShifts(date)]);
      setShifts(sh);
      setBoard(b);
      setStaff(s);
      setJobs(j);
      setAvailability(a);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [date]);
  useEffect(() => {
    load();
  }, [load]);

  const past = date < todayIso();

  async function toggleRole(row, role) {
    const roles = row.roles.includes(role) ? row.roles.filter((r) => r !== role) : [...row.roles, role];
    setBusy(true);
    setError("");
    try {
      const saved = await setShift({ date, staffId: row.staffId, roles });
      setShifts((list) => list.map((x) => (x.staffId === row.staffId ? saved : x)));
      setJobs(await getJobs(date));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  async function copyRoles() {
    const from = shiftIso(date, -1);
    if (!window.confirm(`Copy the shift roles from ${from} to ${date}?`)) return;
    setBusy(true);
    setError("");
    try {
      setShifts(await copyShifts(from, date));
      setNotice(`Shift roles copied from ${from}.`);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  // locked = the task must go to this staff member (e.g. whoever inspected the goods puts them away).
  function openCreate(prefill = {}, staffIds = [], locked = false) {
    setForm({ ...EMPTY, ...prefill });
    setSelected(staffIds);
    setModalError("");
    setModal({ mode: "create", locked });
  }
  function openEdit(task) {
    setForm({
      taskType: task.taskType,
      title: task.title,
      description: task.description || "",
      priority: task.priority,
      dueTime: task.dueTime ? task.dueTime.slice(0, 5) : "",
      referenceCode: task.referenceCode || "",
      referencePath: task.referencePath || "",
      targetQuantity: task.targetQuantity ?? "",
      managerNote: task.managerNote || "",
    });
    setSelected([task.assignedToId]);
    setModalError("");
    setModal({ mode: "edit", task });
  }

  function payload(staffId) {
    return {
      staffId,
      taskDate: date,
      taskType: form.taskType,
      title: form.title,
      description: form.description || null,
      priority: form.priority,
      dueTime: form.dueTime || null,
      referenceCode: form.referenceCode || null,
      referencePath: form.referencePath || null,
      targetQuantity: form.targetQuantity === "" ? null : Number(form.targetQuantity),
      managerNote: form.managerNote || null,
      includePutaway: form.taskType === "RECEIVING" ? form.includePutaway : null,
    };
  }

  async function run(action, message) {
    setBusy(true);
    setModalError("");
    try {
      await action();
      setModal(null);
      setNotice(message);
      await load();
    } catch (e) {
      setModalError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  function submit(e) {
    e.preventDefault();
    if (modal.mode === "create") {
      if (!selected.length) return setModalError("Select at least one staff member.");
      return run(() => createTasksBulk(selected, payload(null)), `Assigned to ${selected.length} staff member(s).`);
    }
    if (modal.mode === "edit") return run(() => updateTask(modal.task.id, payload(selected[0])), "Task updated.");
    const f = Object.fromEntries(new FormData(e.currentTarget));
    if (modal.mode === "complete")
      return run(
        () => managerCompleteTask(modal.task.id, { completedQuantity: f.completedQuantity === "" ? null : Number(f.completedQuantity), note: f.note || null }),
        "Task marked as completed.",
      );
    if (modal.mode === "cancel") return run(() => cancelTask(modal.task.id, f.reason), "Task cancelled.");
  }

  async function copyYesterday() {
    const from = shiftIso(date, -1);
    if (!window.confirm(`Copy all non-cancelled tasks from ${from} to ${date}?`)) return;
    setBusy(true);
    setError("");
    try {
      const created = await copyTasks(from, date);
      setNotice(`Copied ${created.length} task(s) from ${from}.`);
      await load();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  return (
    <MainLayout>
      <PageHeader
        title="Daily task assignment"
        actions={
          <>
            <button className="ops-button secondary" onClick={() => setDate(shiftIso(date, -1))} aria-label="Previous day">
              ←
            </button>
            <input type="date" aria-label="Task date" value={date} onChange={(e) => e.target.value && setDate(e.target.value)} />
            <button className="ops-button secondary" onClick={() => setDate(shiftIso(date, 1))} aria-label="Next day">
              →
            </button>
            <button className="ops-button secondary" disabled={busy || past} onClick={copyYesterday}>
              Copy previous day's tasks
            </button>
            <button className="ops-button" disabled={past || !staff.length} onClick={() => openCreate()}>
              + Assign task
            </button>
          </>
        }
      />
      <p className="mo-muted">Plan what each warehouse staff member must do today and follow progress in real time.</p>
      <Notice error={error}>{notice}</Notice>
      {loading && !board ? (
        <div className="ops-empty" role="status">Loading tasks…</div>
      ) : board ? (
        <>
          <div className="metric-strip">
            {[
              ["Tasks", board.total],
              ["Completed", board.completed],
              ["In progress", board.inProgress],
              ["Not started", board.assigned],
              ["Overdue", board.overdue],
            ].map(([l, v]) => (
              <div key={l}>
                <span>{l}</span>
                <strong>{number(v)}</strong>
              </div>
            ))}
          </div>

          <section className="ops-panel">
            <div className="panel-heading">
              <div>
                <h2>1. Shift roles for {date === todayIso() ? "today" : date}</h2>
                <span>Give each staff member one or more roles. Staff then take the work of their role themselves in “My tasks today”.</span>
              </div>
              {!past && <button className="ops-button secondary" disabled={busy} onClick={copyRoles}>Copy previous day's roles</button>}
            </div>
            <div className="ops-table-wrap">
              <table className="ops-table">
                <thead><tr><th>Staff</th><th>Now</th>{WORK_ROLES.map(([k, l]) => <th key={k}>{l}</th>)}</tr></thead>
                <tbody>
                  {!shifts.length && <tr><td colSpan={2 + WORK_ROLES.length}>No warehouse staff in your warehouse.</td></tr>}
                  {shifts.map((row) => {
                    const a = availability.find((x) => x.staffId === row.staffId);
                    return (
                      <tr key={row.staffId}>
                        <td><strong>{row.name}</strong><small>{row.roles.length ? `${row.roles.length} role(s)` : "No role – day off or not planned"}</small></td>
                        <td>{a ? <Availability value={a.status} /> : "—"}{a?.currentWork?.length ? <small>{a.currentWork.join(" · ")}</small> : null}</td>
                        {WORK_ROLES.map(([k, l]) => (
                          <td key={k}>
                            <input type="checkbox" aria-label={`${row.name}: ${l}`} checked={row.roles.includes(k)} disabled={busy || past}
                              onChange={() => toggleRole(row, k)} />
                          </td>
                        ))}
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>

          <div className="mo-grid two">
            <section className="mo-card">
              <header>
                <div>
                  <h3>2. Work queue for {date === todayIso() ? "today" : date}</h3>
                  <small>Staff with the matching role take these jobs themselves. Use “Assign” only for urgent work or to give it to someone specific.</small>
                </div>
                <select aria-label="Job type" value={jobFilter} onChange={(e) => setJobFilter(e.target.value)}>
                  {Object.entries(JOB_CATEGORY).map(([k, l]) => <option key={k} value={k}>{l}</option>)}
                </select>
              </header>
              <div className="mo-card-body">
                {!jobs.filter((j) => jobFilter === "ALL" || j.category === jobFilter).length && <p className="mo-muted">No work in this category.</p>}
                {jobs.filter((j) => jobFilter === "ALL" || j.category === jobFilter).map((j) => (
                  <div className={`mo-suggestion ${j.assignedTo.length ? "done" : ""}`} key={j.key}>
                    <div>
                      <strong>{j.title}</strong>{" "}
                      <span className="mo-chip">{WORK_ROLE_LABEL[j.role] || j.role}</span>{" "}
                      {j.priority !== "NORMAL" && <span className={`mo-chip ${j.priority === "URGENT" ? "danger" : j.priority === "HIGH" ? "warn" : ""}`}>{j.priority}</span>}{" "}
                      {j.lockedStaffName && <span className="mo-chip good">→ {j.lockedStaffName}</span>}
                      <p>{j.description}</p>
                      {j.assignedTo.length > 0 ? <p><strong>Taken by:</strong> {j.assignedTo.join(", ")}</p> : <p>Waiting for {j.lockedStaffName || "a staff member with this role"}</p>}
                    </div>
                    {!past && (
                      <button
                        className="ops-button secondary"
                        disabled={!staff.length}
                        onClick={() =>
                          openCreate(
                            {
                              taskType: j.taskType,
                              title: j.title,
                              description: j.description,
                              priority: j.priority,
                              referenceCode: j.referenceCode || "",
                              referencePath: j.referencePath || "",
                              targetQuantity: j.quantity != null ? String(j.quantity) : "",
                            },
                            j.lockedStaffId ? [j.lockedStaffId] : [],
                            !!j.lockedStaffId,
                          )
                        }
                      >
                        {j.assignedTo.length ? "Add staff" : "Assign"}
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </section>
            <section className="mo-card">
              <header>
                <div>
                  <h3>3. Staff availability</h3>
                  <small>Busy = doing a task now (receiving, putaway, picking, counting…). Free staff are listed first.</small>
                </div>
              </header>
              <div className="mo-card-body">
                {!availability.length && <p className="mo-muted">No warehouse staff in your warehouse.</p>}
                {availability.map((a) => (
                  <div className="mo-suggestion" key={a.staffId}>
                    <div>
                      <strong>{a.name}</strong> <Availability value={a.status} />
                      <p>
                        {a.currentWork.length ? `Now: ${a.currentWork.join(" · ")}` : "Not working on anything right now"}
                      </p>
                      <p>{a.openTasks} open · {a.completedToday} done today{a.nextDue ? ` · next due ${fmtTime(a.nextDue)}` : ""}</p>
                      <p>{a.roles?.length ? a.roles.map((r) => WORK_ROLE_LABEL[r] || r).join(" · ") : "No role today"}</p>
                    </div>
                    {!past && <button className="text-link" onClick={() => openCreate({}, [a.staffId])}>+ Task</button>}
                  </div>
                ))}
              </div>
            </section>
          </div>

          {!board.staff.length ? (
            <div className="ops-empty">
              <h3>No warehouse staff in your warehouse</h3>
              <p>Ask an administrator to add Warehouse Staff accounts to this warehouse.</p>
            </div>
          ) : (
            <div className="mo-grid">
              {board.staff.map((s) => {
                const pct = s.total ? Math.round((s.completed * 100) / s.total) : 0;
                return (
                  <article className="mo-card" key={s.staffId}>
                    <header>
                      <div>
                        <h3>{s.staffName}</h3>
                        <small>{s.email}</small>
                        <div className={`mo-progress ${pct === 100 ? "good" : ""}`} aria-label={`${pct}% completed`}>
                          <span style={{ width: `${pct}%` }} />
                        </div>
                        <small>
                          {s.completed}/{s.total} done · {s.inProgress} in progress {s.overdue ? `· ${s.overdue} overdue` : ""}
                        </small>
                      </div>
                      {!past && (
                        <button className="text-link" onClick={() => openCreate({}, [s.staffId])}>
                          + Task
                        </button>
                      )}
                    </header>
                    <div className="mo-card-body">
                      {!s.tasks.length && <p className="mo-muted">No tasks assigned for this day.</p>}
                      {s.tasks.map((t) => (
                        <TaskCard key={t.id} task={t}>
                          {["ASSIGNED", "IN_PROGRESS"].includes(t.status) && (
                            <>
                              {t.referencePath && (
                                <Link className="text-link" to={t.referencePath}>
                                  Open page ↗
                                </Link>
                              )}
                              <button className="text-link" onClick={() => openEdit(t)}>Edit</button>
                              <button className="text-link" onClick={() => { setModalError(""); setModal({ mode: "complete", task: t }); }}>Mark done</button>
                              <button className="text-link" onClick={() => { setModalError(""); setModal({ mode: "cancel", task: t }); }}>Cancel</button>
                            </>
                          )}
                        </TaskCard>
                      ))}
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </>
      ) : null}

      <Modal
        open={!!modal}
        title={{ create: "Assign task", edit: "Edit task", complete: "Mark task as done", cancel: "Cancel task" }[modal?.mode] || "Task"}
        onClose={() => !busy && setModal(null)}
      >
        <form className="ops-form" onSubmit={submit} key={modal ? modal.mode + (modal.task?.id || "") : "none"}>
          <Notice error={modalError} />
          {(modal?.mode === "create" || modal?.mode === "edit") && (
            <>
              <Field label={modal.mode === "create" ? "Assign to (one or more staff)" : "Assigned staff"}>
                {modal.mode === "create" && modal.locked ? (
                  <p className="mo-muted">
                    <strong>{staff.find((x) => x.id === selected[0])?.name || "Assigned staff"}</strong> — this staff member inspected these goods, so they must also put them away.
                  </p>
                ) : modal.mode === "create" ? (
                  <div className="mo-checklist">
                    {(availability.length ? availability.map((a) => ({ id: a.staffId, name: a.name, status: a.status, open: a.openTasks })) : staff).map((s) => (
                      <label key={s.id}>
                        <input
                          type="checkbox"
                          checked={selected.includes(s.id)}
                          onChange={(e) => setSelected(e.target.checked ? [...selected, s.id] : selected.filter((x) => x !== s.id))}
                        />
                        {s.name} {s.status && <Availability value={s.status} />}
                      </label>
                    ))}
                  </div>
                ) : (
                  <select value={selected[0] || ""} onChange={(e) => setSelected([Number(e.target.value)])}>
                    {staff.map((s) => (
                      <option key={s.id} value={s.id}>{s.name} · {s.email}</option>
                    ))}
                  </select>
                )}
              </Field>
              <Field label="Task type">
                <select value={form.taskType} onChange={set("taskType")}>
                  {TASK_TYPES.map(([v, l]) => (
                    <option key={v} value={v}>{l}</option>
                  ))}
                </select>
              </Field>
              {form.taskType === "RECEIVING" && modal.mode === "create" && (
                <label className="review-check">
                  <input type="checkbox" checked={form.includePutaway} onChange={(e) => setForm({ ...form, includePutaway: e.target.checked })} />
                  Also assign putaway of these goods to the same staff (whoever inspects the goods puts them on the shelves)
                </label>
              )}
              {form.taskType === "PUTAWAY" && (
                <p className="mo-muted">Rule: goods must be put away by the staff member who received and inspected them. Enter the receipt code (GR-…) as reference so the system can check.</p>
              )}
              <Field label="Title" value={form.title} onChange={set("title")} required minLength={3} maxLength={200} placeholder="e.g. Put away GR-000123 into zone A" />
              <Field label="Instructions">
                <textarea rows={3} maxLength={1000} value={form.description} onChange={set("description")} placeholder="Zone, shelf, products, quality checks…" />
              </Field>
              <Field label="Priority">
                <select value={form.priority} onChange={set("priority")}>
                  {PRIORITIES.map((p) => (
                    <option key={p}>{p}</option>
                  ))}
                </select>
              </Field>
              <Field label="Due time" type="time" value={form.dueTime} onChange={set("dueTime")} />
              <Field label="Reference (document / zone)" value={form.referenceCode} onChange={set("referenceCode")} maxLength={100} placeholder="GR-…, ST-…, SC-…, Zone A" />
              <Field label="Target quantity (lines / items / locations)" type="number" min="0" step="1" value={form.targetQuantity} onChange={set("targetQuantity")} />
              <Field label="Note for staff" value={form.managerNote} onChange={set("managerNote")} maxLength={1000} />
            </>
          )}
          {modal?.mode === "complete" && (
            <>
              <p>{modal.task.title} — {modal.task.assignedTo}</p>
              <Field label="Completed quantity" name="completedQuantity" type="number" min="0" step="1" defaultValue={modal.task.completedQuantity ?? modal.task.targetQuantity ?? ""} />
              <Field label="Note" name="note" maxLength={1000} />
            </>
          )}
          {modal?.mode === "cancel" && (
            <>
              <p>{modal.task.title} — {modal.task.assignedTo}</p>
              <Field label="Reason" name="reason" required minLength={3} maxLength={500} />
            </>
          )}
          <button className="ops-button" disabled={busy}>{busy ? "Saving…" : "Confirm"}</button>
        </form>
      </Modal>
    </MainLayout>
  );
}
