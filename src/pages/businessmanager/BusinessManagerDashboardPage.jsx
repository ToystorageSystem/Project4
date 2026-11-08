import { useEffect, useState } from "react";
import MainLayout from "../../components/layout/MainLayout";
import PageHeader from "../../components/layout/PageHeader";
import MetricCard, { formatCount } from "../../components/business/MetricCard";
import { businessManagerMenuItems } from "../../components/icons/businessMenuItems";
import { getBusinessManagerInventory } from "../../api/business/businessDashboard";

export default function BusinessManagerDashboardPage() {
    const [report, setReport] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let cancelled = false;
        getBusinessManagerInventory()
            .then((data) => {
                if (!cancelled) {
                    if (!data || typeof data !== "object" || Array.isArray(data)) {
                        throw new Error("Invalid inventory report data");
                    }
                    setReport(data);
                }
            })
            .catch((err) => {
                if (!cancelled) {
                    setError(err.response?.status === 403
                        ? "You do not have permission to view inventory reports."
                        : "Unable to load inventory report. Check your connection and try again.");
                }
            })
            .finally(() => { if (!cancelled) setLoading(false); });
        return () => { cancelled = true; };
    }, []);

    return (
        <MainLayout title="Business Operations" items={businessManagerMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Business Overview"
                    description="Monitor inventory across warehouses and stores."
                />

                {loading && <p role="status" className="text-sm text-slate-500">Loading inventory report...</p>}
                {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}
                {!loading && !error && report && (
                    <>
                        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                            <MetricCard label="Products in Stock" value={formatCount(report.totalProducts)} />
                            <MetricCard label="Total Physical Stock" value={formatCount(report.totalQuantity)} />
                            <MetricCard label="Available Stock" value={formatCount(report.totalAvailableQuantity)} />
                            <MetricCard label="Reserved Stock" value={formatCount(report.totalReservedQuantity)} />
                        </div>
                        <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                            <h2 className="font-semibold text-slate-900">Data Status</h2>
                            <p className="mt-2 text-sm text-slate-600">Data is synchronized with the system.</p>
                            <p className="mt-1 text-sm text-slate-500">Inventory location records: {formatCount(report.totalBalanceRecords)}</p>
                            {report.snapshotAt && <p className="mt-1 text-xs text-slate-500">Report generated at: {report.snapshotAt}</p>}
                        </section>
                    </>
                )}
            </div>
        </MainLayout>
    );
}
