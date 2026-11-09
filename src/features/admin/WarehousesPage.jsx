import { isAdmin } from "../../auth/access";

import { useCallback, useEffect, useMemo, useState } from "react";

import {
  AlertTriangle,
  Building2,
  CheckCircle2,
  ClipboardList,
  Database,
  Eye,
  Pencil,
  Plus,
  Power,
  RefreshCw,
  Search,
  ShieldCheck,
  Store,
  UserMinus,
  UserPlus,
  Users,
  Warehouse,
  X,
} from "lucide-react";

import { getAdminUsers } from "./api/admin/userAccountsApi";

import {
  activateWarehouse,
  assignWarehouseEmployee,
  assignWarehouseManager,
  createWarehouse,
  deactivateWarehouse,
  getWarehouseDeactivationImpact,
  getWarehouseEmployees,
  getWarehouseSession,
  getWarehouses,
  unassignWarehouseEmployee,
  updateWarehouse,
} from "./api/admin/warehousesApi";

import "./css/WarehousesPage.css";

// ======================================================
// HELPERS
// ======================================================

function normalizeRole(value) {
  return String(value ?? "")
    .toUpperCase()
    .replace(/^ROLE[-_]/, "")
    .replaceAll("-", "_");
}

function messageFor(error) {
  if (error?.status === 401) {
    return "Session expired. Please sign in again.";
  }

  if (error?.status === 403) {
    return "Access denied. Please check your Admin permissions.";
  }

  const message =
    error?.message || "An unexpected error occurred.";

  return /[\u00c0-\u1ef9]/.test(message)
    ? "The Backend rejected this operation. Check the business rules and server logs."
    : message;
}

function mapLocation(item) {
  return {
    id: item.id,
    code: item.warehousesCode || "",
    name: item.name || "",
    type: item.type,
    address: item.address || "",
    phone: item.phone || "",
    status: item.status,
    employees: [],
    employeesLoaded: false,
  };
}

function mapEmployee(item) {
  return {
    id: item.userId,
    code: item.userCode || "",
    name: item.name || "",
    email: item.email || "",
    status: item.status,
    warehouseId: item.warehouseId,
    roles: Array.isArray(item.roles)
      ? item.roles.map(normalizeRole)
      : [],
  };
}

function managerRole(location) {
  return location?.type === "MAIN_WAREHOUSE"
    ? "WH_MANAGER"
    : "STORE_MANAGER";
}

function staffRole(location) {
  return location?.type === "MAIN_WAREHOUSE"
    ? "WH_STAFF"
    : "STORE_STAFF";
}

function managerOf(location) {
  return location?.employees.find((user) =>
    user.roles.includes(managerRole(location))
  );
}

function StatusBadge({ status }) {
  return (
    <span
      className={`wh-status ${String(status || "").toLowerCase()}`}
    >
      {status}
    </span>
  );
}

function InfoRow({ label, children }) {
  return (
    <div className="wh-info-row">
      <span>{label}</span>
      <strong>{children ?? "Unavailable"}</strong>
    </div>
  );
}

// ======================================================
// MAIN COMPONENT - ACCOUNT DATA
// ======================================================

