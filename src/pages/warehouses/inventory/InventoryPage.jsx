import { useState } from "react";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import useResource from "../../../hooks/useResource";
import {
  ResourceState,
  Table,
  number,
} from "../../../components/operations/WorkspaceUI";
export default function InventoryPage() {
  const resource = useResource("/warehouse/inventory/balances");
  const [search, setSearch] = useState(""),
    [filter, setFilter] = useState("all"),
    [selected, setSelected] = useState(null);
  const rows = Array.isArray(resource.data) ? resource.data : [];
  const filtered = rows.filter(
    (row) =>
      [row.productCode, row.productName, row.barcode]
        .join(" ")
        .toLowerCase()
        .includes(search.toLowerCase()) &&
      (filter !== "zero" || row.available === 0),
  );
  return (
    <MainLayout>
      <PageHeader
        title="Inventory"
        description="On-hand, reserved and available quantities in your warehouse."
        actions={
          <button
            className="ops-button secondary"
            disabled={resource.loading}
            onClick={resource.reload}
          >
            Refresh ↻
          </button>
        }
      />
      <div className="metric-strip">
        {[
          ["Products", rows.length],
          ["On hand", rows.reduce((s, r) => s + r.onHand, 0)],
          ["Reserved", rows.reduce((s, r) => s + r.reserved, 0)],
          ["Available", rows.reduce((s, r) => s + r.available, 0)],
        ].map(([label, value]) => (
          <div key={label}>
            <span>{label}</span>
            <strong>
              {resource.loading || resource.error ? "—" : number(value)}
            </strong>
          </div>
        ))}
      </div>
      <div className="ops-toolbar">
        <input
          aria-label="Search products"
          placeholder="Search product name, SKU or barcode…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select
          aria-label="Filter inventory"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        >
          <option value="all">All products</option>
          <option value="zero">No available stock</option>
        </select>
        <span>{filtered.length} products</span>
      </div>
      <ResourceState resource={resource} empty={!filtered.length}>
        <Table
          head={[
            "Product / SKU",
            "Barcode",
            "Unit",
            "On hand",
            "Reserved",
            "Available",
            "Location",
          ]}
        >
          {filtered.map((row) => (
            <tr key={row.productId}>
              <td>
                <strong>{row.productName}</strong>
                <small>{row.productCode}</small>
              </td>
              <td className="mono">{row.barcode}</td>
              <td>{row.unit}</td>
              <td>{number(row.onHand)}</td>
              <td>{number(row.reserved)}</td>
              <td className={row.available === 0 ? "text-amber-700" : ""}>
                <strong>{number(row.available)}</strong>
              </td>
              <td>
                <button
                  className="text-link"
                  onClick={() =>
                    setSelected(
                      selected === row.productId ? null : row.productId,
                    )
                  }
                >
                  {row.locations.length} locations{" "}
                  {selected === row.productId ? "−" : "+"}
                </button>
                {selected === row.productId && (
                  <div className="location-breakdown">
                    {row.locations.map((loc) => (
                      <div key={loc.locationId}>
                        <strong>{loc.locationCode}</strong>
                        <small>
                          {loc.locationName} · {loc.locationType} · {loc.zone} /{" "}
                          {loc.shelf}
                        </small>
                        <span>
                          {number(loc.onHand)} on hand / {number(loc.reserved)}{" "}
                          reserved / {number(loc.available)} available
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </td>
            </tr>
          ))}
        </Table>
      </ResourceState>
    </MainLayout>
  );
}
