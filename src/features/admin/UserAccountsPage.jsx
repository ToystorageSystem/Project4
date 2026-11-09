
import { useCallback, useEffect, useMemo, useState } from "react";
import { isAdmin } from "../../auth/access";
import {
  CheckCircle2,
  Database,
  Eye,
  KeyRound,
  Lock,
  LockOpen,
  Pencil,
  Plus,
  RotateCcw,
  Search,
  ShieldAlert,
  UserRoundX,
  X,
} from "lucide-react";

import {
  createAdminUser,
  deactivateAdminUser,
  getAdminRoles,
  getAdminUserById,
  getAdminUsers,
  getSelectableWarehouses,
  lockAdminUser,
  getAdminSession,
  resetAdminUserPassword,
  unlockAdminUser,
  updateAdminUser,
  updateAdminUserRoles,
} from "./api/admin/userAccountsApi";

import {
  mapBackendUser,
  mapBackendUserPage,
} from "./api/admin/userAccountsMapper";

import "./css/UserAccountsPage.css";

const PAGE_SIZE = 100;

const EMPTY_FORM = {
  name: "",
  email: "",
  phone: "",
  identityCard: "",
  password: "",
  roleIds: [],
  warehouseId: "",
};

function StatusBadge({ status }) {
  const value = String(status || "UNKNOWN");

  return (
    <span className={`user-status ${value.toLowerCase()}`}>
      {value}
    </span>
  );
}

function InfoRow({ label, value }) {
  return (
    <div className="user-info-row">
      <span>{label}</span>
      <strong>{value || "Not assigned"}</strong>
    </div>
  );
}

function messageOf(error) {
  const raw =
    error instanceof Error
      ? error.message
      : "Something went wrong.";

  if (/email.*tồn tại/i.test(raw)) {
    return "This email address is already in use.";
  }

  if (/cccd.*tồn tại/i.test(raw)) {
    return "This identity card is already in use.";
  }

  if (/[À-ỹ]/.test(raw)) {
    return "The server rejected this request. Check the entered data and account permissions.";
  }

  return raw;
}

