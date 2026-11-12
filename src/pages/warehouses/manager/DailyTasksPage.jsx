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

const EMPTY = { taskType: "RECEIVING", title: "", description: "", priority: "NORMAL", dueTime: "", referenceCode: "", referencePath: "", targetQuantity: "", managerNote: "" };

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

export default function DailyTasksPage() {
  const [date, setDate] = useState(todayIso());
  const [board, setBoard] = useState(null);
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
      const [b, s] = await Promise.all([getTaskBoard(date), getTaskStaff()]);
      setBoard(b);
      setStaff(s);
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

  function openCreate(prefill = {}, staffIds = []) {
    setForm({ ...EMPTY, ...prefill });
    setSelected(staffIds);
    setModalError("");
    setModal({ mode: "create" });
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
              Copy previous day
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

          {!past && board.suggestions?.length > 0 && (
            <section className="ops-panel">
              <div className="panel-heading">
                <h2>What needs to be done today</h2>
                <span>Generated from open receipts, putaway, transfers, damage and stock-count windows</span>
              </div>
              <div className="mo-suggestions">
                {board.suggestions.map((s) => (
                  <div className="mo-suggestion" key={s.taskType + s.title}>
                    <div>
                      <strong>{s.title}</strong> {s.priority !== "NORMAL" && <span className={`mo-chip ${s.priority === "URGENT" ? "danger" : s.priority === "HIGH" ? "warn" : ""}`}>{s.priority}</span>}
                      <p>{s.description}</p>
                    </div>
                    <button
                      className="ops-button secondary"
                      disabled={!staff.length}
                      onClick={() =>
                        openCreate({
                          taskType: s.taskType,
                          title: s.title,
                          description: s.description,
                          priority: s.priority,
                          referencePath: s.referencePath || "",
                          targetQuantity: s.pendingCount > 0 ? String(s.pendingCount) : "",
                        })
                      }
                    >
                      Assign
                    </button>
                  </div>
                ))}
              </div>
            </section>
          )}

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
                {modal.mode === "create" ? (
                  <div className="mo-checklist">
                    {staff.map((s) => (
                      <label key={s.id}>
                        <input
                          type="checkbox"
                          checked={selected.includes(s.id)}
                          onChange={(e) => setSelected(e.target.checked ? [...selected, s.id] : selected.filter((x) => x !== s.id))}
                        />
                        {s.name}
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
