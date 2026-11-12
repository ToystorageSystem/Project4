
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  CheckCircle2,
  Database,
  Eye,
  KeyRound,
  Lock,
  LockOpen,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  Shield,
  ShieldAlert,
  ShieldCheck,
  Trash2,
  Users,
  X,
} from "lucide-react";

import {
  createRole,
  deleteRole,
  getPermissionImpact,
  getPermissions,
  getRolePermissions,
  getRoles,
  getRoleUsers,
  grantRolePermission,
  lockRole,
  revokeRolePermission,
  unlockRole,
  updateRole,
} from "./api/admin/rolesPermissionsApi";

import "./css/RolesPermissionsPage.css";

// App.jsx van import ham nay.
// Giu export de tranh loi khi build.
// Ham khong con tao du lieu Demo.
export function createDemoRoles() {
  return [];
}

// Tam an nut Create Role, giu nguyen logic de co the bat lai sau nay.
const SHOW_CREATE_ROLE_BUTTON = false;

const EMPTY_FORM = {
  code: "",
  name: "",
  description: "",
};

const SYSTEM_ROLES = new Set([
  "ADMIN",
  "BIZ_MANAGER",
  "BIZ_STAFF",
  "WH_MANAGER",
  "WH_STAFF",
  "STORE_MANAGER",
  "STORE_STAFF",
  "DRIVER",
]);

function cleanCode(value) {
  return String(value || "")
    .trim()
    .toUpperCase()
    .replace(/^ROLE[-_]/, "")
    .replaceAll("-", "_");
}

function getError(error) {
  if (error?.status === 401) {
    return "Session expired. Please sign in again.";
  }

  if (error?.status === 403) {
    return "Access denied. ROLE_ADMIN and ROLE_MANAGE are required.";
  }

  const message =
    error?.message || "An unexpected error occurred.";

  // Hi\u1ec7n \u0111\u00fang message backend tr\u1ea3 v\u1ec1
  return message;
}

function StatusBadge({ status }) {
  const value = String(status || "UNKNOWN");

  return (
    <span className={`role-status ${value.toLowerCase()}`}>
      {value}
    </span>
  );
}

function InfoRow({ label, value }) {
  return (
    <div className="role-info-row">
      <span>{label}</span>
      <strong>{value ?? "Unavailable"}</strong>
    </div>
  );
}

