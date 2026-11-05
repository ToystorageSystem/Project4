
import { useEffect, useMemo, useRef, useState } from "react";

import {
  Activity,
  AlertCircle,
  CheckCircle2,
  Clock3,
  Database,
  Download,
  Eye,
  Filter,
  RotateCcw,
  Search,
  Users,
  X,
} from "lucide-react";

import {
  downloadAdminActivityLogsCsv,
  getAdminActivityLogById,
  getAdminActivityLogs,
} from "./api/admin/activityLogsApi";

import "./css/ActivityLogsPage.css";

// ======================================================
// ISSUE #26 - ACTIVITY LOGS - LIVE MYSQL
// ======================================================

const PAGE_SIZE = 5;

// Danh sach action theo enum Backend
const ACTIONS = [
  "CREATE",
  "UPDATE",
  "DELETE",
  "APPROVE",
  "REJECT",
  "CANCEL",
  "LOGIN",
  "LOGOUT",
  "LOGIN_FAILED",
  "CHANGE_PASSWORD",
  "ENABLE_2FA",
  "DISABLE_2FA",
  "ASSIGN_ROLE",
  "REMOVE_ROLE",
  "GRANT_PERMISSION",
  "REVOKE_PERMISSION",
];

// Danh sach entity type theo enum Backend
const ENTITIES = [
  "USER",
  "ROLE",
  "PERMISSION",
  "USER_ROLE",
  "ROLE_PERMISSION",
  "AUTHENTICATION",
  "WAREHOUSE",
  "PRODUCT",
  "SUPPLIER",
  "PURCHASE_ORDER",
  "GOODS_RECEIPT",
  "INVENTORY",
  "STOCK_TRANSFER",
  "PACKAGE",
  "SHIPMENT",
  "STORE_RECEIPT",
  "STORE_RETURN",
  "SALES_ORDER",
  "STOCK_COUNT",
  "INVENTORY_ADJUSTMENT",
  "SUSPICIOUS_ALERT",
];

const EMPTY_PAGE = {
  content: [],
  totalElements: 0,
  totalPages: 0,
};

// ======================================================
// HELPER FUNCTIONS
// ======================================================

function formatLabel(value) {
  return String(value ?? "N/A").replaceAll("_", " ");
}

function formatTime(value) {
  // Giu nguyen gio LocalDateTime tu Backend
  if (!value) return "N/A";

  return String(value)
    .replace("T", " ")
    .slice(0, 19);
}

function formatRoles(roles) {
  if (!Array.isArray(roles) || roles.length === 0) {
    return "N/A";
  }

  return roles
    .map((role) =>
      role
        .replace(/^ROLE-/, "")
        .replaceAll("-", " ")
    )
    .join(", ");
}

// Chuyen JSON String thanh du lieu co the hien thi
function parseSnapshot(value) {
  if (
    value === null ||
    value === undefined ||
    value === ""
  ) {
    return null;
  }

  if (typeof value === "object") {
    return value;
  }

  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}

function showSnapshot(value) {
  const parsed = parseSnapshot(value);

  if (parsed === null) {
    return "No snapshot available.";
  }

  if (typeof parsed === "string") {
    return parsed;
  }

  return JSON.stringify(parsed, null, 2);
}

// Backend khong co truong outcome rieng.
// Chi hien thi khi ban ghi co result ro rang.
function explicitOutcome(log) {
  const data = parseSnapshot(log?.newValue);

  if (!data || typeof data !== "object") {
    return null;
  }

  const result = String(
    data.result ?? ""
  ).toUpperCase();

  if (result === "SUCCESS") {
    return "SUCCESS";
  }

  if (
    result === "FAILED" ||
    result === "FAILURE"
  ) {
    return "FAILED";
  }

  return null;
}

function errorText(error) {
  if (error?.status === 401) {
    return "Session expired. Please sign in as Admin again.";
  }

  if (error?.status === 403) {
    return "Access denied. Admin role and AUDIT_READ permission are required.";
  }

  return (
    error?.message ||
    "Unable to load activity logs."
  );
}

// ======================================================
// RESULT BADGE
// ======================================================

function ResultBadge({ outcome }) {
  if (!outcome) {
    return (
      <span className="al-result">
        N/A
      </span>
    );
  }

  return (
    <span
      className={`al-result ${outcome.toLowerCase()}`}
    >
      {outcome === "SUCCESS" ? (
        <CheckCircle2 size={13} />
      ) : (
        <AlertCircle size={13} />
      )}

      {outcome}
    </span>
  );
}

