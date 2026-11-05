
import { useMemo, useState } from "react";
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

import "./css/WarehousesPage.css";

// Issue #25 - Demo UI only.
// No Backend API or database calls are made.

const EMPTY_FORM = {
  name: "",
  type: "MAIN_WAREHOUSE",
  address: "",
  phone: "",
};

const PENDING_FIELDS = [
  ["purchaseOrders", "Open Purchase Orders"],
  ["goodsReceipts", "Open Goods Receipts"],
  ["stockTransfers", "Open Stock Transfers"],
  ["shipments", "Open Shipments"],
];

function pendingCount(location) {
  return Object.values(location?.pending || {}).reduce(
    (total, value) => total + value,
    0
  );
}

function nextLocationCode(locations, type) {
  const prefix = type === "MAIN_WAREHOUSE" ? "WH" : "ST";

  let number =
    locations.filter((item) => item.type === type).length + 1;

  let code = `${prefix}-${String(number).padStart(3, "0")}`;

  while (locations.some((item) => item.code === code)) {
    number += 1;
    code = `${prefix}-${String(number).padStart(3, "0")}`;
  }

  return code;
}

function StatusBadge({ status }) {
  return (
    <span className={`wh-status ${status.toLowerCase()}`}>
      {status}
    </span>
  );
}

function InfoRow({ label, children }) {
  return (
    <div className="wh-info-row">
      <span>{label}</span>
      <strong>{children || "Not assigned"}</strong>
    </div>
  );
}