export default function RolesPermissionsPage() {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");

  const [dialog, setDialog] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState("");

  const [selectedPermissions, setSelectedPermissions] =
    useState([]);

  const [permissionSearch, setPermissionSearch] =
    useState("");

  const [notice, setNotice] = useState(null);
  const [busy, setBusy] = useState(false);

  const [roles, setRoles] = useState([]);
  const [permissions, setPermissions] = useState([]);

  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [detailWarning, setDetailWarning] = useState("");
  const [connected, setConnected] = useState(false);

  // ========================================
  // LOAD LIVE DATA FROM BACKEND
  // ========================================

  const loadLive = useCallback(async () => {
    setLoading(true);
    setLoadError("");
    setDetailWarning("");

    try {
      const [roleData, catalogData] = await Promise.all([
        getRoles(),
        getPermissions(),
      ]);

      if (
        !Array.isArray(roleData) ||
        !Array.isArray(catalogData)
      ) {
        throw new Error(
          "Unexpected roles or permissions response from Backend."
        );
      }

      const list = await Promise.all(
        roleData.map(async (role) => {
          const [p, u] = await Promise.allSettled([
            getRolePermissions(role.id),
            getRoleUsers(role.id),
          ]);

          const permissionsLoaded =
            p.status === "fulfilled" &&
            Array.isArray(p.value?.permissions);

          const usersLoaded =
            u.status === "fulfilled" &&
            Array.isArray(u.value);

          const code = cleanCode(role.roleCode);

          return {
            id: role.id,
            rawCode: role.roleCode,
            code,
            name: role.roleName || "",
            description: role.description || "",
            status: role.status || "INACTIVE",
            system: SYSTEM_ROLES.has(code),

            permissions: permissionsLoaded
              ? p.value.permissions
                  .map((item) => item.permissionCode)
                  .filter(Boolean)
              : [],

            assignedUsers: usersLoaded
              ? u.value.length
              : null,

            permissionsLoaded,
            usersLoaded,
          };
        })
      );

      setRoles(list);

      setPermissions(
        catalogData.map((item) => ({
          id: item.id,
          code: item.permissionCode,
          name:
            item.description ||
            item.actionName ||
            item.permissionCode,
          group: item.moduleName || "Other",
          status: item.status || "INACTIVE",
        }))
      );

      setConnected(true);

      if (
        list.some(
          (item) =>
            !item.permissionsLoaded ||
            !item.usersLoaded
        )
      ) {
        setDetailWarning(
          "Some role details could not be loaded. Refresh before changing those roles."
        );
      }

      return true;
    } catch (error) {
      setConnected(false);
      setRoles([]);
      setPermissions([]);
      setLoadError(getError(error));

      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  // ========================================
  // AUTO LOAD USING SHARED ADMIN SESSION
  // ========================================

  useEffect(() => {
    void loadLive();
  }, [loadLive]);

  // ========================================
  // SEARCH AND FILTER
  // ========================================

  const filteredRoles = useMemo(() => {
    const query = search.trim().toLowerCase();

    return roles.filter((role) => {
      const matchesSearch = [
        role.code,
        role.name,
        role.description,
      ]
        .join(" ")
        .toLowerCase()
        .includes(query);

      const matchesStatus =
        statusFilter === "ALL" ||
        role.status === statusFilter;

      return matchesSearch && matchesStatus;
    });
  }, [roles, search, statusFilter]);

  const groups = useMemo(
    () =>
      [
        ...new Set(
          permissions.map((item) => item.group)
        ),
      ].sort(),
    [permissions]
  );

  const filteredPermissions = permissions.filter(
    (item) =>
      [
        item.code,
        item.name,
        item.group,
      ]
        .join(" ")
        .toLowerCase()
        .includes(
          permissionSearch.trim().toLowerCase()
        )
  );

  const incomplete = roles.some(
    (item) => !item.permissionsLoaded
  );

  const assignedPermissions = incomplete
    ? "—"
    : roles.reduce(
        (sum, item) =>
          sum + item.permissions.length,
        0
      );

  // ========================================
  // DIALOG HELPERS
  // ========================================

  function closeDialog() {
    if (busy) return;

    setDialog(null);
    setFormError("");
  }

  function openCreate() {
    if (!connected || loading || busy) return;

    setForm({ ...EMPTY_FORM });
    setFormError("");

    setDialog({
      type: "create",
    });
  }

  function openEdit(role) {
    if (
      !connected ||
      busy ||
      role.code === "ADMIN"
    ) {
      return;
    }

    setForm({
      code: role.code,
      name: role.name,
      description: role.description,
    });

    setFormError("");

    setDialog({
      type: "edit",
      role,
    });
  }

  function openPermissions(role) {
    if (
      !connected ||
      busy ||
      role.code === "ADMIN" ||
      !role.permissionsLoaded ||
      !role.usersLoaded
    ) {
      return;
    }

    setSelectedPermissions([
      ...role.permissions,
    ]);

    setPermissionSearch("");
    setFormError("");

    setDialog({
      type: "permissions",
      role,
    });
  }

  function updateForm(field, value) {
    setForm((previous) => ({
      ...previous,
      [field]: value,
    }));

    setFormError("");
  }

  function togglePermission(code) {
    setSelectedPermissions((previous) =>
      previous.includes(code)
        ? previous.filter(
            (item) => item !== code
          )
        : [...previous, code]
    );
  }

  // ========================================
  // CREATE / UPDATE ROLE
  // ========================================

  async function handleSaveRole(event) {
    event.preventDefault();

    if (
      !dialog ||
      busy ||
      !connected ||
      dialog.role?.code === "ADMIN"
    ) {
      return;
    }

    const code = cleanCode(form.code);
    const name = form.name.trim();
    const description = form.description.trim();

    if (
      !/^[A-Z][A-Z0-9_]{2,39}$/.test(code)
    ) {
      setFormError(
        "Role code must be 3-40 characters using letters, numbers or underscores."
      );
      return;
    }

    if (
      name.length < 3 ||
      name.length > 80
    ) {
      setFormError(
        "Role name must contain 3-80 characters."
      );
      return;
    }

    if (description.length > 255) {
      setFormError(
        "Description must not exceed 255 characters."
      );
      return;
    }

    if (
      dialog.type === "create" &&
      roles.some(
        (item) => item.code === code
      )
    ) {
      setFormError(
        "This role code already exists."
      );
      return;
    }

    setBusy(true);
    setFormError("");

    try {
      if (dialog.type === "create") {
        await createRole({
          roleCode:
            `ROLE-${code.replaceAll("_", "-")}`,
          roleName: name,
          description,
        });
      } else {
        await updateRole(
          dialog.role.id,
          {
            roleName: name,
            description,
          }
        );
      }

      setDialog(null);

      setNotice({
        type: "success",
        message:
          dialog.type === "create"
            ? "Role created."
            : "Role updated.",
      });

      await loadLive();
    } catch (error) {
      setFormError(getError(error));
    } finally {
      setBusy(false);
    }
  }

  // ========================================
  // SAVE ROLE PERMISSIONS
  // ========================================

  async function handleSavePermissions() {
    if (
      dialog?.type !== "permissions" ||
      busy ||
      !connected
    ) {
      return;
    }

    const role = dialog.role;

    if (
      role.code === "ADMIN" ||
      !role.permissionsLoaded ||
      !role.usersLoaded
    ) {
      setFormError(
        "This role cannot be changed safely."
      );
      return;
    }

    const revoke = role.permissions.filter(
      (code) =>
        !selectedPermissions.includes(code)
    );

    const grant = selectedPermissions.filter(
      (code) =>
        !role.permissions.includes(code)
    );

    if (
      revoke.length === 0 &&
      grant.length === 0
    ) {
      setDialog(null);

      setNotice({
        type: "success",
        message:
          "No permission changes were needed.",
      });

      return;
    }

    const lookup = new Map(
      permissions.map((item) => [
        item.code,
        item,
      ])
    );

    if (
      [...revoke, ...grant].some(
        (code) =>
          !lookup.get(code)?.id
      )
    ) {
      setFormError(
        "Permission details are unavailable. Refresh and try again."
      );
      return;
    }

    if (
      grant.some(
        (code) =>
          lookup.get(code).status !== "ACTIVE"
      )
    ) {
      setFormError(
        "Inactive permissions cannot be granted."
      );
      return;
    }

    setBusy(true);
    setFormError("");

    let saved = 0;

    try {
      // Kiem tra nguy co truoc khi thu hoi quyen.
      for (const code of revoke) {
        const permission = lookup.get(code);

        const impact =
          await getPermissionImpact(
            role.id,
            permission.id
          );

        if (impact?.lastAdminRisk) {
          throw new Error(
            "This operation may remove the last administrator's access."
          );
        }
      }

      // Xac nhan neu Role dang co User.
      if (role.assignedUsers > 0) {
        const approved = window.confirm(
          `This role has ${role.assignedUsers} assigned user(s). ` +
          "Changing permissions can invalidate their sessions. Continue?"
        );

        if (!approved) {
          return;
        }
      }

      // Revoke permissions
      for (const code of revoke) {
        await revokeRolePermission(
          role.id,
          lookup.get(code).id
        );

        saved += 1;
      }

      // Grant permissions
      for (const code of grant) {
        await grantRolePermission(
          role.id,
          lookup.get(code).id
        );

        saved += 1;
      }

      setDialog(null);

      setNotice({
        type: "success",
        message:
          `${saved} permission change(s) saved.`,
      });

      await loadLive();
    } catch (error) {
      const extra =
        saved > 0
          ? " Some changes were saved. Refresh to review the current permissions."
          : "";

      setFormError(
        getError(error) + extra
      );
    } finally {
      setBusy(false);
    }
  }

  // ========================================
  // LOCK / UNLOCK ROLE
  // ========================================

  function openStatusConfirm(role) {
    if (
      !connected ||
      busy ||
      role.code === "ADMIN" ||
      !role.usersLoaded
    ) {
      return;
    }

    setFormError("");

    setDialog({
      type: "status",
      role,
    });
  }

  async function handleConfirmStatus() {
    if (
      dialog?.type !== "status" ||
      busy ||
      !connected
    ) {
      return;
    }

    const role = dialog.role;

    if (
      role.code === "ADMIN" ||
      !role.usersLoaded
    ) {
      return;
    }

    const next =
      role.status === "ACTIVE"
        ? "INACTIVE"
        : "ACTIVE";

    setBusy(true);
    setFormError("");

    try {
      if (next === "INACTIVE") {
        await lockRole(role.id);
      } else {
        await unlockRole(role.id);
      }

      setDialog(null);

      setNotice({
        type: "success",
        message:
          `Role status changed to ${next}.`,
      });

      await loadLive();
    } catch (error) {
      setFormError(getError(error));
    } finally {
      setBusy(false);
    }
  }

  // ========================================
  // DELETE ROLE
  // ========================================

  function canDeleteRole(role) {
    return (
      connected &&
      !role.system &&
      role.assignedUsers === 0 &&
      role.permissionsLoaded &&
      role.permissions.length === 0
    );
  }

  function openDeleteConfirm(role) {
    if (
      !canDeleteRole(role) ||
      busy
    ) {
      setNotice({
        type: "error",
        message:
          "Only eligible custom roles can be deleted.",
      });

      return;
    }

    setFormError("");

    setDialog({
      type: "delete",
      role,
    });
  }

  async function handleDeleteRole() {
    if (
      dialog?.type !== "delete" ||
      busy ||
      !canDeleteRole(dialog.role)
    ) {
      return;
    }

    setBusy(true);
    setFormError("");

    try {
      await deleteRole(dialog.role.id);

      setDialog(null);

      setNotice({
        type: "success",
        message:
          "Role deleted.",
      });

      await loadLive();
    } catch (error) {
      setFormError(getError(error));
    } finally {
      setBusy(false);
    }
  }

  const modalTitle = {
    create: "Create Role",
    edit: "Edit Role",
    view: "Role Details",
    permissions: "Manage Permissions",
    status: "Change Role Status",
    delete: "Delete Role",
  }[dialog?.type];

  // ========================================
  // RENDER
  // ========================================

  return (
    <div className="page-stack role-page">

      {/* HEADER */}
      <div className="page-heading role-heading">
        <div>
          <span className="eyebrow">
            ADMINISTRATION / ISSUE #24
          </span>

          <h1>Roles &amp; Permissions</h1>

          <p>
            Manage roles and permissions from the shared account data.
          </p>
        </div>

        {/* Tam an Create Role - giu nguyen phan xu ly role hien tai. */}
        {SHOW_CREATE_ROLE_BUTTON && (
          <div className="role-header-actions">
            <button
              type="button"
              className="role-button role-button-primary"
              disabled={
                !connected ||
                loading ||
                busy
              }
              onClick={openCreate}
            >
              <Plus size={17} />
              Create Role
            </button>
          </div>
        )}
      </div>

      {/* BACKEND CONNECTION */}
      <section className="panel role-table-panel">
        <div className="role-table-header">
          <div>
            <h2>Account access</h2>

            <p>
              {loading
                ? "Loading roles and permissions..."
                : connected
                  ? "Your account is connected"
                  : "Admin session or permission required"}
            </p>
          </div>

          <div className="role-header-actions">
            <button
              type="button"
              className="role-button role-button-secondary"
              disabled={loading || busy}
              onClick={() => void loadLive()}
            >
              <RefreshCw size={15} />
              Retry Connection
            </button>

            {connected && (
              <span className="demo-chip">
                ACCOUNT DATA
              </span>
            )}
          </div>
        </div>

        {loadError && (
          <div className="role-modal-body">
            <div
              className="role-form-error"
              role="alert"
            >
              {loadError}
            </div>
          </div>
        )}

        {detailWarning && (
          <div className="role-modal-body">
            <div
              className="role-form-error"
              role="alert"
            >
              {detailWarning}
            </div>
          </div>
        )}
      </section>

      {/* STATISTICS */}
      <div className="role-stats">

        <div className="panel role-stat">
          <span>Total Roles</span>
          <strong>{roles.length}</strong>
          <small>Live roles</small>
        </div>

        <div className="panel role-stat">
          <span>Active Roles</span>

          <strong className="green">
            {
              roles.filter(
                (role) =>
                  role.status === "ACTIVE"
              ).length
            }
          </strong>

          <small>Currently enabled</small>
        </div>

        <div className="panel role-stat">
          <span>Inactive Roles</span>

          <strong className="gray">
            {
              roles.filter(
                (role) =>
                  role.status === "INACTIVE"
              ).length
            }
          </strong>

          <small>Currently disabled</small>
        </div>

        <div className="panel role-stat">
          <span>Permission Assignments</span>
          <strong className="blue">
            {assignedPermissions}
          </strong>
          <small>Across all roles</small>
        </div>

      </div>

      {/* NOTIFICATION */}
      {notice && (
        <div
          className={`role-notice ${notice.type}`}
          role="status"
        >
          {notice.type === "success" ? (
            <CheckCircle2 size={18} />
          ) : (
            <ShieldAlert size={18} />
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

      {/* ROLE TABLE */}
      <section className="panel role-table-panel">
        <div className="role-table-header">
          <div>
            <h2>Role Management</h2>

            <p>
              {filteredRoles.length} of{" "}
              {roles.length} live roles shown
            </p>
          </div>

          <div className="role-header-actions">
            <button
              type="button"
              className="role-button role-button-secondary"
              disabled={loading || busy}
              onClick={() => void loadLive()}
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
        <div className="role-toolbar">
          <label className="role-search">
            <Search size={17} />

            <input
              value={search}
              onChange={(event) =>
                setSearch(event.target.value)
              }
              placeholder="Search roles..."
            />
          </label>

          <select
            aria-label="Filter role status"
            value={statusFilter}
            onChange={(event) =>
              setStatusFilter(event.target.value)
            }
          >
            <option value="ALL">
              All statuses
            </option>

            <option value="ACTIVE">
              Active
            </option>

            <option value="INACTIVE">
              Inactive
            </option>
          </select>
        </div>

        {/* TABLE */}
        <div className="role-table-scroll">
          <table className="role-table">
            <thead>
              <tr>
                <th>Role Code</th>
                <th>Role Name</th>
                <th>Permissions</th>
                <th>Assigned Users</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody>
              {filteredRoles.length === 0 ? (
                <tr>
                  <td
                    className="role-empty"
                    colSpan={6}
                  >
                    {loading
                      ? "Loading roles..."
                      : "No matching roles found."}
                  </td>
                </tr>
              ) : (
                filteredRoles.map((role) => {
                  const adminProtected =
                    role.code === "ADMIN";

                  const deleteAvailable =
                    canDeleteRole(role);

                  return (
                    <tr key={role.id ?? role.code}>

                      <td>
                        <span className="role-code">
                          {role.code}
                        </span>
                      </td>

                      <td>
                        <div className="role-name-cell">
                          <span className="role-name-icon">
                            <Shield size={17} />
                          </span>

                          <div>
                            <strong>{role.name}</strong>
                            <small>{role.description}</small>
                          </div>
                        </div>
                      </td>

                      <td>
                        <span className="role-count">
                          <KeyRound size={14} />

                          {role.permissionsLoaded === false
                            ? "Unavailable"
                            : `${role.permissions.length} permissions`}
                        </span>
                      </td>

                      <td>
                        <span className="role-count">
                          <Users size={14} />

                          {role.assignedUsers === null
                            ? "Unavailable"
                            : `${role.assignedUsers} users`}
                        </span>
                      </td>

                      <td>
                        <StatusBadge
                          status={role.status}
                        />
                      </td>

                      <td>
                        <div className="role-row-actions">

                          {/* VIEW */}
                          <button
                            type="button"
                            title="View Role"
                            aria-label={`View ${role.code}`}
                            onClick={() =>
                              setDialog({
                                type: "view",
                                role,
                              })
                            }
                          >
                            <Eye size={16} />
                          </button>

                          {/* EDIT */}
                          <button
                            type="button"
                            title="Edit Role"
                            aria-label={`Edit ${role.code}`}
                            disabled={
                              !connected ||
                              adminProtected ||
                              busy
                            }
                            onClick={() =>
                              openEdit(role)
                            }
                          >
                            <Pencil size={16} />
                          </button>

                          {/* MANAGE PERMISSIONS */}
                          <button
                            type="button"
                            title="Manage Permissions"
                            aria-label={`Manage permissions for ${role.code}`}
                            disabled={
                              !connected ||
                              adminProtected ||
                              busy ||
                              !role.permissionsLoaded ||
                              !role.usersLoaded
                            }
                            onClick={() =>
                              openPermissions(role)
                            }
                          >
                            <KeyRound size={16} />
                          </button>

                          {/* LOCK / UNLOCK */}
                          <button
                            type="button"
                            title={
                              role.status === "ACTIVE"
                                ? "Deactivate Role"
                                : "Activate Role"
                            }
                            aria-label={`Change status of ${role.code}`}
                            disabled={
                              !connected ||
                              adminProtected ||
                              busy ||
                              !role.usersLoaded
                            }
                            onClick={() =>
                              openStatusConfirm(role)
                            }
                          >
                            {role.status === "ACTIVE" ? (
                              <Lock size={16} />
                            ) : (
                              <LockOpen size={16} />
                            )}
                          </button>

                          {/* DELETE */}
                          {!role.system && (
                            <button
                              type="button"
                              title="Delete Role"
                              aria-label={`Delete ${role.code}`}
                              disabled={
                                !deleteAvailable ||
                                busy
                              }
                              onClick={() =>
                                openDeleteConfirm(role)
                              }
                            >
                              <Trash2 size={16} />
                            </button>
                          )}

                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        <div className="role-table-footer">
          <Database size={15} />
          Account data. Changes can affect real users and active sessions.
        </div>
      </section>

      {/* PERMISSION CATALOG */}
      <section className="panel role-catalog">
        <div className="role-catalog-heading">
          <div>
            <h2>Permission Catalog</h2>

            <p>
              Available permissions from the shared account data.
            </p>
          </div>

          <span className="role-count">
            {permissions.length} live permissions
          </span>
        </div>

        <div className="role-catalog-groups">
          {groups.map((group) => (
            <div
              className="role-catalog-item"
              key={group}
            >
              <span>{group}</span>

              <strong>
                {
                  permissions.filter(
                    (item) =>
                      item.group === group
                  ).length
                }
              </strong>
            </div>
          ))}
        </div>
      </section>

      {/* MODALS */}
      {dialog && (
        <div
          className="role-modal-overlay"
          onMouseDown={(event) => {
            if (
              event.target ===
              event.currentTarget
            ) {
              closeDialog();
            }
          }}
        >
          <section
            className="role-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="role-modal-title"
          >

            {/* MODAL HEADER */}
            <div className="role-modal-header">
              <div>
                <span className="eyebrow">
                  ISSUE #24 · ACCOUNT DATA
                </span>

                <h2 id="role-modal-title">
                  {modalTitle}
                </h2>
              </div>

              <button
                type="button"
                className="role-close"
                aria-label="Close dialog"
                disabled={busy}
                onClick={closeDialog}
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
                className="role-modal-body"
                onSubmit={handleSaveRole}
              >
                <div className="role-form-grid">

                  <label className="role-field">
                    Role Code *

                    <input
                      required
                      placeholder="e.g. INVENTORY_AUDITOR"
                      maxLength={40}
                      disabled={
                        dialog.type === "edit"
                      }
                      value={form.code}
                      onChange={(event) =>
                        updateForm(
                          "code",
                          event.target.value.toUpperCase()
                        )
                      }
                    />
                  </label>

                  <label className="role-field">
                    Role Name *

                    <input
                      required
                      minLength={3}
                      maxLength={80}
                      placeholder="Enter role name"
                      value={form.name}
                      onChange={(event) =>
                        updateForm(
                          "name",
                          event.target.value
                        )
                      }
                    />
                  </label>

                  <label className="role-field role-field-full">
                    Description

                    <textarea
                      rows={4}
                      maxLength={255}
                      placeholder="Describe this role..."
                      value={form.description}
                      onChange={(event) =>
                        updateForm(
                          "description",
                          event.target.value
                        )
                      }
                    />
                  </label>

                </div>

                <></>

                {formError && (
                  <div
                    className="role-form-error"
                    role="alert"
                  >
                    {formError}
                  </div>
                )}

                <div className="role-modal-footer">
                  <button
                    type="button"
                    className="role-button role-button-secondary"
                    disabled={busy}
                    onClick={closeDialog}
                  >
                    Cancel
                  </button>

                  <button
                    type="submit"
                    className="role-button role-button-primary"
                    disabled={busy}
                  >
                    {busy
                      ? "Saving..."
                      : dialog.type === "create"
                        ? "Create Role"
                        : "Save Changes"}
                  </button>
                </div>
              </form>
            )}

            {/* ROLE DETAILS */}
            {dialog.type === "view" && (
              <div className="role-modal-body">
                <InfoRow
                  label="Role Code"
                  value={dialog.role.code}
                />

                <InfoRow
                  label="Role Name"
                  value={dialog.role.name}
                />

                <InfoRow
                  label="Description"
                  value={
                    dialog.role.description ||
                    "No description"
                  }
                />

                <InfoRow
                  label="Status"
                  value={dialog.role.status}
                />

                <InfoRow
                  label="Assigned Users"
                  value={dialog.role.assignedUsers}
                />

                <InfoRow
                  label="Permissions"
                  value={
                    dialog.role.permissionsLoaded === false
                      ? "Unavailable"
                      : dialog.role.permissions.length
                  }
                />

                <div className="role-details-permissions">
                  <h3>Assigned Permissions</h3>

                  <div className="role-chips">
                    {dialog.role.permissionsLoaded === false ? (
                      <p>
                        Permissions could not be loaded.
                      </p>
                    ) : dialog.role.permissions.length === 0 ? (
                      <p>
                        No permissions assigned.
                      </p>
                    ) : (
                      dialog.role.permissions.map(
                        (code) => (
                          <span
                            className="role-chip"
                            key={code}
                          >
                            {code}
                          </span>
                        )
                      )
                    )}
                  </div>
                </div>

                <div className="role-modal-footer">
                  <button
                    type="button"
                    className="role-button role-button-secondary"
                    onClick={closeDialog}
                  >
                    Close
                  </button>

                  <button
                    type="button"
                    className="role-button role-button-primary"
                    disabled={
                      !connected ||
                      dialog.role.code === "ADMIN" ||
                      !dialog.role.permissionsLoaded ||
                      !dialog.role.usersLoaded
                    }
                    onClick={() =>
                      openPermissions(dialog.role)
                    }
                  >
                    <KeyRound size={16} />
                    Manage Permissions
                  </button>
                </div>
              </div>
            )}

            {/* MANAGE PERMISSIONS */}
            {dialog.type === "permissions" && (
              <div className="role-modal-body">
                <div className="role-permissions-intro">
                  <div>
                    <strong>
                      {dialog.role.name}
                    </strong>

                    <span>
                      {dialog.role.code}
                    </span>
                  </div>

                  <span className="role-selected-count">
                    {selectedPermissions.length} selected
                  </span>
                </div>

                <label className="role-search role-permission-search">
                  <Search size={17} />

                  <input
                    placeholder="Search permissions..."
                    value={permissionSearch}
                    onChange={(event) =>
                      setPermissionSearch(
                        event.target.value
                      )
                    }
                  />
                </label>

                <div className="role-permission-list">
                  {filteredPermissions.length === 0 ? (
                    <p className="role-empty">
                      No matching permissions found.
                    </p>
                  ) : (
                    groups.map((group) => {
                      const items =
                        filteredPermissions.filter(
                          (item) =>
                            item.group === group
                        );

                      if (items.length === 0) {
                        return null;
                      }

                      return (
                        <div
                          className="role-permission-group"
                          key={group}
                        >
                          <h3>{group}</h3>

                          {items.map((item) => (
                            <label
                              className="role-permission-option"
                              key={item.code}
                            >
                              <input
                                type="checkbox"
                                checked={
                                  selectedPermissions.includes(
                                    item.code
                                  )
                                }
                                disabled={
                                  item.status !== "ACTIVE" &&
                                  !dialog.role.permissions.includes(
                                    item.code
                                  )
                                }
                                onChange={() =>
                                  togglePermission(
                                    item.code
                                  )
                                }
                              />

                              <span>
                                <strong>
                                  {item.name}
                                </strong>

                                <small>
                                  {item.code}
                                  {item.status !== "ACTIVE" &&
                                    " · INACTIVE"}
                                </small>
                              </span>
                            </label>
                          ))}
                        </div>
                      );
                    })
                  )}
                </div>

                <p className="role-demo-note">
                  Changes affect real access for{" "}
                  {dialog.role.assignedUsers ?? 0}{" "}
                  assigned user(s).
                </p>

                {formError && (
                  <div
                    className="role-form-error"
                    role="alert"
                  >
                    {formError}
                  </div>
                )}

                <div className="role-modal-footer">
                  <button
                    type="button"
                    className="role-button role-button-secondary"
                    disabled={busy}
                    onClick={closeDialog}
                  >
                    Cancel
                  </button>

                  <button
                    type="button"
                    className="role-button role-button-primary"
                    disabled={busy}
                    onClick={() =>
                      void handleSavePermissions()
                    }
                  >
                    <ShieldCheck size={16} />

                    {busy
                      ? "Saving..."
                      : "Save Permissions"}
                  </button>
                </div>
              </div>
            )}

            {/* ACTIVATE / DEACTIVATE */}
            {dialog.type === "status" && (
              <div className="role-modal-body">
                <p className="role-confirm-text">
                  Are you sure you want to{" "}
                  <strong>
                    {dialog.role.status === "ACTIVE"
                      ? "deactivate"
                      : "activate"}
                  </strong>{" "}
                  the role{" "}
                  <strong>
                    {dialog.role.code}
                  </strong>
                  ?
                </p>

                <p className="role-demo-note">
                  This changes the real role and can expire sessions for{" "}
                  {dialog.role.assignedUsers ?? 0}{" "}
                  user(s).
                </p>

                {formError && (
                  <div
                    className="role-form-error"
                    role="alert"
                  >
                    {formError}
                  </div>
                )}

                <div className="role-modal-footer">
                  <button
                    type="button"
                    className="role-button role-button-secondary"
                    disabled={busy}
                    onClick={closeDialog}
                  >
                    Cancel
                  </button>

                  <button
                    type="button"
                    className="role-button role-button-primary"
                    disabled={busy}
                    onClick={() =>
                      void handleConfirmStatus()
                    }
                  >
                    {busy
                      ? "Processing..."
                      : "Confirm"}
                  </button>
                </div>
              </div>
            )}

            {/* DELETE ROLE */}
            {dialog.type === "delete" && (
              <div className="role-modal-body">
                <p className="role-confirm-text">
                  Delete the custom role{" "}
                  <strong>
                    {dialog.role.code}
                  </strong>
                  ?
                </p>

                <></>

                {formError && (
                  <div
                    className="role-form-error"
                    role="alert"
                  >
                    {formError}
                  </div>
                )}

                <div className="role-modal-footer">
                  <button
                    type="button"
                    className="role-button role-button-secondary"
                    disabled={busy}
                    onClick={closeDialog}
                  >
                    Cancel
                  </button>

                  <button
                    type="button"
                    className="role-button role-button-danger"
                    disabled={busy}
                    onClick={() =>
                      void handleDeleteRole()
                    }
                  >
                    <Trash2 size={16} />

                    {busy
                      ? "Deleting..."
                      : "Delete Role"}
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