export default function WarehousesPage() {
  const [locations, setLocations] = useState([]);
  const [connected, setConnected] = useState(false);
  const [canManage, setCanManage] = useState(false);

  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);

  const [error, setError] = useState("");
  const [detailWarning, setDetailWarning] = useState("");
  const [notice, setNotice] = useState(null);

  // Chi hien login khi can.

  // Search and filter.
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");

  // Dialog.
  const [dialog, setDialog] = useState(null);

  const [form, setForm] = useState({
    name: "",
    type: "STORE",
    address: "",
    phone: "",
  });

  const [formError, setFormError] = useState("");

  // Employees and managers.
  const [candidates, setCandidates] = useState([]);
  const [candidateError, setCandidateError] = useState("");
  const [candidateLoading, setCandidateLoading] = useState(false);
  const [selectedUserId, setSelectedUserId] = useState("");

  // Deactivation impact.
  const [impact, setImpact] = useState(null);
  const [impactLoading, setImpactLoading] = useState(false);

  // ====================================================
  // LOAD LOCATIONS
  // ====================================================

  const loadLocations = useCallback(async () => {
    setLoading(true);
    setError("");
    setDetailWarning("");

    try {
      // Su dung session JSESSIONID dang co.
      // Khong can dang nhap lai de xac nhan quyen.
      const [first, session] = await Promise.all([
        getWarehouses(0, 100),
        getWarehouseSession(),
      ]);

      const authorities = Array.isArray(session?.authorities)
        ? session.authorities
        : [];

      const hasManagePermission =
        isAdmin(session) &&
        authorities.includes("WAREHOUSE_MANAGE");

      setCanManage(hasManagePermission);

      if (!Array.isArray(first?.content)) {
        throw new Error(
          "Unexpected warehouse list response."
        );
      }

      const pages = Number(first.totalPages || 1);

      if (pages > 20) {
        throw new Error(
          "Too many locations. A paginated view is required."
        );
      }

      const all = [...first.content];

      for (let page = 1; page < pages; page += 1) {
        const next = await getWarehouses(page, 100);

        if (!Array.isArray(next?.content)) {
          throw new Error(
            "A warehouse page could not be loaded."
          );
        }

        all.push(...next.content);
      }

      // Lay employees cua tung location.
      const loaded = await Promise.all(
        all.map(async (raw) => {
          const location = mapLocation(raw);

          try {
            const employees =
              await getWarehouseEmployees(location.id);

            if (!Array.isArray(employees)) {
              throw new Error("Invalid employee response.");
            }

            return {
              ...location,
              employees: employees.map(mapEmployee),
              employeesLoaded: true,
            };
          } catch {
            return location;
          }
        })
      );

      setLocations(loaded);
      setConnected(true);

      if (loaded.some((item) => !item.employeesLoaded)) {
        setDetailWarning(
          "Some employee lists are unavailable. Assignment actions are disabled for those locations."
        );
      }

      return true;
    } catch (err) {
      setError(messageFor(err));
      setConnected(false);
      setLocations([]);
      setCanManage(false);

      // 401: session khong hop le.
      // 403: session hop le nhung khong du quyen.

      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  // ====================================================
  // AUTO LOAD
  // ====================================================

  useEffect(() => {
    void loadLocations();
  }, [loadLocations]);

  // ====================================================
  // LOGIN FALLBACK
  // ====================================================


  // ====================================================
  // SEARCH AND FILTER
  // ====================================================

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();

    return locations.filter((item) => {
      const searchable = [
        item.code,
        item.name,
        item.address,
        item.phone,
        item.type,
      ]
        .join(" ")
        .toLowerCase();

      return (
        searchable.includes(query) &&
        (typeFilter === "ALL" || item.type === typeFilter) &&
        (statusFilter === "ALL" || item.status === statusFilter)
      );
    });
  }, [locations, search, typeFilter, statusFilter]);

  // ====================================================
  // SELECTED DATA
  // ====================================================

  const selected = locations.find(
    (item) => item.id === dialog?.warehouseId
  );

  const selectedEmployee = selected?.employees.find(
    (item) => item.id === dialog?.employeeId
  );

  const requiredRole =
    dialog?.type === "manager"
      ? managerRole(selected)
      : staffRole(selected);

  const available = candidates.filter((item) =>
    item.roles.includes(requiredRole)
  );

  // ====================================================
  // DIALOG HELPERS
  // ====================================================

  function closeDialog() {
    if (busy) return;

    setDialog(null);
    setFormError("");
    setImpact(null);
    setSelectedUserId("");
  }

  function openCreate() {
    setForm({
      name: "",
      type: "STORE",
      address: "",
      phone: "",
    });

    setFormError("");
    setDialog({ type: "create" });
  }

  function openEdit(item) {
    setForm({
      name: item.name,
      type: item.type,
      address: item.address,
      phone: item.phone,
    });

    setFormError("");

    setDialog({
      type: "edit",
      warehouseId: item.id,
    });
  }

  function openDialog(type, location, employeeId = null) {
    setFormError("");
    setSelectedUserId("");

    setDialog({
      type,
      warehouseId: location.id,
      employeeId,
    });

    if (type === "impact") {
      void readImpact(location.id);
    }

    if (type === "employee" || type === "manager") {
      void readCandidates();
    }
  }

  // ====================================================
  // DEACTIVATION IMPACT
  // ====================================================

  async function readImpact(id) {
    setImpact(null);
    setImpactLoading(true);
    setFormError("");

    try {
      const data = await getWarehouseDeactivationImpact(id);

      if (
        typeof data?.canDeactivate !== "boolean" ||
        typeof data?.totalBlockingRecords !== "number"
      ) {
        throw new Error(
          "Unexpected deactivation impact response."
        );
      }

      setImpact(data);
    } catch (err) {
      setFormError(messageFor(err));
    } finally {
      setImpactLoading(false);
    }
  }

  // ====================================================
  // AVAILABLE EMPLOYEES
  // ====================================================

  async function readCandidates() {
    setCandidateLoading(true);
    setCandidateError("");
    setCandidates([]);

    try {
      const first = await getAdminUsers(0, 100);

      if (!Array.isArray(first?.content)) {
        throw new Error("Unexpected user list response.");
      }

      const pages = Number(first.totalPages || 1);

      if (pages > 20) {
        throw new Error(
          "Too many users. Please use a filtered employee search."
        );
      }

      const users = [...first.content];

      for (let page = 1; page < pages; page += 1) {
        const next = await getAdminUsers(page, 100);

        if (!Array.isArray(next?.content)) {
          throw new Error("A user page could not be loaded.");
        }

        users.push(...next.content);
      }

      setCandidates(
        users
          .filter(
            (user) =>
              user.status === "ACTIVE" &&
              user.warehouseId == null
          )
          .map((user) => ({
            id: user.id,
            name: user.name || "",
            email: user.email || "",
            roles: Array.isArray(user.roles)
              ? user.roles.map((role) =>
                  normalizeRole(role.roleCode)
                )
              : [],
          }))
      );
    } catch (err) {
      setCandidateError(
        err?.status === 403
          ? "USER_READ permission is required to list available users."
          : messageFor(err)
      );
    } finally {
      setCandidateLoading(false);
    }
  }

  // ====================================================
  // CREATE / EDIT
  // ====================================================

  async function saveLocation(event) {
    event.preventDefault();

    if (busy || !canManage) return;

    const name = form.name.trim();
    const address = form.address.trim();
    const phone = form.phone.trim();

    if (name.length < 3 || name.length > 150) {
      setFormError(
        "Location name must contain 3-150 characters."
      );
      return;
    }

    if (address.length < 5 || address.length > 255) {
      setFormError(
        "Address must contain 5-255 characters."
      );
      return;
    }

    if (!/^\+?[0-9]{9,15}$/.test(phone)) {
      setFormError(
        "Phone must contain 9-15 digits, optionally starting with +."
      );
      return;
    }

    const duplicate = locations.some(
      (item) =>
        item.name.toLowerCase() === name.toLowerCase() &&
        item.id !== selected?.id
    );

    if (duplicate) {
      setFormError("This location name already exists.");
      return;
    }

    if (
      dialog?.type === "edit" &&
      selected &&
      name === selected.name &&
      address === selected.address &&
      phone === selected.phone
    ) {
      setFormError("No changes were made.");
      return;
    }

    setBusy(true);
    setFormError("");

    try {
      if (dialog.type === "create") {
        await createWarehouse({
          name,
          type: form.type,
          address,
          phone,
        });
      } else {
        await updateWarehouse(selected.id, {
          name,
          address,
          phone,
        });
      }

      setDialog(null);

      setNotice({
        type: "success",
        message:
          dialog.type === "create"
            ? "Location created."
            : "Location updated.",
      });

      await loadLocations();
    } catch (err) {
      setFormError(messageFor(err));
    } finally {
      setBusy(false);
    }
  }

  // ====================================================
  // ACTIVATE / DEACTIVATE
  // ====================================================

  async function changeStatus() {
    if (!selected || !canManage || busy) {
      return;
    }

    if (
      selected.status === "ACTIVE" &&
      (impactLoading || !impact || !impact.canDeactivate)
    ) {
      setFormError(
        "Deactivation is blocked or impact results are unavailable."
      );
      return;
    }

    setBusy(true);
    setFormError("");

    try {
      if (selected.status === "ACTIVE") {
        await deactivateWarehouse(selected.id);
      } else {
        await activateWarehouse(selected.id);
      }

      setDialog(null);

      setNotice({
        type: "success",
        message: "Location status updated.",
      });

      await loadLocations();
    } catch (err) {
      setFormError(messageFor(err));
    } finally {
      setBusy(false);
    }
  }

  // ====================================================
  // ASSIGN EMPLOYEE / MANAGER
  // ====================================================

  async function changeAssignment() {
    if (!selected || !canManage || busy) {
      return;
    }

    const id = Number(selectedUserId);

    const candidate = available.find(
      (item) => item.id === id
    );

    if (!candidate) {
      setFormError("Select an eligible, unassigned user.");
      return;
    }

    const isManager = dialog?.type === "manager";

    setBusy(true);
    setFormError("");

    try {
      if (isManager) {
        await assignWarehouseManager(selected.id, id);
      } else {
        await assignWarehouseEmployee(selected.id, id);
      }

      setDialog(null);

      setNotice({
        type: "success",
        message: isManager
          ? "Manager assigned."
          : "Employee assigned.",
      });

      await loadLocations();
    } catch (err) {
      setFormError(messageFor(err));
    } finally {
      setBusy(false);
    }
  }

  // ====================================================
  // UNASSIGN EMPLOYEE
  // ====================================================

  async function removeAssignment() {
    if (!selected || !selectedEmployee || !canManage || busy) {
      return;
    }

    if (
      selectedEmployee.roles.includes(
        managerRole(selected)
      )
    ) {
      setFormError(
        "A manager cannot be unassigned here. Assign a replacement manager first."
      );
      return;
    }

    setBusy(true);
    setFormError("");

    try {
      await unassignWarehouseEmployee(
        selected.id,
        selectedEmployee.id
      );

      setDialog(null);

      setNotice({
        type: "success",
        message: "Employee unassigned.",
      });

      await loadLocations();
    } catch (err) {
      setFormError(messageFor(err));
    } finally {
      setBusy(false);
    }
  }

  // ====================================================
  // MODAL TITLE
  // ====================================================

  const modalTitle = {
    create: "Create Location",
    edit: "Edit Location",
    details: "Location Details",
    impact: "Deactivation Impact",
    activate: "Activate Location",
    employee: "Assign Employee",
    manager: "Assign Manager",
    unassign: "Unassign Employee",
  }[dialog?.type];

  // ====================================================
  // RENDER
  // ====================================================

  return (
    <div className="page-stack wh-page">
      {/* HEADER */}
      <div className="page-heading wh-heading">
        <div>
          <span className="eyebrow">
            ADMINISTRATION / ISSUE #25
          </span>

          <h1>Warehouses &amp; Stores</h1>

          <p>
            Manage live locations, operations
            and employee assignments.
          </p>
        </div>

        <div className="wh-header-actions">
          <span className="demo-chip">ACCOUNT DATA</span>

          <button
            type="button"
            className="wh-button wh-button-primary"
            onClick={openCreate}
            disabled={
              !connected ||
              !canManage ||
              loading ||
              busy
            }
          >
            <Plus size={17} />
            Create Location
          </button>
        </div>
      </div>

      {/* BACKEND CONNECTION */}
      <section className="panel wh-table-panel">
        <div className="wh-section-header">
          <div>
            <h2>Account access</h2>

            <p>
              {loading
                ? "Loading locations..."
                : connected
                  ? "Live warehouse data is available"
                  : error
                    ? "Unable to load locations. See details below."
                    : "Sign in to read live data"}
            </p>
          </div>

          <div className="wh-header-actions">
            <span className="demo-chip">
              {canManage
                ? "WAREHOUSE_MANAGE ENABLED"
                : "READ ONLY / WAREHOUSE_MANAGE NOT GRANTED"}
            </span>
          </div>
        </div>

        {error && (
          <div className="wh-modal-body">
            <p className="wh-form-error" role="alert">
              {error}
            </p>
          </div>
        )}

        {detailWarning && (
          <div className="wh-modal-body">
            <p className="wh-form-error">
              {detailWarning}
            </p>
          </div>
        )}


      </section>

      {/* STATISTICS */}
      <div className="wh-stat-grid">
        {[
          [
            "Total Locations",
            locations.length,
            Building2,
          ],
          [
            "Active Locations",
            locations.filter(
              (item) => item.status === "ACTIVE"
            ).length,
            CheckCircle2,
          ],
          [
            "Main Warehouses",
            locations.filter(
              (item) => item.type === "MAIN_WAREHOUSE"
            ).length,
            Warehouse,
          ],
          [
            "Stores",
            locations.filter(
              (item) => item.type === "STORE"
            ).length,
            Store,
          ],
        ].map(([label, value, Icon]) => (
          <div className="panel wh-stat" key={label}>
            <span className="wh-stat-icon">
              <Icon size={19} />
            </span>

            <span>{label}</span>
            <strong>{value}</strong>
            <small>Current records</small>
          </div>
        ))}
      </div>

      {/* NOTIFICATION */}
      {notice && (
        <div
          className={`wh-notice ${notice.type}`}
          role="status"
        >
          {notice.type === "success" ? (
            <CheckCircle2 size={18} />
          ) : (
            <AlertTriangle size={18} />
          )}

          <span>{notice.message}</span>

          <button
            type="button"
            aria-label="Close notification"
            onClick={() => setNotice(null)}
          >
            <X size={17} />
          </button>
        </div>
      )}

      {/* LOCATION TABLE */}
      <section className="panel wh-table-panel">
        <div className="wh-section-header">
          <div>
            <h2>Location Management</h2>

            <p>
              {filtered.length} of {locations.length}
              {" "}live locations shown
            </p>
          </div>

          <div className="wh-header-actions">
            <button
              type="button"
              className="wh-button wh-button-secondary"
              onClick={() => void loadLocations()}
              disabled={loading || busy}
            >
              <RefreshCw size={15} />
              Refresh
            </button>

            <span className="demo-chip">
              ACCOUNT DATA
            </span>
          </div>
        </div>

        {/* FILTERS */}
        <div className="wh-toolbar">
          <label className="wh-search">
            <Search size={17} />

            <input
              value={search}
              onChange={(event) =>
                setSearch(event.target.value)
              }
              placeholder="Search locations..."
            />
          </label>

          <select
            aria-label="Filter by type"
            value={typeFilter}
            onChange={(event) =>
              setTypeFilter(event.target.value)
            }
          >
            <option value="ALL">All types</option>
            <option value="MAIN_WAREHOUSE">
              Main Warehouse
            </option>
            <option value="STORE">Store</option>
          </select>

          <select
            aria-label="Filter by status"
            value={statusFilter}
            onChange={(event) =>
              setStatusFilter(event.target.value)
            }
          >
            <option value="ALL">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </div>

        {/* TABLE */}
        <div className="wh-table-scroll">
          <table className="wh-table">
            <thead>
              <tr>
                <th>Location Code</th>
                <th>Location Name</th>
                <th>Type</th>
                <th>Manager</th>
                <th>Employees</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={7} className="wh-empty">
                    {loading
                      ? "Loading locations..."
                      : "No matching locations found."}
                  </td>
                </tr>
              ) : (
                filtered.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <span className="wh-code">
                        {item.code}
                      </span>
                    </td>

                    <td>
                      <div className="wh-location-cell">
                        <span className="wh-location-icon">
                          {item.type === "STORE" ? (
                            <Store size={18} />
                          ) : (
                            <Warehouse size={18} />
                          )}
                        </span>

                        <div>
                          <strong>{item.name}</strong>
                          <small>{item.address}</small>
                        </div>
                      </div>
                    </td>

                    <td>
                      <span className="wh-type">
                        {item.type === "STORE"
                          ? "Store"
                          : "Main Warehouse"}
                      </span>
                    </td>

                    <td>
                      {!item.employeesLoaded
                        ? "Unavailable"
                        : managerOf(item)?.name ||
                          "Not assigned"}
                    </td>

                    <td>
                      <span className="wh-count">
                        <Users size={15} />

                        {item.employeesLoaded
                          ? item.employees.length
                          : "—"}
                      </span>
                    </td>

                    <td>
                      <StatusBadge status={item.status} />
                    </td>

                    <td>
                      <div className="wh-actions">
                        {/* VIEW */}
                        <button
                          type="button"
                          title="View Details"
                          aria-label={`View ${item.name}`}
                          onClick={() =>
                            openDialog("details", item)
                          }
                        >
                          <Eye size={16} />
                        </button>

                        {/* EDIT */}
                        <button
                          type="button"
                          title="Edit Location"
                          disabled={!canManage || busy}
                          onClick={() => openEdit(item)}
                        >
                          <Pencil size={16} />
                        </button>

                        {/* EMPLOYEES */}
                        <button
                          type="button"
                          title="Manage Employees"
                          disabled={!item.employeesLoaded}
                          onClick={() =>
                            openDialog("details", item)
                          }
                        >
                          <Users size={16} />
                        </button>

                        {/* MANAGER */}
                        <button
                          type="button"
                          title="Assign Manager"
                          disabled={
                            !canManage ||
                            !item.employeesLoaded ||
                            item.status !== "ACTIVE" ||
                            busy
                          }
                          onClick={() =>
                            openDialog("manager", item)
                          }
                        >
                          <ShieldCheck size={16} />
                        </button>

                        {/* STATUS / IMPACT */}
                        <button
                          type="button"
                          title={
                            item.status === "ACTIVE"
                              ? "Check Deactivation Impact"
                              : "Activate Location"
                          }
                          disabled={
                            busy ||
                            (
                              item.status === "INACTIVE" &&
                              !canManage
                            )
                          }
                          onClick={() =>
                            openDialog(
                              item.status === "ACTIVE"
                                ? "impact"
                                : "activate",
                              item
                            )
                          }
                        >
                          <Power size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="wh-table-footer">
          <Database size={15} />
          Current records. No location delete
          endpoint is available.
        </div>
      </section>

      {/* MODALS */}
      {dialog && (
        <div
          className="wh-overlay"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeDialog();
            }
          }}
        >
          <section
            className="wh-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="wh-dialog-title"
          >
            {/* MODAL HEADER */}
            <div className="wh-modal-header">
              <div>
                <span className="eyebrow">
                  ISSUE #25 · ACCOUNT DATA
                </span>

                <h2 id="wh-dialog-title">
                  {modalTitle}
                </h2>
              </div>

              <button
                type="button"
                className="wh-close"
                onClick={closeDialog}
                disabled={busy}
                aria-label="Close dialog"
              >
                <X size={20} />
              </button>
            </div>

            {/* CREATE / EDIT */}
            {(
              dialog.type === "create" ||
              dialog.type === "edit"
            ) && (
              <form
                className="wh-modal-body"
                onSubmit={saveLocation}
              >
                <div className="wh-form-grid">
                  <label className="wh-field">
                    Location Name *

                    <input
                      required
                      minLength={3}
                      maxLength={150}
                      value={form.name}
                      onChange={(event) =>
                        setForm((previous) => ({
                          ...previous,
                          name: event.target.value,
                        }))
                      }
                    />
                  </label>

                  <label className="wh-field">
                    Location Type *

                    <select
                      value={form.type}
                      disabled={dialog.type === "edit"}
                      onChange={(event) =>
                        setForm((previous) => ({
                          ...previous,
                          type: event.target.value,
                        }))
                      }
                    >
                      <option value="MAIN_WAREHOUSE">
                        Main Warehouse
                      </option>
                      <option value="STORE">
                        Store
                      </option>
                    </select>
                  </label>

                  <label className="wh-field wh-field-full">
                    Address *

                    <input
                      required
                      minLength={5}
                      maxLength={255}
                      value={form.address}
                      onChange={(event) =>
                        setForm((previous) => ({
                          ...previous,
                          address: event.target.value,
                        }))
                      }
                    />
                  </label>

                  <label className="wh-field wh-field-full">
                    Phone Number *

                    <input
                      required
                      type="tel"
                      placeholder="e.g. 0901234567"
                      value={form.phone}
                      onChange={(event) =>
                        setForm((previous) => ({
                          ...previous,
                          phone: event.target.value,
                        }))
                      }
                    />
                  </label>
                </div>

                <></>

                {formError && (
                  <p
                    className="wh-form-error"
                    role="alert"
                  >
                    {formError}
                  </p>
                )}

                <div className="wh-modal-footer">
                  <button
                    type="button"
                    className="wh-button wh-button-secondary"
                    disabled={busy}
                    onClick={closeDialog}
                  >
                    Cancel
                  </button>

                  <button
                    type="submit"
                    className="wh-button wh-button-primary"
                    disabled={busy}
                  >
                    {busy
                      ? "Saving..."
                      : dialog.type === "create"
                        ? "Create Location"
                        : "Save Changes"}
                  </button>
                </div>
              </form>
            )}

            {/* DETAILS */}
            {dialog.type === "details" && selected && (
              <div className="wh-modal-body">
                <InfoRow label="Location Code">
                  {selected.code}
                </InfoRow>

                <InfoRow label="Location Name">
                  {selected.name}
                </InfoRow>

                <InfoRow label="Type">
                  {selected.type === "STORE"
                    ? "Store"
                    : "Main Warehouse"}
                </InfoRow>

                <InfoRow label="Address">
                  {selected.address}
                </InfoRow>

                <InfoRow label="Phone">
                  {selected.phone || "Not provided"}
                </InfoRow>

                <InfoRow label="Manager">
                  {selected.employeesLoaded
                    ? managerOf(selected)?.name ||
                      "Not assigned"
                    : "Unavailable"}
                </InfoRow>

                <InfoRow label="Status">
                  {selected.status}
                </InfoRow>

                <div className="wh-employees-header">
                  <div>
                    <h3>Assigned Employees</h3>

                    <p>
                      {selected.employeesLoaded
                        ? `${selected.employees.length} employees assigned`
                        : "Employee list unavailable"}
                    </p>
                  </div>

                  <button
                    type="button"
                    className="wh-button wh-button-secondary"
                    disabled={
                      !canManage ||
                      !selected.employeesLoaded ||
                      selected.status !== "ACTIVE"
                    }
                    onClick={() =>
                      openDialog(
                        "employee",
                        selected
                      )
                    }
                  >
                    <UserPlus size={15} />
                    Assign Employee
                  </button>
                </div>

                <div className="wh-employee-list">
                  {selected.employees.length === 0 ? (
                    <p className="wh-empty-employees">
                      {selected.employeesLoaded
                        ? "No employees assigned."
                        : "Unable to load employees."}
                    </p>
                  ) : (
                    selected.employees.map((user) => (
                      <div
                        className="wh-employee-row"
                        key={user.id}
                      >
                        <div>
                          <strong>{user.name}</strong>

                          <small>
                            {user.roles.join(", ") ||
                              "No role"}
                          </small>
                        </div>

                        {user.roles.includes(
                          managerRole(selected)
                        ) ? (
                          <span className="wh-manager-tag">
                            Manager
                          </span>
                        ) : (
                          <button
                            type="button"
                            className="wh-small-action"
                            disabled={!canManage}
                            onClick={() =>
                              openDialog(
                                "unassign",
                                selected,
                                user.id
                              )
                            }
                          >
                            <UserMinus size={15} />
                            Unassign
                          </button>
                        )}
                      </div>
                    ))
                  )}
                </div>

                <div className="wh-modal-footer">
                  <button
                    type="button"
                    className="wh-button wh-button-secondary"
                    onClick={closeDialog}
                  >
                    Close
                  </button>

                  <button
                    type="button"
                    className="wh-button wh-button-primary"
                    disabled={!canManage}
                    onClick={() =>
                      openEdit(selected)
                    }
                  >
                    <Pencil size={15} />
                    Edit Location
                  </button>
                </div>
              </div>
            )}

            {/* DEACTIVATION IMPACT */}
            {dialog.type === "impact" && selected && (
              <div className="wh-modal-body">
                <p className="wh-confirm-text">
                  Review open business operations
                  before deactivating{" "}
                  <strong>{selected.name}</strong>.
                </p>

                {impactLoading && (
                  <></>
                )}

                {impact && (
                  <>
                    <div className="wh-impact-list">
                      {Object.entries(
                        impact.blockingByModule || {}
                      ).length === 0 ? (
                        <div className="wh-impact-row">
                          <span>
                            <ClipboardList size={16} />
                            No blocking modules
                          </span>

                          <strong>0</strong>
                        </div>
                      ) : (
                        Object.entries(
                          impact.blockingByModule
                        ).map(([module, count]) => (
                          <div
                            className="wh-impact-row"
                            key={module}
                          >
                            <span>
                              <ClipboardList size={16} />

                              {module.replaceAll(
                                "_",
                                " "
                              )}
                            </span>

                            <strong>{count}</strong>
                          </div>
                        ))
                      )}
                    </div>

                    <div
                      className={`wh-impact-result ${
                        impact.canDeactivate
                          ? "clear"
                          : "blocked"
                      }`}
                    >
                      {impact.canDeactivate ? (
                        <CheckCircle2 size={19} />
                      ) : (
                        <AlertTriangle size={19} />
                      )}

                      <span>
                        {impact.canDeactivate
                          ? "No blocking operations. Deactivation is allowed by the impact check."
                          : `${impact.totalBlockingRecords} blocking record(s). Resolve them before deactivation.`}
                      </span>
                    </div>
                  </>
                )}

                {formError && (
                  <p
                    className="wh-form-error"
                    role="alert"
                  >
                    {formError}
                  </p>
                )}

                <div className="wh-modal-footer">
                  <button
                    type="button"
                    className="wh-button wh-button-secondary"
                    onClick={closeDialog}
                    disabled={busy}
                  >
                    Cancel
                  </button>

                  <button
                    type="button"
                    className="wh-button wh-button-danger"
                    disabled={
                      !canManage ||
                      busy ||
                      impactLoading ||
                      !impact?.canDeactivate
                    }
                    onClick={() =>
                      void changeStatus()
                    }
                  >
                    {busy
                      ? "Processing..."
                      : "Deactivate Location"}
                  </button>
                </div>
              </div>
            )}

            {/* ACTIVATE */}
            {dialog.type === "activate" && selected && (
              <div className="wh-modal-body">
                <p className="wh-confirm-text">
                  Activate{" "}
                  <strong>{selected.name}</strong>?
                </p>

                <></>

                {formError && (
                  <p
                    className="wh-form-error"
                    role="alert"
                  >
                    {formError}
                  </p>
                )}

                <div className="wh-modal-footer">
                  <button
                    type="button"
                    className="wh-button wh-button-secondary"
                    onClick={closeDialog}
                    disabled={busy}
                  >
                    Cancel
                  </button>

                  <button
                    type="button"
                    className="wh-button wh-button-primary"
                    disabled={busy}
                    onClick={() =>
                      void changeStatus()
                    }
                  >
                    {busy
                      ? "Processing..."
                      : "Activate Location"}
                  </button>
                </div>
              </div>
            )}

            {/* ASSIGN EMPLOYEE / MANAGER */}
            {(
              dialog.type === "employee" ||
              dialog.type === "manager"
            ) && selected && (
              <div className="wh-modal-body">
                <p className="wh-confirm-text">
                  Select an unassigned{" "}
                  {dialog.type === "manager"
                    ? "manager"
                    : "employee"}{" "}
                  for{" "}
                  <strong>{selected.name}</strong>.
                </p>

                <label className="wh-field">
                  {dialog.type === "manager"
                    ? "Available Managers"
                    : "Available Employees"}

                  <select
                    value={selectedUserId}
                    disabled={
                      candidateLoading ||
                      busy ||
                      Boolean(candidateError)
                    }
                    onChange={(event) => {
                      setSelectedUserId(
                        event.target.value
                      );
                      setFormError("");
                    }}
                  >
                    <option value="">
                      Select a person...
                    </option>

                    {available.map((user) => (
                      <option
                        key={user.id}
                        value={user.id}
                      >
                        {user.name} ({user.email})
                      </option>
                    ))}
                  </select>
                </label>

                <></>

                {candidateLoading && (
                  <p className="wh-note">
                    Loading eligible users...
                  </p>
                )}

                {candidateError && (
                  <p className="wh-form-error">
                    {candidateError}
                  </p>
                )}

                {!candidateLoading &&
                  !candidateError &&
                  available.length === 0 && (
                    <p className="wh-note">
                      No eligible users available.
                    </p>
                  )}

                {formError && (
                  <p
                    className="wh-form-error"
                    role="alert"
                  >
                    {formError}
                  </p>
                )}

                <div className="wh-modal-footer">
                  <button
                    type="button"
                    className="wh-button wh-button-secondary"
                    disabled={busy}
                    onClick={closeDialog}
                  >
                    Cancel
                  </button>

                  <button
                    type="button"
                    className="wh-button wh-button-primary"
                    disabled={
                      busy ||
                      candidateLoading ||
                      Boolean(candidateError) ||
                      !selectedUserId
                    }
                    onClick={() =>
                      void changeAssignment()
                    }
                  >
                    {busy
                      ? "Saving..."
                      : dialog.type === "manager"
                        ? "Save Manager"
                        : "Assign Employee"}
                  </button>
                </div>
              </div>
            )}

            {/* UNASSIGN EMPLOYEE */}
            {dialog.type === "unassign" && selected && (
              <div className="wh-modal-body">
                <p className="wh-confirm-text">
                  Unassign{" "}
                  <strong>
                    {selectedEmployee?.name ||
                      "this employee"}
                  </strong>{" "}
                  from{" "}
                  <strong>{selected.name}</strong>?
                </p>

                <></>

                {formError && (
                  <p
                    className="wh-form-error"
                    role="alert"
                  >
                    {formError}
                  </p>
                )}

                <div className="wh-modal-footer">
                  <button
                    type="button"
                    className="wh-button wh-button-secondary"
                    disabled={busy}
                    onClick={closeDialog}
                  >
                    Cancel
                  </button>

                  <button
                    type="button"
                    className="wh-button wh-button-danger"
                    disabled={
                      busy ||
                      !selectedEmployee
                    }
                    onClick={() =>
                      void removeAssignment()
                    }
                  >
                    {busy
                      ? "Saving..."
                      : "Unassign Employee"}
                  </button>
                </div>
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}

