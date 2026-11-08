import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
    getSupplierInvoiceDetail,
    supplierInvoiceErrorMessage,
} from "../../../api/business/supplierInvoices";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import EmptyState from "../../../components/ui/EmptyState";
import Spinner from "../../../components/ui/Spinner";
import Table from "../../../components/ui/Table";
import {
    SupplierInvoiceAttachmentUploader,
    SupplierInvoiceCancelAction,
    supplierInvoiceCanChange,
} from "./SupplierInvoiceActions";
import {
    formatSupplierInvoiceDate,
    formatSupplierInvoiceMoney,
    SupplierInvoiceMatchBadge,
    SupplierInvoiceStatusBadge,
} from "./supplierInvoiceFormat";

const ROOT = "/business/supplier-invoices";

function DetailField({ label, value }) {
    return (
        <div className="rounded-xl border border-slate-100 bg-slate-50 p-4">
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
            <p className="mt-1 break-words text-sm font-semibold text-slate-900">{value ?? "—"}</p>
        </div>
    );
}

function SafeAttachmentLink({ label, url }) {
    // Attachment URLs originate from the backend; render only https links.
    if (typeof url !== "string" || !/^https:\/\/[^\s]+$/i.test(url)) return null;
    return <a href={url} target="_blank" rel="noopener noreferrer" className="font-semibold text-[#c94810] underline-offset-2 hover:underline">{label}</a>;
}

