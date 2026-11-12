import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { errorMessage } from "../../../api/api";
import { completeMyTask, getMyTasks, shiftIso, startMyTask, todayIso } from "../../../api/warehouses/manager/managerOps";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Modal from "../../../components/ui/Modal";
import { Field, Notice, number } from "../../../components/operations/WorkspaceUI";
import { TaskCard } from "../manager/DailyTasksPage";
import "../../../manager-ops.css";

// Warehouse Staff: tasks the manager assigned for today.
export default function MyTasksPage() {
  const [date, setDate] = useState(todayIso());
  const [tasks, setTasks] = useState([]);
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
      setTasks(await getMyTasks(date));
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
      {loading ? (
        <div className="ops-empty" role="status">Loading tasks…</div>
      ) : !tasks.length ? (
        <div className="ops-empty">
          <h3>No tasks assigned</h3>
          <p>Your manager has not assigned any tasks for this day yet.</p>
        </div>
      ) : (
        <div className="mo-card-body" style={{ padding: 0 }}>
          {tasks.map((t) => (
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
