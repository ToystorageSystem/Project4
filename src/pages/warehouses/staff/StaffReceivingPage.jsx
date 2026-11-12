import { useState } from "react";
import usePager from "../../../hooks/usePager";
import Pager from "../../../components/operations/Pager";
import api from "../../../api/api";
import useResource from "../../../hooks/useResource";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import {
  Notice,
  ResourceState,
  Status,
  Table,
  useAction,
  number,
} from "../../../components/operations/WorkspaceUI";
import { confirmDialog } from "../../../utils/confirmDialog";
const BASE = "/warehouse/goods-receipts";
export default function StaffReceivingPage() {
  const list = useResource(BASE + "/receiving"),
    [id, setId] = useState(null),
    [drafts, setDrafts] = useState({}),
    [search, setSearch] = useState("");
  const detail = useResource(id ? `${BASE}/${id}/receiving-detail` : null),
    row = detail.data;
  const action = useAction(async () => {
    await Promise.all([list.reload(), detail.reload()]);
  });
  const rows = (list.data || []).filter((r) =>
    [r.receiptCode, r.supplierName, r.purchaseOrderCode]
      .join(" ")
      .toLowerCase()
      .includes(search.toLowerCase()),
  );
  function edit(productId, key, value) {
    setDrafts((d) => ({
      ...d,
      [productId]: { ...d[productId], [key]: value },
    }));
  }
  const items = row?.items || [],
    editable = row?.status === "RECEIVING";
  function value(i, key) {
    return (
      drafts[i.productId]?.[key] ??
      i[key] ??
      (key === "damagedQuantity" ? 0 : "")
    );
  }
  async function save(e) {
    e.preventDefault();
    const payload = items.map((i) => ({
      productId: i.productId,
      actualQuantity: Number(value(i, "actualQuantity")),
      damagedQuantity: Number(value(i, "damagedQuantity")),
      packageCode: value(i, "packageCode"),
      notes: value(i, "notes"),
    }));
    if (payload.some((i) => i.damagedQuantity > i.actualQuantity)) return;
    if (
      await action.run(() =>
        api.put(`${BASE}/${id}/inspections`, { items: payload }),
      )
    )
      setDrafts({});
  }
  const pager = usePager(rows, 20, [search]);
  return (
    <MainLayout>
      <PageHeader
        title={row?.receiptCode || "Receive & inspect"}
        description="Compare supplier goods against the receipt. Accepted quantities and discrepancies are calculated by the server."
        actions={
          <>
            {id && (
              <button
                className="ops-button secondary"
                onClick={() => {
                  setId(null);
                  setDrafts({});
                  action.clear();
                }}
              >
                ← Back to list
              </button>
            )}
            <button
              className="ops-button secondary"
              onClick={() => {
                list.reload();
                detail.reload();
              }}
              disabled={action.busy}
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
              value={search}
              aria-label="Search receipts"
              placeholder="Search receipt code or supplier…"
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <ResourceState resource={list} empty={!rows.length}>
            <Table
              head={[
                "Receipt",
                "Purchase order",
                "Supplier",
                "Lines / Quantity",
                "Status",
                "",
              ]}
            >
              {pager.rows.map((r) => (
                <tr key={r.receiptId}>
                  <td>
                    <strong>{r.receiptCode}</strong>
                  </td>
                  <td>{r.purchaseOrderCode}</td>
                  <td>{r.supplierName}</td>
                  <td>
                    {r.totalItems} / {r.totalExpectedQuantity}
                  </td>
                  <td>
                    <Status value={r.status} />
                  </td>
                  <td>
                    <button
                      className="text-link"
                      onClick={() => {
                        setId(r.receiptId);
                        setDrafts({});
                        action.clear();
                      }}
                    >
                      Inspect ↗
                    </button>
                  </td>
                </tr>
              ))}
            </Table>
            <Pager pager={pager} label="receipts" />
          </ResourceState>
        </>
      ) : (
        <ResourceState resource={detail}>
          {row && (
            <>
              <div className="detail-ribbon">
                <div>
                  <span>Supplier</span>
                  <strong>{row.supplierName}</strong>
                </div>
                <div>
                  <span>Purchase order</span>
                  <strong>{row.purchaseOrderCode}</strong>
                </div>
                <div>
                  <span>Staff</span>
                  <strong>{row.receivedByName || "Unclaimed"}</strong>
                </div>
                <Status value={row.status} />
              </div>
              {row.status === "CONFIRMED" && (
                <button
                  className="ops-button mb-5"
                  disabled={action.busy}
                  onClick={() =>
                    action.run(() => api.patch(`${BASE}/${id}/start-receiving`))
                  }
                >
                  Claim & start inspection →
                </button>
              )}
              <form onSubmit={save}>
                <Table
                  head={[
                    "Product / Barcode",
                    "Expected",
                    "Received",
                    "Damaged",
                    "Package code",
                    "Notes",
                    "Result",
                  ]}
                >
                  {items.map((i) => (
                    <tr key={i.productId}>
                      <td>
                        <strong>{i.productName}</strong>
                        <small>
                          {i.productCode} · {i.barcode}
                        </small>
                      </td>
                      <td>{number(i.expectedQuantity)}</td>
                      <td>
                        {editable ? (
                          <input
                            className="quantity-input"
                            type="number"
                            min="0"
                            step="1"
                            required
                            aria-label={`Received ${i.productCode}`}
                            value={value(i, "actualQuantity")}
                            onChange={(e) =>
                              edit(
                                i.productId,
                                "actualQuantity",
                                e.target.value,
                              )
                            }
                          />
                        ) : (
                          number(i.actualQuantity)
                        )}
                      </td>
                      <td>
                        {editable ? (
                          <input
                            className="quantity-input"
                            type="number"
                            min="0"
                            max={value(i, "actualQuantity")}
                            step="1"
                            required
                            aria-label={`Damaged ${i.productCode}`}
                            value={value(i, "damagedQuantity")}
                            onChange={(e) =>
                              edit(
                                i.productId,
                                "damagedQuantity",
                                e.target.value,
                              )
                            }
                          />
                        ) : (
                          number(i.damagedQuantity)
                        )}
                      </td>
                      <td>
                        {editable ? (
                          <input
                            className="quantity-input"
                            aria-label={`Package ${i.productCode}`}
                            value={value(i, "packageCode")}
                            onChange={(e) =>
                              edit(i.productId, "packageCode", e.target.value)
                            }
                          />
                        ) : (
                          i.packageCode
                        )}
                      </td>
                      <td>
                        {editable ? (
                          <input
                            className="quantity-input"
                            aria-label={`Notes ${i.productCode}`}
                            value={value(i, "notes")}
                            onChange={(e) =>
                              edit(i.productId, "notes", e.target.value)
                            }
                          />
                        ) : (
                          i.notes
                        )}
                      </td>
                      <td>
                        {i.inspected ? "Inspected" : "Not inspected"}
                        <small>
                          Accepted: {number(i.acceptedQuantity)} / Difference:{" "}
                          {number(i.differenceQuantity)}
                        </small>
                      </td>
                    </tr>
                  ))}
                </Table>
                {editable && (
                  <div className="ops-toolbar">
                    <button
                      className="ops-button"
                      disabled={action.busy || !items.length}
                    >
                      Save inspection results
                    </button>
                    <button
                      type="button"
                      className="ops-button secondary"
                      disabled={
                        action.busy ||
                        Object.keys(drafts).length > 0 ||
                        items.some((i) => !i.inspected)
                      }
                      onClick={async () => {
                        if (
                          await confirmDialog(
                            "Finish inspection? Passed goods are added to stock right away, damaged goods go to the damaged area, and the putaway task is assigned to you.",
                          )
                        )
                          action.run(() =>
                            api.patch(`${BASE}/${id}/finish-inspection`),
                          );
                      }}
                    >
                      Finish inspection →
                    </button>
                  </div>
                )}
              </form>
            </>
          )}
        </ResourceState>
      )}
    </MainLayout>
  );
}
