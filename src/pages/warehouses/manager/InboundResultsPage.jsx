import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { errorMessage } from "../../../api/api";
import {
  decideDiscrepancy,
  fmtDateTime,
  getDamagedLots,
  getInboundResults,
  getStockCountIssues,
} from "../../../api/warehouses/manager/managerOps";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Modal from "../../../components/ui/Modal";
import { Field, Notice, Table, number } from "../../../components/operations/WorkspaceUI";
import "../../../manager-ops.css";

const TABS = [
  ["SUPPLIER", "Supplier receipts"],
  ["STORE_RETURN", "Store returns"],
  ["STOCK_COUNT", "Stock count issues"],
  ["LOTS", "Damaged stock (lots)"],
];
const ISSUE_TYPE = { SHORTAGE: ["Shortage", "danger"], DAMAGED: ["Damaged", "warn"], SURPLUS: ["Surplus", ""] };
const DECISION = { PENDING: ["Waiting for decision", "warn"], INTERNAL: ["Kept internal", "good"], BUSINESS: ["Sent to Business", ""], CLOSED: ["Closed", "good"] };

function Chip({ map, value }) {
  const [label, tone] = map[value] || [value?.replaceAll("_", " ") || "—", ""];
  return <span className={`mo-chip ${tone}`}>{label}</span>;
}

function IssueRows({ issues, onDecide, showReference }) {
  if (!issues.length) return <p className="mo-muted">No shortage, damage or surplus.</p>;
  return (
    <Table head={[...(showReference ? ["Document"] : []), "Product", "Issue", "Details", "Decision", ""]}>
      {issues.map((i) => (
        <tr key={i.id}>
          {showReference && <td><strong>{i.referenceCode}</strong><small>{fmtDateTime(i.createdAt)}</small></td>}
          <td><strong>{i.productName || "—"}</strong><small>{i.code}</small></td>
          <td><Chip map={ISSUE_TYPE} value={i.type} /></td>
          <td>{i.description || "—"}{i.resolutionNote && <small>Note: {i.resolutionNote}</small>}</td>
          <td><Chip map={DECISION} value={i.decision} />{i.handledBy && <small>{i.handledBy}</small>}</td>
          <td>
            {i.decision === "PENDING" && (
              <div className="mo-task-actions">
                <button className="text-link" onClick={() => onDecide(i, "INTERNAL")}>Keep internal</button>
                <button className="text-link" onClick={() => onDecide(i, "BUSINESS")}>Send to Business</button>
              </div>
            )}
          </td>
        </tr>
      ))}
    </Table>
  );
}

