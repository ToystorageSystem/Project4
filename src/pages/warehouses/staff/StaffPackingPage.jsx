import { useState } from "react";
import Barcode from "react-barcode";
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
const BASE = "/warehouse/staff/packing",
  SUB = "/warehouse/staff/packing-submissions/transfers";
export default function StaffPackingPage() {
  const list = useResource(SUB),
    [id, setId] = useState(null),
    [packageId, setPackageId] = useState(null);
  const detail = useResource(id ? `${SUB}/${id}` : null),
    pack = useResource(packageId ? `${BASE}/packages/${packageId}` : null);
  const action = useAction(async () => {
    await Promise.all([list.reload(), detail.reload(), pack.reload()]);
  });
  const [barcode, setBarcode] = useState(""),
    [quantity, setQuantity] = useState(1),
    [seal, setSeal] = useState("");
  const row = detail.data,
    packages = row?.packages || [],
    current = pack.data;
  async function create() {
    const result = await action.run(
      async () => (await api.post(`${BASE}/transfers/${id}/packages`)).data,
    );
    if (result?.packageId) setPackageId(result.packageId);
  }
  async function scan(e) {
    e.preventDefault();
    if (
      await action.run(() =>
        api.post(`${BASE}/packages/${packageId}/scan`, {
          productBarcode: barcode.trim(),
          quantity: Number(quantity),
        }),
      )
    ) {
      setBarcode("");
      setQuantity(1);
    }
  }
  return (
    <MainLayout>
      <PageHeader
        title={row?.transferCode || "Packing & sealing"}
        description="Create packages, record their contents and submit them for manager review."
        actions={
          <>
            {id && (
              <button
                className="ops-button secondary"
                onClick={() => {
                  setId(null);
                  setPackageId(null);
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
                pack.reload();
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
              "Transfer",
              "Destination",
              "Packed / Picked",
              "Packages",
              "Status",
              "",
            ]}
          >
            {(list.data || []).map((r) => (
              <tr key={r.transferId}>
                <td>
                  <strong>{r.transferCode}</strong>
                </td>
                <td>{r.toWarehouseName}</td>
                <td>
                  {number(r.totalPackedQuantity)} /{" "}
                  {number(r.totalPickedQuantity)}
                </td>
                <td>{number(r.totalPackages)}</td>
                <td>
                  <Status value={r.status} />
                </td>
                <td>
                  <button
                    className="text-link"
                    onClick={() => {
                      setId(r.transferId);
                      action.clear();
                    }}
                  >
                    Pack ↗
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
                  <span>Status</span>
                  <Status value={row.status} />
                </div>
                <div>
                  <span>Packed / Picked</span>
                  <strong>
                    {row.totalPackedQuantity} / {row.totalPickedQuantity}
                  </strong>
                </div>
                <div>
                  <span>Destination</span>
                  <strong>{row.toWarehouseName}</strong>
                </div>
              </div>
              <div className="ops-toolbar">
                {row.status === "PACKING" && (
                  <>
                    <button
                      className="ops-button"
                      disabled={action.busy}
                      onClick={create}
                    >
                      + Create package
                    </button>
                    <button
                      className="ops-button secondary"
                      disabled={
                        action.busy ||
                        !packages.length ||
                        row.totalPackedQuantity !== row.totalPickedQuantity ||
                        packages.some((p) => p.packageStatus !== "PACKED")
                      }
                      onClick={() => {
                        if (
                          window.confirm(
                            "Submit all sealed packages for manager review?",
                          )
                        )
                          action.run(() => api.post(`${SUB}/${id}/submit`));
                      }}
                    >
                      Submit for manager review →
                    </button>
                  </>
                )}
              </div>
              <Table
                head={["Package code", "Seal", "Quantity", "Status", ""]}
              >
                {packages.map((p) => (
                  <tr key={p.packageId}>
                    <td className="mono">{p.packageCode}</td>
                    <td>{p.sealNumber || "—"}</td>
                    <td>{number(p.totalQuantity)}</td>
                    <td>
                      <Status value={p.packageStatus} />
                    </td>
                    <td>
                      <button
                        className="text-link"
                        onClick={() => {
                          setPackageId(p.packageId);
                          setSeal("");
                          setBarcode("");
                          action.clear();
                        }}
                      >
                        Open package ↗
                      </button>
                    </td>
                  </tr>
                ))}
              </Table>
              {packageId && (
                <ResourceState resource={pack}>
                  {current && (
                    <section className="ops-panel mt-6">
                      <div className="panel-heading">
                        <h2>{current.packageCode}</h2>
                        <Status value={current.status} />
                        <button
                          className="text-link"
                          onClick={() => window.print()}
                        >
                          In barcode
                        </button>
                      </div>
                      <div className="package-label">
                        <Barcode
                          value={current.packageCode}
                          height={42}
                          fontSize={12}
                        />
                        <small>
                          Transfer: {current.transferCode} · Seal:{" "}
                          {current.sealNumber || "—"}
                        </small>
                      </div>
                      <Table head={["Product", "Code / Barcode", "Quantity"]}>
                        {(current.items || []).map((i, n) => (
                          <tr key={i.itemId || i.productId || n}>
                            <td>{i.productName}</td>
                            <td>
                              {i.productCode} / {i.barcode}
                            </td>
                            <td>{number(i.quantity)}</td>
                          </tr>
                        ))}
                      </Table>
                      {row.status === "PACKING" &&
                        ["CREATED", "PACKING"].includes(current.status) && (
                          <div className="packing-forms">
                            <form className="ops-form" onSubmit={scan}>
                              <h3>Add items to package</h3>
                              <Field
                                label="Scan barcode"
                                value={barcode}
                                onChange={(e) => setBarcode(e.target.value)}
                                autoComplete="off"
                                required
                              />
                              <Field
                                label="Quantity"
                                type="number"
                                min="1"
                                step="1"
                                value={quantity}
                                onChange={(e) => setQuantity(e.target.value)}
                                required
                              />
                              <button
                                className="ops-button"
                                disabled={action.busy}
                              >
                                Add to package
                              </button>
                            </form>
                            <form
                              className="ops-form"
                              onSubmit={async (e) => {
                                e.preventDefault();
                                if (window.confirm("Seal this package?"))
                                  await action.run(() =>
                                    api.patch(
                                      `${BASE}/packages/${packageId}/seal`,
                                      { sealNumber: seal.trim() },
                                    ),
                                  );
                              }}
                            >
                              <h3>Seal</h3>
                              <Field
                                label="Seal number"
                                value={seal}
                                onChange={(e) => setSeal(e.target.value)}
                                required
                              />
                              <></>
                              <button
                                className="ops-button secondary"
                                disabled={action.busy || !current.items?.length}
                              >
                                Seal package
                              </button>
                            </form>
                          </div>
                        )}
                    </section>
                  )}
                </ResourceState>
              )}
            </>
          )}
        </ResourceState>
      )}
    </MainLayout>
  );
}
