import { useState } from "react";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Modal from "../../../components/ui/Modal";
import useResource from "../../../hooks/useResource";
import { Notice, ResourceState, Table, number } from "../../../components/operations/WorkspaceUI";
import { STORE_API } from "../../../api/stores/manager/storeApi";
import { Metrics, Pager } from "../components/StoreUI";
import { dateTime, daysAgo, isoDate, money } from "../storeFormat";

const PAGE_SIZE = 20;

function SaleModal({ id, onClose }) {
  const resource = useResource(STORE_API.sale(id));
  const s = resource.data;
  return (
    <Modal open title={s ? `Sale ${s.code}` : "Sale"} onClose={onClose}>
      <ResourceState resource={resource} empty={false}>
        {s && (
          <>
            <p style={{ marginTop: 0 }}>{dateTime(s.createdAt)} · {s.createdByName || "—"}{s.note ? ` · ${s.note}` : ""}</p>
            <Table head={["Product", "Qty", "Unit price", "Line total", "From shelf", "From backroom"]}>
              {(s.items || []).map((i) => (
                <tr key={i.productId}>
                  <td><strong>{i.name}</strong><small>{i.productCode}</small></td>
                  <td>{number(i.quantity)}</td>
                  <td>{money(i.unitPrice)}</td>
                  <td><strong>{money(i.lineTotal)}</strong></td>
                  <td>{number(i.fromShelf)}</td>
                  <td>{number(i.fromBackroom)}</td>
                </tr>
              ))}
            </Table>
            <p style={{ textAlign: "right", fontSize: 16 }}>{number(s.totalQuantity)} items · <strong>{money(s.totalAmount)}</strong></p>
          </>
        )}
      </ResourceState>
    </Modal>
  );
}

export default function SalesPage() {
  const [from, setFrom] = useState(isoDate());
  const [to, setTo] = useState(isoDate());
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState(null);
  const valid = from && to && from <= to;
  const summary = useResource(valid ? STORE_API.salesSummary({ from, to }) : null);
  const list = useResource(valid ? STORE_API.sales({ page, size: PAGE_SIZE, from, to }) : null);
  const rows = list.data?.content || [];
  const s = summary.data;
  const setRange = (f, t) => { setFrom(f); setTo(t); setPage(0); };
  const firstOfMonth = isoDate(new Date(new Date().getFullYear(), new Date().getMonth(), 1));

  return (
    <MainLayout>
      <PageHeader
        title="Sales"
        description="Sales made at the counter, by date (both dates included)."
        actions={<button className="ops-button secondary" disabled={list.loading} onClick={() => { summary.reload(); list.reload(); }}>Refresh ↻</button>}
      />
      <div className="ops-toolbar">
        <label>From <input type="date" value={from} max={to || undefined} onChange={(e) => setRange(e.target.value, to)} /></label>
        <label>To <input type="date" value={to} min={from || undefined} onChange={(e) => setRange(from, e.target.value)} /></label>
        <button className="ops-button secondary" onClick={() => setRange(isoDate(), isoDate())}>Today</button>
        <button className="ops-button secondary" onClick={() => setRange(daysAgo(6), isoDate())}>Last 7 days</button>
        <button className="ops-button secondary" onClick={() => setRange(firstOfMonth, isoDate())}>This month</button>
      </div>
      {!valid && <Notice error="Choose a start date that is on or before the end date." />}
      <Metrics
        items={[
          { label: "Revenue", value: s ? money(s.revenue) : "—" },
          { label: "Sales", value: s ? number(s.salesCount) : "—" },
          { label: "Items sold", value: s ? number(s.itemsSold) : "—" },
        ]}
      />
      <Notice error={summary.error} />
      {valid && (
        <ResourceState resource={list} empty={!rows.length}>
          <Table head={["Sale", "Time", "Cashier", "Items", "Amount", ""]}>
            {rows.map((r) => (
              <tr key={r.id}>
                <td className="mono">{r.code}</td>
                <td>{dateTime(r.createdAt)}</td>
                <td>{r.createdByName || "—"}</td>
                <td>{number(r.totalQuantity)}</td>
                <td><strong>{money(r.totalAmount)}</strong></td>
                <td><button className="text-link" onClick={() => setSelected(r.id)}>Receipt →</button></td>
              </tr>
            ))}
          </Table>
        </ResourceState>
      )}
      <Pager data={list.data} onChange={setPage} />
      {selected && <SaleModal id={selected} onClose={() => setSelected(null)} />}
    </MainLayout>
  );
}
