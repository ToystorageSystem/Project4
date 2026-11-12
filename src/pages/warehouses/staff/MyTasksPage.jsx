import { useCallback, useEffect, useState } from "react";
import usePager, { searchRows } from "../../../hooks/usePager";
import Pager, { SearchBox } from "../../../components/operations/Pager";
import { Link } from "react-router-dom";
import { errorMessage } from "../../../api/api";
import { TASK_TYPE_LABEL, WORK_ROLE_LABEL, completeMyTask, getMyTasks, getStaffQueue, shiftIso, startMyTask, takeJob, todayIso } from "../../../api/warehouses/manager/managerOps";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Modal from "../../../components/ui/Modal";
import { Field, Notice, number } from "../../../components/operations/WorkspaceUI";
import { TaskCard } from "../manager/DailyTasksPage";
import "../../../manager-ops.css";

// Warehouse Staff: roles for today, work to take from the queue, and my tasks.
export default function MyTasksPage() {
  const [date, setDate] = useState(todayIso());
  const [tasks, setTasks] = useState([]);
  const [queue, setQueue] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [completing, setCompleting] = useState(null);
  const [modalError, setModalError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [mine, q] = await Promise.all([getMyTasks(date), getStaffQueue(date)]);
      setTasks(mine);
      setQueue(q);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [date]);
  useEffect(() => {
    load();
  }, [load]);

  async function start(task) {
    setBusy(true);
    setError("");
    try {
      await startMyTask(task.id);
      setNotice(`Started: ${task.title}`);
      await load();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  async function take(job) {
    setBusy(true);
    setError("");
    try {
      await takeJob(date, job.key);
      setNotice(`You took: ${job.title}. Open the work page to start.`);
      await load();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  async function complete(e) {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(e.currentTarget));
    setBusy(true);
    setModalError("");
    try {
      await completeMyTask(completing.id, { completedQuantity: f.completedQuantity === "" ? null : Number(f.completedQuantity), note: f.note || null });
      setCompleting(null);
      setNotice("Task completed. Good job!");
      await load();
    } catch (err) {
      setModalError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  const done = tasks.filter((t) => t.status === "COMPLETED").length;
  const open = tasks.filter((t) => ["ASSIGNED", "IN_PROGRESS"].includes(t.status)).length;
  const isToday = date === todayIso();
  const [query, setQuery] = useState("");
  const pick = (x) => [x.title, x.description, x.referenceCode, x.taskType, x.status, x.priority];
  const queueJobs = searchRows(queue?.jobs, query, pick);
  const myTasks = searchRows(tasks, query, pick);
  const taskPager = usePager(myTasks, 10, [query, date]);

  return (
    <MainLayout>
      <PageHeader
        title="My tasks today"
        actions={
          <>
            <button className="ops-button secondary" onClick={() => setDate(shiftIso(date, -1))} aria-label="Previous day">←</button>
            <input type="date" aria-label="Task date" value={date} onChange={(e) => e.target.value && setDate(e.target.value)} />
            <button className="ops-button secondary" onClick={() => setDate(shiftIso(date, 1))} aria-label="Next day">→</button>
            <button className="ops-button secondary" disabled={loading} onClick={load}>Refresh ↻</button>
          </>
        }
      />
      <Notice error={error}>{notice}</Notice>
      <div className="metric-strip">
        {[
          ["Assigned", tasks.filter((t) => t.status !== "CANCELLED").length],
          ["Open", open],
          ["Completed", done],
          ["Overdue", tasks.filter((t) => t.overdue).length],
        ].map(([l, v]) => (
          <div key={l}>
            <span>{l}</span>
            <strong>{loading ? "—" : number(v)}</strong>
          </div>
        ))}
      </div>
      {queue && (
        <section className="ops-panel" style={{ marginBottom: 16 }}>
          <div className="panel-heading">
            <div>
              <h2>Work you can take</h2>
              <span>
                Your role{queue.roles.length > 1 ? "s" : ""} {isToday ? "today" : "on this day"}:{" "}
                {queue.roles.length ? queue.roles.map((r) => WORK_ROLE_LABEL[r] || r).join(" · ") : "not assigned yet – ask your manager"}
              </span>
            </div>
          </div>
          <div className="mo-suggestions">
            {!queueJobs.length && <p className="mo-muted">No open work for your role right now.</p>}
            {queueJobs.map((j) => (
              <div className="mo-suggestion" key={j.key}>
                <div>
                  <strong>{j.title}</strong> <span className="mo-chip">{TASK_TYPE_LABEL[j.taskType] || j.taskType}</span>{" "}
                  {j.priority !== "NORMAL" && <span className={`mo-chip ${j.priority === "URGENT" ? "danger" : j.priority === "HIGH" ? "warn" : ""}`}>{j.priority}</span>}{" "}
                  {j.lockedStaffId && <span className="mo-chip good">Only you</span>}
                  <p>{j.description}</p>
                </div>
                {isToday && <button className="ops-button" disabled={busy} onClick={() => take(j)}>Take</button>}
              </div>
            ))}
          </div>
        </section>
      )}
      <div className="ops-toolbar">
        <SearchBox value={query} onChange={setQuery} placeholder="Search tasks and work: title, PO / transfer code…" />
      </div>
      <h2 style={{ margin: "8px 0 12px" }}>My tasks</h2>
      {loading ? (
        <div className="ops-empty" role="status">Loading tasks…</div>
      ) : !tasks.length ? (
        <div className="ops-empty">
          <h3>No tasks assigned</h3>
          <p>Take a job from the list above, or wait for your manager to assign one.</p>
        </div>
      ) : (
        <div className="mo-card-body" style={{ padding: 0 }}>
          {taskPager.rows.map((t) => (
            <TaskCard key={t.id} task={t}>
              {t.referencePath && ["ASSIGNED", "IN_PROGRESS"].includes(t.status) && (
                <Link className="text-link" to={t.referencePath}>Go to work page ↗</Link>
              )}
              {t.status === "ASSIGNED" && isToday && (
                <button className="ops-button secondary" disabled={busy} onClick={() => start(t)}>Start</button>
              )}
              {["ASSIGNED", "IN_PROGRESS"].includes(t.status) && (
                <button className="ops-button" disabled={busy} onClick={() => { setModalError(""); setCompleting(t); }}>Mark completed</button>
              )}
            </TaskCard>
          ))}
        </div>
      )}
      {!loading && <Pager pager={taskPager} sizes={[10, 20, 50]} label="tasks" />}
      <Modal open={!!completing} title="Complete task" onClose={() => !busy && setCompleting(null)}>
        <form className="ops-form" onSubmit={complete} key={completing?.id || "none"}>
          <Notice error={modalError} />
          <p>{completing?.title}</p>
          <Field label="Completed quantity" name="completedQuantity" type="number" min="0" step="1" defaultValue={completing?.targetQuantity ?? ""} />
          <Field label="Note for your manager (issues, shortages, damages…)" name="note" maxLength={1000} />
          <button className="ops-button" disabled={busy}>{busy ? "Saving…" : "Confirm"}</button>
        </form>
      </Modal>
    </MainLayout>
  );
}
