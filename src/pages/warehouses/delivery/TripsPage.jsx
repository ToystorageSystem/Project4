import { useState } from "react";
import usePager, { rowText, searchRows } from "../../../hooks/usePager";
import Pager, { SearchBox } from "../../../components/operations/Pager";
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
} from "../../../components/operations/WorkspaceUI";
const BASE = "/delivery-staff/trips";
export default function TripsPage() {
  const list = useResource(BASE),
    [id, setId] = useState(null),
    [handover, setHandover] = useState(null);
  const detail = useResource(id ? `${BASE}/${id}` : null),
    row = detail.data;
  const action = useAction(async () => {
    await Promise.all([list.reload(), detail.reload()]);
  });
  async function mutate(path, label) {
    if (window.confirm(label))
      await action.run(() => api.patch(`${BASE}/${id}/${path}`));
  }
  const [listSearch, setListSearch] = useState("");
  const listRows = searchRows(list.data, listSearch, rowText);
  const pager = usePager(listRows, 20, [listSearch]);
  return (
    <MainLayout>
      <PageHeader
        title={row?.shipmentCode || "My deliveries"}
        description="Track your schedule, accept deliveries and hand over packages."
        actions={
          <>
            {id && (
              <button
                className="ops-button secondary"
                onClick={() => {
                  setId(null);
                  setHandover(null);
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
      {!id && (<div className="ops-toolbar"><SearchBox value={listSearch} onChange={setListSearch} placeholder="Search trip, destination, status…" /><span>{listRows.length} trips</span></div>)}
      {!id ? (
        <ResourceState resource={list} empty={!list.data?.length}>
          <Table
            head={[
              "Delivery",
              "From",
              "To",
              "Expected delivery",
              "Package",
              "Status",
              "",
            ]}
          >
            {pager.rows.map((r) => (
              <tr key={r.deliveryId}>
                <td>
                  <strong>{r.shipmentCode}</strong>
                </td>
                <td>{r.fromLocationName}</td>
                <td>{r.toLocationName}</td>
                <td>{r.expectedDeliveryAt?.replace("T", " ")}</td>
                <td>{r.packageCount}</td>
                <td>
                  <Status value={r.status} />
                </td>
                <td>
                  <button
                    className="text-link"
                    onClick={() => {
                      setId(r.deliveryId);
                      action.clear();
                    }}
                  >
                    Details ↗
                  </button>
                </td>
              </tr>
            ))}
          </Table>
          <Pager pager={pager} label="trips" />
        </ResourceState>
      ) : (
        <ResourceState resource={detail}>
          {row && (
            <>
              <div className="detail-ribbon">
                <div>
                  <span>Pickup location</span>
                  <strong>{row.fromLocationName}</strong>
                  <small>{row.fromAddress}</small>
                </div>
                <div>
                  <span>Delivery location</span>
                  <strong>{row.toLocationName}</strong>
                  <small>{row.toAddress}</small>
                </div>
                <Status value={row.status} />
              </div>
              <div className="ops-toolbar">
                {row.status === "ASSIGNED" && (
                  <>
                    <button
                      className="ops-button"
                      disabled={action.busy}
                      onClick={() => mutate("accept", "Accept this delivery?")}
                    >
                      Accept delivery
                    </button>
                    <form
                      className="inline-form"
                      onSubmit={(e) => {
                        e.preventDefault();
                        action.run(() =>
                          api.patch(`${BASE}/${id}/reject`, {
                            reason: new FormData(e.currentTarget).get("reason"),
                          }),
                        );
                      }}
                    >
                      <input
                        aria-label="Reason for rejection"
                        name="reason"
                        required
                        minLength={3}
                        placeholder="Reason for rejection"
                      />
                      <button
                        className="ops-button secondary"
                        disabled={action.busy}
                      >
                        Reject
                      </button>
                    </form>
                  </>
                )}
                {row.status === "IN_TRANSIT" && (
                  <button
                    className="ops-button"
                    disabled={action.busy}
                    onClick={() =>
                      mutate("arrived", "Confirm arrival at the delivery location?")
                    }
                  >
                    Mark as arrived
                  </button>
                )}
                {["IN_TRANSIT", "ARRIVED"].includes(row.status) && (
                  <button
                    className="ops-button danger"
                    disabled={action.busy}
                    onClick={() =>
                      mutate("failed", "Mark this delivery as failed?")
                    }
                  >
                    Delivery failed
                  </button>
                )}
              </div>
              <Table head={["Package code", "Seal", "Status"]}>
                {(row.packages || []).map((p) => (
                  <tr key={p.packageId}>
                    <td className="mono">{p.packageCode}</td>
                    <td>{p.sealNumber || "—"}</td>
                    <td>
                      <Status value={p.status} />
                    </td>
                  </tr>
                ))}
              </Table>
              {row.status === "ARRIVED" && (
                <div className="packing-forms mt-6">
                  <form
                    className="ops-panel ops-form"
                    onSubmit={async (e) => {
                      e.preventDefault();
                      const f = Object.fromEntries(
                        new FormData(e.currentTarget),
                      );
                      const result = await action.run(
                        async () =>
                          (
                            await api.post(
                              `${BASE}/${id}/handover/packages/scan`,
                              f,
                            )
                          ).data,
                      );
                      if (result) setHandover(result);
                    }}
                  >
                    <h2>Scan package handover</h2>
                    <Field label="Package code" name="packageCode" required />
                    <Field label="Actual seal number" name="actualSealNumber" />
                    <Field label="Condition">
                      <select name="condition">
                        {[
                          "NORMAL",
                          "DAMAGED",
                          "SEAL_MISMATCH",
                          "SEAL_BROKEN",
                          "MISSING",
                        ].map((c) => (
                          <option key={c}>{c}</option>
                        ))}
                      </select>
                    </Field>
                    <Field label="Issue notes" name="issueNote" />
                    <button className="ops-button" disabled={action.busy}>
                      Record package
                    </button>
                  </form>
                  <form
                    className="ops-panel ops-form"
                    onSubmit={async (e) => {
                      e.preventDefault();
                      const f = Object.fromEntries(
                        new FormData(e.currentTarget),
                      );
                      if (window.confirm("Complete the handover to the recipient?")) {
                        const result = await action.run(
                          async () =>
                            (
                              await api.patch(
                                `${BASE}/${id}/handover/complete`,
                                {
                                  receiverId: Number(f.receiverId),
                                  note: f.note,
                                },
                              )
                            ).data,
                        );
                        if (result) setHandover(result);
                      }
                    }}
                  >
                    <h2>Complete handover</h2>
                    {handover && (
                      <p>
                        Handed over {handover.handedOverPackageCount} /{" "}
                        {handover.expectedPackageCount} packages · Missing{" "}
                        {handover.missingPackageCount}
                      </p>
                    )}
                    <Field
                      label="Recipient ID"
                      name="receiverId"
                      type="number"
                      min="1"
                      required
                    />
                    <Field label="Notes" name="note" />
                    <button className="ops-button" disabled={action.busy}>
                      Complete handover
                    </button>
                  </form>
                </div>
              )}
            </>
          )}
        </ResourceState>
      )}
    </MainLayout>
  );
}