// ======================================================
// DETAIL ITEM
// ======================================================

function DetailItem({ label, value }) {
  return (
    <div className="al-detail-item">
      <span>{label}</span>

      <strong>
        {value === null ||
        value === undefined ||
        value === ""
          ? "Not available"
          : value}
      </strong>
    </div>
  );
}

// ======================================================
// MAIN COMPONENT
// ======================================================

export default function ActivityLogsPage() {
  // Tim kiem va bo loc
  const [search, setSearch] = useState("");
  const [keyword, setKeyword] = useState("");
  const [actionFilter, setActionFilter] = useState("ALL");
  const [entityFilter, setEntityFilter] = useState("ALL");

  // Loc ngay
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  // Phan trang
  const [page, setPage] = useState(1);

  // Du lieu Backend
  const [pageData, setPageData] = useState(EMPTY_PAGE);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Refresh
  const [reloadKey, setReloadKey] = useState(0);

  // Thong bao va xuat CSV
  const [notice, setNotice] = useState("");
  const [exporting, setExporting] = useState(false);

  // Chi tiet nhat ky
  const [selectedLog, setSelectedLog] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState("");
  const detailController = useRef(null);

  // Kiem tra ngay hop le
  const invalidDateRange = Boolean(
    startDate &&
    endDate &&
    startDate > endDate
  );

  // ====================================================
  // SEARCH DEBOUNCE
  // ====================================================

  useEffect(() => {
    const timer = setTimeout(() => {
      setKeyword(search.trim());
    }, 350);

    return () => {
      clearTimeout(timer);
    };
  }, [search]);

  // ====================================================
  // LOAD ACTIVITY LOGS FROM BACKEND
  // ====================================================

  useEffect(() => {
    if (invalidDateRange) {
      return undefined;
    }

    const controller = new AbortController();

    getAdminActivityLogs(
      {
        page: page - 1,
        size: PAGE_SIZE,
        keyword,
        action: actionFilter,
        entityType: entityFilter,
        fromDate: startDate,
        toDate: endDate,
      },
      {
        signal: controller.signal,
      }
    )
      .then((data) => {
        if (!controller.signal.aborted) {
          setPageData(data);
        }
      })
      .catch((requestError) => {
        if (controller.signal.aborted) {
          return;
        }

        setError(errorText(requestError));
        setPageData(EMPTY_PAGE);
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => {
      controller.abort();
    };
  }, [
    page,
    keyword,
    actionFilter,
    entityFilter,
    startDate,
    endDate,
    reloadKey,
    invalidDateRange,
  ]);

  useEffect(
    () => () => detailController.current?.abort(),
    []
  );

  // ====================================================
  // SERVER PAGINATION DATA
  // ====================================================

  const visibleLogs = useMemo(
    () =>
      invalidDateRange
        ? []
        : pageData.content ?? [],
    [invalidDateRange, pageData.content]
  );

  const busy = loading && !invalidDateRange;

  const totalEvents = invalidDateRange
    ? 0
    : pageData.totalElements ?? 0;

  const totalPages = Math.max(
    1,
    invalidDateRange
      ? 1
      : pageData.totalPages ?? 1
  );

  const uniqueActors = useMemo(() => {
    return new Set(
      visibleLogs
        .map(
          (log) =>
            log.actorUserId ??
            log.actorUserCode
        )
        .filter(Boolean)
    ).size;
  }, [visibleLogs]);

  // ====================================================
  // FILTER HANDLERS
  // ====================================================

  function updateFilter(setter, value) {
    setter(value);
    setPage(1);
    setLoading(true);
    setError("");
  }

  function resetFilters() {
    setSearch("");
    setKeyword("");
    setActionFilter("ALL");
    setEntityFilter("ALL");
    setStartDate("");
    setEndDate("");
    setPage(1);
    setNotice("");
    setError("");
    setLoading(true);

    setReloadKey((n) => n + 1);
  }

  function refresh() {
    setError("");
    setLoading(true);

    setReloadKey((n) => n + 1);
  }

  // ====================================================
  // EXPORT REAL CSV FROM BACKEND
  // ====================================================

  async function exportCsv() {
    if (invalidDateRange || exporting) {
      return;
    }

    setExporting(true);
    setError("");
    setNotice("");

    try {
      const filename =
        await downloadAdminActivityLogsCsv({
          keyword: search.trim(),
          action: actionFilter,
          entityType: entityFilter,
          fromDate: startDate,
          toDate: endDate,
        });

      setNotice(
        `CSV exported: ${filename}`
      );
    } catch (requestError) {
      setError(
        errorText(requestError)
      );
    } finally {
      setExporting(false);
    }
  }

  // ====================================================
  // GET ACTIVITY LOG DETAIL
  // ====================================================

  async function openDetail(log) {
    detailController.current?.abort();

    const controller = new AbortController();
    detailController.current = controller;

    setSelectedLog(log);
    setDetailLoading(true);
    setDetailError("");

    try {
      const data =
        await getAdminActivityLogById(
          log.id,
          {
            signal: controller.signal,
          }
        );

      if (!controller.signal.aborted) {
        setSelectedLog(data);
      }
    } catch (requestError) {
      if (!controller.signal.aborted) {
        setDetailError(
          errorText(requestError)
        );
      }
    } finally {
      if (!controller.signal.aborted) {
        setDetailLoading(false);
      }
    }
  }

  function closeDetail() {
    detailController.current?.abort();

    setSelectedLog(null);
    setDetailError("");
  }

  // ====================================================
  // LIVE STATISTICS
  // ====================================================

  const stats = [
    [
      "Total Events",
      totalEvents,
      Activity,
      "Matching backend records",
    ],
    [
      "Events on Page",
      visibleLogs.length,
      Database,
      "Loaded from MySQL",
    ],
    [
      "Total Pages",
      invalidDateRange
        ? 0
        : pageData.totalPages ?? 0,
      Filter,
      `${PAGE_SIZE} records per page`,
    ],
    [
      "Actors on Page",
      uniqueActors,
      Users,
      "Unique users on this page",
    ],
  ];

  // ====================================================
  // RENDER UI
  // ====================================================

  return (
    <div className="page-stack al-page">

      {/* PAGE HEADER */}
      <div className="page-heading al-heading">
        <div>
          <span className="eyebrow">
            ADMINISTRATION / ISSUE #26
          </span>

          <h1>Activity Logs</h1>

          <p>
            Review user actions, system events,
            and audit trail details.
          </p>
        </div>

        <span className="al-readonly">
          <Database size={15} />
          Live MySQL / Read-only
        </span>
      </div>

      {/* STATISTICS */}
      <div className="al-stats">
        {stats.map(
          ([label, value, Icon, description]) => (
            <div
              className="panel al-stat"
              key={label}
            >
              <span className="al-stat-icon">
                <Icon size={20} />
              </span>

              <span>{label}</span>

              <strong>
                {busy ? "..." : value}
              </strong>

              <small>{description}</small>
            </div>
          )
        )}
      </div>

      {/* SUCCESS NOTIFICATION */}
      {notice && (
        <div
          className="al-notice"
          role="status"
        >
          <CheckCircle2 size={18} />

          <span>{notice}</span>

          <button
            type="button"
            aria-label="Close notification"
            onClick={() => setNotice("")}
          >
            <X size={17} />
          </button>
        </div>
      )}

      {/* AUDIT TRAIL */}
      <section className="panel al-panel">

        {/* PANEL HEADER */}
        <div className="al-panel-header">
          <div>
            <h2>Audit Trail</h2>

            <p>
              {busy
                ? "Loading audit records..."
                : `${totalEvents} matching live records`}
            </p>
          </div>

          <div className="al-header-actions">
            <button
              type="button"
              className="al-button al-button-secondary"
              onClick={refresh}
              disabled={busy}
            >
              <RotateCcw size={15} />
              Refresh
            </button>

            <button
              type="button"
              className="al-button al-button-secondary"
              onClick={resetFilters}
            >
              <RotateCcw size={15} />
              Reset Filters
            </button>

            <button
              type="button"
              className="al-button al-button-primary"
              onClick={exportCsv}
              disabled={
                invalidDateRange ||
                busy ||
                exporting ||
                totalEvents === 0
              }
            >
              <Download size={15} />

              {exporting
                ? "Exporting..."
                : "Export CSV"}
            </button>

            <span className="demo-chip">
              LIVE MYSQL
            </span>
          </div>
        </div>

        {/* SEARCH AND FILTER */}
        <div className="al-filters">
          <label className="al-search">
            <Search size={17} />

            <input
              value={search}
              placeholder="Search actor name, email, or user code..."
              onChange={(event) =>
                updateFilter(
                  setSearch,
                  event.target.value
                )
              }
            />
          </label>

          {/* ACTION FILTER */}
          <select
            aria-label="Filter by action"
            value={actionFilter}
            onChange={(event) =>
              updateFilter(
                setActionFilter,
                event.target.value
              )
            }
          >
            <option value="ALL">
              All actions
            </option>

            {ACTIONS.map((action) => (
              <option
                key={action}
                value={action}
              >
                {formatLabel(action)}
              </option>
            ))}
          </select>

          {/* ENTITY FILTER */}
          <select
            aria-label="Filter by entity"
            value={entityFilter}
            onChange={(event) =>
              updateFilter(
                setEntityFilter,
                event.target.value
              )
            }
          >
            <option value="ALL">
              All entities
            </option>

            {ENTITIES.map((entity) => (
              <option
                key={entity}
                value={entity}
              >
                {formatLabel(entity)}
              </option>
            ))}
          </select>
        </div>

        {/* DATE RANGE */}
        <div className="al-date-filters">
          <span className="al-date-label">
            <Filter size={15} />
            Date Range
          </span>

          <label>
            From

            <input
              type="date"
              value={startDate}
              onChange={(event) =>
                updateFilter(
                  setStartDate,
                  event.target.value
                )
              }
            />
          </label>

          <label>
            To

            <input
              type="date"
              value={endDate}
              onChange={(event) =>
                updateFilter(
                  setEndDate,
                  event.target.value
                )
              }
            />
          </label>
        </div>

        {/* DATE VALIDATION */}
        {invalidDateRange && (
          <p className="al-validation">
            Start date cannot be later than end date.
          </p>
        )}

        {/* BACKEND ERROR */}
        {error && (
          <p
            className="al-validation"
            role="alert"
          >
            {error}
          </p>
        )}

        {/* TABLE */}
        <div className="al-table-wrap">
          <table className="al-table">

            <thead>
              <tr>
                <th>Log ID</th>
                <th>Timestamp</th>
                <th>Performed By</th>
                <th>Action</th>
                <th>Entity</th>
                <th>Target</th>
                <th>Outcome</th>
                <th>Details</th>
              </tr>
            </thead>

            <tbody>
              {busy ? (
                <tr>
                  <td
                    colSpan={8}
                    className="al-empty"
                  >
                    Loading activity logs...
                  </td>
                </tr>
              ) : visibleLogs.length === 0 ? (
                <tr>
                  <td
                    colSpan={8}
                    className="al-empty"
                  >
                    No matching activity logs found.
                  </td>
                </tr>
              ) : (
                visibleLogs.map((log) => (
                  <tr key={log.id}>

                    {/* LOG ID */}
                    <td>
                      <span
                        className="al-code"
                        title={
                          log.activityLogCode || ""
                        }
                      >
                        #{log.id}
                      </span>
                    </td>

                    {/* TIMESTAMP */}
                    <td>
                      <span className="al-time">
                        <Clock3 size={14} />

                        {formatTime(
                          log.createdAt
                        )}
                      </span>
                    </td>

                    {/* ACTOR */}
                    <td>
                      <div className="al-actor">
                        <strong>
                          {log.actorName ||
                            "System / Unknown"}
                        </strong>

                        <small>
                          {formatRoles(
                            log.actorRoles
                          )}
                        </small>
                      </div>
                    </td>

                    {/* ACTION */}
                    <td>
                      <span className="al-action">
                        {formatLabel(
                          log.action
                        )}
                      </span>
                    </td>

                    {/* ENTITY */}
                    <td>
                      {formatLabel(
                        log.entityType
                      )}
                    </td>

                    {/* TARGET */}
                    <td>
                      {log.entityId ?? "N/A"}
                    </td>

                    {/* OUTCOME */}
                    <td>
                      <ResultBadge
                        outcome={
                          explicitOutcome(log)
                        }
                      />
                    </td>

                    {/* DETAILS */}
                    <td>
                      <button
                        type="button"
                        className="al-view-button"
                        title="View Details"
                        aria-label={`View activity log ${log.id}`}
                        onClick={() =>
                          openDetail(log)
                        }
                      >
                        <Eye size={16} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* PAGINATION */}
        <div className="al-pagination">
          <span>
            Showing{" "}
            {totalEvents === 0
              ? 0
              : (page - 1) * PAGE_SIZE + 1}
            {" – "}
            {Math.min(
              page * PAGE_SIZE,
              totalEvents
            )}
            {" "}of {totalEvents} records
          </span>

          <div className="al-page-actions">
            <button
              type="button"
              disabled={
                busy || page <= 1
              }
              onClick={() => {
                setLoading(true);
                setPage((n) =>
                  Math.max(1, n - 1)
                );
              }}
            >
              Previous
            </button>

            <span>
              Page {page} of {totalPages}
            </span>

            <button
              type="button"
              disabled={
                busy || page >= totalPages
              }
              onClick={() => {
                setLoading(true);
                setPage((n) => n + 1);
              }}
            >
              Next
            </button>
          </div>
        </div>

        {/* TABLE FOOTER */}
        <div className="al-table-footer">
          <Database size={15} />
          Live audit records loaded from Backend.
          Read-only.
        </div>
      </section>

      {/* =====================================
          ACTIVITY LOG DETAILS MODAL
         ===================================== */}

      {selectedLog && (
        <div
          className="al-modal-overlay"
          onMouseDown={(event) => {
            if (
              event.target ===
              event.currentTarget
            ) {
              closeDetail();
            }
          }}
        >
          <section
            className="al-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="al-modal-title"
          >
            {/* MODAL HEADER */}
            <div className="al-modal-header">
              <div>
                <span className="eyebrow">
                  ISSUE #26 / LIVE MYSQL
                </span>

                <h2 id="al-modal-title">
                  Activity Log Details
                </h2>

                <p>
                  {selectedLog.activityLogCode ||
                    `#${selectedLog.id}`}
                </p>
              </div>

              <button
                type="button"
                className="al-close"
                aria-label="Close dialog"
                onClick={closeDetail}
              >
                <X size={20} />
              </button>
            </div>

            {/* MODAL BODY */}
            <div className="al-modal-body">

              {detailLoading && (
                <p>Loading details...</p>
              )}

              {detailError && (
                <p
                  className="al-validation"
                  role="alert"
                >
                  {detailError}
                </p>
              )}

              {!detailLoading && (
                <>
                  {/* LOG INFORMATION */}
                  <div className="al-detail-grid">

                    <DetailItem
                      label="Timestamp"
                      value={formatTime(
                        selectedLog.createdAt
                      )}
                    />

                    <DetailItem
                      label="Performed By"
                      value={
                        selectedLog.actorName
                      }
                    />

                    <DetailItem
                      label="User Code"
                      value={
                        selectedLog.actorUserCode
                      }
                    />

                    <DetailItem
                      label="Email"
                      value={
                        selectedLog.actorEmail
                      }
                    />

                    <DetailItem
                      label="Role"
                      value={formatRoles(
                        selectedLog.actorRoles
                      )}
                    />

                    <DetailItem
                      label="IP Address"
                      value={
                        selectedLog.ipAddress
                      }
                    />

                    <DetailItem
                      label="Action"
                      value={formatLabel(
                        selectedLog.action
                      )}
                    />

                    <DetailItem
                      label="Entity"
                      value={formatLabel(
                        selectedLog.entityType
                      )}
                    />

                    <DetailItem
                      label="Target ID"
                      value={
                        selectedLog.entityId
                      }
                    />

                    <DetailItem
                      label="Outcome"
                      value={
                        explicitOutcome(
                          selectedLog
                        ) || "Not recorded"
                      }
                    />

                    <DetailItem
                      label="Device"
                      value={
                        selectedLog.deviceInfo
                      }
                    />

                    <DetailItem
                      label="Log ID"
                      value={
                        selectedLog.id
                      }
                    />
                  </div>

                  {/* DESCRIPTION */}
                  <div className="al-description">
                    <h3>Activity Summary</h3>

                    <p>
                      {selectedLog.actorName ||
                        "System / Unknown"}{" "}
                      performed{" "}
                      {formatLabel(
                        selectedLog.action
                      )}{" "}
                      on{" "}
                      {formatLabel(
                        selectedLog.entityType
                      )}{" "}
                      (ID:{" "}
                      {selectedLog.entityId ??
                        "N/A"}).
                    </p>
                  </div>

                  {/* BEFORE / AFTER */}
                  <div className="al-snapshot-grid">
                    <div>
                      <h3>Before Change</h3>

                      <pre>
                        {showSnapshot(
                          selectedLog.oldValue
                        )}
                      </pre>
                    </div>

                    <div>
                      <h3>After Change</h3>

                      <pre>
                        {showSnapshot(
                          selectedLog.newValue
                        )}
                      </pre>
                    </div>
                  </div>

                  <p className="al-demo-note">
                    This record was retrieved from
                    the Backend. Outcome is displayed
                    only when explicitly recorded
                    in the event data.
                  </p>
                </>
              )}

              {/* MODAL FOOTER */}
              <div className="al-modal-footer">
                <button
                  type="button"
                  className="al-button al-button-primary"
                  onClick={closeDetail}
                >
                  Close
                </button>
              </div>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
