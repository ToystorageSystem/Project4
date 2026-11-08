
import { useEffect, useRef, useState } from "react";

import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock3,
  Database,
  Eye,
  Filter,
  RefreshCw,
  Search,
  ShieldAlert,
  ShieldCheck,
  X,
} from "lucide-react";

import {
  getSecurityAlerts,
  getSecurityAlertById,
  getSecurityAlertUserHistory,
  startSecurityAlertInvestigation,
  resolveSecurityAlert,
  markSecurityAlertFalsePositive,
} from "./api/admin/securityAlertsApi";

import "./css/SecurityAlertsPage.css";

// ======================================================
// ISSUE #27 - SECURITY ALERTS - ACCOUNT DATA
// ======================================================

const PAGE_SIZE = 5;

const LEVELS = [
  "CRITICAL",
  "HIGH",
  "MEDIUM",
  "LOW",
];

const STATUSES = [
  "NEW",
  "INVESTIGATING",
  "RESOLVED",
  "FALSE_POSITIVE",
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
  // Giu nguyen gio LocalDateTime tu Backend.
  if (!value) return "N/A";

  return String(value)
    .replace("T", " ")
    .slice(0, 19);
}

function displayText(value) {
  if (
    value === null ||
    value === undefined ||
    value === ""
  ) {
    return "Not available";
  }

  return value;
}

function getErrorMessage(error) {
  if (error?.status === 401) {
    return "Session expired. Please sign in as Admin again.";
  }

  if (error?.status === 403) {
    return "Access denied. Please sign in with an Admin account.";
  }

  return (
    error?.message ||
    "The request could not be completed."
  );
}

// ======================================================
// SEVERITY BADGE
// ======================================================

function SeverityBadge({ severity }) {
  const level = severity || "LOW";

  return (
    <span
      className={`sa-severity ${level.toLowerCase()}`}
    >
      <AlertTriangle size={13} />
      {formatLabel(level)}
    </span>
  );
}

// ======================================================
// STATUS BADGE
// ======================================================

function StatusBadge({ status }) {
  const current = status || "NEW";

  // FALSE_POSITIVE cung la trang thai da dong.
  const tone =
    current === "FALSE_POSITIVE"
      ? "resolved"
      : current.toLowerCase();

  return (
    <span className={`sa-status ${tone}`}>
      {current === "RESOLVED" ||
      current === "FALSE_POSITIVE" ? (
        <CheckCircle2 size={13} />
      ) : current === "INVESTIGATING" ? (
        <Clock3 size={13} />
      ) : (
        <ShieldAlert size={13} />
      )}

      {formatLabel(current)}
    </span>
  );
}

// ======================================================
// DETAIL ROW
// ======================================================

function DetailRow({ label, children }) {
  return (
    <div className="sa-detail-row">
      <span>{label}</span>

      <strong>{displayText(children)}</strong>
    </div>
  );
}

// ======================================================
// MAIN COMPONENT
// ======================================================

