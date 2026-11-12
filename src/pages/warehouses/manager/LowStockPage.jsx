import { useCallback, useEffect, useMemo, useState } from "react";
import usePager from "../../../hooks/usePager";
import Pager from "../../../components/operations/Pager";
import { errorMessage } from "../../../api/api";
import {
  fmtDateTime,
  getLowStock,
  getSentLowStockReport,
  getSentLowStockReports,
  sendLowStockReport,
  setMinimumLevel,
} from "../../../api/warehouses/manager/managerOps";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Modal from "../../../components/ui/Modal";
import { Field, Notice, Status, Table, number } from "../../../components/operations/WorkspaceUI";
import "../../../manager-ops.css";

const SEVERITY = {
  OUT_OF_STOCK: ["Out of stock", "danger"],
  CRITICAL: ["Critical", "warn"],
  LOW: ["Low", ""],
};
export function Severity({ value }) {
  if (!value) return <span className="mo-chip good">OK</span>;
  const [label, tone] = SEVERITY[value] || [value, ""];
  return <span className={`mo-chip ${tone}`}>{label}</span>;
}

export default function LowStockPage() {
  const [tab, setTab] = useState("low");
  const [threshold, setThreshold] = useState(10);
  const [data, setData] = useState(null);
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [search, setSearch] = useState("");
  const [selected, setSelected] = useState({});
  const [quantities, setQuantities] = useState({});
  const [minimums, setMinimums] = useState({});
  const [busy, setBusy] = useState(false);
  const [sending, setSending] = useState(false);
  const [detail, setDetail] = useState(null);
  const [modalError, setModalError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      if (tab === "reports") setReports(await getSentLowStockReports());
      else {
        const result = await getLowStock(threshold, tab === "low");
        setData(result);
        if (tab === "low") setSelected(Object.fromEntries(result.items.map((i) => [i.productId, true])));
      }
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [tab, threshold]);
  useEffect(() => {
    load();
  }, [load]);

  const items = useMemo(
    () =>
      (data?.items || []).filter((i) =>
        [i.productCode, i.productName, i.barcode].join(" ").toLowerCase().includes(search.toLowerCase()),
      ),
    [data, search],
  );
  const chosen = (data?.items || []).filter((i) => selected[i.productId]);

  async function saveMinimum(item) {
    const value = Number(minimums[item.productId]);
    if (!Number.isInteger(value) || value < 0) return setError("Minimum level must be a whole number ≥ 0.");
    setBusy(true);
    setError("");
    try {
      await setMinimumLevel(item.productId, value);
      setNotice(`Minimum level for ${item.productName} set to ${value}.`);
      setMinimums((m) => ({ ...m, [item.productId]: undefined }));
      await load();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  async function send(e) {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(e.currentTarget));
    setBusy(true);
    setModalError("");
    try {
      const suggested = {};
      chosen.forEach((i) => {
        const q = Number(quantities[i.productId]);
        if (Number.isInteger(q) && q > 0) suggested[i.productId] = q;
      });
      const result = await sendLowStockReport({ productIds: chosen.map((i) => i.productId), threshold, note: f.note || null, suggestedQuantities: suggested });
      setSending(false);
      setNotice(`Report ${result.report.code} sent to Business Operations (${result.report.itemCount} products).`);
      setQuantities({});
    } catch (err) {
      setModalError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function openReport(id) {
    setError("");
    try {
      setDetail(await getSentLowStockReport(id));
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  const itemPager = usePager(items, 20, [search, tab, threshold]);
  const reportPager = usePager(reports, 20, [tab]);

  return (
    <MainLayout>
      <PageHeader
        title="Low-stock report"
        actions={
          <>
            {tab !== "reports" && (
              <label className="mo-muted" style={{ display: "inline-flex", gap: 6, alignItems: "center" }}>
                Default minimum
                <input className="mo-inline-input" type="number" min="0" step="1" value={threshold}
                  onChange={(e) => setThreshold(Math.max(0, Number(e.target.value) || 0))} aria-label="Default minimum level" />
              </label>
            )}
            <button className="ops-button secondary" disabled={loading} onClick={load}>Refresh ↻</button>
            {tab === "low" && (
              <button className="ops-button" disabled={!chosen.length || busy} onClick={() => { setModalError(""); setSending(true); }}>
                Send to Business ({chosen.length})
              </button>
            )}
          </>
        }
      />
      <p className="mo-muted">
        Products whose available stock is at or below their minimum level. Products without a configured minimum use the default minimum.
        Send the list to Business Operations so they can raise purchase orders or transfers.
      </p>
      <div className="mo-tabs" role="tablist">
        {[["low", "Low stock"], ["all", "Minimum levels"], ["reports", "Sent reports"]].map(([k, l]) => (
          <button key={k} role="tab" aria-selected={tab === k} onClick={() => { setTab(k); setNotice(""); }}>{l}</button>
        ))}
      </div>
      <Notice error={error}>{notice}</Notice>

      {tab !== "reports" && data && (
        <>
          {tab === "low" && (
            <div className="metric-strip">
              <div><span>Out of stock</span><strong>{number(data.outOfStock)}</strong></div>
              <div><span>Critical (≤ 50% of min.)</span><strong>{number(data.critical)}</strong></div>
              <div><span>Low</span><strong>{number(data.low)}</strong></div>
              <div><span>Selected for report</span><strong>{number(chosen.length)}</strong></div>
            </div>
          )}
          <div className="ops-toolbar">
            <input aria-label="Search products" placeholder="Search product name, SKU or barcode…" value={search} onChange={(e) => setSearch(e.target.value)} />
            {tab === "low" && (
              <>
                <button className="text-link" onClick={() => setSelected(Object.fromEntries(data.items.map((i) => [i.productId, true])))}>Select all</button>
                <button className="text-link" onClick={() => setSelected({})}>Clear</button>
              </>
            )}
            <span>{items.length} products</span>
          </div>
        </>
      )}

      {loading ? (
        <div className="ops-empty" role="status">Loading…</div>
      ) : tab === "reports" ? (
        !reports.length ? (
          <div className="ops-empty"><h3>No reports sent yet</h3></div>
        ) : (
          <>
          <Table head={["Report", "Sent", "Products", "Out of stock", "Status", "Business response", ""]}>
            {reportPager.rows.map((r) => (
              <tr key={r.id}>
                <td><strong>{r.code}</strong><small>{r.createdBy}</small></td>
                <td>{fmtDateTime(r.createdAt)}</td>
                <td>{r.itemCount}</td>
                <td>{r.outOfStock}</td>
                <td><Status value={r.status} /></td>
                <td>{r.businessNote || "—"}{r.handledBy && <small>{r.handledBy}</small>}</td>
                <td><button className="text-link" onClick={() => openReport(r.id)}>Open ↗</button></td>
              </tr>
            ))}
          </Table>
          <Pager pager={reportPager} label="reports" />
        </>
        )
      ) : !items.length ? (
        <div className="ops-empty"><h3>{tab === "low" ? "No low-stock products" : "No products"}</h3><p>Stock levels are above the minimum.</p></div>
      ) : (
        <>
        <Table head={[...(tab === "low" ? [""] : []), "Product / SKU", "On hand", "Reserved", "Available", "Minimum", "Status", ...(tab === "low" ? ["Suggested qty"] : []), "Set minimum"]}>
          {itemPager.rows.map((i) => (
            <tr key={i.productId}>
              {tab === "low" && (
                <td>
                  <input type="checkbox" aria-label={`Include ${i.productName}`} checked={!!selected[i.productId]}
                    onChange={(e) => setSelected({ ...selected, [i.productId]: e.target.checked })} />
                </td>
              )}
              <td><strong>{i.productName}</strong><small>{i.productCode} · {i.barcode}</small></td>
              <td>{number(i.onHand)}</td>
              <td>{number(i.reserved)}</td>
              <td><strong>{number(i.available)}</strong> <small>{i.unit}</small></td>
              <td>{number(i.minimumLevel)}{i.defaultMinimum && <small>default</small>}</td>
              <td><Severity value={i.severity} /></td>
              {tab === "low" && (
                <td>
                  <input className="mo-inline-input" type="number" min="1" step="1" aria-label={`Suggested quantity ${i.productName}`}
                    placeholder={String(i.suggestedQuantity)} value={quantities[i.productId] ?? ""}
                    onChange={(e) => setQuantities({ ...quantities, [i.productId]: e.target.value })} />
                </td>
              )}
              <td>
                {i.locations > 0 ? (
                  <>
                    <input className="mo-inline-input" type="number" min="0" step="1" aria-label={`Minimum level ${i.productName}`}
                      placeholder={i.defaultMinimum ? "—" : String(i.minimumLevel)} value={minimums[i.productId] ?? ""}
                      onChange={(e) => setMinimums({ ...minimums, [i.productId]: e.target.value })} />{" "}
                    <button className="text-link" disabled={busy || minimums[i.productId] == null || minimums[i.productId] === ""} onClick={() => saveMinimum(i)}>Save</button>
                  </>
                ) : (
                  <small>No location yet</small>
                )}
              </td>
            </tr>
          ))}
        </Table>
        <Pager pager={itemPager} label="products" />
        </>
      )}

      <Modal open={sending} title="Send low-stock report to Business" onClose={() => !busy && setSending(false)}>
        <form className="ops-form" onSubmit={send}>
          <Notice error={modalError} />
          <p>
            {chosen.length} products · {chosen.filter((i) => i.severity === "OUT_OF_STOCK").length} out of stock ·{" "}
            {chosen.filter((i) => i.severity === "CRITICAL").length} critical
          </p>
          <Field label="Note for Business Operations"><textarea name="note" rows={3} maxLength={1000} placeholder="e.g. Peak season next week, please prioritise out-of-stock items." /></Field>
          <button className="ops-button" disabled={busy}>{busy ? "Sending…" : "Send report"}</button>
        </form>
      </Modal>

      <Modal open={!!detail} title={detail ? `Report ${detail.report.code}` : "Report"} onClose={() => setDetail(null)}>
        {detail && (
          <>
            <div className="mo-kv" style={{ marginBottom: 12 }}>
              <span>Status</span><span><Status value={detail.report.status} /></span>
              <span>Sent</span><span>{fmtDateTime(detail.report.createdAt)}</span>
              <span>Acknowledged</span><span>{fmtDateTime(detail.report.acknowledgedAt)}</span>
              <span>Resolved</span><span>{fmtDateTime(detail.report.resolvedAt)}</span>
              <span>Your note</span><span>{detail.report.managerNote || "—"}</span>
              <span>Business response</span><span>{detail.report.businessNote || "—"}</span>
            </div>
            <Table head={["Product", "Available", "Minimum", "Suggested", "Status"]}>
              {detail.items.map((i) => (
                <tr key={i.productId}>
                  <td><strong>{i.productName}</strong><small>{i.productCode}</small></td>
                  <td>{number(i.available)}</td>
                  <td>{number(i.minimumLevel)}</td>
                  <td>{number(i.suggestedQuantity)}</td>
                  <td><Severity value={i.severity} /></td>
                </tr>
              ))}
            </Table>
          </>
        )}
      </Modal>
    </MainLayout>
  );
}
