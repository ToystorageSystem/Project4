import { Fragment, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Modal from "../../../components/ui/Modal";
import StatusBadge from "../../../components/ui/StatusBadge";
import useResource from "../../../hooks/useResource";
import { errorMessage } from "../../../api/api";
import { Notice, ResourceState, Table, number, useAction } from "../../../components/operations/WorkspaceUI";
import { STORE_API, confirmReceiving, finishInspection, getPackageItems } from "../../../api/stores/manager/storeApi";
import { Metrics, PackageBadge, ReceivingBadge, RefreshButton, SectionTitle } from "../components/StoreUI";
import { dateTime } from "../storeFormat";

// Product lines of one package (GET /receiving/packages/{code}/items).
function PackageLines({ packageCode }) {
  const resource = useResource(STORE_API.packageItems(packageCode));
  const lines = resource.data?.lines || [];
  return (
    <ResourceState resource={resource} empty={!lines.length}>
      <Table head={["Product", "Barcode", "Expected", "Counted", "Damaged", "Difference", "Note"]}>
        {lines.map((l) => {
          const diff = l.actualQuantity == null ? null : l.actualQuantity - l.expectedQuantity;
          return (
            <tr key={l.productId}>
              <td><strong>{l.name}</strong><small>{l.productCode}{l.expectedQuantity === 0 ? " · not on the list (extra)" : ""}</small></td>
              <td className="mono">{l.barcode}</td>
              <td>{number(l.expectedQuantity)}</td>
              <td>{number(l.actualQuantity)}</td>
              <td>{number(l.damagedQuantity)}</td>
              <td style={{ color: diff ? (diff > 0 ? "#1e7a46" : "#c0392b") : undefined }}>
                {diff == null ? "—" : diff > 0 ? `+${diff}` : diff}
              </td>
              <td>{l.note || "—"}</td>
            </tr>
          );
        })}
      </Table>
    </ResourceState>
  );
}

// Before confirmation: add up the counts of all counted packages (there is no GET for the result).
function useCountSummary(packages, enabled) {
  const key = packages.filter((p) => p.status === "CHECKED").map((p) => p.packageCode).join(",");
  const [state, setState] = useState({ loading: false, error: "", lines: [] });
  useEffect(() => {
    if (!enabled || !key) return undefined;
    let alive = true;
    setState({ loading: true, error: "", lines: [] });
    Promise.all(key.split(",").map(getPackageItems))
      .then((list) => {
        if (!alive) return;
        const byProduct = new Map();
        for (const pkg of list)
          for (const l of pkg.lines || []) {
            const row = byProduct.get(l.productId) || { productId: l.productId, productCode: l.productCode, name: l.name, expected: 0, actual: 0, damaged: 0 };
            row.expected += l.expectedQuantity || 0;
            row.actual += l.actualQuantity || 0;
            row.damaged += l.damagedQuantity || 0;
            byProduct.set(l.productId, row);
          }
        setState({ loading: false, error: "", lines: [...byProduct.values()] });
      })
      .catch((e) => alive && setState({ loading: false, error: errorMessage(e), lines: [] }));
    return () => {
      alive = false;
    };
  }, [key, enabled]);
  return state;
}

function CountSummary({ packages }) {
  const summary = useCountSummary(packages, true);
  const missing = packages.filter((p) => p.status === "NOT_RECEIVED");
  if (summary.loading) return <div className="ops-empty" role="status">Adding up the counts…</div>;
  return (
    <>
      <Notice error={summary.error} />
      {missing.length > 0 && (
        <div role="alert" className="ops-alert">
          {missing.length} package(s) not received ({missing.map((p) => p.packageCode).join(", ")}, {number(missing.reduce((s, p) => s + (p.pieces || 0), 0))} pieces) – recorded as shortage.
        </div>
      )}
      <Table head={["Product", "Expected (counted packages)", "Counted", "Damaged", "Shortage", "Extra", "Into backroom"]}>
        {summary.lines.map((r) => (
          <tr key={r.productId}>
            <td><strong>{r.name}</strong><small>{r.productCode}</small></td>
            <td>{number(r.expected)}</td>
            <td>{number(r.actual)}</td>
            <td>{number(r.damaged)}</td>
            <td style={{ color: r.expected > r.actual ? "#c0392b" : undefined }}>{number(Math.max(r.expected - r.actual, 0))}</td>
            <td style={{ color: r.actual > r.expected ? "#1e7a46" : undefined }}>{number(Math.max(r.actual - r.expected, 0))}</td>
            <td><strong>{number(r.actual - r.damaged)}</strong></td>
          </tr>
        ))}
      </Table>
    </>
  );
}

// ReceivingResultResponse from finish-inspection / confirm.
function ResultView({ result }) {
  return (
    <>
      <Metrics
        items={[
          { label: "Expected", value: number(result.totalExpected) },
          { label: "Counted", value: number(result.totalActual) },
          { label: "Damaged", value: number(result.totalDamaged), highlight: result.totalDamaged > 0 },
          { label: "Shortage", value: number(result.totalShortage), highlight: result.totalShortage > 0 },
          { label: "Extra", value: number(result.totalSurplus), highlight: result.totalSurplus > 0 },
        ]}
      />
      <Table head={["Transfer", "Product", "Expected", "Counted", "Damaged", "Shortage", "Extra"]}>
        {(result.items || []).map((i) => (
          <tr key={`${i.transferCode}-${i.productId}`}>
            <td className="mono">{i.transferCode}</td>
            <td><strong>{i.name}</strong><small>{i.productCode}</small></td>
            <td>{number(i.expected)}</td>
            <td>{number(i.actual)}</td>
            <td>{number(i.damaged)}</td>
            <td style={{ color: i.shortage ? "#c0392b" : undefined }}>{number(i.shortage)}</td>
            <td style={{ color: i.surplus ? "#1e7a46" : undefined }}>{number(i.surplus)}</td>
          </tr>
        ))}
      </Table>
      {result.discrepancyReportCodes?.length > 0 && (
        <div role="status" className="ops-notice">
          Discrepancy reports created: <strong className="mono">{result.discrepancyReportCodes.join(", ")}</strong>
        </div>
      )}
    </>
  );
}

export default function ReceivingDetailPage() {
  const { code } = useParams();
  const resource = useResource(STORE_API.manifest(code));
  const action = useAction(); // no auto reload: a completed manifest may no longer be served
  const [result, setResult] = useState(null);
  const [note, setNote] = useState("");
  const [asking, setAsking] = useState(false);
  const [openPackage, setOpenPackage] = useState(null);

  const m = resource.data?.manifest;
  const packages = resource.data?.packages || [];
  const receipts = resource.data?.receipts || [];
  const status = result?.receivingStatus || m?.receivingStatus;
  const uncounted = packages.filter((p) => p.status === "RECEIVED");
  const completed = status === "COMPLETED";

  async function finish() {
    const r = await action.run(() => finishInspection(code), "Inspection finished. Review the result and confirm.");
    if (r) {
      setResult(r);
      resource.reload();
    }
  }
  async function confirm() {
    setAsking(false);
    const r = await action.run(() => confirmReceiving(code, note), "Stock-in confirmed. Backroom stock has been updated.");
    if (r) setResult(r);
  }

  return (
    <MainLayout>
      <PageHeader
        title={`Manifest ${code}`}
        description={m ? `${m.shipmentCode} · from ${m.fromWarehouseName || "—"} · expected ${dateTime(m.expectedDeliveryAt)}` : "Delivery details"}
        actions={
          <>
            <Link className="ops-button secondary" to="/store/receiving">← All deliveries</Link>
            {!completed && <RefreshButton resource={resource} />}
          </>
        }
      />
      {!m && !result ? (
        <ResourceState resource={resource} empty={false} />
      ) : (
        <>
          {m && (
            <Metrics
              items={[
                { label: "Receiving", value: <ReceivingBadge status={status} /> },
                { label: "Delivery", value: <StatusBadge status={m.deliveryStatus} /> },
                { label: "Packages received", value: `${m.receivedPackages} / ${m.totalPackages}` },
                { label: "Packages counted", value: `${m.checkedPackages} / ${m.receivedPackages}` },
              ]}
            />
          )}
          <Notice error={action.error}>{action.success}</Notice>

          <SectionTitle>Next step</SectionTitle>
          <section className="ops-panel" style={{ padding: 20 }}>
            {completed && result ? (
              <>
                <p style={{ marginTop: 0 }}>Stock-in is complete. Good items were added to the backroom; damaged items were not.</p>
                <ResultView result={result} />
              </>
            ) : completed ? (
              <p style={{ margin: 0 }}>This delivery is already completed.</p>
            ) : status === "INSPECTED" ? (
              <>
                <p style={{ marginTop: 0 }}>
                  Staff finished counting. Check the result, then confirm: <strong>counted − damaged</strong> goes into the backroom and discrepancy reports are created for shortage, extra and damaged items.
                </p>
                {result ? <ResultView result={result} /> : <CountSummary packages={packages} />}
                <label className="ops-field" style={{ marginTop: 16 }}>
                  <span>Note (optional)</span>
                  <textarea rows={2} maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} placeholder="e.g. Package 2 was dented, driver informed" />
                </label>
                <button className="ops-button" style={{ marginTop: 12 }} disabled={action.busy} onClick={() => setAsking(true)}>
                  {action.busy ? "Confirming…" : "Confirm stock-in"}
                </button>
              </>
            ) : status === "PACKAGES_RECEIVED" ? (
              uncounted.length ? (
                <p style={{ margin: 0 }}>Waiting for staff to count: <strong className="mono">{uncounted.map((p) => p.packageCode).join(", ")}</strong>. Press Refresh to update.</p>
              ) : (
                <>
                  <p style={{ marginTop: 0 }}>All received packages are counted. Staff usually finish the inspection on the app; you can also finish it here.</p>
                  <button className="ops-button" disabled={action.busy} onClick={finish}>{action.busy ? "Finishing…" : "Finish inspection"}</button>
                </>
              )
            ) : m && !["ARRIVED", "DELIVERED"].includes(m.deliveryStatus) ? (
              <p style={{ margin: 0 }}>The delivery has not arrived yet.</p>
            ) : (
              <p style={{ margin: 0 }}>Staff are receiving packages on the mobile app. Press Refresh to update.</p>
            )}
          </section>

          {packages.length > 0 && (
            <>
              <SectionTitle>Packages</SectionTitle>
              <Table head={["Package", "Transfer", "Seal", "Condition", "Notes", "Lines / pieces", "Status", ""]}>
                {packages.map((p) => (
                  <Fragment key={p.packageId}>
                    <tr>
                      <td><strong className="mono">{p.packageCode}</strong><small>{p.sealNumber || ""}</small></td>
                      <td className="mono">{p.transferCode}</td>
                      <td>{p.sealStatus || "—"}</td>
                      <td>{p.packageCondition || "—"}</td>
                      <td>{p.notes || "—"}</td>
                      <td>{p.lineCount} / {p.pieces}</td>
                      <td><PackageBadge status={p.status} /></td>
                      <td>
                        {p.status !== "NOT_RECEIVED" && (
                          <button className="text-link" onClick={() => setOpenPackage(openPackage === p.packageCode ? null : p.packageCode)}>
                            {openPackage === p.packageCode ? "Hide count −" : "View count +"}
                          </button>
                        )}
                      </td>
                    </tr>
                    {openPackage === p.packageCode && (
                      <tr>
                        <td colSpan={8}><PackageLines packageCode={p.packageCode} /></td>
                      </tr>
                    )}
                  </Fragment>
                ))}
              </Table>
            </>
          )}

          {receipts.length > 0 && (
            <>
              <SectionTitle>Store receipts</SectionTitle>
              <Table head={["Receipt", "Transfer", "Status", "Packages received at"]}>
                {receipts.map((r) => (
                  <tr key={r.receiptId}>
                    <td className="mono">{r.receiptCode}</td>
                    <td className="mono">{r.transferCode}</td>
                    <td><StatusBadge status={r.status} /></td>
                    <td>{dateTime(r.receivedAt)}</td>
                  </tr>
                ))}
              </Table>
            </>
          )}
        </>
      )}
      {asking && (
        <Modal
          open
          title="Confirm stock-in?"
          onClose={() => setAsking(false)}
          footer={
            <>
              <button className="ops-button secondary" onClick={() => setAsking(false)}>Cancel</button>
              <button className="ops-button" onClick={confirm}>Confirm</button>
            </>
          }
        >
          <p>Backroom stock will be increased and discrepancy reports created. This cannot be undone.</p>
        </Modal>
      )}
    </MainLayout>
  );
}
