import { useState } from "react";
import { errorMessage } from "../../api/api";
export function Notice({ error, children }) {
  return error ? (
    <div role="alert" className="ops-alert">
      {error}
    </div>
  ) : children ? (
    <div role="status" className="ops-notice">
      {children}
    </div>
  ) : null;
}
export function ResourceState({ resource, empty, children }) {
  if (resource.loading)
    return (
      <div className="ops-empty" role="status">
        Loading data…
      </div>
    );
  if (resource.error)
    return (
      <div className="ops-empty">
        <Notice error={resource.error} />
        <button className="ops-button secondary" onClick={resource.reload}>
          Try again
        </button>
      </div>
    );
  if (empty)
    return (
      <div className="ops-empty">
        <span className="empty-symbol" aria-hidden="true">
          ▤
        </span>
        <h3>No records yet</h3>
        <p>Try changing your filters or refreshing the list.</p>
      </div>
    );
  return children;
}
export function Status({ value }) {
  return (
    <span
      className={`ops-status ${["COMPLETED", "DELIVERED", "CONFIRMED", "ACTIVE"].includes(value) ? "positive" : ["CANCELLED", "FAILED", "INACTIVE"].includes(value) ? "negative" : ""}`}
    >
      {value?.replaceAll("_", " ") || "—"}
    </span>
  );
}
export function Field({ label, children, ...props }) {
  return (
    <label className="ops-field">
      <span>{label}</span>
      {children || <input {...props} />}
    </label>
  );
}
export function Table({ head, children }) {
  return (
    <div className="ops-table-wrap">
      <table className="ops-table">
        <thead>
          <tr>
            {head.map((h) => (
              <th key={h}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  );
}
export function useAction(reload) {
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [success, setSuccess] = useState("");
  async function run(action, message = "Changes saved.") {
    if (busy) return null;
    setBusy(true);
    setError("");
    setSuccess("");
    try {
      const result = await action();
      if (reload) await reload();
      setSuccess(message);
      return result ?? true;
    } catch (e) {
      setError(errorMessage(e));
      return null;
    } finally {
      setBusy(false);
    }
  }
  return {
    busy,
    error,
    success,
    run,
    clear: () => {
      setError("");
      setSuccess("");
    },
  };
}
export const number = (value) =>
  value == null ? "—" : Number(value).toLocaleString("en-GB");