export default function UserAccountsPage() {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");

  const [notice, setNotice] = useState(null);
  const [modal, setModal] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [busy, setBusy] = useState(false);

  // Du lieu that tu MySQL
  const [liveUsers, setLiveUsers] = useState([]);
  const [total, setTotal] = useState(0);
  const [pages, setPages] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [liveError, setLiveError] = useState("");
  const [loginRequired, setLoginRequired] = useState(false);

  // Quyen lay tu session Admin chung
  const [canManage, setCanManage] = useState(false);
  const [currentAdminEmail, setCurrentAdminEmail] = useState("");

  // Role va Warehouse tu Backend
  const [roleOptions, setRoleOptions] = useState([]);
  const [warehouseOptions, setWarehouseOptions] = useState([]);
  const [lookupWarning, setLookupWarning] = useState("");

  // ========================================
  // LOAD LIVE USERS
  // ========================================

  const loadLiveUsers = useCallback(async (targetPage = 0) => {
    setLoading(true);
    setLiveError("");

    try {
      const response = await getAdminUsers(
        targetPage,
        PAGE_SIZE
      );

      if (!Array.isArray(response?.content)) {
        throw new Error("Unexpected users API response.");
      }

      const result = mapBackendUserPage(response);

      setLiveUsers(result.users);
      setTotal(result.totalElements);
      setPages(result.totalPages);
      setPage(result.page);
      setLoginRequired(false);
    } catch (error) {
      setLiveError(messageOf(error));

      if (error?.status === 401) {
        setLoginRequired(true);
        setCanManage(false);
      } else if (error?.status === 403) {
        setLoginRequired(false);
        setCanManage(false);
      }

      setLiveUsers([]);
      setTotal(0);
      setPages(0);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadLiveUsers(0);
  }, [loadLiveUsers]);

  // ========================================
  // LOAD ROLES AND WAREHOUSES
  // ========================================

  async function loadOptions() {
    const [rolesResult, warehousesResult] =
      await Promise.allSettled([
        getAdminRoles(),
        getSelectableWarehouses(),
      ]);

    if (
      rolesResult.status === "fulfilled" &&
      Array.isArray(rolesResult.value)
    ) {
      setRoleOptions(
        rolesResult.value.filter(
          (item) => item.status === "ACTIVE"
        )
      );
    } else {
      setRoleOptions([]);
    }

    if (
      warehousesResult.status === "fulfilled" &&
      Array.isArray(warehousesResult.value)
    ) {
      setWarehouseOptions(
        warehousesResult.value.filter(
          (item) => item.status === "ACTIVE"
        )
      );
    } else {
      setWarehouseOptions([]);
    }

    const warnings = [];

    if (rolesResult.status === "rejected") {
      warnings.push("Role options require ROLE_MANAGE.");
    }

    if (warehousesResult.status === "rejected") {
      warnings.push("Location options require WAREHOUSE_READ.");
    }

    setLookupWarning(warnings.join(" "));
  }

  // ========================================
  // LAY QUYEN TU SESSION CHUNG
  // ========================================

  useEffect(() => {
    let cancelled = false;

    async function syncPermissions() {
      try {
        const session = await getAdminSession();

        if (cancelled) return;

        const authorities = Array.isArray(session?.authorities)
          ? session.authorities
          : [];

        const allowed =
          isAdmin(session) &&
          authorities.includes("USER_MANAGE");

        setCanManage(allowed);

        setCurrentAdminEmail(
          String(session?.email || session?.username || "")
            .trim()
            .toLowerCase()
        );

        if (allowed) {
          await loadOptions();
        } else {
          setRoleOptions([]);
          setWarehouseOptions([]);
          setLookupWarning("");
        }
      } catch {
        if (cancelled) return;

        setCanManage(false);
        setCurrentAdminEmail("");
      }
    }

    void syncPermissions();

    return () => {
      cancelled = true;
    };
  }, []);

  // ========================================
  // SEARCH AND FILTER
  // ========================================

  const visibleUsers = useMemo(
    () =>
      liveUsers.filter((user) => {
        const value = [
          user.code,
          user.name,
          user.email,
          user.role,
          ...(user.roles || []),
          user.location,
        ]
          .join(" ")
          .toLowerCase();

        return (
          value.includes(search.trim().toLowerCase()) &&
          (
            statusFilter === "ALL" ||
            user.status === statusFilter
          )
        );
      }),
    [liveUsers, search, statusFilter]
  );

  const countStatus = (status) =>
    liveUsers.filter(
      (user) => user.status === status
    ).length;

  function updateField(field, value) {
    setForm((old) => ({
      ...old,
      [field]: value,
    }));

    setFormError("");
  }

  function toggleRole(roleId, selected) {
    setForm((old) => ({
      ...old,
      roleIds: selected
        ? [...new Set([...old.roleIds, roleId])]
        : old.roleIds.filter((id) => id !== roleId),
    }));
  }

  // ========================================
  // CREATE USER
  // ========================================

  function openCreate() {
    if (!canManage) return;

    if (roleOptions.length === 0) {
      setNotice({
        type: "error",
        message:
          "Cannot create a user: role options are unavailable. ROLE_MANAGE is required to load them.",
      });
      return;
    }

    setForm({
      ...EMPTY_FORM,
      roleIds: [],
    });

    setFormError("");

    setModal({
      type: "form",
      mode: "create",
    });
  }

  // ========================================
  // EDIT USER
  // ========================================

  async function openEdit(user) {
    if (!canManage) return;

    setBusy(true);

    try {
      const detail = mapBackendUser(
        await getAdminUserById(user.id)
      );

      setForm({
        ...EMPTY_FORM,
        name: detail.name,
        email: detail.email,
        phone: detail.phone,
        identityCard: detail.identityCard,
        warehouseId:
          detail.warehouseId == null
            ? ""
            : String(detail.warehouseId),
        roleIds: detail.roleIds,
      });

      setFormError("");

      setModal({
        type: "form",
        mode: "edit",
        user: detail,
      });
    } catch (error) {
      setNotice({
        type: "error",
        message: messageOf(error),
      });
    } finally {
      setBusy(false);
    }
  }

  // ========================================
  // CREATE / UPDATE USER
  // ========================================

  async function handleSubmit(event) {
    event.preventDefault();

    const name = form.name.trim();
    const emailValue = form.email.trim().toLowerCase();

    if (name.length < 2) {
      setFormError(
        "Full name must contain at least 2 characters."
      );
      return;
    }

    if (!canManage) return;

    if (!form.phone.trim() || form.phone.length > 20) {
      setFormError(
        "Phone is required (maximum 20 characters)."
      );
      return;
    }

    if (!/^\d{12}$/.test(form.identityCard.trim())) {
      setFormError(
        "Identity card must contain exactly 12 digits."
      );
      return;
    }

    if (
      modal.mode === "create" &&
      form.password.length < 6
    ) {
      setFormError(
        "Initial password must be at least 6 characters."
      );
      return;
    }

    if (
      modal.mode === "create" &&
      form.roleIds.length === 0
    ) {
      setFormError("Select at least one role.");
      return;
    }

    const payload = {
      name,
      email: emailValue,
      phone: form.phone.trim(),
      identityCard: form.identityCard.trim(),
      warehouseId:
        form.warehouseId === ""
          ? null
          : Number(form.warehouseId),
    };

    setBusy(true);

    try {
      if (modal.mode === "create") {
        await createAdminUser({
          ...payload,
          password: form.password,
          roleIds: form.roleIds,
        });
      } else {
        // Update basic information
        await updateAdminUser(
          modal.user.id,
          payload
        );

        // Update roles only when changed
        if (roleOptions.length > 0) {
          const oldIds = [...modal.user.roleIds]
            .sort((a, b) => a - b)
            .join(",");

          const newIds = [...form.roleIds]
            .sort((a, b) => a - b)
            .join(",");

          if (oldIds !== newIds) {
            if (form.roleIds.length === 0) {
              throw new Error(
                "Basic info saved, but a user must have at least one role."
              );
            }

            await updateAdminUserRoles(
              modal.user.id,
              form.roleIds
            );
          }
        }
      }

      setModal(null);

      setNotice({
        type: "success",
        message:
          modal.mode === "create"
            ? "User created."
            : "User updated.",
      });

      await loadLiveUsers(page);
    } catch (error) {
      setFormError(
        `${messageOf(error)} If editing, refresh to check whether basic information was saved.`
      );
    } finally {
      setBusy(false);
    }
  }

  // ========================================
  // ACCOUNT ACTIONS
  // ========================================

  function openConfirm(action, user) {
    setConfirmPassword("");
    setFormError("");

    setModal({
      type: "confirm",
      action,
      user,
    });
  }

  async function handleConfirm() {
    if (!modal) return;

    const { action, user } = modal;

    if (!canManage) return;

    // Khong cho phep Admin tu khoa chinh minh.
    if (
      ["lock", "deactivate"].includes(action) &&
      currentAdminEmail !== "" &&
      String(user.email || "")
        .trim()
        .toLowerCase() === currentAdminEmail
    ) {
      setFormError(
        "You cannot disable your own signed-in account."
      );
      return;
    }

    if (
      action === "reset" &&
      confirmPassword.length < 6
    ) {
      setFormError(
        "New password must be at least 6 characters."
      );
      return;
    }

    setBusy(true);
    setFormError("");

    try {
      if (action === "lock") {
        await lockAdminUser(user.id);
      } else if (action === "unlock") {
        await unlockAdminUser(user.id);
      } else if (action === "deactivate") {
        await deactivateAdminUser(user.id);
      } else if (action === "reset") {
        await resetAdminUserPassword(
          user.id,
          confirmPassword
        );
      } else {
        return;
      }

      setConfirmPassword("");
      setModal(null);

      setNotice({
        type: "success",
        message:
          action === "reset"
            ? "Password reset. The user must change it on next sign-in."
            : "Account status updated.",
      });

      await loadLiveUsers(page);
    } catch (error) {
      setFormError(messageOf(error));
    } finally {
      setBusy(false);
    }
  }

  // ========================================
  // RENDER
  // ========================================

  return (
    <div className="page-stack user-management-page">

      {/* HEADER */}
      <div className="page-heading user-page-heading">
        <div>
          <span className="eyebrow">
            ADMINISTRATION / ISSUE #23
          </span>

          <h1>User Accounts</h1>

          <p>
            Manage user accounts, assigned roles and locations.
          </p>
        </div>

        <div className="user-table-actions">
          <button
            className="user-primary-button"
            type="button"
            disabled={
              !canManage ||
              roleOptions.length === 0
            }
            onClick={openCreate}
            title={
              !canManage
                ? "USER_MANAGE permission required"
                : "Create User"
            }
          >
            <Plus size={17} />
            Create User
          </button>
        </div>
      </div>

      {/* BACKEND STATUS */}
      <section className="panel table-panel">
        <div className="panel-head table-panel-head">
          <div>
            <h2>Account access</h2>

            <p>
              {loading
                ? "Loading user accounts..."
                : loginRequired
                  ? "Session expired. Refresh to sign in again."
                  : "Live data is available"}
            </p>
          </div>

          <div className="user-table-actions">
            {loginRequired && (
              <button
                type="button"
                className="user-secondary-button"
                onClick={() => window.location.reload()}
              >
                Return to Sign In
              </button>
            )}

            <span className="demo-chip">
              {canManage
                ? "USER_MANAGE ENABLED"
                : "READ ONLY / USER_MANAGE NOT GRANTED"}
            </span>
          </div>
        </div>

        {(liveError || lookupWarning) && (
          <div className="user-modal-content">
            <div
              className="user-notice error"
              role="alert"
            >
              <ShieldAlert size={18} />

              <span>
                {[liveError, lookupWarning]
                  .filter(Boolean)
                  .join(" ")}
              </span>
            </div>
          </div>
        )}
      </section>

      {/* SUMMARY */}
      <div className="user-stat-grid">
        {[
          ["Total Users", total, "blue"],
          ["Active", countStatus("ACTIVE"), "green"],
          ["Locked", countStatus("LOCKED"), "red"],
          ["Inactive", countStatus("INACTIVE"), "gray"],
        ].map(([label, count, color]) => (
          <div
            className="panel user-stat"
            key={label}
          >
            <span>{label}</span>

            <strong className={color}>
              {count}
            </strong>

            <small>
              {label === "Total Users"
                ? "Live records"
                : "On loaded page"}
            </small>
          </div>
        ))}
      </div>

      {/* NOTIFICATION */}
      {notice && (
        <div
          className={`user-notice ${notice.type}`}
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
            onClick={() => setNotice(null)}
            aria-label="Close notification"
          >
            <X size={17} />
          </button>
        </div>
      )}

      {/* USER LIST */}
      <section className="panel table-panel">

        <div className="panel-head table-panel-head">
          <div>
            <h2>User Accounts List</h2>

            <p>
              {visibleUsers.length} shown /{" "}
              {total} total users
            </p>
          </div>

          <div className="user-table-actions">
            <button
              type="button"
              className="user-secondary-button"
              disabled={loading}
              onClick={() => void loadLiveUsers(page)}
            >
              <RotateCcw size={15} />
              Refresh
            </button>

            <span className="demo-chip">
              LIVE DATA
            </span>
          </div>
        </div>

        {/* FILTERS */}
        <div className="table-toolbar">
          <label className="search-field">
            <Search size={18} />

            <input
              aria-label="Search users"
              placeholder="Search loaded users..."
              value={search}
              onChange={(e) =>
                setSearch(e.target.value)
              }
            />
          </label>

          <select
            aria-label="Filter by status"
            value={statusFilter}
            onChange={(e) =>
              setStatusFilter(e.target.value)
            }
          >
            <option value="ALL">
              All statuses
            </option>

            <option value="ACTIVE">
              Active
            </option>

            <option value="LOCKED">
              Locked
            </option>

            <option value="INACTIVE">
              Inactive
            </option>
          </select>
        </div>

        {/* TABLE */}
        <div className="table-overflow">
          <table>
            <thead>
              <tr>
                <th>User Code</th>
                <th>Full Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Assigned Location</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody>
              {visibleUsers.length === 0 ? (
                <tr>
                  <td
                    colSpan={7}
                    className="no-results"
                  >
                    {loading
                      ? "Loading users..."
                      : loginRequired
                        ? "Sign in to view live users."
                        : "No matching user accounts found."}
                  </td>
                </tr>
              ) : (
                visibleUsers.map((user) => (
                  <tr key={user.id ?? user.code}>
                    <td className="table-code">
                      {user.code}
                    </td>

                    <td>{user.name}</td>

                    <td>{user.email}</td>

                    <td>
                      {user.roles?.length
                        ? user.roles.join(", ")
                        : user.role}
                    </td>

                    <td>{user.location}</td>

                    <td>
                      <StatusBadge status={user.status} />
                    </td>

                    <td>
                      <div className="user-row-actions">

                        {/* VIEW DETAILS */}
                        <button
                          type="button"
                          title="View Details"
                          aria-label={`View ${user.name}`}
                          onClick={() =>
                            setModal({
                              type: "details",
                              user,
                            })
                          }
                        >
                          <Eye size={16} />
                        </button>

                        {/* EDIT */}
                        <button
                          type="button"
                          disabled={!canManage || busy}
                          title="Edit User"
                          aria-label={`Edit ${user.name}`}
                          onClick={() =>
                            void openEdit(user)
                          }
                        >
                          <Pencil size={16} />
                        </button>

                        {/* LOCK */}
                        {user.status === "ACTIVE" && (
                          <button
                            type="button"
                            disabled={!canManage || busy}
                            title="Lock User"
                            aria-label={`Lock ${user.name}`}
                            onClick={() =>
                              openConfirm("lock", user)
                            }
                          >
                            <Lock size={16} />
                          </button>
                        )}

                        {/* UNLOCK */}
                        {user.status === "LOCKED" && (
                          <button
                            type="button"
                            disabled={!canManage || busy}
                            title="Unlock User"
                            aria-label={`Unlock ${user.name}`}
                            onClick={() =>
                              openConfirm("unlock", user)
                            }
                          >
                            <LockOpen size={16} />
                          </button>
                        )}

                        {/* DEACTIVATE */}
                        {user.status !== "INACTIVE" && (
                          <button
                            type="button"
                            disabled={!canManage || busy}
                            title="Deactivate User"
                            aria-label={`Deactivate ${user.name}`}
                            onClick={() =>
                              openConfirm(
                                "deactivate",
                                user
                              )
                            }
                          >
                            <UserRoundX size={16} />
                          </button>
                        )}

                        {/* RESET PASSWORD */}
                        <button
                          type="button"
                          disabled={!canManage || busy}
                          title="Reset Password"
                          aria-label={`Reset password for ${user.name}`}
                          onClick={() =>
                            openConfirm("reset", user)
                          }
                        >
                          <KeyRound size={16} />
                        </button>

                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="table-footer user-table-footer">
          <Database size={15} />
          Account changes require USER_MANAGE and confirmation.
        </div>

      </section>

      {/* PAGINATION */}
      {pages > 1 && !loginRequired && (
        <div className="user-modal-footer">

          <button
            type="button"
            className="user-secondary-button"
            disabled={loading || page === 0}
            onClick={() =>
              void loadLiveUsers(page - 1)
            }
          >
            Previous
          </button>

          <span>
            Page {page + 1} of {pages}
          </span>

          <button
            type="button"
            className="user-secondary-button"
            disabled={
              loading ||
              page === pages - 1
            }
            onClick={() =>
              void loadLiveUsers(page + 1)
            }
          >
            Next
          </button>

        </div>
      )}

      {/* MODAL */}
      {modal && (
        <div
          className="user-modal-overlay"
          onMouseDown={(e) => {
            if (
              e.target === e.currentTarget &&
              !busy
            ) {
              setModal(null);
            }
          }}
        >
          <section
            className="user-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="user-modal-heading"
          >

            {/* MODAL HEADER */}
            <div className="user-modal-header">
              <div>
                <span className="eyebrow">
                  ISSUE #23
                </span>

                <h2 id="user-modal-heading">
                  {modal.type === "details"
                    ? "User Details"
                    : modal.type === "form"
                      ? modal.mode === "create"
                        ? "Create User"
                        : "Edit User"
                      : {
                          lock: "Lock User",
                          unlock: "Unlock User",
                          deactivate: "Deactivate User",
                          reset: "Reset Password",
                        }[modal.action]}
                </h2>
              </div>

              <button
                type="button"
                className="user-close-button"
                disabled={busy}
                onClick={() => setModal(null)}
                aria-label="Close dialog"
              >
                <X size={20} />
              </button>
            </div>

            {/* USER DETAILS */}
            {modal.type === "details" && (
              <div className="user-modal-content">
                {[
                  ["User Code", modal.user.code],
                  ["Full Name", modal.user.name],
                  ["Email", modal.user.email],
                  [
                    "Role",
                    modal.user.roles?.length
                      ? modal.user.roles.join(", ")
                      : modal.user.role,
                  ],
                  ["Assigned Location", modal.user.location],
                  ["Status", modal.user.status],
                  ["Phone", modal.user.phone],
                  ["Identity Card", modal.user.identityCard],
                ].map(([label, value]) => (
                  <InfoRow
                    key={label}
                    label={label}
                    value={value}
                  />
                ))}

                <div className="user-modal-footer">
                  <button
                    className="user-secondary-button"
                    type="button"
                    onClick={() => setModal(null)}
                  >
                    Close
                  </button>

                  <button
                    className="user-primary-button"
                    type="button"
                    disabled={!canManage}
                    onClick={() =>
                      void openEdit(modal.user)
                    }
                  >
                    <Pencil size={16} />
                    Edit User
                  </button>
                </div>
              </div>
            )}

            {/* CREATE / EDIT FORM */}
            {modal.type === "form" && (
              <form
                className="user-modal-content"
                onSubmit={handleSubmit}
              >
                <div className="user-form-grid">

                  <label>
                    Full Name *
                    <input
                      required
                      minLength={2}
                      maxLength={150}
                      value={form.name}
                      onChange={(e) =>
                        updateField(
                          "name",
                          e.target.value
                        )
                      }
                    />
                  </label>

                  <label>
                    Email Address *
                    <input
                      required
                      type="email"
                      maxLength={150}
                      value={form.email}
                      onChange={(e) =>
                        updateField(
                          "email",
                          e.target.value
                        )
                      }
                    />
                  </label>

                  <label>
                    Phone *
                    <input
                      required
                      maxLength={20}
                      value={form.phone}
                      onChange={(e) =>
                        updateField(
                          "phone",
                          e.target.value
                        )
                      }
                    />
                  </label>

                  <label>
                    Identity Card (12 digits) *
                    <input
                      required
                      inputMode="numeric"
                      pattern="[0-9]{12}"
                      maxLength={12}
                      value={form.identityCard}
                      onChange={(e) =>
                        updateField(
                          "identityCard",
                          e.target.value
                        )
                      }
                    />
                  </label>

                  {modal.mode === "create" && (
                    <label>
                      Initial Password *
                      <input
                        required
                        minLength={6}
                        type="password"
                        autoComplete="new-password"
                        value={form.password}
                        onChange={(e) =>
                          updateField(
                            "password",
                            e.target.value
                          )
                        }
                      />
                    </label>
                  )}

                  <label>
                    Assigned Location
                    <select
                      value={form.warehouseId}
                      onChange={(e) =>
                        updateField(
                          "warehouseId",
                          e.target.value
                        )
                      }
                    >
                      <option value="">
                        Not assigned
                      </option>

                      {warehouseOptions.map((loc) => (
                        <option
                          key={loc.id}
                          value={String(loc.id)}
                        >
                          {loc.name}
                        </option>
                      ))}

                      {form.warehouseId &&
                        !warehouseOptions.some(
                          (loc) =>
                            String(loc.id) ===
                            String(form.warehouseId)
                        ) && (
                          <option value={form.warehouseId}>
                            {modal.user?.location ||
                              "Current location"}
                          </option>
                        )}
                    </select>
                  </label>

                  <fieldset
                    style={{
                      border: "1px solid #dde5f1",
                      borderRadius: 8,
                      padding: 12,
                    }}
                  >
                    <legend>Roles *</legend>

                    {roleOptions.length === 0 ? (
                      <p>
                        Role options unavailable.
                        Ask an administrator for ROLE_MANAGE.
                      </p>
                    ) : (
                      roleOptions.map((role) => (
                        <label
                          key={role.id}
                          style={{
                            display: "flex",
                            gap: 8,
                            alignItems: "center",
                            marginBottom: 7,
                          }}
                        >
                          <input
                            type="checkbox"
                            checked={form.roleIds.includes(
                              Number(role.id)
                            )}
                            onChange={(e) =>
                              toggleRole(
                                Number(role.id),
                                e.target.checked
                              )
                            }
                          />

                          {role.roleName ||
                            role.roleCode}
                        </label>
                      ))
                    )}
                  </fieldset>

                </div>

                <></>

                {formError && (
                  <p
                    className="user-error"
                    role="alert"
                  >
                    {formError}
                  </p>
                )}

                <div className="user-modal-footer">
                  <button
                    type="button"
                    className="user-secondary-button"
                    disabled={busy}
                    onClick={() => setModal(null)}
                  >
                    Cancel
                  </button>

                  <button
                    type="submit"
                    className="user-primary-button"
                    disabled={busy}
                  >
                    {busy
                      ? "Saving..."
                      : modal.mode === "create"
                        ? "Create User"
                        : "Save Changes"}
                  </button>
                </div>
              </form>
            )}

            {/* CONFIRM ACCOUNT ACTION */}
            {modal.type === "confirm" && (
              <div className="user-modal-content">

                <p className="user-confirm-text">
                  Are you sure you want to{" "}
                  {modal.action === "reset"
                    ? "reset the password for"
                    : `${modal.action} the account of`}{" "}
                  <strong>{modal.user.name}</strong>?
                </p>

                {modal.action === "reset" && (
                  <label>
                    New Password (minimum 6 characters)
                    <input
                      type="password"
                      autoComplete="new-password"
                      minLength={6}
                      required
                      value={confirmPassword}
                      onChange={(e) =>
                        setConfirmPassword(
                          e.target.value
                        )
                      }
                    />
                  </label>
                )}

                <></>

                {formError && (
                  <p
                    className="user-error"
                    role="alert"
                  >
                    {formError}
                  </p>
                )}

                <div className="user-modal-footer">
                  <button
                    type="button"
                    className="user-secondary-button"
                    disabled={busy}
                    onClick={() => setModal(null)}
                  >
                    Cancel
                  </button>

                  <button
                    type="button"
                    className="user-primary-button"
                    disabled={busy}
                    onClick={() =>
                      void handleConfirm()
                    }
                  >
                    {busy
                      ? "Processing..."
                      : "Confirm"}
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

