import { useState } from "react";
import { Link } from "react-router-dom";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Modal from "../../../components/ui/Modal";
import Badge from "../../../components/ui/Badge";
import useResource from "../../../hooks/useResource";
import { Field, Notice, ResourceState, Table, number, useAction } from "../../../components/operations/WorkspaceUI";
import { STORE_API, changeShelfStatus, createShelf, updateShelf } from "../../../api/stores/manager/storeApi";
import { Metrics, RefreshButton } from "../components/StoreUI";

// Store Manager: all shelves (active + inactive) with create / rename / deactivate (2026-11-13).
export default function ShelfListPage() {
  const resource = useResource(STORE_API.manageShelves);
  const action = useAction();
  const [search, setSearch] = useState("");
  const [editing, setEditing] = useState(null); // null = closed, {} = new shelf, shelf = edit
  const rows = Array.isArray(resource.data) ? resource.data : [];
  const active = rows.filter((s) => s.status === "ACTIVE");
  const filtered = rows.filter((s) => [s.name, s.code, s.description].join(" ").toLowerCase().includes(search.trim().toLowerCase()));
  const shown = (v) => (resource.data ? number(v) : "—");

  async function toggle(shelf) {
    const next = shelf.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    const done = await action.run(() => changeShelfStatus(shelf.id, next),
      next === "ACTIVE" ? `${shelf.name} is active again.` : `${shelf.name} is no longer used.`);
    if (done) resource.reload();
  }

  return (
    <MainLayout>
      <PageHeader
        title="Shelves"
        description="Create and manage the shelves of your store and see how much stock is on each one."
        actions={
          <>
            <button className="ops-button" onClick={() => setEditing({})}>+ New shelf</button>
            <Link className="ops-button secondary" to="/store/low-shelf">Restock shelves →</Link>
            <RefreshButton resource={resource} />
          </>
        }
      />
      <Metrics
        items={[
          { label: "Active shelves", value: shown(active.length) },
          { label: "Empty active shelves", value: shown(active.filter((s) => !s.productCount).length) },
          { label: "Units on shelves", value: shown(rows.reduce((t, s) => t + (s.shelfQuantity || 0), 0)) },
        ]}
      />
      <Notice error={action.error}>{action.success}</Notice>
      <div className="ops-toolbar">
        <input aria-label="Search shelves" placeholder="Search shelf name or code…" value={search} onChange={(e) => setSearch(e.target.value)} />
        <span>{filtered.length} shelves</span>
      </div>
      <ResourceState resource={resource} empty={!filtered.length}>
        <Table head={["Shelf", "Description", "Status", "Products", "Units on shelf", ""]}>
          {filtered.map((s) => (
            <tr key={s.id}>
              <td><strong>{s.name}</strong><small className="mono">{s.code}</small></td>
              <td>{s.description || "—"}</td>
              <td>{s.status === "ACTIVE" ? <Badge variant="green">Active</Badge> : <Badge>Not used</Badge>}</td>
              <td>{number(s.productCount)}</td>
              <td>{number(s.shelfQuantity)}</td>
              <td style={{ whiteSpace: "nowrap" }}>
                {s.status === "ACTIVE" && <Link className="text-link" to={`/store/shelves/${s.id}`}>Open →</Link>}{" "}
                <button className="text-link" type="button" onClick={() => setEditing(s)}>Edit</button>{" "}
                <button className="text-link" type="button" disabled={action.busy} onClick={() => toggle(s)}>
                  {s.status === "ACTIVE" ? "Stop using" : "Use again"}
                </button>
              </td>
            </tr>
          ))}
        </Table>
      </ResourceState>
      {editing && (
        <ShelfModal
          shelf={editing}
          onClose={() => setEditing(null)}
          onSaved={() => { setEditing(null); resource.reload(); }}
        />
      )}
    </MainLayout>
  );
}

function ShelfModal({ shelf, onClose, onSaved }) {
  const isNew = !shelf.id;
  const action = useAction();
  const [name, setName] = useState(shelf.name || "");
  const [description, setDescription] = useState(shelf.description || "");
  const [invalid, setInvalid] = useState("");

  async function save(e) {
    e.preventDefault();
    if (!name.trim() || name.trim().length > 100) {
      setInvalid("Enter a shelf name (maximum 100 characters).");
      return;
    }
    if (description.trim().length > 255) {
      setInvalid("Description must be at most 255 characters.");
      return;
    }
    setInvalid("");
    const result = await action.run(
      () => (isNew ? createShelf(name, description) : updateShelf(shelf.id, name, description)),
      isNew ? "Shelf created." : "Shelf updated.",
    );
    if (result) onSaved(result);
  }

  return (
    <Modal open title={isNew ? "New shelf" : `Edit shelf – ${shelf.code}`} onClose={onClose}>
      <form onSubmit={save}>
        <Field label="Shelf name *" maxLength={100} value={name} onChange={(e) => setName(e.target.value)} autoFocus />
        <Field label="Description (where is it?)" maxLength={255} value={description} onChange={(e) => setDescription(e.target.value)} />
        <Notice error={invalid || action.error} />
        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end", marginTop: 16 }}>
          <button type="button" className="ops-button secondary" onClick={onClose}>Cancel</button>
          <button className="ops-button" disabled={action.busy}>{action.busy ? "Saving…" : "Save"}</button>
        </div>
      </form>
    </Modal>
  );
}
