import { useState } from "react";
import usePager from "../../../hooks/usePager";
import Pager from "../../../components/operations/Pager";
import api from "../../../api/api";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Modal from "../../../components/ui/Modal";
import useResource from "../../../hooks/useResource";
import {
  Field,
  Notice,
  ResourceState,
  Table,
  useAction,
} from "../../../components/operations/WorkspaceUI";
import { useAuth } from "../../../auth/AuthContext";
import { hasPermission } from "../../../auth/access";
export default function StoresPage() {
  const resource = useResource("/stores"),
    action = useAction(resource.reload);
  const { user } = useAuth();
  const [search, setSearch] = useState(""),
    [selected, setSelected] = useState(null);
  const rows = (resource.data || []).filter((r) =>
    [r.storeName, r.storeCode, r.storeManagerName]
      .join(" ")
      .toLowerCase()
      .includes(search.toLowerCase()),
  );
  async function save(e) {
    e.preventDefault();
    const data = new FormData(e.currentTarget);
    const result = await action.run(() =>
      api.put(`/stores/${selected.warehouseId}`, Object.fromEntries(data)),
    );
    if (result) setSelected(null);
  }
  const pager = usePager(rows, 20, [search]);
  return (
    <MainLayout>
      <PageHeader
        title="Stores"
        description="Contact details for stores supplied by the warehouse."
        actions={
          <button
            className="ops-button secondary"
            onClick={resource.reload}
            disabled={resource.loading}
          >
            Refresh ↻
          </button>
        }
      />
      <Notice error={action.error}>{action.success}</Notice>
      <div className="ops-toolbar">
        <input
          aria-label="Search stores"
          placeholder="Search store or manager…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>
      <ResourceState resource={resource} empty={!rows.length}>
        <Table
          head={["Stores", "Manager", "Opening hours", "Phone", "Actions"]}
        >
          {pager.rows.map((r) => (
            <tr key={r.warehouseId}>
              <td>
                <strong>{r.storeName}</strong>
                <small>{r.storeCode}</small>
              </td>
              <td>{r.storeManagerName || "—"}</td>
              <td>{r.openingHours || "—"}</td>
              <td>{r.storePhone || "—"}</td>
              <td>
                {hasPermission(user, "STORE_MANAGE") && (
                  <button
                    className="text-link"
                    onClick={() => {
                      action.clear();
                      setSelected(r);
                    }}
                  >
                    Edit ↗
                  </button>
                )}
              </td>
            </tr>
          ))}
        </Table>
        <Pager pager={pager} label="stores" />
      </ResourceState>
      <Modal
        open={!!selected}
        title="Store details"
        onClose={() => {
          if (!action.busy) setSelected(null);
        }}
      >
        {selected && (
          <form key={selected.warehouseId} onSubmit={save} className="ops-form">
            <Notice error={action.error} />
            <Field
              label="Store manager"
              name="storeManagerName"
              defaultValue={selected.storeManagerName || ""}
              maxLength={150}
            />
            <Field
              label="Opening hours"
              name="openingHours"
              defaultValue={selected.openingHours || ""}
              maxLength={255}
            />
            <Field
              label="Phone"
              name="storePhone"
              type="tel"
              defaultValue={selected.storePhone || ""}
              maxLength={30}
            />
            <button className="ops-button" disabled={action.busy}>
              Save details
            </button>
          </form>
        )}
      </Modal>
    </MainLayout>
  );
}