export default function SecurityAlertsPage() {
  // ====================================================
  // LIVE DATA
  // ====================================================

  const [pageData, setPageData] =
    useState(EMPTY_PAGE);

  const [loading, setLoading] =
    useState(true);

  const [listError, setListError] =
    useState("");

  const [reloadKey, setReloadKey] =
    useState(0);

  // ====================================================
  // SEARCH AND FILTER
  // ====================================================

  const [search, setSearch] =
    useState("");

  const [keyword, setKeyword] =
    useState("");

  const [levelFilter, setLevelFilter] =
    useState("ALL");

  const [statusFilter, setStatusFilter] =
    useState("ALL");

  const [page, setPage] =
    useState(1);

  // ====================================================
  // STATISTICS
  // ====================================================

  const [summary, setSummary] =
    useState(null);

  const [summaryLoading, setSummaryLoading] =
    useState(true);

  const [notice, setNotice] =
    useState(null);

  // ====================================================
  // ALERT DETAIL
  // ====================================================

  const [selectedAlert, setSelectedAlert] =
    useState(null);

  const [detailLoading, setDetailLoading] =
    useState(false);

  const [detailError, setDetailError] =
    useState("");

  // ====================================================
  // USER ACTIVITY HISTORY
  // ====================================================

  const [history, setHistory] =
    useState([]);

  const [historyLoading, setHistoryLoading] =
    useState(false);

  const [historyError, setHistoryError] =
    useState("");

  // ====================================================
  // INVESTIGATION
  // ====================================================

  const [note, setNote] =
    useState("");

  const [formError, setFormError] =
    useState("");

  const [actionBusy, setActionBusy] =
    useState(false);

  const detailRequests = useRef(null);

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
  // LOAD SECURITY ALERTS FROM BACKEND
  // ====================================================

  useEffect(() => {
    const controller = new AbortController();

    setLoading(true);
    setListError("");

    getSecurityAlerts(
      {
        page: page - 1,
        size: PAGE_SIZE,
        keyword,
        alertLevel: levelFilter,
        status: statusFilter,
      },
      {
        signal: controller.signal,
      }
    )
      .then((data) => {
        if (!controller.signal.aborted) {
          setPageData(data ?? EMPTY_PAGE);
        }
      })
      .catch((error) => {
        if (controller.signal.aborted) {
          return;
        }

        setListError(getErrorMessage(error));
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
    levelFilter,
    statusFilter,
    reloadKey,
  ]);

  // ====================================================
  // LOAD LIVE STATISTICS
  //
  // Backend chua co API thong ke tong hop.
  // Su dung cac truy van dem voi size = 1.
  // Khong dem tren 5 ban ghi cua trang hien tai.
  // ====================================================

  useEffect(() => {
    const controller = new AbortController();

    setSummaryLoading(true);

    const requests = [
      {},
      { status: "NEW" },
      { status: "INVESTIGATING" },
      { status: "RESOLVED" },
      {
        alertLevel: "CRITICAL",
        status: "NEW",
      },
      {
        alertLevel: "CRITICAL",
        status: "INVESTIGATING",
      },
    ];

    Promise.all(
      requests.map((filters) =>
        getSecurityAlerts(
          {
            ...filters,
            page: 0,
            size: 1,
          },
          {
            signal: controller.signal,
          }
        )
      )
    )
      .then((results) => {
        if (controller.signal.aborted) {
          return;
        }

        const counts = results.map(
          (result) => result?.totalElements ?? 0
        );

        setSummary({
          total: counts[0],
          new: counts[1],
          investigating: counts[2],
          resolved: counts[3],
          criticalOpen: counts[4] + counts[5],
        });
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setSummary(null);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setSummaryLoading(false);
        }
      });

    return () => {
      controller.abort();
    };
  }, [reloadKey]);

  // Huy request chi tiet khi roi khoi trang.
  useEffect(
    () => () => detailRequests.current?.abort(),
    []
  );

  // ====================================================
  // PAGINATION
  // ====================================================

  const alerts = pageData.content ?? [];

  const total =
    pageData.totalElements ?? 0;

  const totalPages = Math.max(
    1,
    pageData.totalPages ?? 0
  );

  const firstIndex =
    total === 0
      ? 0
      : (page - 1) * PAGE_SIZE + 1;

  const lastIndex = Math.min(
    page * PAGE_SIZE,
    total
  );

  useEffect(() => {
    if (!loading && page > totalPages) {
      setPage(totalPages);
    }
  }, [loading, page, totalPages]);

  // ====================================================
  // RESET FILTERS
  // ====================================================

  function resetFilters() {
    setSearch("");
    setKeyword("");
    setLevelFilter("ALL");
    setStatusFilter("ALL");
    setPage(1);
    setListError("");

    setReloadKey((key) => key + 1);
  }

  // ====================================================
  // REFRESH
  // ====================================================

  function refresh() {
    setListError("");

    setReloadKey((key) => key + 1);
  }

  // ====================================================
  // OPEN DETAILS
  // ====================================================

  async function openDetails(alert) {
    detailRequests.current?.abort();

    const controller = new AbortController();

    detailRequests.current = controller;

    setSelectedAlert(alert);
    setNote("");
    setFormError("");
    setDetailError("");
    setHistoryError("");
    setHistory([]);
    setDetailLoading(true);
    setHistoryLoading(true);

    // Lay chi tiet va lich su hoat dong that.
    const [detailResult, historyResult] =
      await Promise.allSettled([
        getSecurityAlertById(alert.id, {
          signal: controller.signal,
        }),
        getSecurityAlertUserHistory(alert.id, {
          signal: controller.signal,
        }),
      ]);

    if (controller.signal.aborted) {
      return;
    }

    if (detailResult.status === "fulfilled") {
      setSelectedAlert(
        detailResult.value ?? alert
      );
    } else {
      setDetailError(
        getErrorMessage(detailResult.reason)
      );
    }

    if (historyResult.status === "fulfilled") {
      setHistory(
        Array.isArray(historyResult.value)
          ? historyResult.value
          : []
      );
    } else {
      setHistoryError(
        getErrorMessage(historyResult.reason)
      );
    }

    setDetailLoading(false);
    setHistoryLoading(false);
  }

  // ====================================================
  // CLOSE DETAILS
  // ====================================================

  function closeDetails() {
    if (actionBusy) {
      return;
    }

    detailRequests.current?.abort();

    setSelectedAlert(null);
    setHistory([]);
    setNote("");
    setFormError("");
  }

  // ====================================================
  // UPDATE SECURITY ALERT STATUS
  // ====================================================

  async function changeStatus(nextStatus) {
    if (!selectedAlert || actionBusy) {
      return;
    }

    const current = selectedAlert.status;

    const allowed =
      (
        current === "NEW" &&
        nextStatus === "INVESTIGATING"
      ) ||
      (
        current === "INVESTIGATING" &&
        (
          nextStatus === "RESOLVED" ||
          nextStatus === "FALSE_POSITIVE"
        )
      );

    if (!allowed) {
      setFormError(
        "This status change is not allowed by the Backend."
      );
      return;
    }

    // RESOLVED va FALSE_POSITIVE bat buoc co note.
    if (nextStatus !== "INVESTIGATING") {
      if (!note.trim()) {
        setFormError(
          "Resolution note is required."
        );
        return;
      }

      if (note.trim().length > 2000) {
        setFormError(
          "Resolution note must not exceed 2000 characters."
        );
        return;
      }

      // Backend khong co API mo lai canh bao da dong.
      const confirmed = window.confirm(
        `Mark ${selectedAlert.alertCode} as ${formatLabel(nextStatus)}? ` +
        "This action cannot be reversed from this screen."
      );

      if (!confirmed) {
        return;
      }
    }

    setActionBusy(true);
    setFormError("");

    try {
      let updated;

      if (nextStatus === "INVESTIGATING") {
        updated =
          await startSecurityAlertInvestigation(
            selectedAlert.id
          );
      } else if (nextStatus === "RESOLVED") {
        updated =
          await resolveSecurityAlert(
            selectedAlert.id,
            note.trim()
          );
      } else {
        updated =
          await markSecurityAlertFalsePositive(
            selectedAlert.id,
            note.trim()
          );
      }

      setSelectedAlert(
        updated ?? {
          ...selectedAlert,
          status: nextStatus,
        }
      );

      setNote("");

      setNotice({
        type: "success",
        message:
          `Alert updated to ${formatLabel(nextStatus)}.`,
      });

      // Reload danh sach va thong ke.
      refresh();

      // Reload lich su user sau khi cap nhat.
      try {
        const rows =
          await getSecurityAlertUserHistory(
            selectedAlert.id
          );

        setHistory(
          Array.isArray(rows) ? rows : []
        );

        setHistoryError("");
      } catch (error) {
        setHistoryError(
          getErrorMessage(error)
        );
      }
    } catch (error) {
      setFormError(
        getErrorMessage(error)
      );
    } finally {
      setActionBusy(false);
    }
  }

  // ====================================================
  // STATISTICS CARDS
  // ====================================================

  const stats = [
    {
      label: "Total Alerts",
      value: summary?.total,
      icon: ShieldAlert,
      tone: "blue",
    },
    {
      label: "New Alerts",
      value: summary?.new,
      icon: AlertTriangle,
      tone: "orange",
    },
    {
      label: "Investigating",
      value: summary?.investigating,
      icon: Activity,
      tone: "purple",
    },
    {
      label: "Resolved",
      value: summary?.resolved,
      icon: CheckCircle2,
      tone: "green",
    },
  ];

  // ====================================================
  // RENDER
  // ====================================================

  return (
    <div className="page-stack sa-page">

      {/* PAGE HEADER */}
      <div className="page-heading sa-heading">
        <div>
          <span className="eyebrow">
            ADMINISTRATION / ISSUE #27
          </span>

          <h1>Security Alerts</h1>

          <p>
            Monitor unusual activity, investigate alerts,
            and track resolution progress.
          </p>
        </div>

        <span className="sa-preview-tag">
          <Database size={15} />
          Account data / Admin
        </span>
      </div>

      {/* STATISTICS */}
      <div className="sa-stat-grid">
        {stats.map(
          ({ label, value, icon: Icon, tone }) => (
            <div
              className="panel sa-stat"
              key={label}
            >
              <span
                className={`sa-stat-icon ${tone}`}
              >
                <Icon size={20} />
              </span>

              <span>{label}</span>

              <strong>
                {summaryLoading
                  ? "..."
                  : value ?? "—"}
              </strong>

              <small>
                Current records
              </small>
            </div>
          )
        )}
      </div>

      {/* CRITICAL ALERT NOTICE */}
      {!summaryLoading &&
        summary?.criticalOpen > 0 && (
        <div className="sa-critical-notice">
          <AlertTriangle size={19} />

          <span>
            {summary.criticalOpen} unresolved
            critical alert
            {summary.criticalOpen > 1 ? "s" : ""}
            {" "}require review.
          </span>
        </div>
      )}

      {/* SUCCESS NOTIFICATION */}
      {notice && (
        <div
          className={`sa-notice ${notice.type}`}
          role="status"
        >
          <CheckCircle2 size={18} />

          <span>
            {notice.message}
          </span>

          <button
            type="button"
            aria-label="Close notification"
            onClick={() => setNotice(null)}
          >
            <X size={17} />
          </button>
        </div>
      )}

      {/* ALERT MANAGEMENT */}
      <section className="panel sa-table-panel">

        {/* PANEL HEADER */}
        <div className="sa-panel-header">
          <div>
            <h2>Alert Monitoring</h2>

            <p>
              {loading
                ? "Loading alerts..."
                : `${total} matching live alerts`}
            </p>
          </div>

          <div className="sa-header-actions">
            <button
              type="button"
              className="sa-button sa-button-secondary"
              onClick={resetFilters}
            >
              <Filter size={15} />
              Reset Filters
            </button>

            <button
              type="button"
              className="sa-button sa-button-secondary"
              onClick={refresh}
              disabled={loading}
            >
              <RefreshCw size={15} />
              Refresh
            </button>

            <span className="demo-chip">
              ACCOUNT DATA
            </span>
          </div>
        </div>

        {/* SEARCH AND FILTER */}
        <div className="sa-filters">

          {/* SEARCH */}
          <label className="sa-search">
            <Search size={17} />

            <input
              value={search}
              placeholder="Search alert code, user code, name or email..."
              onChange={(event) => {
                setSearch(event.target.value);
                setPage(1);
              }}
            />
          </label>

          {/* SEVERITY */}
          <select
            aria-label="Filter by severity"
            value={levelFilter}
            onChange={(event) => {
              setLevelFilter(event.target.value);
              setPage(1);
            }}
          >
            <option value="ALL">
              All severities
            </option>

            {LEVELS.map((level) => (
              <option
                key={level}
                value={level}
              >
                {formatLabel(level)}
              </option>
            ))}
          </select>

          {/* STATUS */}
          <select
            aria-label="Filter by status"
            value={statusFilter}
            onChange={(event) => {
              setStatusFilter(event.target.value);
              setPage(1);
            }}
          >
            <option value="ALL">
              All statuses
            </option>

            {STATUSES.map((status) => (
              <option
                key={status}
                value={status}
              >
                {formatLabel(status)}
              </option>
            ))}
          </select>
        </div>

        {/* BACKEND ERROR */}
        {listError && (
          <p
            className="sa-form-error"
            role="alert"
          >
            {listError}
          </p>
        )}

        {/* TABLE */}
        <div className="sa-table-scroll">
          <table className="sa-table">
            <thead>
              <tr>
                <th>Alert ID</th>
                <th>Alert</th>
                <th>Severity</th>
                <th>Account</th>
                <th>Detected At</th>
                <th>Status</th>
                <th>Details</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td
                    className="sa-empty"
                    colSpan={7}
                  >
                    Loading security alerts...
                  </td>
                </tr>
              ) : alerts.length === 0 ? (
                <tr>
                  <td
                    className="sa-empty"
                    colSpan={7}
                  >
                    No matching security alerts found.
                  </td>
                </tr>
              ) : (
                alerts.map((alert) => (
                  <tr key={alert.id}>

                    {/* ALERT CODE */}
                    <td>
                      <span className="sa-code">
                        {alert.alertCode ||
                          `#${alert.id}`}
                      </span>
                    </td>

                    {/* ALERT DESCRIPTION */}
                    <td>
                      <div className="sa-alert-cell">
                        <strong>
                          {alert.description ||
                            "Security alert"}
                        </strong>

                        <small>
                          {[
                            alert.relatedLogAction,
                            alert.relatedLogEntityType,
                          ]
                            .filter(Boolean)
                            .join(" / ") ||
                            "No related activity"}
                        </small>
                      </div>
                    </td>

                    {/* SEVERITY */}
                    <td>
                      <SeverityBadge
                        severity={alert.alertLevel}
                      />
                    </td>

                    {/* ACCOUNT */}
                    <td>
                      <div className="sa-account-cell">
                        <strong>
                          {alert.userName ||
                            "Unknown user"}
                        </strong>

                        <small>
                          {alert.userCode || "N/A"}
                        </small>
                      </div>
                    </td>

                    {/* DETECTED AT */}
                    <td>
                      <span className="sa-time">
                        <Clock3 size={14} />

                        {formatTime(
                          alert.detectedAt
                        )}
                      </span>
                    </td>

                    {/* STATUS */}
                    <td>
                      <StatusBadge
                        status={alert.status}
                      />
                    </td>

                    {/* DETAILS */}
                    <td>
                      <button
                        type="button"
                        className="sa-view-button"
                        aria-label={`View ${
                          alert.alertCode || alert.id
                        }`}
                        title="View Alert Details"
                        onClick={() =>
                          openDetails(alert)
                        }
                      >
                        <Eye size={17} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* PAGINATION */}
        <div className="sa-pagination">
          <span>
            Showing {firstIndex} – {lastIndex}
            {" "}of {total} alerts
          </span>

          <div className="sa-page-controls">
            <button
              type="button"
              disabled={loading || page <= 1}
              onClick={() =>
                setPage((n) =>
                  Math.max(1, n - 1)
                )
              }
            >
              <ChevronLeft size={15} />
              Previous
            </button>

            <span>
              Page {page} of {totalPages}
            </span>

            <button
              type="button"
              disabled={
                loading ||
                page >= totalPages
              }
              onClick={() =>
                setPage((n) => n + 1)
              }
            >
              Next
              <ChevronRight size={15} />
            </button>
          </div>
        </div>

        {/* TABLE FOOTER */}
        <div className="sa-table-footer">
          <Database size={15} />

          Live security alerts from Backend.
          Changes are saved.
        </div>
      </section>

      {/* =================================================
          ALERT DETAILS MODAL
          ================================================= */}

      {selectedAlert && (
        <div
          className="sa-overlay"
          onMouseDown={(event) => {
            if (
              event.target ===
              event.currentTarget
            ) {
              closeDetails();
            }
          }}
        >
          <section
            className="sa-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="sa-dialog-title"
          >

            {/* MODAL HEADER */}
            <div className="sa-modal-header">
              <div>
                <span className="eyebrow">
                  ISSUE #27 /{" "}
                  {selectedAlert.alertCode ||
                    selectedAlert.id}
                </span>

                <h2 id="sa-dialog-title">
                  Security Alert Details
                </h2>

                <p>
                  {selectedAlert.description ||
                    "Security alert"}
                </p>
              </div>

              <button
                type="button"
                className="sa-close"
                aria-label="Close dialog"
                disabled={actionBusy}
                onClick={closeDetails}
              >
                <X size={20} />
              </button>
            </div>

            {/* MODAL BODY */}
            <div className="sa-modal-body">

              {/* STATUS BADGES */}
              <div className="sa-badge-row">
                <SeverityBadge
                  severity={
                    selectedAlert.alertLevel
                  }
                />

                <StatusBadge
                  status={
                    selectedAlert.status
                  }
                />
              </div>

              {detailLoading && (
                <p>
                  Loading alert details...
                </p>
              )}

              {detailError && (
                <p
                  className="sa-form-error"
                  role="alert"
                >
                  {detailError}
                </p>
              )}

              {/* ALERT INFORMATION */}
              <div className="sa-detail-grid">
                <DetailRow label="Alert Code">
                  {selectedAlert.alertCode}
                </DetailRow>

                <DetailRow label="Risk Score">
                  {selectedAlert.riskScore}
                </DetailRow>

                <DetailRow label="Related User">
                  {selectedAlert.userName}
                </DetailRow>

                <DetailRow label="User Code">
                  {selectedAlert.userCode}
                </DetailRow>

                <DetailRow label="User Email">
                  {selectedAlert.userEmail}
                </DetailRow>

                <DetailRow label="Related Log">
                  {selectedAlert.relatedLogCode ||
                    (
                      selectedAlert.relatedLogId
                        ? `#${selectedAlert.relatedLogId}`
                        : null
                    )}
                </DetailRow>

                <DetailRow label="Related Action">
                  {selectedAlert.relatedLogAction}
                </DetailRow>

                <DetailRow label="Related Entity">
                  {selectedAlert.relatedLogEntityType}
                </DetailRow>

                <DetailRow label="Detected At">
                  {formatTime(
                    selectedAlert.detectedAt
                  )}
                </DetailRow>

                <DetailRow label="Last Updated">
                  {formatTime(
                    selectedAlert.updatedAt
                  )}
                </DetailRow>

                <DetailRow label="Reviewed By">
                  {selectedAlert.reviewedByName}
                </DetailRow>

                <DetailRow label="Reviewed At">
                  {selectedAlert.reviewedAt
                    ? formatTime(
                        selectedAlert.reviewedAt
                      )
                    : null}
                </DetailRow>

                <DetailRow label="Resolved At">
                  {selectedAlert.resolvedAt
                    ? formatTime(
                        selectedAlert.resolvedAt
                      )
                    : null}
                </DetailRow>

                <DetailRow label="Related Log Time">
                  {selectedAlert.relatedLogCreatedAt
                    ? formatTime(
                        selectedAlert.relatedLogCreatedAt
                      )
                    : null}
                </DetailRow>
              </div>

              {/* DESCRIPTION */}
              <div className="sa-modal-section">
                <h3>Description</h3>

                <p className="sa-description">
                  {selectedAlert.description ||
                    "No description recorded."}
                </p>
              </div>

              {/* RESOLUTION NOTE */}
              {selectedAlert.resolutionNote && (
                <div className="sa-modal-section">
                  <h3>Resolution Note</h3>

                  <p className="sa-description">
                    {selectedAlert.resolutionNote}
                  </p>
                </div>
              )}

              {/* USER ACTIVITY HISTORY */}
              <div className="sa-modal-section">
                <h3>
                  Related User Activity
                  {" "}(
                  {historyLoading
                    ? "..."
                    : history.length}
                  )
                </h3>

                {historyError && (
                  <p
                    className="sa-form-error"
                    role="alert"
                  >
                    {historyError}
                  </p>
                )}

                {historyLoading ? (
                  <p className="sa-description">
                    Loading user activity history...
                  </p>
                ) : history.length === 0 ? (
                  <p className="sa-description">
                    No activity history found
                    for this account.
                  </p>
                ) : (
                  <div className="sa-history">
                    {history
                      .slice(0, 20)
                      .map((entry, index) => (
                        <div
                          className="sa-history-item"
                          key={
                            entry.id ?? index
                          }
                        >
                          <span className="sa-history-dot" />

                          <div>
                            <strong>
                              {formatLabel(
                                entry.action
                              )}
                              {" / "}
                              {formatLabel(
                                entry.entityType
                              )}
                            </strong>

                            <small>
                              {entry.actorName ||
                                selectedAlert.userName ||
                                "Unknown"}
                              {" · "}
                              {formatTime(
                                entry.createdAt
                              )}
                            </small>

                            <p>
                              Activity log #{entry.id}
                              {entry.entityId != null
                                ? ` · Target ID: ${entry.entityId}`
                                : ""}
                            </p>
                          </div>
                        </div>
                      ))}

                    {history.length > 20 && (
                      <p className="sa-description">
                        Showing the 20 newest of
                        {" "}{history.length}{" "}
                        activity records.
                      </p>
                    )}
                  </div>
                )}
              </div>

              {/* INVESTIGATION ACTION */}
              <div className="sa-modal-section">
                <h3>
                  Investigation Action
                </h3>

                <p className="sa-action-description">
                  {selectedAlert.status === "NEW"
                    ? "Start investigating this alert. Its status will be saved."
                    : selectedAlert.status === "INVESTIGATING"
                      ? "Enter a resolution note, then select Resolved or False Positive. Completed alerts cannot be reopened from this screen."
                      : "This alert is closed. The Backend does not provide an endpoint to reopen it."}
                </p>

                {selectedAlert.status ===
                  "INVESTIGATING" && (
                  <label className="sa-note-field">
                    Resolution Note

                    <textarea
                      rows={3}
                      maxLength={2000}
                      value={note}
                      placeholder="Enter investigation findings (required, max 2000 characters)..."
                      onChange={(event) => {
                        setNote(event.target.value);
                        setFormError("");
                      }}
                    />
                  </label>
                )}

                {formError && (
                  <p
                    className="sa-form-error"
                    role="alert"
                  >
                    {formError}
                  </p>
                )}
              </div>

              {/* BACKEND NOTE */}
              <p className="sa-demo-note">
                Live Backend record. Status changes
                are saved and recorded
                in Activity Logs.
              </p>

              {/* MODAL FOOTER */}
              <div className="sa-modal-footer">
                <button
                  type="button"
                  className="sa-button sa-button-secondary"
                  onClick={closeDetails}
                  disabled={actionBusy}
                >
                  Close
                </button>

                {/* NEW -> INVESTIGATING */}
                {selectedAlert.status === "NEW" && (
                  <button
                    type="button"
                    className="sa-button sa-button-primary"
                    disabled={
                      actionBusy ||
                      detailLoading
                    }
                    onClick={() =>
                      changeStatus(
                        "INVESTIGATING"
                      )
                    }
                  >
                    <Activity size={16} />

                    {actionBusy
                      ? "Saving..."
                      : "Start Investigation"}
                  </button>
                )}

                {/* INVESTIGATING -> COMPLETED */}
                {selectedAlert.status ===
                  "INVESTIGATING" && (
                  <>
                    <button
                      type="button"
                      className="sa-button sa-button-secondary"
                      disabled={
                        actionBusy ||
                        !note.trim()
                      }
                      onClick={() =>
                        changeStatus(
                          "FALSE_POSITIVE"
                        )
                      }
                    >
                      <ShieldCheck size={16} />
                      False Positive
                    </button>

                    <button
                      type="button"
                      className="sa-button sa-button-success"
                      disabled={
                        actionBusy ||
                        !note.trim()
                      }
                      onClick={() =>
                        changeStatus(
                          "RESOLVED"
                        )
                      }
                    >
                      <CheckCircle2 size={16} />

                      {actionBusy
                        ? "Saving..."
                        : "Resolve Alert"}
                    </button>
                  </>
                )}
              </div>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

