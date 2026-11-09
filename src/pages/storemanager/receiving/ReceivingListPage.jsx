import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import StatusBadge from "../../../components/ui/StatusBadge";
import useResource from "../../../hooks/useResource";
import { ResourceState, Table, number } from "../../../components/operations/WorkspaceUI";
import { STORE_API } from "../../../api/stores/manager/storeApi";
import { Metrics, ReceivingBadge, RefreshButton } from "../components/StoreUI";
import { dateTime } from "../storeFormat";

const arrived = (m) => ["ARRIVED", "DELIVERED"].includes(m.deliveryStatus);
const FILTERS = {
  ALL: ["All open deliveries", () => true],
  INSPECTED: ["Awaiting confirmation", (m) => m.receivingStatus === "INSPECTED"],
  PACKAGES_RECEIVED: ["Counting products", (m) => m.receivingStatus === "PACKAGES_RECEIVED"],
  TO_RECEIVE: ["To receive", (m) => arrived(m) && ["NOT_STARTED", "RECEIVING"].includes(m.receivingStatus)],
  IN_TRANSIT: ["In transit", (m) => !arrived(m)],
};
const PRIORITY = { INSPECTED: 0, PACKAGES_RECEIVED: 1, RECEIVING: 2, NOT_STARTED: 3 };

export default function ReceivingListPage() {
  const resource = useResource(STORE_API.manifests);
  const [params, setParams] = useSearchParams();
  const filter = FILTERS[params.get("status")] ? params.get("status") : "ALL";
  const [search, setSearch] = useState("");
  const rows = Array.isArray(resource.data) ? resource.data : [];
  const filtered = rows
    .filter(FILTERS[filter][1])
    .filter((m) => [m.manifestCode, m.shipmentCode, m.fromWarehouseName].join(" ").toLowerCase().includes(search.trim().toLowerCase()))
    .sort((a, b) => (PRIORITY[a.receivingStatus] ?? 9) - (PRIORITY[b.receivingStatus] ?? 9));
  const count = (key) => (resource.data ? number(rows.filter(FILTERS[key][1]).length) : "—");
  const setFilter = (key) => setParams(key === "ALL" ? {} : { status: key });

  return (
    <MainLayout>
      <PageHeader
        title="Incoming deliveries"
        description="Shipment manifests coming to your store. Staff receive and count on the mobile app; you confirm the stock-in here."
        actions={<RefreshButton resource={resource} />}
      />
      <Metrics
        items={["INSPECTED", "PACKAGES_RECEIVED", "TO_RECEIVE", "IN_TRANSIT"].map((key) => ({
          label: FILTERS[key][0],
          value: count(key),
          highlight: key === "INSPECTED" && rows.some(FILTERS.INSPECTED[1]),
        }))}
      />
      <div className="ops-toolbar">
        <input aria-label="Search manifests" placeholder="Search manifest, shipment or warehouse…" value={search} onChange={(e) => setSearch(e.target.value)} />
        <select aria-label="Filter by status" value={filter} onChange={(e) => setFilter(e.target.value)}>
          {Object.entries(FILTERS).map(([key, [label]]) => <option key={key} value={key}>{label}</option>)}
        </select>
        <span>{filtered.length} manifests</span>
      </div>
      <ResourceState resource={resource} empty={!filtered.length}>
        <Table head={["Manifest", "From", "Delivery", "Expected", "Packages (received / counted / total)", "Receiving", ""]}>
          {filtered.map((m) => (
            <tr key={m.manifestId}>
              <td><strong className="mono">{m.manifestCode}</strong><small>{m.shipmentCode}</small></td>
              <td>{m.fromWarehouseName || "—"}</td>
              <td><StatusBadge status={m.deliveryStatus} /></td>
              <td>{dateTime(m.expectedDeliveryAt)}</td>
              <td>{m.receivedPackages} / {m.checkedPackages} / {m.totalPackages}</td>
              <td><ReceivingBadge status={m.receivingStatus} /></td>
              <td>
                <Link className="text-link" to={`/store/receiving/${encodeURIComponent(m.manifestCode)}`}>
                  {m.receivingStatus === "INSPECTED" ? "Review & confirm →" : "Open →"}
                </Link>
              </td>
            </tr>
          ))}
        </Table>
      </ResourceState>
    </MainLayout>
  );
}
