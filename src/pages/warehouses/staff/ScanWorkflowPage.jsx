import { useState } from "react";
import usePager from "../../../hooks/usePager";
import Pager from "../../../components/operations/Pager";
import api from "../../../api/api";
import useResource from "../../../hooks/useResource";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import {
  Field,
  Notice,
  ResourceState,
  Status,
  Table,
  useAction,
  number,
} from "../../../components/operations/WorkspaceUI";
const configs = {
  picking: {
    title: "Picking",
    description:
      "Claim a task, scan products at their location and complete picking.",
    list: "/warehouse/picking/transfers",
    base: "/warehouse/picking/transfers",
    id: "transferId",
    code: "transferCode",
    start: "PENDING_SOURCE_CONFIRMATION",
    active: ["PICKING"],
    finish: "complete",
  },
  putaway: {
    title: "Putaway",
    description:
      "Scan products and destination locations to record putaway quantities.",
    list: "/warehouse/putaway/my-tasks",
    base: "/warehouse/putaway",
    id: "taskId",
    code: "taskCode",
    start: "AVAILABLE",
    active: ["IN_PROGRESS"],
    finish: "complete",
  },
  counts: {
    title: "Stock count tasks",
    description:
      "Count stock, scan locations and barcodes, then submit the results for review.",
    list: "/warehouse/staff/stock-counts",
    base: "/warehouse/staff/stock-counts",
    id: "stockCountId",
    code: "countCode",
    start: "PLANNED",
    active: ["COUNTING", "RECOUNTING"],
    finish: "submit",
  },
};
export default function ScanWorkflowPage({ kind }) {
  const c = configs[kind],
    list = useResource(c.list);
  const [id, setId] = useState(null),
    [search, setSearch] = useState("");
  const detail = useResource(id ? `${c.base}/${id}` : null);
  const [line, setLine] = useState(""),
    [barcode, setBarcode] = useState(""),
    [location, setLocation] = useState(""),
    [quantity, setQuantity] = useState("");
  const action = useAction(async () => {
    await Promise.all([list.reload(), detail.reload()]);
  });
  const row = detail.data,
    rows = (list.data || []).filter((r) =>
      [r[c.code], r.status, r.toWarehouseName, r.assignedToName]
        .join(" ")
        .toLowerCase()
        .includes(search.toLowerCase()),
    );
  const active = row && c.active.includes(row.status),
    items = row?.items || [];
  async function scan(e) {
    e.preventDefault();
    let result;
    if (kind === "counts")
      result = await action.run(() =>
        api.put(`${c.base}/${id}/items`, {
          locationCode: location.trim(),
          productBarcode: barcode.trim(),
          quantity: Number(quantity),
        }),
      );
    else
      result = await action.run(() =>
        api.patch(`${c.base}/${id}/items/${line}`, {
          locationCode: location.trim(),
          productBarcode: barcode.trim(),
          quantity: Number(quantity),
        }),
      );
    if (result) {
      setBarcode("");
      setQuantity("");
    }
  }
  const pager = usePager(rows, 20, [search, kind]);
  return (
    <MainLayout>
      <PageHeader
        title={id ? row?.[c.code] || c.title : c.title}
        description={c.description}
        actions={
          <>
            {id && (
              <button
                className="ops-button secondary"
                onClick={() => {
                  setId(null);
                  action.clear();
                }}
              >
                ← Back to list
              </button>
            )}
            <button
              className="ops-button secondary"
              disabled={list.loading || detail.loading}
              onClick={() => {
                list.reload();
                detail.reload();
              }}
            >
              Refresh ↻
            </button>
          </>
        }
      />
      <Notice error={action.error}>{action.success}</Notice>
      {!id ? (
        <>
          <div className="ops-toolbar">
            <input
              aria-label="Search tasks"
              placeholder="Search task code or status…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <ResourceState resource={list} empty={!rows.length}>
            <Table
              head={[
                "Task code",
                "Warehouse / Destination",
                "Performed by",
                "Status",
                "",
              ]}
            >
              {pager.rows.map((r) => (
                <tr key={r[c.id]}>
                  <td>
                    <strong>{r[c.code]}</strong>
                  </td>
                  <td>
                    {r.toWarehouseName ||
                      r.warehouseName ||
                      r.receiptCode ||
                      "—"}
                  </td>
                  <td>{r.assignedToName || "Unassigned"}</td>
                  <td>
                    <Status value={r.status} />
                  </td>
                  <td>
                    <button
                      className="text-link"
                      onClick={() => {
                        setId(r[c.id]);
                        setLine("");
                        setBarcode("");
                        setLocation("");
                        setQuantity("");
                        action.clear();
                      }}
                    >
                      Open task ↗
                    </button>
                  </td>
                </tr>
              ))}
            </Table>
            <Pager pager={pager} label="tasks" />
          </ResourceState>
        </>
      ) : (
        <ResourceState resource={detail}>
          {row && (
            <>
              <div className="detail-ribbon">
                <div>
                  <span>Status</span>
                  <Status value={row.status} />
                </div>
                <div>
                  <span>Staff</span>
                  <strong>{row.assignedToName || "—"}</strong>
                </div>
                <div>
                  <span>Item lines</span>
                  <strong>{items.length}</strong>
                </div>
              </div>
              {(row.status === c.start ||
                (kind === "putaway" && row.status === "PENDING")) && (
                <button
                  className="ops-button mb-5"
                  disabled={action.busy}
                  onClick={() =>
                    action.run(() => api.patch(`${c.base}/${id}/start`))
                  }
                >
                  Claim & start task →
                </button>
              )}
              <div className={active ? "scan-workspace" : ""}>
                <Table
                  head={[
                    "Product",
                    "Location",
                    "Expected / System",
                    "Processed / First count",
                    "Remaining / Second count",
                  ]}
                >
                  {items.map((i) => (
                    <tr key={i.itemId}>
                      <td>
                        <strong>{i.productName}</strong>
                        <small>
                          {i.productCode} · {i.barcode}
                        </small>
                      </td>
                      <td>
                        {i.locationCode ||
                          i.toLocationCode ||
                          i.fromLocationCode ||
                          "—"}
                      </td>
                      <td>
                        {number(
                          i.approvedQuantity ??
                            i.expectedQuantity ??
                            i.systemQuantity,
                        )}
                      </td>
                      <td>
                        {number(
                          i.pickedQuantity ??
                            i.putawayQuantity ??
                            i.firstCountQuantity,
                        )}
                      </td>
                      <td>
                        {number(i.remainingQuantity ?? i.secondCountQuantity)}
                      </td>
                    </tr>
                  ))}
                </Table>
                {active && (
                  <form
                    className="ops-panel scan-form ops-form"
                    onSubmit={scan}
                  >
                    <span className="eyebrow">RECORD ACTUAL QUANTITIES</span>
                    <h2>
                      {kind === "counts" ? "Enter count" : "Scan & record"}
                    </h2>
                    {kind !== "counts" && (
                      <Field label="Item line">
                        <select
                          value={line}
                          onChange={(e) => setLine(e.target.value)}
                          required
                        >
                          <option value="">Select a product</option>
                          {items.map((i) => (
                            <option key={i.itemId} value={i.itemId}>
                              {i.productCode} · {i.productName}
                            </option>
                          ))}
                        </select>
                      </Field>
                    )}
                    <Field
                      label="Scan product barcode"
                      value={barcode}
                      onChange={(e) => setBarcode(e.target.value)}
                      autoComplete="off"
                      required
                    />
                    <Field
                      label={
                        kind === "putaway"
                          ? "Scan destination location"
                          : "Scan location code"
                      }
                      value={location}
                      onChange={(e) => setLocation(e.target.value)}
                      autoComplete="off"
                      required
                    />
                    <Field
                      label="Quantity"
                      value={quantity}
                      onChange={(e) => setQuantity(e.target.value)}
                      type="number"
                      min={kind === "counts" ? 0 : 1}
                      step="1"
                      required
                    />
                    <button className="ops-button" disabled={action.busy}>
                      Record
                    </button>
                    <></>
                  </form>
                )}
              </div>
              {active && (
                <div className="ops-toolbar">
                  <button
                    className="ops-button"
                    disabled={action.busy}
                    onClick={() => {
                      if (
                        window.confirm(
                          "Complete this task and submit the results?",
                        )
                      )
                        action.run(() =>
                          api.patch(`${c.base}/${id}/${c.finish}`),
                        );
                    }}
                  >
                    {kind === "counts"
                      ? "Submit for manager review →"
                      : "Complete task →"}
                  </button>
                </div>
              )}
            </>
          )}
        </ResourceState>
      )}
    </MainLayout>
  );
}
