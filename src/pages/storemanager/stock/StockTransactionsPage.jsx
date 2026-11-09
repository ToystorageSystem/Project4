import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import useResource from "../../../hooks/useResource";
import { Notice, ResourceState, Table, number } from "../../../components/operations/WorkspaceUI";
import { STORE_API } from "../../../api/stores/manager/storeApi";
import { Change, Pager, TX_TYPES, TxBadge } from "../components/StoreUI";
import { dateTime, daysAgo, isoDate } from "../storeFormat";

const PAGE_SIZE = 20;

export default function StockTransactionsPage() {
  const [params, setParams] = useSearchParams();
  const productId = params.get("productId") || "";
  const [type, setType] = useState("");
  const [from, setFrom] = useState(daysAgo(6));
  const [to, setTo] = useState(isoDate());
  const [page, setPage] = useState(0);
  const valid = !from || !to || from <= to;
  const resource = useResource(valid ? STORE_API.stockTransactions({ productId, type, from, to, page, size: PAGE_SIZE }) : null);
  const rows = resource.data?.content || [];
  const change = (setter) => (e) => { setter(e.target.value); setPage(0); };
  const product = productId && rows[0] ? `${rows[0].productName} (${rows[0].productCode})` : `Product #${productId}`;

  return (
    <MainLayout>
      <PageHeader
        title="Stock history"
        description="Every change to shelf and backroom stock: receiving, moves, sales and adjustments."
        actions={<button className="ops-button secondary" disabled={resource.loading} onClick={resource.reload}>Refresh ↻</button>}
      />
      <div className="ops-toolbar">
        <select aria-label="Type" value={type} onChange={change(setType)}>
          <option value="">All types</option>
          {TX_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
        </select>
        <label>From <input type="date" value={from} onChange={change(setFrom)} /></label>
        <label>To <input type="date" value={to} onChange={change(setTo)} /></label>
        {productId && (
          <span>
            {product}{" "}
            <button className="text-link" onClick={() => { setParams({}); setPage(0); }}>✕ all products</button>
          </span>
        )}
      </div>
      {!valid && <Notice error="Choose a start date that is on or before the end date." />}
      {valid && (
        <ResourceState resource={resource} empty={!rows.length}>
          <Table head={["Time", "Type", "Product", "Shelf", "Backroom", "Shelf after", "Backroom after", "By", "Reference / note"]}>
            {rows.map((t) => (
              <tr key={t.id}>
                <td>{dateTime(t.createdAt)}<small className="mono">{t.code}</small></td>
                <td><TxBadge status={t.type} /></td>
                <td>
                  <Link className="text-link" to={`/store/products/${t.productId}`}><strong>{t.productName}</strong></Link>
                  <small>{t.productCode}</small>
                </td>
                <td><Change value={t.shelfChange} /></td>
                <td><Change value={t.backroomChange} /></td>
                <td>{number(t.shelfAfter)}</td>
                <td>{number(t.backroomAfter)}</td>
                <td>{t.createdByName || "—"}</td>
                <td>{t.note || "—"}</td>
              </tr>
            ))}
          </Table>
        </ResourceState>
      )}
      <Pager data={resource.data} onChange={setPage} />
    </MainLayout>
  );
}
