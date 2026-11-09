import { useState } from "react";
import { Link } from "react-router-dom";
import Modal from "../../../components/ui/Modal";
import Badge from "../../../components/ui/Badge";
import { Field, Notice, useAction } from "../../../components/operations/WorkspaceUI";
import { changeMinShelf } from "../../../api/stores/manager/storeApi";

export function SectionTitle({ children, actions }) {
  return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12, margin: "24px 0 10px" }}>
      <h2 style={{ fontSize: 17, margin: 0 }}>{children}</h2>
      {actions}
    </div>
  );
}

const highlight = { borderColor: "#f25d19", background: "#faeadd" };
// items: [{ label, value, highlight?, to? }]
export function Metrics({ items }) {
  return (
    <div className="metric-strip">
      {items.map((i) => (
        <div key={i.label} style={i.highlight ? highlight : undefined}>
          <span>{i.label}</span>
          <strong>{i.value}</strong>
          {i.to && <Link className="text-link" to={i.to}>View →</Link>}
        </div>
      ))}
    </div>
  );
}

const RECEIVING = {
  NOT_STARTED: ["Not started", "default"],
  RECEIVING: ["Receiving packages", "orange"],
  PACKAGES_RECEIVED: ["Counting products", "blue"],
  INSPECTED: ["Awaiting confirmation", "yellow"],
  COMPLETED: ["Completed", "green"],
};
const PACKAGE = {
  NOT_RECEIVED: ["Not received", "red"],
  RECEIVED: ["Received, not counted", "yellow"],
  CHECKED: ["Counted", "green"],
};
const TX = {
  RECEIVE: ["Received", "green"],
  MOVE_TO_SHELF: ["Backroom → shelf", "blue"],
  MOVE_TO_BACKROOM: ["Shelf → backroom", "default"],
  SALE: ["Sale", "orange"],
  ADJUST: ["Adjustment", "yellow"],
};
export const TX_TYPES = Object.keys(TX).map((value) => ({ value, label: TX[value][0] }));
const badge = (map) =>
  function MappedBadge({ status }) {
    const [label, variant] = map[status] || [status?.replaceAll("_", " ") || "—", "default"];
    return <Badge variant={variant}>{label}</Badge>;
  };
export const ReceivingBadge = badge(RECEIVING);
export const PackageBadge = badge(PACKAGE);
export const TxBadge = badge(TX);

export function LowBadge({ low }) {
  return low ? <Badge variant="red">Low</Badge> : <Badge variant="green">OK</Badge>;
}

export function Change({ value }) {
  if (!value) return <span style={{ color: "#656b65" }}>0</span>;
  return (
    <strong style={{ color: value > 0 ? "#1e7a46" : "#c0392b" }}>
      {value > 0 ? "+" : "−"}
      {Math.abs(value)}
    </strong>
  );
}

// PageResponse { page (0-based), totalPages, totalElements }
export function Pager({ data, onChange }) {
  if (!data?.totalPages) return null;
  return (
    <div className="ops-toolbar" style={{ justifyContent: "flex-end" }}>
      <span>
        {data.totalElements} records · page {data.page + 1} / {data.totalPages}
      </span>
      <button className="ops-button secondary" disabled={data.page <= 0} onClick={() => onChange(data.page - 1)}>
        ← Previous
      </button>
      <button className="ops-button secondary" disabled={data.page + 1 >= data.totalPages} onClick={() => onChange(data.page + 1)}>
        Next →
      </button>
    </div>
  );
}

// Store Manager only (backend: 403 "Only store manager can confirm").
export function MinShelfModal({ product, onClose, onSaved }) {
  const action = useAction();
  const [value, setValue] = useState(String(product.minShelfQuantity ?? 0));
  const [invalid, setInvalid] = useState("");
  async function save(e) {
    e.preventDefault();
    const n = Number(value);
    if (value.trim() === "" || !Number.isInteger(n) || n < 0 || n > 100000) {
      setInvalid("Enter a whole number from 0 to 100,000.");
      return;
    }
    setInvalid("");
    const result = await action.run(() => changeMinShelf(product.productId, n), "Minimum shelf quantity updated.");
    if (result) {
      onSaved?.(result);
      onClose();
    }
  }
  return (
    <Modal open title={`Minimum on shelf – ${product.name}`} onClose={onClose}>
      <form onSubmit={save}>
        <p style={{ marginTop: 0 }}>
          The product shows as <strong>low</strong> when the shelf has this many units or fewer.
          Now on shelf: <strong>{product.shelfQuantity ?? 0}</strong>.
        </p>
        <Field label="Minimum shelf quantity" type="number" min="0" max="100000" step="1" value={value} onChange={(e) => setValue(e.target.value)} autoFocus />
        <Notice error={invalid || action.error} />
        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end", marginTop: 16 }}>
          <button type="button" className="ops-button secondary" onClick={onClose}>Cancel</button>
          <button className="ops-button" disabled={action.busy}>{action.busy ? "Saving…" : "Save"}</button>
        </div>
      </form>
    </Modal>
  );
}

export function RefreshButton({ resource }) {
  return (
    <button className="ops-button secondary" disabled={resource.loading} onClick={resource.reload}>
      Refresh ↻
    </button>
  );
}
