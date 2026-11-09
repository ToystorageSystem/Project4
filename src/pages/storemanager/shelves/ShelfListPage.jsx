import { useState } from "react";
import { Link } from "react-router-dom";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import useResource from "../../../hooks/useResource";
import { ResourceState, Table, number } from "../../../components/operations/WorkspaceUI";
import { STORE_API } from "../../../api/stores/manager/storeApi";
import { Metrics, RefreshButton } from "../components/StoreUI";

export default function ShelfListPage() {
  const resource = useResource(STORE_API.shelves);
  const [search, setSearch] = useState("");
  const rows = Array.isArray(resource.data) ? resource.data : [];
  const filtered = rows.filter((s) => [s.name, s.code, s.description].join(" ").toLowerCase().includes(search.trim().toLowerCase()));
  const shown = (v) => (resource.data ? number(v) : "—");
  return (
    <MainLayout>
      <PageHeader
        title="Shelves"
        description="Active shelves in your store and how much stock is on each one."
        actions={
          <>
            <Link className="ops-button secondary" to="/store/low-shelf">Restock shelves →</Link>
            <RefreshButton resource={resource} />
          </>
        }
      />
      <Metrics
        items={[
          { label: "Shelves", value: shown(rows.length) },
          { label: "Empty shelves", value: shown(rows.filter((s) => !s.productCount).length) },
          { label: "Units on shelves", value: shown(rows.reduce((t, s) => t + (s.shelfQuantity || 0), 0)) },
        ]}
      />
      <div className="ops-toolbar">
        <input aria-label="Search shelves" placeholder="Search shelf name or code…" value={search} onChange={(e) => setSearch(e.target.value)} />
        <span>{filtered.length} shelves</span>
      </div>
      <ResourceState resource={resource} empty={!filtered.length}>
        <Table head={["Shelf", "Description", "Products", "Units on shelf", ""]}>
          {filtered.map((s) => (
            <tr key={s.id}>
              <td><strong>{s.name}</strong><small className="mono">{s.code}</small></td>
              <td>{s.description || "—"}</td>
              <td>{number(s.productCount)}</td>
              <td>{number(s.shelfQuantity)}</td>
              <td><Link className="text-link" to={`/store/shelves/${s.id}`}>Open →</Link></td>
            </tr>
          ))}
        </Table>
      </ResourceState>
    </MainLayout>
  );
}
