import { useCallback, useEffect, useState } from "react";
import usePager, { searchRows, rowText } from "../../hooks/usePager";
import Pager, { SearchBox } from "../../components/operations/Pager";
import { Link } from "react-router-dom";
import { errorMessage } from "../../api/api";
import {
  acknowledgeLowStockReport,
  getLowStockReport,
  listLowStockReports,
  resolveLowStockReport,
} from "../../api/businessmanager/lowStockReports";
import MainLayout from "../../components/layout/MainLayout";
import PageHeader from "../../components/layout/PageHeader";
import Modal from "../../components/ui/Modal";
import { Field, Notice, Status, Table, number } from "../../components/operations/WorkspaceUI";
import { Severity } from "../warehouses/manager/LowStockPage";
import "../../manager-ops.css";

const fmt = (v) => (v ? String(v).replace("T", " ").slice(0, 16) : "—");

// Business Operations: low-stock reports sent by Warehouse Managers.
export default function LowStockReportsPage() {
  const [status, setStatus] = useState("SENT");
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [detail, setDetail] = useState(null);
  const [mode, setMode] = useState(null);
  const [busy, setBusy] = useState(false);
  const [modalError, setModalError] = useState("");
  const [query, setQuery] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setRows(await listLowStockReports(status));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [status]);
  useEffect(() => {
    load();
  }, [load]);

  async function open(id) {
    setError("");
    setMode(null);
    setModalError("");
    try {
      setDetail(await getLowStockReport(id));
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  async function submit(e) {
    e.preventDefault();
    const note = new FormData(e.currentTarget).get("note");
    setBusy(true);
    setModalError("");
    try {
      const id = detail.report.id;
      const result = mode === "ack" ? await acknowledgeLowStockReport(id, note) : await resolveLowStockReport(id, note);
      setDetail(result);
      setMode(null);
      setNotice(`${result.report.code} ${mode === "ack" ? "acknowledged" : "resolved"}.`);
      await load();
    } catch (err) {
      setModalError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  const shown = searchRows(rows, query, (x) => [x.code, x.warehouseName, x.createdBy, x.status]);
  const pager = usePager(shown, 20, [query, status]);
  const r = detail?.report;
  const totalSuggested = (detail?.items || []).reduce((s, i) => s + i.suggestedQuantity, 0);

  return (
    <MainLayout>
      <PageHeader
        title="Low-stock reports"
        actions={
          <>
            <select aria-label="Status" value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="SENT">New</option>
              <option value="ACKNOWLEDGED">Acknowledged</option>
              <option value="RESOLVED">Resolved</option>
              <option value="ALL">All</option>
            </select>
            <button className="ops-button secondary" disabled={loading} onClick={load}>Refresh ↻</button>
          </>
        }
      />
      <p className="mo-muted">Warehouse Managers report products at or below their minimum stock level. Acknowledge the report, then record the replenishment plan (purchase order or stock transfer).</p>
      <Notice error={error}>{notice}</Notice>
      <div className="ops-toolbar"><SearchBox value={query} onChange={setQuery} label="Search reports" placeholder="Search report code, warehouse or sender…" /></div>
      {loading ? (
        <div className="ops-empty" role="status">Loading reports…</div>
      ) : !shown.length ? (
        <div className="ops-empty"><h3>No reports</h3><p>{query ? "No report matches your search." : "There are no low-stock reports with this status."}</p></div>
      ) : (
        <>
        <Table head={["Report", "Warehouse", "Sent by", "Sent", "Products", "Out of stock", "Critical", "Status", ""]}>
          {pager.rows.map((x) => (
            <tr key={x.id}>
              <td><strong>{x.code}</strong></td>
              <td>{x.warehouseName}</td>
              <td>{x.createdBy}</td>
              <td>{fmt(x.createdAt)}</td>
              <td>{x.itemCount}</td>
              <td className={x.outOfStock ? "difference-value" : ""}>{x.outOfStock}</td>
              <td>{x.critical}</td>
              <td><Status value={x.status} /></td>
              <td><button className="text-link" onClick={() => open(x.id)}>Open ↗</button></td>
            </tr>
          ))}
        </Table>
        <Pager pager={pager} label="reports" />
        </>
      )}

      <Modal open={!!detail} title={r ? `${r.code} · ${r.warehouseName}` : "Report"} onClose={() => !busy && setDetail(null)}>
        {detail && (
          <>
            <div className="mo-kv" style={{ marginBottom: 12 }}>
              <span>Status</span><span><Status value={r.status} /></span>
              <span>Sent by</span><span>{r.createdBy} · {fmt(r.createdAt)}</span>
              <span>Warehouse note</span><span>{r.managerNote || "—"}</span>
              <span>Business response</span><span>{r.businessNote || "—"}</span>
              <span>Handled by</span><span>{r.handledBy || "—"}</span>
              <span>Total suggested quantity</span><span>{number(totalSuggested)}</span>
            </div>
            <Table head={["Product", "On hand", "Reserved", "Available", "Minimum", "Suggested", "Status"]}>
              {detail.items.map((i) => (
                <tr key={i.productId}>
                  <td><strong>{i.productName}</strong><small>{i.productCode}</small></td>
                  <td>{number(i.onHand)}</td>
                  <td>{number(i.reserved)}</td>
                  <td><strong>{number(i.available)}</strong></td>
                  <td>{number(i.minimumLevel)}</td>
                  <td>{number(i.suggestedQuantity)}</td>
                  <td><Severity value={i.severity} /></td>
                </tr>
              ))}
            </Table>
            {mode ? (
              <form className="ops-form" onSubmit={submit} style={{ marginTop: 16 }}>
                <Notice error={modalError} />
                <Field label={mode === "ack" ? "Note (optional)" : "Replenishment plan (PO / transfer code, ETA…)"}>
                  <textarea name="note" rows={3} maxLength={1000} required={mode === "resolve"} minLength={mode === "resolve" ? 3 : undefined} />
                </Field>
                <div className="mo-task-actions">
                  <button className="ops-button" disabled={busy}>{busy ? "Saving…" : mode === "ack" ? "Acknowledge" : "Mark resolved"}</button>
                  <button type="button" className="ops-button secondary" disabled={busy} onClick={() => setMode(null)}>Back</button>
                </div>
              </form>
            ) : (
              r.status !== "RESOLVED" && (
                <div className="mo-task-actions" style={{ marginTop: 16 }}>
                  {r.status === "SENT" && <button className="ops-button secondary" onClick={() => { setModalError(""); setMode("ack"); }}>Acknowledge</button>}
                  <button className="ops-button" onClick={() => { setModalError(""); setMode("resolve"); }}>Record replenishment plan</button>
                  <Link className="text-link" to="/business/purchase-orders/new">Create purchase order ↗</Link>
                  <Link className="text-link" to="/business/stock-transfers/new">Create stock transfer ↗</Link>
                </div>
              )
            )}
          </>
        )}
      </Modal>
    </MainLayout>
  );
}
