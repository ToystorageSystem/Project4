import { useCallback, useEffect, useMemo, useState } from "react";
import usePager, { searchRows } from "../../../hooks/usePager";
import Pager, { SearchBox } from "../../../components/operations/Pager";
import { errorMessage } from "../../../api/api";
import { DELIVERY_STATUS, getInboundSchedule, shiftIso, todayIso } from "../../../api/warehouses/manager/managerOps";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import { Notice, Table, number } from "../../../components/operations/WorkspaceUI";
import "../../../manager-ops.css";

export function DeliveryStatus({ value }) {
  const [label, tone] = DELIVERY_STATUS[value] || [value, ""];
  return <span className={`mo-chip ${tone}`}>{label}</span>;
}
const weekday = (iso) => new Date(`${iso}T00:00:00`).toLocaleDateString("en-GB", { weekday: "short", day: "numeric", month: "short" });
const DATE_SOURCE = { AGREED: "Agreed with supplier", SUPPLIER_PROPOSED: "Proposed by supplier", EXPECTED: "PO expected date", EXPECTED_RECEIPT: "Transfer ETA" };

// Warehouse Manager: which deliveries are expected on which day, to plan how many people work in Receiving.
export default function InboundSchedulePage() {
  const today = todayIso();
  const [start, setStart] = useState(today);
  const [day, setDay] = useState(today);
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const days = useMemo(() => Array.from({ length: 7 }, (_, i) => shiftIso(start, i)), [start]);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setRows(await getInboundSchedule(days[0], days[6]));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [days]);
  useEffect(() => {
    load();
  }, [load]);

  // Late deliveries are shown on today until they arrive.
  const dayOf = (r) => (r.status === "LATE" && r.deliveryDate < today ? today : r.deliveryDate);
  const [query, setQuery] = useState("");
  const visible = searchRows(rows.filter((r) => dayOf(r) === day), query, (r) => [r.orderCode, r.supplierName, r.supplierPhone, r.receiptCode, r.receivingStaff, r.status, r.note]);
  const units = visible.reduce((s, r) => s + (r.status === "RECEIVED" ? 0 : r.remainingQuantity || 0), 0);
  const go = (d) => { setStart(d); setDay(d); };

  return (
    <MainLayout>
      <PageHeader
        title="Inbound schedule"
        actions={
          <>
            <button className="ops-button secondary" onClick={() => go(shiftIso(start, -7))} aria-label="Previous week">←</button>
            <button className="ops-button secondary" onClick={() => go(today)}>This week</button>
            <button className="ops-button secondary" onClick={() => go(shiftIso(start, 7))} aria-label="Next week">→</button>
            <button className="ops-button secondary" disabled={loading} onClick={load}>Refresh ↻</button>
          </>
        }
      />
      <p className="mo-muted">
        Planning view: purchase orders and stock transfers that are <strong>expected</strong> today and in the next 6 days
        (dates come from the purchase order / transfer), so you can plan how many staff work in the Receiving role.
        Late deliveries stay on today until they arrive. Use ← to look at previous weeks.
      </p>
      <Notice error={error} />
      <div className="mo-tabs" role="tablist" aria-label="Days">
        {days.map((d) => (
          <button key={d} role="tab" aria-selected={day === d} onClick={() => setDay(d)}>
            {d === today ? "Today" : weekday(d)} <span className="mo-chip">{loading ? "…" : rows.filter((r) => dayOf(r) === d).length}</span>
          </button>
        ))}
      </div>
      <div className="ops-toolbar">
        <SearchBox value={query} onChange={setQuery} placeholder="Search PO / transfer code, supplier, receipt, staff…" />
      </div>
      {!loading && visible.length > 0 && (
        <p className="mo-muted">{visible.length} deliveries · {number(units)} units still to receive</p>
      )}
      {loading ? (
        <div className="ops-empty" role="status">Loading schedule…</div>
      ) : !visible.length ? (
        <div className="ops-empty"><h3>No deliveries expected</h3><p>No purchase order or transfer is expected on {weekday(day)}.</p></div>
      ) : (
        <Table head={["From", "Order", "Expected", "Goods", "Status", "Receiving"]}>
          {visible.map((r) => (
            <tr key={r.type + (r.purchaseOrderId || r.transferId)}>
              <td>
                <strong>{r.supplierName}</strong>
                <small>{r.type === "SUPPLIER" ? r.supplierPhone || "Supplier" : "Stock transfer"}</small>
              </td>
              <td><strong style={{ whiteSpace: "nowrap" }}>{r.orderCode}</strong>{r.note && <small>{r.note}</small>}</td>
              <td>{r.deliveryDate}<small>{DATE_SOURCE[r.dateSource] || r.dateSource}</small></td>
              <td>{r.lines} lines<small>{number(r.remainingQuantity)} / {number(r.orderedQuantity)} units to receive</small></td>
              <td><DeliveryStatus value={r.status} /></td>
              <td>{r.receiptCode || "No receipt yet"}<small>{r.receivingStaff ? `By ${r.receivingStaff}` : r.receiptStatus || ""}</small></td>
            </tr>
          ))}
        </Table>
      )}
    </MainLayout>
  );
}
