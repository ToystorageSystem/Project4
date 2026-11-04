import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import Alert from "../../../components/ui/Alert";
import LoadingPage from "../../../components/feedback/LoadingPage";
import StatusBadge from "../../../components/ui/StatusBadge.jsx";

import { getStoreReturns } from "../../../api/stores/returns/storeReturnInspection.js";

export default function StoreReturnsPage() {
    const navigate = useNavigate();
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [search, setSearch] = useState("");

    const load = useCallback(async () => {
        try {
            setLoading(true);
            setError("");
            const response = await getStoreReturns();
            setItems(Array.isArray(response) ? response : []);
        } catch (err) {
            setError(err.response?.data?.message ?? "Unable to load Store Returns.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        load();
    }, [load]);

    const filtered = useMemo(() => {
        const keyword = search.trim().toLowerCase();
        if (!keyword) return items;

        return items.filter((item) =>
            [item?.returnCode, item?.storeName, item?.status, item?.returnType]
                .filter(Boolean)
                .some((value) => String(value).toLowerCase().includes(keyword))
        );
    }, [items, search]);

    return (
        <MainLayout>
            <div className="flex min-h-0 flex-1 flex-col gap-6">
                <PageHeader
                    title="Store Returns"
                    description="Warehouse Manager reviews returned stock, discrepancies and blocked items."
                    actions={<Button variant="outline" onClick={load}>Refresh</Button>}
                />

                <Card>
                    <input
                        value={search}
                        onChange={(event) => setSearch(event.target.value)}
                        placeholder="Search return code, store or status..."
                        className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-[#f25d19]"
                    />
                </Card>

                {error && <Alert type="danger">{error}</Alert>}

                {loading ? (
                    <LoadingPage />
                ) : filtered.length === 0 ? (
                    <Card>
                        <div className="py-10 text-center text-slate-500">No Store Return found.</div>
                    </Card>
                ) : (
                    <div className="grid gap-4 xl:grid-cols-2">
                        {filtered.map((item) => {
                            const discrepancies = Array.isArray(item?.discrepancies)
                                ? item.discrepancies
                                : [];
                            const unresolvedMajor = discrepancies.filter(
                                (report) =>
                                    Boolean(report?.blocking) &&
                                    ["OPEN", "INVESTIGATING"].includes(
                                        String(report?.status ?? "").toUpperCase()
                                    )
                            ).length;

                            return (
                                <Card key={item?.returnId ?? item?.id}>
                                    <div className="flex items-start justify-between gap-4">
                                        <div>
                                            <p className="text-xs font-semibold uppercase tracking-wide text-[#f25d19]">
                                                {item?.returnType ?? "STORE RETURN"}
                                            </p>
                                            <h3 className="mt-1 text-lg font-semibold text-slate-900">
                                                {item?.returnCode ?? `Return #${item?.returnId ?? "-"}`}
                                            </h3>
                                            <p className="mt-1 text-sm text-slate-500">
                                                {item?.storeName ?? `Store #${item?.storeId ?? "-"}`}
                                            </p>
                                        </div>
                                        <StatusBadge status={String(item?.status ?? "-").toUpperCase()} />
                                    </div>

                                    <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                                        <div className="rounded-xl bg-slate-50 p-3">
                                            <p className="text-xs text-slate-400">Received</p>
                                            <p className="mt-1 font-semibold text-slate-800">
                                                {item?.totalReceivedQuantity ?? 0}
                                            </p>
                                        </div>
                                        <div className="rounded-xl bg-slate-50 p-3">
                                            <p className="text-xs text-slate-400">Unresolved MAJOR</p>
                                            <p className={unresolvedMajor ? "mt-1 font-semibold text-red-600" : "mt-1 font-semibold text-emerald-600"}>
                                                {unresolvedMajor}
                                            </p>
                                        </div>
                                    </div>

                                    <div className="mt-4 flex justify-end">
                                        <Button
                                            onClick={() =>
                                                navigate(`/warehouse/store-returns/${item?.returnId ?? item?.id}`)
                                            }
                                        >
                                            Review
                                        </Button>
                                    </div>
                                </Card>
                            );
                        })}
                    </div>
                )}
            </div>
        </MainLayout>
    );
}
