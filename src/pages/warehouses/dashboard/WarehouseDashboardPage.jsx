import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import {
  getReceivingMonitoringList,
  getWaitingReviewList,
  getCompletedReceivingList,
} from "../../../api/receipts/receiving/receivingMonitor";
import { getPutawayPlans } from "../../../api/warehouses/putaway/putawayPlanning";

const sections = [
  {
    title: "Receiving in progress",
    hint: "Receipts being inspected",
    path: "/warehouse/receiving/in-progress",
    color: "bg-blue-50 text-blue-700",
    mark: "↓",
  },
  {
    title: "Waiting for review",
    hint: "Receipts ready for a decision",
    path: "/warehouse/receiving/waiting-review",
    color: "bg-orange-50 text-orange-700",
    mark: "◎",
  },
  {
    title: "Open putaway tasks",
    hint: "Pending, available or in progress",
    path: "/warehouse/putaway",
    color: "bg-violet-50 text-violet-700",
    mark: "▦",
  },
  {
    title: "Receiving completed",
    hint: "All completed receipts",
    path: "/warehouse/receiving/completed",
    color: "bg-emerald-50 text-emerald-700",
    mark: "✓",
  },
];
const shortcuts = [
  [
    "Receive goods",
    "Inspect an incoming receipt",
    "/warehouse/receiving/process",
    "↓",
  ],
  [
    "Store returns",
    "Receive returned products",
    "/warehouse/store-returns",
    "↩",
  ],
  ["Packing", "Review packed transfers", "/warehouse/packing", "□"],
  ["Dispatch", "Hand over outgoing shipments", "/warehouse/dispatch", "↗"],
];
function pageData(data) {
  if (!Array.isArray(data?.content) || !Number.isFinite(data.totalElements))
    throw new Error("Unexpected response");
  return { count: data.totalElements, rows: data.content };
}
function taskData(data) {
  if (!Array.isArray(data)) throw new Error("Unexpected response");
  const rows = data.filter((task) =>
    ["PENDING", "AVAILABLE", "IN_PROGRESS"].includes(task.status),
  );
  return { count: rows.length, rows };
}
function Progress({ value }) {
  const percent = Number.isFinite(value)
    ? Math.max(0, Math.min(100, value))
    : null;
  return (
    <div className="flex items-center gap-3">
      <div className="h-1.5 w-20 overflow-hidden rounded-full bg-slate-100">
        <div
          className="h-full rounded-full bg-[#f25d19]"
          style={{ width: `${percent ?? 0}%` }}
        />
      </div>
      <span className="text-xs tabular-nums text-slate-500">
        {percent === null ? "—" : `${Math.round(percent)}%`}
      </span>
    </div>
  );
}
export default function WarehouseDashboardPage() {
  const [refresh, setRefresh] = useState(0);
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [updated, setUpdated] = useState(null);
  useEffect(() => {
    let current = true;
    Promise.allSettled([
      getReceivingMonitoringList({ size: 5 }).then(pageData),
      getWaitingReviewList({ size: 5 }).then(pageData),
      getPutawayPlans().then(taskData),
      getCompletedReceivingList({ size: 1 }).then(pageData),
    ]).then((results) => {
      if (!current) return;
      setData(
        results.map((result) =>
          result.status === "fulfilled" ? result.value : { error: true },
        ),
      );
      setUpdated(new Date());
      setLoading(false);
    });
    return () => {
      current = false;
    };
  }, [refresh]);
  const failed = data.some((item) => item.error);
  return (
    <MainLayout>
      <div className="space-y-6">
        <PageHeader
          title="Dashboard"
          description="Your warehouse at a glance. A clear view of what needs attention."
          actions={
            <button
              disabled={loading}
              onClick={() => {
                setLoading(true);
                setRefresh((value) => value + 1);
              }}
              className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-50 disabled:opacity-50"
            >
              {loading ? "Loading…" : "Refresh overview"}
            </button>
          }
        />
        <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-slate-500">
          <span className="rounded-full border border-slate-200 bg-white px-3 py-1.5">
            Warehouse operations · All time
          </span>
          <span role="status">
            {loading
              ? "Fetching overview…"
              : `${failed ? "Last attempt" : "Updated"} ${updated?.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`}
          </span>
        </div>
        {failed && !loading && (
          <div
            role="alert"
            className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900"
          >
            Some data could not be loaded. Check your connection or access
            permissions, then refresh. Other sections remain available.
          </div>
        )}
        <section
          aria-label="Warehouse totals"
          className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4"
        >
          {sections.map((section, index) => (
            <Link
              key={section.title}
              to={section.path}
              className="rounded-xl border border-slate-200 bg-white p-4 sm:p-5 transition hover:border-orange-300 hover:shadow-sm"
            >
              <div className="mb-5 flex items-center justify-between gap-2">
                <span className="text-xs font-semibold text-slate-500">
                  {section.title}
                </span>
                <span
                  aria-hidden="true"
                  className={`flex h-8 w-8 items-center justify-center rounded-lg text-lg ${section.color}`}
                >
                  {section.mark}
                </span>
              </div>
              <p className="text-3xl font-bold tracking-tight tabular-nums">
                {loading || data[index]?.error
                  ? "—"
                  : data[index]?.count?.toLocaleString()}
              </p>
              <p className="mt-2 text-xs text-slate-500">
                {!loading && data[index]?.error
                  ? "Unavailable · Open list →"
                  : section.hint}
              </p>
            </Link>
          ))}
        </section>
        <div className="grid items-start gap-5 xl:grid-cols-[minmax(0,1fr)_300px]">
          <section className="overflow-hidden rounded-xl border border-slate-200 bg-white">
            <div className="flex items-center justify-between gap-3 border-b border-slate-100 px-5 py-4">
              <div>
                <h2 className="text-sm font-bold">Receiving activity</h2>
                <p className="mt-1 text-xs text-slate-500">
                  Up to 5 receipts currently being inspected
                </p>
              </div>
              <Link
                className="shrink-0 text-xs font-semibold text-orange-700 hover:underline"
                to="/warehouse/receiving/in-progress"
              >
                View all →
              </Link>
            </div>
            {loading ? (
              <p
                role="status"
                className="p-10 text-center text-sm text-slate-500"
              >
                Loading receipts…
              </p>
            ) : data[0]?.error ? (
              <p className="p-10 text-center text-sm text-slate-500">
                Receiving data is unavailable. Try refreshing.
              </p>
            ) : !data[0]?.rows?.length ? (
              <div className="p-10 text-center">
                <p className="font-semibold text-slate-700">
                  No receipts in progress
                </p>
                <p className="mt-2 text-sm text-slate-500">
                  New inspections will appear here.
                </p>
                <Link
                  to="/warehouse/receiving/process"
                  className="mt-4 inline-block text-sm font-semibold text-orange-700"
                >
                  Receive goods →
                </Link>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full whitespace-nowrap text-left text-sm">
                  <thead className="bg-slate-50 text-[11px] uppercase tracking-wide text-slate-500">
                    <tr>
                      <th className="px-5 py-3 font-medium">Receipt</th>
                      <th className="px-5 py-3 font-medium">Assigned to</th>
                      <th className="px-5 py-3 font-medium">Inspection</th>
                      <th className="px-5 py-3 font-medium">Issues</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {data[0].rows.map((row) => (
                      <tr key={row.receiptId} className="hover:bg-slate-50/70">
                        <td className="px-5 py-4 font-semibold">
                          {row.receiptCode || `#${row.receiptId}`}
                        </td>
                        <td className="px-5 py-4 text-slate-500">
                          {row.staffName || "Unassigned"}
                        </td>
                        <td className="px-5 py-4">
                          <Progress value={row.progressPercent} />
                        </td>
                        <td className="px-5 py-4">
                          <span
                            className={`rounded-md px-2 py-1 text-xs ${row.issueProducts > 0 ? "bg-amber-50 text-amber-700" : "text-slate-400"}`}
                          >
                            {row.issueProducts ?? "—"}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
          <section className="rounded-xl border border-slate-200 bg-white p-5">
            <p className="mb-4 text-[10px] font-bold uppercase tracking-[0.14em] text-slate-400">
              Action center
            </p>
            <h2 className="text-base font-bold">Ready for your review</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500">
              {loading
                ? "Checking the review queue…"
                : data[1]?.error
                  ? "Review queue is unavailable."
                  : data[1]?.count > 0
                    ? `${data[1].count} receipts are waiting for inspection approval.`
                    : "No receipts waiting for review."}
            </p>
            <Link
              to="/warehouse/receiving/waiting-review"
              className="mt-5 flex items-center justify-between rounded-lg bg-[#f25d19] px-4 py-2.5 text-sm font-semibold text-white hover:bg-[#d94f12]"
            >
              Open review queue <span aria-hidden="true">→</span>
            </Link>
            <div className="mt-5 border-t border-slate-100 pt-4">
              <Link
                to="/warehouse/discrepancies"
                className="text-xs font-semibold text-slate-600 hover:text-orange-700"
              >
                Resolve discrepancies →
              </Link>
            </div>
          </section>
        </div>
        <section aria-labelledby="quick-actions">
          <h2 id="quick-actions" className="mb-3 text-sm font-bold">
            Quick actions
          </h2>
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            {shortcuts.map(([title, description, path, icon]) => (
              <Link
                key={path}
                to={path}
                className="flex items-center gap-3 rounded-xl border border-slate-200 bg-white p-4 hover:border-orange-300"
              >
                <span
                  aria-hidden="true"
                  className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-slate-50 text-xl text-slate-500"
                >
                  {icon}
                </span>
                <div>
                  <h3 className="text-sm font-semibold">{title}</h3>
                  <p className="mt-1 text-xs text-slate-500">{description}</p>
                </div>
              </Link>
            ))}
          </div>
        </section>
        <p className="text-xs text-slate-400">
          Counts reflect the records available to your account. Refresh to load
          the latest snapshot.
        </p>
      </div>
    </MainLayout>
  );
}
