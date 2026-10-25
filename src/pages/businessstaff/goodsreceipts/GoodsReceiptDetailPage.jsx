import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { getGoodsReceiptById, goodsReceiptErrorMessage } from "../../../api/business/goodsReceipts";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import EmptyState from "../../../components/ui/EmptyState";
import Spinner from "../../../components/ui/Spinner";
import Table from "../../../components/ui/Table";
import { formatReceiptDate, formatReceiptMoney, receiptStatusLabels, receiptText as T } from "./receiptText";

const ROOT = "/business-staff/goods-receipts";

function StatusBadge({ status }) {
    const style = status === "COMPLETED"
        ? "bg-emerald-100 text-emerald-800"
        : status === "CANCELLED"
            ? "bg-red-100 text-red-800"
            : "bg-amber-100 text-amber-800";
    return <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${style}`}>
        {receiptStatusLabels[status] || status || "-"}
    </span>;
}

function Info({ label, value }) {
    return (
        <div className="rounded-xl border border-slate-100 bg-slate-50 p-4">
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
            <p className="mt-1 break-words text-sm font-semibold text-slate-900">{value ?? "-"}</p>
        </div>
    );
}

export default function GoodsReceiptDetailPage() {
    const { receiptId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const [receipt, setReceipt] = useState(null);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(true);
    const [reload, setReload] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        const numericId = Number(receiptId);
        setError("");
        setLoading(true);
        if (!Number.isSafeInteger(numericId) || numericId <= 0) {
            setError(T.invalidReceiptId);
            setLoading(false);
            return () => controller.abort();
        }
        getGoodsReceiptById(numericId, controller.signal)
            .then((result) => { if (!controller.signal.aborted) setReceipt(result); })
            .catch((requestError) => {
                if (!controller.signal.aborted) setError(goodsReceiptErrorMessage(requestError, T.detailError));
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [receiptId, reload]);

    const columns = [
        { key: "productCode", label: "SKU" },
        { key: "productName", label: T.product },
        { key: "expectedQuantity", label: T.quantity },
        { key: "unitPrice", label: T.price, render: (value) => formatReceiptMoney(value) },
        { key: "subtotal", label: T.subtotal, render: (value) => formatReceiptMoney(value) },
    ];

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader title={T.detailTitle} description={T.draftNote}
                    actions={<Button type="button" variant="outline" onClick={() => navigate(ROOT)}>{T.back}</Button>} />
                {location.state?.notice && <div role="status"><Alert type="success">{location.state.notice}</Alert></div>}
                {error && <div role="alert" className="space-y-2"><Alert type="danger">{error}</Alert>
                    <Button type="button" variant="outline" onClick={() => setReload((x) => x + 1)}>{T.retry}</Button>
                </div>}
                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-6 text-sm text-slate-600"><Spinner />{T.loading}</div>
                ) : receipt && !error ? (
                    <>
                        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
                            <div className="mb-5 flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4">
                                <div>
                                    <p className="text-sm text-slate-500">{T.receiptCode}</p>
                                    <h2 className="text-xl font-bold text-slate-900">{receipt.receiptCode}</h2>
                                </div>
                                <StatusBadge status={receipt.status} />
                            </div>
                            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                                <Info label={T.supplier.replace(" *", "")} value={`${receipt.supplierCode || ""} - ${receipt.supplierName || ""}`} />
                                <Info label={T.warehouse.replace(" *", "")} value={`${receipt.warehouseCode || ""} - ${receipt.warehouseName || ""}`} />
                                <Info label={T.purchaseOrder} value={receipt.purchaseOrderCode} />
                                <Info label={T.receiptStatus} value={receiptStatusLabels[receipt.status] || receipt.status} />
                                <Info label={T.creator} value={receipt.createdByName} />
                                <Info label={T.created} value={formatReceiptDate(receipt.createdAt)} />
                            </div>
                            {receipt.note && (
                                <div className="mt-5 rounded-xl bg-slate-50 p-4">
                                    <p className="mb-1 text-sm font-semibold text-slate-500">{T.note}</p>
                                    <p className="whitespace-pre-wrap text-sm text-slate-900">{receipt.note}</p>
                                </div>
                            )}
                        </div>
                        <div className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
                            <h2 className="text-lg font-semibold text-slate-900">{T.itemTable}</h2>
                            {Array.isArray(receipt.items) && receipt.items.length ? (
                                <Table rowKey="productId" columns={columns} data={receipt.items} />
                            ) : <EmptyState title={T.noItems} />}
                            <div className="flex justify-end border-t border-slate-100 pt-4">
                                <div className="text-right">
                                    <p className="text-sm text-slate-500">{T.total}</p>
                                    <strong className="text-2xl text-slate-900">{formatReceiptMoney(receipt.estimatedTotalAmount)}</strong>
                                </div>
                            </div>
                        </div>
                        <Alert type="info">{T.warehouseNotice}</Alert>
                        {receipt.status !== "COMPLETED" && <Alert type="warning">{T.draftNote}</Alert>}
                    </>
                ) : !error ? <EmptyState title={T.unknown} /> : null}
            </div>
        </MainLayout>
    );
}
