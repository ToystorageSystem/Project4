import { useState } from "react";
import { Link } from "react-router-dom";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import useResource from "../../../hooks/useResource";
import { Notice, ResourceState, Table, number } from "../../../components/operations/WorkspaceUI";
import { STORE_API } from "../../../api/stores/manager/storeApi";
import { Metrics, MinShelfModal, RefreshButton } from "../components/StoreUI";

// Suggested move: bring the shelf above its minimum, limited by what is in the backroom.
const suggest = (p) => Math.min(Math.max(p.minShelfQuantity - p.shelfQuantity + 1, 1), p.backroomQuantity);

export default function LowShelfPage() {
  const resource = useResource(STORE_API.lowShelf);
  const [search, setSearch] = useState("");
  const [editing, setEditing] = useState(null);
  const [saved, setSaved] = useState("");
  const rows = (Array.isArray(resource.data) ? resource.data : [])
    .filter((p) => [p.name, p.productCode, p.barcode, p.shelf?.name].join(" ").toLowerCase().includes(search.trim().toLowerCase()))
    .sort((a, b) => (a.shelf?.name || "").localeCompare(b.shelf?.name || "") || a.name.localeCompare(b.name));
  return (
    <MainLayout>
      <PageHeader
        title="Restock shelves"
        description="Products at or below their shelf minimum that still have stock in the backroom. Staff move the stock with the mobile app."
        actions={<RefreshButton resource={resource} />}
      />
      <Metrics
        items={[
          { label: "Products to restock", value: resource.data ? number(resource.data.length) : "—", highlight: resource.data?.length > 0 },
          { label: "Units to move (suggested)", value: resource.data ? number(resource.data.reduce((t, p) => t + suggest(p), 0)) : "—" },
        ]}
      />
      <div className="ops-toolbar">
        <input aria-label="Search" placeholder="Search product, barcode or shelf…" value={search} onChange={(e) => setSearch(e.target.value)} />
        <span>{rows.length} products</span>
      </div>
      <Notice>{saved}</Notice>
      <ResourceState resource={resource} empty={!rows.length}>
        <Table head={["Shelf", "Product", "Barcode", "On shelf", "Minimum", "Backroom", "Suggested move", ""]}>
          {rows.map((p) => (
            <tr key={p.productId}>
              <td>{p.shelf ? <Link className="text-link" to={`/store/shelves/${p.shelf.id}`}>{p.shelf.name}</Link> : "—"}</td>
              <td><Link className="text-link" to={`/store/products/${p.productId}`}><strong>{p.name}</strong></Link><small>{p.productCode}</small></td>
              <td className="mono">{p.barcode}</td>
              <td style={{ color: "#c0392b" }}><strong>{number(p.shelfQuantity)}</strong></td>
              <td>{number(p.minShelfQuantity)}</td>
              <td>{number(p.backroomQuantity)}</td>
              <td><strong>{number(suggest(p))}</strong></td>
              <td><button className="text-link" onClick={() => { setSaved(""); setEditing(p); }}>Edit minimum</button></td>
            </tr>
          ))}
        </Table>
      </ResourceState>
      {editing && (
        <MinShelfModal
          product={editing}
          onClose={() => setEditing(null)}
          onSaved={(r) => { setSaved(`Minimum for ${r.name} is now ${r.minShelfQuantity}.`); resource.reload(); }}
        />
      )}
    </MainLayout>
  );
}
