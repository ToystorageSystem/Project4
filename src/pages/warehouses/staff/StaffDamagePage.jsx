import { useState } from "react";
import usePager, { rowText, searchRows } from "../../../hooks/usePager";
import Pager, { SearchBox } from "../../../components/operations/Pager";
import useResource from "../../../hooks/useResource";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Modal from "../../../components/ui/Modal";
import {
  Field,
  Notice,
  ResourceState,
  Status,
  Table,
  useAction,
  number,
} from "../../../components/operations/WorkspaceUI";
import { createDamagedGoodsReport } from "../../../api/warehouses/damagedgoods/staffDamagedGoods";
const BASE = "/warehouse/staff/damaged-goods";
export default function StaffDamagePage() {
  const list = useResource(BASE),
    stock = useResource("/warehouse/inventory/balances");
  const [open, setOpen] = useState(false),
    [productId, setProductId] = useState(""),
    [image, setImage] = useState(null),
    [selected, setSelected] = useState(null);
  const detail = useResource(selected ? `${BASE}/${selected}` : null),
    action = useAction(list.reload);
  const product = (stock.data || []).find(
    (p) => String(p.productId) === productId,
  );
  async function submit(e) {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(e.currentTarget));
    const result = await action.run(() =>
      createDamagedGoodsReport(
        {
          productId: Number(productId),
          locationId: Number(f.locationId),
          quantity: Number(f.quantity),
          damageType: f.damageType,
          sourceType: "MANUAL",
          description: f.description,
          conditionNote: f.conditionNote,
        },
        image,
      ),
    );
    if (result) {
      setOpen(false);
      setImage(null);
    }
  }
  const [listSearch, setListSearch] = useState("");
  const listRows = searchRows(list.data, listSearch, rowText);
  const pager = usePager(listRows, 20, [listSearch]);
  return (
    <MainLayout>
      <PageHeader
        title="Damage reports"
        description="Report damaged stock and send evidence to your manager for review."
        actions={
          <>
            <button
              className="ops-button"
              onClick={() => {
                setOpen(true);
                setProductId("");
                action.clear();
              }}
            >
              + Report damage
            </button>
            <button
              className="ops-button secondary"
              onClick={list.reload}
              disabled={list.loading}
            >
              Refresh ↻
            </button>
          </>
        }
      />
      <Notice error={action.error}>{action.success}</Notice>
      <div className="ops-toolbar"><SearchBox value={listSearch} onChange={setListSearch} placeholder="Search report code, source, status…" /><span>{listRows.length} reports</span></div>
      <ResourceState resource={list} empty={!list.data?.length}>
        <Table head={["Report", "Source", "Description", "Status", ""]}>
          {pager.rows.map((r) => (
            <tr key={r.reportId}>
              <td>
                <strong>{r.reportCode}</strong>
                <small>{r.createdAt?.replace("T", " ")}</small>
              </td>
              <td>{r.sourceType}</td>
              <td>{r.description}</td>
              <td>
                <Status value={r.status} />
              </td>
              <td>
                <button
                  className="text-link"
                  onClick={() => setSelected(r.reportId)}
                >
                  Details ↗
                </button>
              </td>
            </tr>
          ))}
        </Table>
        <Pager pager={pager} label="reports" />
      </ResourceState>
      <Modal
        open={open}
        title="Report damaged goods"
        onClose={() => {
          if (!action.busy) setOpen(false);
        }}
      >
        <form className="ops-form" onSubmit={submit}>
          <Notice error={action.error || stock.error} />
          <Field label="Product">
            <select
              value={productId}
              onChange={(e) => setProductId(e.target.value)}
              required
            >
              <option value="">Select a product</option>
              {(stock.data || []).map((p) => (
                <option key={p.productId} value={p.productId}>
                  {p.productCode} · {p.productName}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Location">
            <select name="locationId" required key={productId}>
              <option value="">Select a location</option>
              {(product?.locations || []).map((l) => (
                <option key={l.locationId} value={l.locationId}>
                  {l.locationCode} · On hand {l.onHand}
                </option>
              ))}
            </select>
          </Field>
          <Field
            label="Affected quantity"
            name="quantity"
            type="number"
            min="1"
            step="1"
            required
          />
          <Field label="Damage type">
            <select name="damageType">
              {[
                "BROKEN",
                "DENTED",
                "SCRATCHED",
                "WET",
                "EXPIRED",
                "MISSING_PARTS",
                "OTHER",
              ].map((t) => (
                <option key={t}>{t}</option>
              ))}
            </select>
          </Field>
          <Field label="Description" name="description" required maxLength={2000} />
          <Field label="Condition notes" name="conditionNote" />
          <Field label="Evidence photo">
            <input
              type="file"
              accept="image/*"
              onChange={(e) => setImage(e.target.files?.[0] || null)}
            />
          </Field>
          <button
            className="ops-button"
            disabled={action.busy || stock.loading}
          >
            Submit report
          </button>
        </form>
      </Modal>
      <Modal
        open={!!selected}
        title={detail.data?.reportCode || "Report details"}
        onClose={() => setSelected(null)}
      >
        <ResourceState resource={detail}>
          {detail.data && (
            <>
              <p className="section-note">{detail.data.description}</p>
              <Status value={detail.data.status} />
              {(detail.data.items || []).map((i) => (
                <div className="location-breakdown" key={i.itemId}>
                  <strong>{i.productName}</strong>
                  <p>
                    {i.locationCode} · {number(i.quantity)} · {i.damageType}
                  </p>
                  <p>
                    {i.conditionNote} / {i.disposition}
                  </p>
                  {i.evidenceImageUrl && (
                    <a
                      href={i.evidenceImageUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-link"
                    >
                      View photo ↗
                    </a>
                  )}
                </div>
              ))}
            </>
          )}
        </ResourceState>
      </Modal>
    </MainLayout>
  );
}
