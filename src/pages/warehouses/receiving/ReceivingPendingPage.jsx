import { useEffect, useMemo, useState } from "react";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Button from "../../../components/ui/Button";
import Alert from "../../../components/ui/Alert";
import Spinner from "../../../components/ui/Spinner";
import EmptyState from "../../../components/ui/EmptyState";
import { getPendingWarehouseGoodsReceipts } from "../../../api/receipts/receiving/pendingGoodsReceipts";

function formatDate(value) {
    if (!value) return "-";
    const date = new Date(value);
    return Number.isNaN(date.getTime())
        ? String(value)
        : date.toLocaleString("vi-VN");
}

export default function ReceivingPendingPage() {
    const [receipts, setReceipts] = useState([]);
    const [search, setSearch] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [refresh, setRefresh] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        getPendingWarehouseGoodsReceipts(controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(data)) {
                    throw new Error("Unexpected receipt list response");
                }
                setReceipts(data);
            })
            .catch((requestError) => {
                if (controller.signal.aborted) return;
                const status = requestError.response?.status;
                const message = requestError.response?.data?.message;
                setError(status === 403
                    ? "Access denied (403). Ask the team to verify the Warehouse role mapping in Backend."
                    : (typeof message === "string" ? message : requestError.message || "Cannot load pending receipts."));
                setReceipts([]);
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoading(false);
            });
        return () => controller.abort();
    }, [refresh]);

    const matching = useMemo(() => {
        const needle = search.trim().toLocaleLowerCase();
        if (!needle) return receipts;
        return receipts.filter((item) => [
            item.receiptCode,
            item.supplierName,
            item.warehouseName,
        ].some((value) => String(value ?? "").toLocaleLowerCase().includes(needle)));
    }, [receipts, search]);

    return (
        <MainLayout>
            <div className="space-y-6">
                <PageHeader
                    title="Pending Goods Receipts"
                    description="Goods receipts in CREATED status assigned to your warehouse. This list is read-only."
                    actions={
                        <Button type="button" variant="outline" onClick={() => setRefresh((v) => v + 1)} disabled={loading}>
                            Refresh
                        </Button>
                    }
                />
                <Alert type="info">
                    A CREATED receipt is awaiting the warehouse arrival confirmation workflow. It does not increase inventory yet.
                </Alert>
                <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                    <label htmlFor="pendingReceiptSearch" className="mb-2 block text-sm font-semibold text-slate-700">
                        Search by receipt code, supplier or warehouse
                    </label>
                    <input
                        id="pendingReceiptSearch"
                        type="search"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        placeholder="Search pending receipts..."
                        className="w-full rounded-xl border border-slate-300 px-4 py-3 text-sm outline-none focus:border-orange-500"
                    />
                    <p className="mt-3 text-sm text-slate-500">
                        {matching.length} of {receipts.length} pending receipt(s)
                    </p>
                </div>
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white p-6 text-slate-600">
                        <Spinner /> Loading pending receipts...
                    </div>
                ) : !error && matching.length === 0 ? (
                    <EmptyState title="No pending receipts" description="No CREATED goods receipts match your search and assigned warehouse." />
                ) : !error ? (
                    <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white shadow-sm">
                        <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
                            <thead className="bg-orange-50 text-slate-600">
                                <tr>
                                    <th scope="col" className="px-5 py-4">Receipt Code</th>
                                    <th scope="col" className="px-5 py-4">Supplier</th>
                                    <th scope="col" className="px-5 py-4">Receiving Warehouse</th>
                                    <th scope="col" className="px-5 py-4">Products</th>
                                    <th scope="col" className="px-5 py-4">Expected Qty</th>
                                    <th scope="col" className="px-5 py-4">Created At</th>
                                    <th scope="col" className="px-5 py-4">Status</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                                {matching.map((receipt) => (
                                    <tr key={receipt.id ?? receipt.receiptCode} className="hover:bg-slate-50">
                                        <td className="px-5 py-4 font-semibold text-[#f25d19]">{receipt.receiptCode}</td>
                                        <td className="px-5 py-4">{receipt.supplierName || "-"}</td>
                                        <td className="px-5 py-4">{receipt.warehouseName || "-"}</td>
                                        <td className="px-5 py-4">{receipt.totalProducts ?? "-"}</td>
                                        <td className="px-5 py-4">{receipt.totalExpectedQuantity ?? "-"}</td>
                                        <td className="whitespace-nowrap px-5 py-4">{formatDate(receipt.createdAt)}</td>
                                        <td className="px-5 py-4">
                                            <span className="rounded-full bg-amber-100 px-3 py-1 text-xs font-semibold text-amber-800">
                                                CREATED
                                            </span>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                ) : null}
            </div>
        </MainLayout>
    );
}
