import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import useResource from "../../../hooks/useResource";
import { Notice, ResourceState, number } from "../../../components/operations/WorkspaceUI";
import { STORE_API } from "../../../api/stores/manager/storeApi";
import { LowBadge, Metrics, MinShelfModal, RefreshButton, SectionTitle } from "../components/StoreUI";
import { dateTime, money } from "../storeFormat";

function Info({ label, children }) {
  return (
    <div style={{ display: "grid", gridTemplateColumns: "160px 1fr", gap: 12, padding: "8px 0", borderBottom: "1px solid #e1e3dc" }}>
      <span style={{ color: "#656b65" }}>{label}</span>
      <span>{children}</span>
    </div>
  );
}

export default function ProductDetailPage() {
  const { id } = useParams();
  const resource = useResource(STORE_API.product(id));
  const [editing, setEditing] = useState(false);
  const [saved, setSaved] = useState("");
  const p = resource.data;
  return (
    <MainLayout>
      <PageHeader
        title={p ? p.name : "Product"}
        description={p ? `${p.productCode} · ${p.barcode}` : "Product details"}
        actions={
          <>
            <Link className="ops-button secondary" to="/store/products">← All products</Link>
            {p && <Link className="ops-button secondary" to={`/store/stock-transactions?productId=${p.productId}`}>Stock history</Link>}
            {p && <button className="ops-button" onClick={() => { setSaved(""); setEditing(true); }}>Edit minimum</button>}
            <RefreshButton resource={resource} />
          </>
        }
      />
      {!p ? (
        <ResourceState resource={resource} empty={false} />
      ) : (
        <>
          <Notice error={resource.error}>{saved}</Notice>
          <Metrics
            items={[
              { label: "On shelf", value: number(p.shelfQuantity), highlight: p.lowStock },
              { label: "Backroom", value: number(p.backroomQuantity) },
              { label: "Total in store", value: number(p.totalQuantity), highlight: p.totalQuantity === 0 },
              { label: "Minimum on shelf", value: number(p.minShelfQuantity) },
            ]}
          />
          <SectionTitle>Details</SectionTitle>
          <section className="ops-panel" style={{ padding: 20, display: "flex", gap: 24, flexWrap: "wrap" }}>
            {p.imageUrl && <img src={p.imageUrl} alt={p.name} style={{ width: 160, height: 160, objectFit: "cover", borderRadius: 12, border: "1px solid #e1e3dc" }} />}
            <div style={{ flex: 1, minWidth: 280 }}>
              <Info label="Shelf">{p.shelf ? <Link className="text-link" to={`/store/shelves/${p.shelf.id}`}>{p.shelf.name}</Link> : "Not on a shelf"}</Info>
              <Info label="Shelf status">{p.shelf ? <LowBadge low={p.lowStock} /> : "—"}</Info>
              <Info label="Selling price">{money(p.sellingPrice)} / {p.baseUnit || "unit"}</Info>
              <Info label="Category">{p.categoryName || "—"}</Info>
              <Info label="Brand">{p.brandName ? `${p.brandName}${p.brandCountry ? " (" + p.brandCountry + ")" : ""}` : "—"}</Info>
              <Info label="Stock updated">{dateTime(p.stockUpdatedAt)}</Info>
            </div>
          </section>
        </>
      )}
      {editing && p && (
        <MinShelfModal
          product={p}
          onClose={() => setEditing(false)}
          onSaved={(r) => { setSaved(`Minimum is now ${r.minShelfQuantity}.`); resource.reload(); }}
        />
      )}
    </MainLayout>
  );
}
