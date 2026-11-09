import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import useResource from "../../../hooks/useResource";
import { errorMessage } from "../../../api/api";
import { Notice, ResourceState, Table, number } from "../../../components/operations/WorkspaceUI";
import { STORE_API, findProductByBarcode } from "../../../api/stores/manager/storeApi";
import { Pager, RefreshButton } from "../components/StoreUI";
import { money } from "../storeFormat";

const PAGE_SIZE = 20;

export default function ProductListPage() {
  const navigate = useNavigate();
  const [text, setText] = useState("");
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(0);
  const [barcode, setBarcode] = useState("");
  const [barcodeError, setBarcodeError] = useState("");
  const [finding, setFinding] = useState(false);
  const resource = useResource(STORE_API.products({ keyword, page, size: PAGE_SIZE }));
  const rows = resource.data?.content || [];

  function search(e) {
    e.preventDefault();
    setPage(0);
    setKeyword(text.trim());
  }
  async function byBarcode(e) {
    e.preventDefault();
    if (!barcode.trim()) return;
    setFinding(true);
    setBarcodeError("");
    try {
      const p = await findProductByBarcode(barcode);
      navigate(`/store/products/${p.productId}`);
    } catch (err) {
      setBarcodeError(errorMessage(err));
    } finally {
      setFinding(false);
    }
  }

  return (
    <MainLayout>
      <PageHeader
        title="Products & stock"
        description="Active products with their shelf and stock in your store."
        actions={<RefreshButton resource={resource} />}
      />
      <div className="ops-toolbar">
        <form onSubmit={search} style={{ display: "flex", gap: 8, flex: 1 }}>
          <input aria-label="Search products" placeholder="Search product name or code…" value={text} onChange={(e) => setText(e.target.value)} style={{ flex: 1 }} />
          <button className="ops-button secondary">Search</button>
        </form>
        <form onSubmit={byBarcode} style={{ display: "flex", gap: 8 }}>
          <input aria-label="Barcode" placeholder="Barcode (scan or type)…" value={barcode} onChange={(e) => setBarcode(e.target.value)} inputMode="numeric" />
          <button className="ops-button secondary" disabled={finding}>{finding ? "Finding…" : "Find"}</button>
        </form>
      </div>
      <Notice error={barcodeError} />
      <ResourceState resource={resource} empty={!rows.length}>
        <Table head={["Product", "Barcode", "Category / brand", "Price", "Shelf", "On shelf", "Total in store", ""]}>
          {rows.map((p) => (
            <tr key={p.productId}>
              <td><strong>{p.name}</strong><small>{p.productCode}</small></td>
              <td className="mono">{p.barcode}</td>
              <td>{p.categoryName || "—"}<small>{p.brandName || ""}</small></td>
              <td>{money(p.sellingPrice)}</td>
              <td>{p.shelf ? <Link className="text-link" to={`/store/shelves/${p.shelf.id}`}>{p.shelf.name}</Link> : <span style={{ color: "#656b65" }}>Not on a shelf</span>}</td>
              <td>{number(p.shelfQuantity)}</td>
              <td style={{ color: p.totalQuantity === 0 ? "#c0392b" : undefined }}><strong>{p.totalQuantity === 0 ? "Out of stock" : number(p.totalQuantity)}</strong></td>
              <td><Link className="text-link" to={`/store/products/${p.productId}`}>Details →</Link></td>
            </tr>
          ))}
        </Table>
      </ResourceState>
      <Pager data={resource.data} onChange={setPage} />
    </MainLayout>
  );
}
