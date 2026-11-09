import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import useResource from "../../../hooks/useResource";
import { Notice, ResourceState, Table, number } from "../../../components/operations/WorkspaceUI";
import { STORE_API } from "../../../api/stores/manager/storeApi";
import { LowBadge, Metrics, MinShelfModal, RefreshButton } from "../components/StoreUI";

export default function ShelfDetailPage() {
  const { id } = useParams();
  const resource = useResource(STORE_API.shelf(id));
  const [editing, setEditing] = useState(null);
  const [saved, setSaved] = useState("");
  const shelf = resource.data;
  const products = shelf?.products || [];
  return (
    <MainLayout>
      <PageHeader
        title={shelf ? shelf.name : "Shelf"}
        description={shelf ? `${shelf.code}${shelf.description ? " · " + shelf.description : ""}` : "Products on this shelf"}
        actions={
          <>
            <Link className="ops-button secondary" to="/store/shelves">← All shelves</Link>
            <RefreshButton resource={resource} />
          </>
        }
      />
      {shelf && (
        <Metrics
          items={[
            { label: "Products", value: number(products.length) },
            { label: "Units on shelf", value: number(products.reduce((t, p) => t + (p.shelfQuantity || 0), 0)) },
            { label: "Low on shelf", value: number(products.filter((p) => p.lowStock).length), highlight: products.some((p) => p.lowStock) },
          ]}
        />
      )}
      <Notice>{saved}</Notice>
      <ResourceState resource={resource} empty={!products.length}>
        <Table head={["Product", "Barcode", "On shelf", "Backroom", "Minimum", "Shelf", ""]}>
          {products.map((p) => (
            <tr key={p.productId}>
              <td><Link className="text-link" to={`/store/products/${p.productId}`}><strong>{p.name}</strong></Link><small>{p.productCode}</small></td>
              <td className="mono">{p.barcode}</td>
              <td><strong>{number(p.shelfQuantity)}</strong></td>
              <td>{number(p.backroomQuantity)}</td>
              <td>{number(p.minShelfQuantity)}</td>
              <td><LowBadge low={p.lowStock} /></td>
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