// Warehouse Manager: receipts are completed as soon as staff finish inspection.
// This page shows the results, damaged lots, and lets the manager keep each issue internal or send it to Business.
export default function InboundResultsPage() {
  const [tab, setTab] = useState("SUPPLIER");
  const [days, setDays] = useState(30);
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [open, setOpen] = useState(null);
  const [deciding, setDeciding] = useState(null);
  const [busy, setBusy] = useState(false);
  const [modalError, setModalError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      if (tab === "LOTS") setData(await getDamagedLots());
      else if (tab === "STOCK_COUNT") setData(await getStockCountIssues(days));
      else setData(await getInboundResults(tab, days));
    } catch (e) {
      setError(errorMessage(e));
      setData([]);
    } finally {
      setLoading(false);
    }
  }, [tab, days]);
  useEffect(() => {
    load();
  }, [load]);

  async function submitDecision(e) {
    e.preventDefault();
    const note = new FormData(e.currentTarget).get("note");
    setBusy(true);
    setModalError("");
    try {
      await decideDiscrepancy(deciding.issue.id, deciding.decision, note);
      setNotice(deciding.decision === "BUSINESS" ? "Sent to Business." : "Kept as an internal record.");
      setDeciding(null);
      await load();
    } catch (err) {
      setModalError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }
  const decide = (issue, decision) => { setModalError(""); setDeciding({ issue, decision }); };

  const pending = (r) => r.issues.filter((i) => i.decision === "PENDING").length;

  return (
    <MainLayout>
      <PageHeader
        title="Review receiving"
        actions={
          <>
            {tab !== "LOTS" && (
              <select aria-label="Period" value={days} onChange={(e) => setDays(Number(e.target.value))}>
                <option value={7}>Last 7 days</option>
                <option value={30}>Last 30 days</option>
                <option value={90}>Last 90 days</option>
              </select>
            )}
            <button className="ops-button secondary" disabled={loading} onClick={load}>Refresh ↻</button>
          </>
        }
      />
      <p className="mo-muted">
        Goods are received as soon as staff finish inspection: passed goods are added to stock, damaged goods go to the damaged area
        under a lot code (supplier goods: the PO code; store returns / stock counts: LOT-&lt;document code&gt;). For each shortage, damage or surplus, choose <strong>Keep internal</strong> or <strong>Send to Business</strong>.
      </p>
      <div className="mo-tabs" role="tablist">
        {TABS.map(([k, l]) => (
          <button key={k} role="tab" aria-selected={tab === k} onClick={() => { setTab(k); setOpen(null); setNotice(""); }}>{l}</button>
        ))}
      </div>
      <Notice error={error}>{notice}</Notice>

      {loading ? (
        <div className="ops-empty" role="status">Loading…</div>
      ) : tab === "LOTS" ? (
        !data.length ? <div className="ops-empty"><h3>No damaged stock</h3></div> : (
          <Table head={["Lot", "Source", "Product", "Quantity", "Location", "Note", "Recorded"]}>
            {data.map((l) => (
              <tr key={l.id}>
                <td><strong>{l.lotCode}</strong></td>
                <td>{l.sourceType?.replaceAll("_", " ")}<small>{l.sourceCode}</small></td>
                <td><strong>{l.productName}</strong><small>{l.productCode}</small></td>
                <td>{number(l.quantity)}</td>
                <td>{l.location}</td>
                <td>{l.note || "—"}</td>
                <td>{fmtDateTime(l.createdAt)}<small>{l.recordedBy || ""}</small></td>
              </tr>
            ))}
          </Table>
        )
      ) : tab === "STOCK_COUNT" ? (
        <IssueRows issues={data} onDecide={decide} showReference />
      ) : !data.length ? (
        <div className="ops-empty"><h3>No completed {tab === "SUPPLIER" ? "receipts" : "store returns"} in this period</h3></div>
      ) : (
        <Table head={["Document", "From", "Inspected by", "Passed", "Damaged", "Shortage", "Surplus", "Issues", ""]}>
          {data.flatMap((r) => [
            <tr key={r.type + r.id}>
              <td><strong>{r.code}</strong><small>{fmtDateTime(r.completedAt)}{r.lotCodes.length ? ` · ${r.lotCodes.join(", ")}` : ""}</small></td>
              <td>{r.from}</td>
              <td>{r.inspectedBy || "—"}</td>
              <td>{number(r.passed)}</td>
              <td className={r.damaged ? "difference-value" : ""}>{number(r.damaged)}</td>
              <td className={r.shortage ? "difference-value" : ""}>{number(r.shortage)}</td>
              <td>{number(r.surplus)}</td>
              <td>{r.issues.length ? <Chip map={{ P: [`${pending(r)} to decide`, pending(r) ? "warn" : "good"] }} value="P" /> : "—"}</td>
              <td>
                <div className="mo-task-actions">
                  <button className="text-link" onClick={() => setOpen(open === r.type + r.id ? null : r.type + r.id)}>
                    {open === r.type + r.id ? "Hide" : "Details"}
                  </button>
                  {r.incidentReportId && (
                    <Link className="text-link" to={`/warehouse/receiving/incident-reports/${r.incidentReportId}/print`}>Print incident report ↗</Link>
                  )}
                </div>
              </td>
            </tr>,
            ...(open === r.type + r.id
              ? [
                  <tr key={`${r.type}${r.id}-detail`}>
                    <td colSpan={9}>
                      <Table head={["Product", "Expected", "Received", "Passed", "Damaged", "Shortage", "Surplus", ...(r.type === "STORE_RETURN" ? ["Condition"] : [])]}>
                        {r.items.map((l) => (
                          <tr key={l.productId}>
                            <td><strong>{l.productName}</strong><small>{l.productCode}</small></td>
                            <td>{number(l.expected)}</td>
                            <td>{number(l.received)}</td>
                            <td>{number(l.passed)}</td>
                            <td>{number(l.damaged)}</td>
                            <td>{number(l.shortage)}</td>
                            <td>{number(l.surplus)}</td>
                            {r.type === "STORE_RETURN" && <td>{l.condition}{l.note && <small>{l.note}</small>}</td>}
                          </tr>
                        ))}
                      </Table>
                      <h3 style={{ margin: "16px 0 8px" }}>Issues</h3>
                      <IssueRows issues={r.issues} onDecide={decide} />
                    </td>
                  </tr>,
                ]
              : []),
          ])}
        </Table>
      )}

      <Modal open={!!deciding} title={deciding?.decision === "BUSINESS" ? "Send to Business" : "Keep internal"} onClose={() => !busy && setDeciding(null)}>
        <form className="ops-form" onSubmit={submitDecision} key={deciding ? deciding.issue.id + deciding.decision : "none"}>
          <Notice error={modalError} />
          <p>{deciding?.issue.productName} · {deciding?.issue.description}</p>
          <Field label={deciding?.decision === "BUSINESS" ? "What should Business do? (claim the supplier, ask the store…)" : "Internal note (optional)"}>
            <textarea name="note" rows={3} maxLength={1000} required={deciding?.decision === "BUSINESS"} minLength={deciding?.decision === "BUSINESS" ? 3 : undefined} />
          </Field>
          <button className="ops-button" disabled={busy}>{busy ? "Saving…" : "Confirm"}</button>
        </form>
      </Modal>
    </MainLayout>
  );
}