export default function WarehousesPage({
  warehouses,
  setWarehouses,
  employees,
  setEmployees,
  onReset,
}) {
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");

  const [dialog, setDialog] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState("");
  const [selectedEmployeeId, setSelectedEmployeeId] = useState("");
  const [notice, setNotice] = useState(null);

  const filteredWarehouses = useMemo(() => {
    const keyword = search.trim().toLowerCase();

    return warehouses.filter((location) => {
      const searchableText = [
        location.code,
        location.name,
        location.address,
        location.phone,
        location.type,
      ]
        .join(" ")
        .toLowerCase();

      return (
        searchableText.includes(keyword) &&
        (typeFilter === "ALL" ||
          location.type === typeFilter) &&
        (statusFilter === "ALL" ||
          location.status === statusFilter)
      );
    });
  }, [warehouses, search, typeFilter, statusFilter]);

  const selectedWarehouse = warehouses.find(
    (item) => item.id === dialog?.warehouseId
  );

  const getManager = (location) =>
    employees.find((item) => item.id === location.managerId);

  const getAssignedEmployees = (location) =>
    employees.filter((item) => item.warehouseId === location.id);

  const requiredStaffRole =
    selectedWarehouse?.type === "MAIN_WAREHOUSE"
      ? "WH_STAFF"
      : "STORE_STAFF";

  const requiredManagerRole =
    selectedWarehouse?.type === "MAIN_WAREHOUSE"
      ? "WH_MANAGER"
      : "STORE_MANAGER";

  const availableStaff = employees.filter(
    (item) =>
      item.warehouseId === null &&
      item.role === requiredStaffRole
  );

  const availableManagers = employees.filter(
    (item) =>
      item.warehouseId === null &&
      item.role === requiredManagerRole
  );

  const total = warehouses.length;
  const active = warehouses.filter(
    (item) => item.status === "ACTIVE"
  ).length;
  const mainCount = warehouses.filter(
    (item) => item.type === "MAIN_WAREHOUSE"
  ).length;
  const storeCount = warehouses.filter(
    (item) => item.type === "STORE"
  ).length;

  function showNotice(type, message) {
    setNotice({ type, message });
  }

  function closeDialog() {
    setDialog(null);
    setFormError("");
    setSelectedEmployeeId("");
  }

  function openDialog(type, warehouseId, employeeId = null) {
    setFormError("");
    setSelectedEmployeeId("");
    setDialog({ type, warehouseId, employeeId });
  }

  function openCreate() {
    setForm({ ...EMPTY_FORM });
    setFormError("");
    setDialog({ type: "create" });
  }

  function openEdit(location) {
    setForm({
      name: location.name,
      type: location.type,
      address: location.address,
      phone: location.phone,
    });

    setFormError("");
    setDialog({
      type: "edit",
      warehouseId: location.id,
    });
  }

  function updateForm(field, value) {
    setForm((previous) => ({
      ...previous,
      [field]: value,
    }));

    setFormError("");
  }

  // Create or update a demo location.
  function handleSaveLocation(event) {
    event.preventDefault();

    const name = form.name.trim();
    const address = form.address.trim();
    const phone = form.phone.trim();

    if (name.length < 3) {
      setFormError(
        "Location name must contain at least 3 characters."
      );
      return;
    }

    if (address.length < 5) {
      setFormError(
        "Address must contain at least 5 characters."
      );
      return;
    }

    if (
      phone &&
      !/^[+()0-9 .-]{8,20}$/.test(phone)
    ) {
      setFormError("Please enter a valid phone number.");
      return;
    }

    const isDuplicate = warehouses.some(
      (item) =>
        item.name.toLowerCase() === name.toLowerCase() &&
        item.id !== selectedWarehouse?.id
    );

    if (isDuplicate) {
      setFormError("This location name already exists.");
      return;
    }

    if (dialog.type === "create") {
      const newId =
        Math.max(0, ...warehouses.map((item) => item.id)) + 1;

      const newLocation = {
        id: newId,
        code: nextLocationCode(warehouses, form.type),
        name,
        type: form.type,
        address,
        phone,
        status: "ACTIVE",
        managerId: null,
        pending: {
          purchaseOrders: 0,
          goodsReceipts: 0,
          stockTransfers: 0,
          shipments: 0,
        },
      };

      setWarehouses((previous) => [
        ...previous,
        newLocation,
      ]);

      showNotice(
        "success",
        "Demo location created successfully."
      );
    } else {
      setWarehouses((previous) =>
        previous.map((item) =>
          item.id === selectedWarehouse.id
            ? {
                ...item,
                name,
                address,
                phone,
              }
            : item
        )
      );

      showNotice(
        "success",
        "Demo location updated successfully."
      );
    }

    closeDialog();
  }

  // Activate or deactivate after impact validation.
  function changeStatus() {
    if (!selectedWarehouse) return;

    if (
      selectedWarehouse.status === "ACTIVE" &&
      pendingCount(selectedWarehouse) > 0
    ) {
      showNotice(
        "error",
        "Cannot deactivate this location while open operations exist."
      );
      return;
    }

    const newStatus =
      selectedWarehouse.status === "ACTIVE"
        ? "INACTIVE"
        : "ACTIVE";

    setWarehouses((previous) =>
      previous.map((item) =>
        item.id === selectedWarehouse.id
          ? { ...item, status: newStatus }
          : item
      )
    );

    closeDialog();

    showNotice(
      "success",
      `Demo location status changed to ${newStatus}.`
    );
  }

  // Assign an unassigned staff member.
  function assignEmployee() {
    if (!selectedWarehouse || !selectedEmployeeId) {
      setFormError("Please select an employee.");
      return;
    }

    if (selectedWarehouse.status !== "ACTIVE") {
      setFormError(
        "Only active locations can receive employee assignments."
      );
      return;
    }

    const employee = employees.find(
      (item) => item.id === Number(selectedEmployeeId)
    );

    if (
      !employee ||
      employee.warehouseId !== null ||
      employee.role !== requiredStaffRole
    ) {
      setFormError("The selected employee is not available.");
      return;
    }

    setEmployees((previous) =>
      previous.map((item) =>
        item.id === employee.id
          ? {
              ...item,
              warehouseId: selectedWarehouse.id,
            }
          : item
      )
    );

    closeDialog();

    showNotice(
      "success",
      "Demo employee assigned successfully."
    );
  }

  // Remove staff assignment, but not a location manager.
  function unassignEmployee() {
    if (!selectedWarehouse) return;

    const employee = employees.find(
      (item) => item.id === dialog.employeeId
    );

    if (
      !employee ||
      employee.warehouseId !== selectedWarehouse.id
    ) {
      setFormError("Employee assignment was not found.");
      return;
    }

    if (selectedWarehouse.managerId === employee.id) {
      setFormError(
        "A location manager must be replaced before removal."
      );
      return;
    }

    setEmployees((previous) =>
      previous.map((item) =>
        item.id === employee.id
          ? { ...item, warehouseId: null }
          : item
      )
    );

    closeDialog();

    showNotice(
      "success",
      "Demo employee unassigned successfully."
    );
  }

  // Replace an existing manager with an available manager.
  function assignManager() {
    if (!selectedWarehouse || !selectedEmployeeId) {
      setFormError("Please select a manager.");
      return;
    }

    if (selectedWarehouse.status !== "ACTIVE") {
      setFormError(
        "Only active locations can receive manager assignments."
      );
      return;
    }

    const newManagerId = Number(selectedEmployeeId);

    const newManager = employees.find(
      (item) => item.id === newManagerId
    );

    if (
      !newManager ||
      newManager.warehouseId !== null ||
      newManager.role !== requiredManagerRole
    ) {
      setFormError("The selected manager is not available.");
      return;
    }

    const oldManagerId = selectedWarehouse.managerId;

    setEmployees((previous) =>
      previous.map((item) => {
        if (item.id === oldManagerId) {
          return { ...item, warehouseId: null };
        }

        if (item.id === newManagerId) {
          return {
            ...item,
            warehouseId: selectedWarehouse.id,
          };
        }

        return item;
      })
    );

    setWarehouses((previous) =>
      previous.map((item) =>
        item.id === selectedWarehouse.id
          ? { ...item, managerId: newManagerId }
          : item
      )
    );

    closeDialog();

    showNotice(
      "success",
      "Demo location manager updated successfully."
    );
  }

  function resetDemo() {
    onReset();
    setSearch("");
    setTypeFilter("ALL");
    setStatusFilter("ALL");
    closeDialog();

    showNotice(
      "success",
      "Demo locations and assignments restored successfully."
    );
  }

  const dialogTitle = {
    create: "Create Location",
    edit: "Edit Location",
    details: "Location Details",
    impact: "Deactivation Impact",
    activate: "Activate Location",
    employee: "Assign Employee",
    manager: "Assign Manager",
    unassign: "Unassign Employee",
  }[dialog?.type];

  return (
    <div className="page-stack wh-page">
      {/* Page heading */}
      <div className="page-heading wh-heading">
        <div>
          <span className="eyebrow">
            ADMINISTRATION / ISSUE #25
          </span>
          <h1>Warehouses & Stores</h1>
          <p>
            Manage locations, operational status,
            managers and employee assignments.
          </p>
        </div>

        <button
          className="wh-button wh-button-primary"
          type="button"
          onClick={openCreate}
        >
          <Plus size={17} />
          Create Location
        </button>
      </div>

      {/* Summary cards */}
      <div className="wh-stat-grid">
        {[
          ["Total Locations", total, Building2],
          ["Active Locations", active, CheckCircle2],
          ["Main Warehouses", mainCount, Warehouse],
          ["Stores", storeCount, Store],
        ].map(([label, value, Icon]) => (
          <div className="panel wh-stat" key={label}>
            <span className="wh-stat-icon">
              <Icon size={19} />
            </span>
            <span>{label}</span>
            <strong>{value}</strong>
            <small>Demo locations</small>
          </div>
        ))}
      </div>

      {/* Notification */}
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
            onClick={() => setNotice(null)}
            aria-label="Close notification"
          >
            <X size={17} />
          </button>
        </div>
      )}

      {/* Locations table */}
      <section className="panel wh-table-panel">
        <div className="wh-section-header">
          <div>
            <h2>Location Management</h2>
            <p>
              {filteredWarehouses.length} of {warehouses.length}
              {" "}sample locations shown
            </p>
          </div>

          <div className="wh-header-actions">
            <button
              type="button"
              className="wh-button wh-button-secondary"
              onClick={resetDemo}
            >
              <RefreshCw size={15} />
              Reset Demo
            </button>
            <span className="demo-chip">DEMO DATA</span>
          </div>
        </div>

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
            aria-label="Filter by location type"
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
              {filteredWarehouses.length === 0 ? (
                <tr>
                  <td colSpan={7} className="wh-empty">
                    No matching locations found.
                  </td>
                </tr>
              ) : (
                filteredWarehouses.map((location) => (
                  <tr key={location.id}>
                    <td>
                      <span className="wh-code">
                        {location.code}
                      </span>
                    </td>

                    <td>
                      <div className="wh-location-cell">
                        <span className="wh-location-icon">
                          {location.type === "STORE" ? (
                            <Store size={18} />
                          ) : (
                            <Warehouse size={18} />
                          )}
                        </span>

                        <div>
                          <strong>{location.name}</strong>
                          <small>{location.address}</small>
                        </div>
                      </div>
                    </td>

                    <td>
                      <span className="wh-type">
                        {location.type === "STORE"
                          ? "Store"
                          : "Main Warehouse"}
                      </span>
                    </td>

                    <td>
                      {getManager(location)?.name || "Not assigned"}
                    </td>

                    <td>
                      <span className="wh-count">
                        <Users size={15} />
                        {getAssignedEmployees(location).length}
                      </span>
                    </td>

                    <td>
                      <StatusBadge status={location.status} />
                    </td>

                    <td>
                      <div className="wh-actions">
                        <button
                          type="button"
                          title="View Details"
                          aria-label={`View ${location.name}`}
                          onClick={() =>
                            openDialog("details", location.id)
                          }
                        >
                          <Eye size={16} />
                        </button>

                        <button
                          type="button"
                          title="Edit Location"
                          aria-label={`Edit ${location.name}`}
                          onClick={() => openEdit(location)}
                        >
                          <Pencil size={16} />
                        </button>

                        <button
                          type="button"
                          title="Manage Employees"
                          aria-label={`Manage employees at ${location.name}`}
                          onClick={() =>
                            openDialog("details", location.id)
                          }
                        >
                          <Users size={16} />
                        </button>

                        <button
                          type="button"
                          title="Assign Manager"
                          disabled={location.status !== "ACTIVE"}
                          aria-label={`Assign manager at ${location.name}`}
                          onClick={() =>
                            openDialog("manager", location.id)
                          }
                        >
                          <ShieldCheck size={16} />
                        </button>

                        <button
                          type="button"
                          title={
                            location.status === "ACTIVE"
                              ? "Check Deactivation Impact"
                              : "Activate Location"
                          }
                          aria-label={`Change status of ${location.name}`}
                          onClick={() =>
                            openDialog(
                              location.status === "ACTIVE"
                                ? "impact"
                                : "activate",
                              location.id
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
          UI preview only. No Backend API requests are made.
        </div>
      </section>

      {/* Dialog */}
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
            <div className="wh-modal-header">
              <div>
                <span className="eyebrow">ISSUE #25</span>
                <h2 id="wh-dialog-title">{dialogTitle}</h2>
              </div>

              <button
                type="button"
                className="wh-close"
                onClick={closeDialog}
                aria-label="Close dialog"
              >
                <X size={20} />
              </button>
            </div>

            {/* Create / Edit */}
            {(dialog.type === "create" ||
              dialog.type === "edit") && (
              <form
                className="wh-modal-body"
                onSubmit={handleSaveLocation}
              >
                <div className="wh-form-grid">
                  <label className="wh-field">
                    Location Name *
                    <input
                      required
                      minLength={3}
                      maxLength={120}
                      value={form.name}
                      placeholder="Enter location name"
                      onChange={(event) =>
                        updateForm("name", event.target.value)
                      }
                    />
                  </label>

                  <label className="wh-field">
                    Location Type *
                    <select
                      value={form.type}
                      disabled={dialog.type === "edit"}
                      onChange={(event) =>
                        updateForm("type", event.target.value)
                      }
                    >
                      <option value="MAIN_WAREHOUSE">
                        Main Warehouse
                      </option>
                      <option value="STORE">Store</option>
                    </select>
                  </label>

                  <label className="wh-field wh-field-full">
                    Address *
                    <input
                      required
                      minLength={5}
                      maxLength={255}
                      value={form.address}
                      placeholder="Enter location address"
                      onChange={(event) =>
                        updateForm("address", event.target.value)
                      }
                    />
                  </label>

                  <label className="wh-field wh-field-full">
                    Phone Number
                    <input
                      type="tel"
                      value={form.phone}
                      placeholder="Enter phone number"
                      onChange={(event) =>
                        updateForm("phone", event.target.value)
                      }
                    />
                  </label>
                </div>

                <p className="wh-note">
                  Location codes are generated automatically.
                  All changes are stored in demo state only.
                </p>

                {formError && (
                  <p className="wh-form-error">{formError}</p>
                )}

                <div className="wh-modal-footer">
                  <button
                    type="button"
                    className="wh-button wh-button-secondary"
                    onClick={closeDialog}
                  >
                    Cancel
                  </button>

                  <button
                    type="submit"
                    className="wh-button wh-button-primary"
                  >
                    {dialog.type === "create"
                      ? "Create Location"
                      : "Save Changes"}
                  </button>
                </div>
              </form>
            )}

            {/* Location details and employees */}
            {dialog.type === "details" &&
              selectedWarehouse && (
                <div className="wh-modal-body">
                  <InfoRow label="Location Code">
                    {selectedWarehouse.code}
                  </InfoRow>

                  <InfoRow label="Location Name">
                    {selectedWarehouse.name}
                  </InfoRow>

                  <InfoRow label="Type">
                    {selectedWarehouse.type === "STORE"
                      ? "Store"
                      : "Main Warehouse"}
                  </InfoRow>

                  <InfoRow label="Address">
                    {selectedWarehouse.address}
                  </InfoRow>

                  <InfoRow label="Phone">
                    {selectedWarehouse.phone || "Not provided"}
                  </InfoRow>

                  <InfoRow label="Manager">
                    {getManager(selectedWarehouse)?.name ||
                      "Not assigned"}
                  </InfoRow>

                  <InfoRow label="Status">
                    {selectedWarehouse.status}
                  </InfoRow>

                  <div className="wh-employees-header">
                    <div>
                      <h3>Assigned Employees</h3>
                      <p>
                        {getAssignedEmployees(selectedWarehouse).length}
                        {" "}employees assigned
                      </p>
                    </div>

                    <button
                      type="button"
                      className="wh-button wh-button-secondary"
                      disabled={selectedWarehouse.status !== "ACTIVE"}
                      onClick={() =>
                        openDialog("employee", selectedWarehouse.id)
                      }
                    >
                      <UserPlus size={15} />
                      Assign Employee
                    </button>
                  </div>

                  <div className="wh-employee-list">
                    {getAssignedEmployees(selectedWarehouse).length ===
                    0 ? (
                      <p className="wh-empty-employees">
                        No employees assigned.
                      </p>
                    ) : (
                      getAssignedEmployees(selectedWarehouse).map(
                        (employee) => (
                          <div
                            className="wh-employee-row"
                            key={employee.id}
                          >
                            <div>
                              <strong>{employee.name}</strong>
                              <small>{employee.role}</small>
                            </div>

                            {employee.id ===
                            selectedWarehouse.managerId ? (
                              <span className="wh-manager-tag">
                                Manager
                              </span>
                            ) : (
                              <button
                                type="button"
                                className="wh-small-action"
                                onClick={() =>
                                  openDialog(
                                    "unassign",
                                    selectedWarehouse.id,
                                    employee.id
                                  )
                                }
                              >
                                <UserMinus size={15} />
                                Unassign
                              </button>
                            )}
                          </div>
                        )
                      )
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
                      onClick={() => openEdit(selectedWarehouse)}
                    >
                      <Pencil size={15} />
                      Edit Location
                    </button>
                  </div>
                </div>
              )}

            {/* Deactivation impact */}
            {dialog.type === "impact" &&
              selectedWarehouse && (
                <div className="wh-modal-body">
                  <p className="wh-confirm-text">
                    Review the operational impact before deactivating
                    {" "}
                    <strong>{selectedWarehouse.name}</strong>.
                  </p>

                  <div className="wh-impact-list">
                    {PENDING_FIELDS.map(([key, label]) => (
                      <div className="wh-impact-row" key={key}>
                        <span>
                          <ClipboardList size={16} />
                          {label}
                        </span>
                        <strong>
                          {selectedWarehouse.pending[key] || 0}
                        </strong>
                      </div>
                    ))}
                  </div>

                  <div
                    className={`wh-impact-result ${
                      pendingCount(selectedWarehouse) > 0
                        ? "blocked"
                        : "clear"
                    }`}
                  >
                    {pendingCount(selectedWarehouse) > 0 ? (
                      <>
                        <AlertTriangle size={19} />
                        <span>
                          Deactivation blocked:{" "}
                          {pendingCount(selectedWarehouse)}
                          {" "}open operations must be resolved first.
                        </span>
                      </>
                    ) : (
                      <>
                        <CheckCircle2 size={19} />
                        <span>
                          No blocking operations found in demo data.
                          The location can be deactivated.
                        </span>
                      </>
                    )}
                  </div>

                  <div className="wh-modal-footer">
                    <button
                      type="button"
                      className="wh-button wh-button-secondary"
                      onClick={closeDialog}
                    >
                      Cancel
                    </button>

                    <button
                      type="button"
                      className="wh-button wh-button-danger"
                      disabled={pendingCount(selectedWarehouse) > 0}
                      onClick={changeStatus}
                    >
                      Deactivate Location
                    </button>
                  </div>
                </div>
              )}

            {/* Activate location */}
            {dialog.type === "activate" &&
              selectedWarehouse && (
                <div className="wh-modal-body">
                  <p className="wh-confirm-text">
                    Activate{" "}
                    <strong>{selectedWarehouse.name}</strong>?
                  </p>

                  <p className="wh-note">
                    This action only updates the demo status.
                  </p>

                  <div className="wh-modal-footer">
                    <button
                      type="button"
                      className="wh-button wh-button-secondary"
                      onClick={closeDialog}
                    >
                      Cancel
                    </button>

                    <button
                      type="button"
                      className="wh-button wh-button-primary"
                      onClick={changeStatus}
                    >
                      Activate Location
                    </button>
                  </div>
                </div>
              )}

            {/* Assign employee / manager */}
            {(dialog.type === "employee" ||
              dialog.type === "manager") &&
              selectedWarehouse && (
                <div className="wh-modal-body">
                  <p className="wh-confirm-text">
                    {dialog.type === "manager"
                      ? "Select an available manager for"
                      : "Select an available employee for"}
                    {" "}
                    <strong>{selectedWarehouse.name}</strong>.
                  </p>

                  <label className="wh-field">
                    {dialog.type === "manager"
                      ? "Available Managers"
                      : "Available Employees"}

                    <select
                      value={selectedEmployeeId}
                      onChange={(event) => {
                        setSelectedEmployeeId(event.target.value);
                        setFormError("");
                      }}
                    >
                      <option value="">
                        Select a person...
                      </option>

                      {(dialog.type === "manager"
                        ? availableManagers
                        : availableStaff
                      ).map((employee) => (
                        <option
                          key={employee.id}
                          value={employee.id}
                        >
                          {employee.name} ({employee.role})
                        </option>
                      ))}
                    </select>
                  </label>

                  <p className="wh-note">
                    Only unassigned people with a matching
                    warehouse or store role are listed.
                  </p>

                  {formError && (
                    <p className="wh-form-error">{formError}</p>
                  )}

                  <div className="wh-modal-footer">
                    <button
                      type="button"
                      className="wh-button wh-button-secondary"
                      onClick={closeDialog}
                    >
                      Cancel
                    </button>

                    <button
                      type="button"
                      className="wh-button wh-button-primary"
                      onClick={
                        dialog.type === "manager"
                          ? assignManager
                          : assignEmployee
                      }
                    >
                      {dialog.type === "manager"
                        ? "Save Manager"
                        : "Assign Employee"}
                    </button>
                  </div>
                </div>
              )}

            {/* Unassign employee */}
            {dialog.type === "unassign" &&
              selectedWarehouse && (
                <div className="wh-modal-body">
                  <p className="wh-confirm-text">
                    Are you sure you want to unassign{" "}
                    <strong>
                      {employees.find(
                        (item) => item.id === dialog.employeeId
                      )?.name || "this employee"}
                    </strong>
                    {" "}from {selectedWarehouse.name}?
                  </p>

                  {formError && (
                    <p className="wh-form-error">{formError}</p>
                  )}

                  <p className="wh-note">
                    No real employee assignments will be changed.
                  </p>

                  <div className="wh-modal-footer">
                    <button
                      type="button"
                      className="wh-button wh-button-secondary"
                      onClick={closeDialog}
                    >
                      Cancel
                    </button>

                    <button
                      type="button"
                      className="wh-button wh-button-danger"
                      onClick={unassignEmployee}
                    >
                      Unassign Employee
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