export default function SupplierInvoiceDetailPage() {
    const { invoiceId } = useParams();
    const navigate = useNavigate();
    const [detail, setDetail] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [refresh, setRefresh] = useState(0);
    const [notice, setNotice] = useState("");

    useEffect(() => {
        const controller = new AbortController();
        const id = Number(invoiceId);
        setDetail(null);
        setNotice("");
        setError("");
        setLoading(true);
        if (!Number.isSafeInteger(id) || id <= 0) {
            setError("Invalid invoice code.");
            setLoading(false);
            return () => controller.abort();
        }
        getSupplierInvoiceDetail(id, controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;
                if (!data || typeof data !== "object" || !data.id) throw new Error("Invalid invoice information");
                setDetail(data);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setError(supplierInvoiceErrorMessage(requestError, "Unable to load invoice details."));
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoading(false);
            });
        return () => controller.abort();
    }, [invoiceId, refresh]);

    const handleUpdated = (updated, message) => {
        setDetail(updated);
        setError("");
        setNotice(message);
    };

    const lineColumns = [
        { key: "productCode", label: "Product Code", render: (value) => value || "—" },
        { key: "productName", label: "Product", render: (value) => value || "—" },
        { key: "orderedQuantity", label: "Ordered Quantity", render: (value) => value ?? "—" },
        { key: "receivedQuantity", label: "Received Quantity", render: (value) => value ?? "—" },
        { key: "unitPrice", label: "PO Unit Price", render: formatSupplierInvoiceMoney },
        { key: "lineTotal", label: "Purchase Order Line Total", render: formatSupplierInvoiceMoney },
    ];

    const attachments = detail ? [
        { label: "Xem PDF", url: detail.pdfFileUrl },
        { label: "View Image", url: detail.imageFileUrl },
        { label: "Xem XML", url: detail.xmlFileUrl },
    ].filter((entry) => typeof entry.url === "string" && /^https:\/\/[^\s]+$/i.test(entry.url)) : [];

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Supplier Invoice Details"
                    description="View invoice details and reconcile them against the related purchase order. Only unconfirmed invoices can be edited."
                    actions={<>
                        <Button type="button" variant="outline" onClick={() => navigate(ROOT)}>← Back to List</Button>
                        {detail && supplierInvoiceCanChange(detail) && (
                            <Button type="button" onClick={() => navigate(`${ROOT}/${detail.id}/edit`)}>Edit Invoice</Button>
                        )}
                        {detail && <SupplierInvoiceCancelAction invoice={detail} onUpdated={handleUpdated} />}
                        <Button type="button" variant="ghost" onClick={() => setRefresh((value) => value + 1)}>Refresh</Button>
                    </>}
                />
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                {notice && <div role="status"><Alert type="success">{notice}</Alert></div>}
                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-7 text-sm text-slate-600" role="status">
                        <Spinner /> Loading invoice details...
                    </div>
                ) : detail ? (
                    <>
                        <Card>
                            <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-100 pb-4">
                                <div>
                                    <p className="text-sm text-slate-500">Invoice Number</p>
                                    <h2 className="text-xl font-bold text-slate-900">{detail.invoiceNumber || "—"}</h2>
                                    <p className="mt-1 text-sm text-slate-500">System ID: {detail.invoiceCode || "—"}</p>
                                </div>
                                <SupplierInvoiceStatusBadge status={detail.status} />
                            </div>
                            <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                                <DetailField label="Supplier" value={detail.supplierName || "—"} />
                                <DetailField label="Supplier Code / Tax ID" value={`${detail.supplierCode || "—"} / ${detail.supplierTaxCode || "—"}`} />
                                <DetailField label="Linked Purchase Order" value={detail.purchaseOrderId ? (
                                    <Link className="text-[#c94810] hover:underline" to={`/business/purchase-orders/${detail.purchaseOrderId}`}>
                                        {detail.purchaseOrderCode || `PO #${detail.purchaseOrderId}`}
                                    </Link>
                                ) : "—"} />
                                <DetailField label="Purchase Order Status" value={detail.purchaseOrderStatus || "—"} />
                                <DetailField label="Issue Date" value={formatSupplierInvoiceDate(detail.invoiceDate)} />
                                <DetailField label="Due Date" value={formatSupplierInvoiceDate(detail.dueDate)} />
                                <DetailField label="Invoice Series" value={detail.invoiceSeries || "—"} />
                                <DetailField label="Verification Code" value={detail.lookupCode || "—"} />
                                <DetailField label="Uploaded By" value={detail.uploadedByName || "—"} />
                                <DetailField label="Recorded At" value={formatSupplierInvoiceDate(detail.uploadedAt, true)} />
                                <DetailField label="Last Updated" value={formatSupplierInvoiceDate(detail.updatedAt, true)} />
                            </div>
                            {detail.verificationNote && <div className="mt-5 rounded-xl bg-slate-50 p-4 text-sm text-slate-700">Confirmation Notes: {detail.verificationNote}</div>}
                        </Card>
                        <Card>
                            <h3 className="mb-4 text-lg font-semibold text-slate-900">Invoice Amount</h3>
                            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                                <DetailField label="Subtotal (before tax)" value={formatSupplierInvoiceMoney(detail.subtotal)} />
                                <DetailField label="Tax Amount" value={formatSupplierInvoiceMoney(detail.taxAmount)} />
                                <DetailField label="Total Including Tax" value={formatSupplierInvoiceMoney(detail.totalAmount)} />
                            </div>
                        </Card>
                        <Card>
                            <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
                                <h3 className="text-lg font-semibold text-slate-900">Purchase Order Reconciliation</h3>
                                <SupplierInvoiceMatchBadge matched={detail.purchaseOrderMatched} />
                            </div>
                            <p className="mb-4 text-sm text-slate-500">The system compares the invoice <strong>pre-tax amount</strong> with the purchase order amount.</p>
                            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                                <DetailField label="Purchase Order Amount" value={formatSupplierInvoiceMoney(detail.purchaseOrderAmount)} />
                                <DetailField label="Invoice Subtotal" value={formatSupplierInvoiceMoney(detail.subtotal)} />
                                <DetailField label="Pre-Tax Amount Difference" value={formatSupplierInvoiceMoney(detail.amountDifference)} />
                            </div>
                            {detail.purchaseOrderMatched === false && <div className="mt-4" role="status"><Alert type="warning">
                                The invoice subtotal differs from the purchase order amount. Review before continuing.
                                {detail.comparisonWarning && <p className="mt-2 text-xs">Details: {detail.comparisonWarning}</p>}
                            </Alert></div>}
                            {detail.purchaseOrderMatched === true && <div className="mt-4"><Alert type="success">The invoice subtotal matches the purchase order amount.</Alert></div>}
                        </Card>
                        <Card>
                            <h3 className="mb-4 text-lg font-semibold text-slate-900">Purchase Order Items</h3>
                            <p className="mb-4 text-sm text-slate-500">Items are loaded from the linked purchase order.</p>
                            {Array.isArray(detail.purchaseOrderItems) && detail.purchaseOrderItems.length > 0 ? (
                                <Table columns={lineColumns} data={detail.purchaseOrderItems} />
                            ) : (
                                <EmptyState title="No purchase order item information available" />
                            )}
                        </Card>
                        <Card>
                            <h3 className="mb-3 text-lg font-semibold text-slate-900">Invoice Attachment</h3>
                            {attachments.length > 0 ? (
                                <div className="flex flex-wrap gap-4 text-sm">
                                    {attachments.map((entry) => <SafeAttachmentLink key={entry.label} label={entry.label} url={entry.url} />)}
                                </div>
                            ) : (
                                <p className="text-sm text-slate-500">No supporting document is attached to this invoice.</p>
                            )}
                            {supplierInvoiceCanChange(detail) ? (
                                <SupplierInvoiceAttachmentUploader invoice={detail} onUpdated={handleUpdated} />
                            ) : (
                                <p className="mt-4 text-xs text-slate-500">Attachments can be added or replaced only for Draft or Pending Verification invoices.</p>
                            )}
                        </Card>
                        <Alert type="info">Only Draft or Pending Verification invoices can be edited, cancelled, or have attachments added. Confirmation is required for each change.</Alert>
                    </>
                ) : !error ? <EmptyState title="Supplier Invoice Not Found" /> : null}
            </div>
        </MainLayout>
    );
}
