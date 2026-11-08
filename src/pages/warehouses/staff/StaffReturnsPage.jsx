import { useState } from "react";
import { useParams } from "react-router-dom";
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
const BASE = "/warehouse/staff/store-returns";
export default function StaffReturnsPage() {
  const params = useParams();
  const [id, setId] = useState(params.returnId || null),
    [image, setImage] = useState(null),
    [selected, setSelected] = useState("");
  const list = useResource(BASE),
    detail = useResource(id ? `${BASE}/${id}` : null),
    row = detail.data;
  const action = useAction(async () => {
    await Promise.all([list.reload(), detail.reload()]);
  });
  async function scan(e) {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(e.currentTarget));
    await action.run(() => api.post(`${BASE}/${id}/scan-package`, f));
  }
  async function inspect(e) {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(e.currentTarget));
    await action.run(() =>
      api.put(`${BASE}/${id}/items`, {
        ...f,
        receivedQuantity: Number(f.receivedQuantity),
      }),
    );
  }
  async function upload(e) {
    e.preventDefault();
    const body = new FormData();
    body.append("image", image);
    await action.run(() =>
      api.post(`${BASE}/${id}/items/${selected}/evidence`, body),
    );
  }
  return (
    <MainLayout>
      <PageHeader
        title={row?.returnCode || "Receive store returns"}
        description="Scan returned packages, inspect products and save evidence photos."
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
        <ResourceState resource={list} empty={!list.data?.length}>
          <Table
            head={[
              "Return code",
              "Stores",
              "Expected / Received",
              "Status",
              "",
            ]}
          >
            {(list.data || []).map((r) => (
              <tr key={r.returnId}>
                <td>
                  <strong>{r.returnCode}</strong>
                </td>
                <td>{r.storeName}</td>
                <td>
                  {number(r.totalExpectedQuantity)} /{" "}
                  {number(r.totalReceivedQuantity)}
                </td>
                <td>
                  <Status value={r.status} />
                </td>
                <td>
                  <button
                    className="text-link"
                    onClick={() => {
                      setId(r.returnId);
                      action.clear();
                    }}
                  >
                    Inspect ↗
                  </button>
                </td>
              </tr>
            ))}
          </Table>
        </ResourceState>
      ) : (
        <ResourceState resource={detail}>
          {row && (
            <>
              <div className="detail-ribbon">
                <div>
                  <span>Stores</span>
                  <strong>{row.storeName}</strong>
                </div>
                <div>
                  <span>Return reason</span>
                  <strong>{row.reason || "—"}</strong>
                </div>
                <Status value={row.status} />
              </div>
              {row.status === "SHIPPED" && (
                <button
                  className="ops-button mb-5"
                  disabled={action.busy}
                  onClick={() =>
                    action.run(() => api.patch(`${BASE}/${id}/start`))
                  }
                >
                  Start return inspection
                </button>
              )}
              <Table
                head={[
                  "Product",
                  "Expected",
                  "Received",
                  "Condition",
                  "Evidence",
                ]}
              >
                {(row.items || []).map((i) => (
                  <tr key={i.itemId}>
                    <td>
                      <strong>{i.productName}</strong>
                      <small>
                        {i.productCode} · {i.barcode}
                      </small>
                    </td>
                    <td>{number(i.expectedQuantity)}</td>
                    <td>{number(i.receivedQuantity)}</td>
                    <td>
                      {i.conditionStatus || "—"}
                      <small>{i.note}</small>
                    </td>
                    <td>
                      {i.evidenceImageUrl ? (
                        <a
                          className="text-link"
                          href={i.evidenceImageUrl}
                          target="_blank"
                          rel="noreferrer"
                        >
                          View photo ↗
                        </a>
                      ) : (
                        "—"
                      )}
                    </td>
                  </tr>
                ))}
              </Table>
              {row.status === "INSPECTING" && (
                <>
                  <div className="packing-forms">
                    <form className="ops-panel ops-form" onSubmit={scan}>
                      <h2>Scan returned package</h2>
                      <Field label="Package code" name="packageBarcode" required />
                      <button className="ops-button" disabled={action.busy}>
                        Record package
                      </button>
                    </form>
                    <form className="ops-panel ops-form" onSubmit={inspect}>
                      <h2>Inspect product</h2>
                      <Field
                        label="Product barcode"
                        name="productBarcode"
                        required
                      />
                      <Field
                        label="Received quantity"
                        name="receivedQuantity"
                        type="number"
                        min="0"
                        step="1"
                        required
                      />
                      <Field label="Condition">
                        <select name="conditionStatus">
                          {["NORMAL", "DAMAGED", "EXPIRED", "QUARANTINE"].map(
                            (s) => (
                              <option key={s}>{s}</option>
                            ),
                          )}
                        </select>
                      </Field>
                      <Field label="Notes" name="note" />
                      <button className="ops-button" disabled={action.busy}>
                        Save inspection
                      </button>
                    </form>
                  </div>
                  <form
                    className="ops-panel scan-form ops-form"
                    onSubmit={upload}
                  >
                    <h2>Evidence photo</h2>
                    <Field label="Product line">
                      <select
                        value={selected}
                        onChange={(e) => setSelected(e.target.value)}
                        required
                      >
                        <option value="">Select a product</option>
                        {(row.items || []).map((i) => (
                          <option value={i.itemId} key={i.itemId}>
                            {i.productCode} · {i.productName}
                          </option>
                        ))}
                      </select>
                    </Field>
                    <Field label="Photo">
                      <input
                        type="file"
                        accept="image/*"
                        required
                        onChange={(e) => setImage(e.target.files?.[0] || null)}
                      />
                    </Field>
                    <button
                      className="ops-button secondary"
                      disabled={action.busy || !image}
                    >
                      Upload evidence
                    </button>
                  </form>
                  <div className="ops-toolbar">
                    <button
                      className="ops-button"
                      disabled={action.busy}
                      onClick={() => {
                        if (window.confirm("Submit this return inspection?"))
                          action.run(() => api.patch(`${BASE}/${id}/submit`));
                      }}
                    >
                      Submit results →
                    </button>
                  </div>
                </>
              )}
            </>
          )}
        </ResourceState>
      )}
    </MainLayout>
  );
}
