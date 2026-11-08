import { useEffect, useRef, useState } from "react";
import {
    approveProductApprovalRequest,
    getProductApprovalRequest,
    listPendingProductApprovals,
    rejectProductApprovalRequest,
} from "../../api/businessmanager/productApprovals";
import { productErrorMessage } from "../../api/business/products";
import ProductStatusBadge, {
    formatProductDate,
    formatProductPrice,
} from "../../components/business/products/ProductStatusBadge";
import { businessManagerMenuItems } from "../../components/icons/businessMenuItems";
import MainLayout from "../../components/layout/MainLayout";
import PageHeader from "../../components/layout/PageHeader";
import Alert from "../../components/ui/Alert";
import Button from "../../components/ui/Button";
import Card from "../../components/ui/Card";
import EmptyState from "../../components/ui/EmptyState";
import Input from "../../components/ui/Input";
import Pagination from "../../components/ui/Pagination";
import Spinner from "../../components/ui/Spinner";
import Table from "../../components/ui/Table";
import TextArea from "../../components/ui/TextArea";

const PAGE_SIZE = 10;
const REQUEST_TYPES = {
    CREATE: "Create New",
    UPDATE: "Edit",
    DEACTIVATE: "Hide Product",
};
const FIELD_LABELS = {
    productCode: "Product Code",
    barcode: "Barcode",
    name: "Product Name",
    categoryId: "Category ID",
    categoryName: "Category Name",
    brandId: "Brand ID",
    brandName: "Brand Name",
    baseUnit: "Base Unit",
    purchasePrice: "Purchase Price",
    sellingPrice: "Selling Price",
    imageUrl: "Image URL",
    description: "Product Description",
    category: "Categories",
    brand: "Brand",
    unit: "Unit of Measure",
    productName: "Product Name",
    productStatus: "Product Status",
    quantity: "Quantity",
    minimumStock: "Minimum Stock Level",
    status: "Product Status",
};

function asObject(value) {
    if (value && typeof value === "object" && !Array.isArray(value)) return value;
    if (typeof value === "string") {
        try {
            const parsed = JSON.parse(value);
            if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) return parsed;
        } catch {
            // Older snapshots are not always valid JSON.
        }
    }
    return {};
}

function formatValue(field, value) {
    if (value === null || value === undefined || value === "") return "—";
    if (field === "purchasePrice" || field === "sellingPrice") return formatProductPrice(value);
    if (typeof value === "boolean") return value ? "Yes" : "Unable";
    if (typeof value === "object") {
        return Object.entries(value).map(([key, part]) => `${FIELD_LABELS[key] || key}: ${formatValue(key, part)}`).join("; ") || "—";
    }
    return String(value);
}

function makeComparisonRows(detail) {
    const oldData = asObject(detail?.oldValue);
    const newData = asObject(detail?.newValue);
    const isCreate = detail?.requestType === "CREATE";
    const keys = Array.from(new Set([...Object.keys(oldData), ...Object.keys(newData)]));
    return keys
        .map((key) => {
            const label = FIELD_LABELS[key] || key.replace(/([a-z])([A-Z])/g, "$1 $2").replace(/_/g, " ");
            const hasOld = Object.hasOwn(oldData, key);
            const hasNew = Object.hasOwn(newData, key);
            const before = hasOld ? formatValue(key, oldData[key]) : "—";
            const after = hasNew ? formatValue(key, newData[key]) : "No Change Requested";
            const changed = hasNew && (isCreate || !hasOld || JSON.stringify(oldData[key]) !== JSON.stringify(newData[key]));
            return { key, label, before, after, changed };
        });
}

function TypeBadge({ type }) {
    return (
        <span className="inline-flex rounded-full bg-[#fff1e9] px-3 py-1 text-xs font-semibold text-[#c94810]">
            {REQUEST_TYPES[type] || type || "—"}
        </span>
    );
}

function DetailField({ label, value }) {
    return (
        <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</dt>
            <dd className="mt-1 break-words text-sm font-medium text-slate-900">{value || "—"}</dd>
        </div>
    );
}

export default function ProductApprovalsPage() {
    const [page, setPage] = useState(0);
    const [reload, setReload] = useState(0);
    const [data, setData] = useState({ items: [], totalElements: 0, totalPages: 0 });
    const [loading, setLoading] = useState(true);
    const [listError, setListError] = useState("");

    const [lookupId, setLookupId] = useState("");
    const [detail, setDetail] = useState(null);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailError, setDetailError] = useState("");
    const [action, setAction] = useState("");
    const [reason, setReason] = useState("");
    const [confirmed, setConfirmed] = useState(false);
    const [actionError, setActionError] = useState("");
    const [saving, setSaving] = useState(false);
    const [notice, setNotice] = useState("");
    const detailControllerRef = useRef(null);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setListError("");
        listPendingProductApprovals({ page, size: PAGE_SIZE }, controller.signal)
            .then((result) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(result?.items)) throw new Error("Invalid approval request list data");
                if (page > 0 && result.totalPages > 0 && page >= result.totalPages) {
                    setPage(result.totalPages - 1);
                    return;
                }
                if (page > 0 && result.totalPages === 0) {
                    setPage(0);
                    return;
                }
                setData(result);
            })
            .catch((error) => {
                if (!controller.signal.aborted) {
                    setListError(productErrorMessage(error, "Unable to load pending approval requests."));
                }
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [page, reload]);

    useEffect(() => () => detailControllerRef.current?.abort(), []);

    const loadDetail = async (requestId) => {
        if (saving) return;
        detailControllerRef.current?.abort();
        const controller = new AbortController();
        detailControllerRef.current = controller;
        setDetail(null);
        setDetailLoading(true);
        setDetailError("");
        setAction("");
        setReason("");
        setConfirmed(false);
        setActionError("");
        setNotice("");
        try {
            const result = await getProductApprovalRequest(requestId, controller.signal);
            if (!controller.signal.aborted) {
                if (!result || !result.id) throw new Error("Invalid product details");
                setDetail(result);
            }
        } catch (error) {
            if (!controller.signal.aborted) {
                setDetailError(productErrorMessage(error, "Unable to load request details."));
            }
        } finally {
            if (!controller.signal.aborted) setDetailLoading(false);
        }
    };

    const lookupRequest = (event) => {
        event.preventDefault();
        if (saving) return;
        if (!/^[1-9]\d*$/.test(lookupId.trim())) {
            setDetailError("Enter a valid request number (e.g., 6).");
            return;
        }
        loadDetail(lookupId.trim());
    };

    const selectAction = (nextAction) => {
        setAction(nextAction);
        setConfirmed(false);
        setActionError("");
        setReason("");
    };

    const submitDecision = async (event) => {
        event.preventDefault();
        if (saving || !confirmed || !detail || detail.status !== "PENDING") return;
        const trimmedReason = reason.trim();
        if (action !== "APPROVE" && action !== "REJECT") return;
        if (action === "REJECT" && (!trimmedReason || trimmedReason.length > 500)) {
            setActionError("Rejection reason is required and must not exceed 500 characters.");
            return;
        }
        setSaving(true);
        setActionError("");
        try {
            const result = action === "APPROVE"
                ? await approveProductApprovalRequest(detail.id)
                : await rejectProductApprovalRequest(detail.id, trimmedReason);
            setDetail(result);
            setNotice(`Successfully ${action === "APPROVE" ? "approved" : "rejected"} request ${result?.requestCode || `#${detail.id}`} . Business Staff can view the result under My Product Requests.`);
            setAction("");
            setReason("");
            setConfirmed(false);
            setReload((value) => value + 1);
        } catch (error) {
            setActionError(productErrorMessage(error, "Unable to process the request. It may have been handled by another user."));
            // Refresh the pending list so concurrent decisions are reflected.
            setReload((value) => value + 1);
        } finally {
            setSaving(false);
        }
    };

    const closeDetail = () => {
        if (saving) return;
        detailControllerRef.current?.abort();
        setDetail(null);
        setDetailLoading(false);
        setDetailError("");
        setAction("");
        setConfirmed(false);
        setActionError("");
    };

    const comparisonRows = detail ? makeComparisonRows(detail) : [];
    const columns = [
        { key: "id", label: "No.", render: (value) => <span className="font-semibold text-slate-800">#{value}</span> },
        { key: "requestCode", label: "Request Code", render: (value) => <span className="break-all font-medium text-slate-900">{value || "—"}</span> },
        { key: "requestType", label: "Request Type", render: (value) => <TypeBadge type={value} /> },
        { key: "productName", label: "Product", render: (value, row) => <div><p className="font-medium text-slate-900">{value || "—"}</p><p className="text-xs text-slate-500">{row.productCode || "—"}</p></div> },
        { key: "createdByName", label: "Submitted By", render: (value) => value || "—" },
        { key: "createdAt", label: "Submitted At", render: formatProductDate },
        { key: "status", label: "Status", render: (value) => <ProductStatusBadge status={value} /> },
        { key: "id-action", label: "Actions", render: (_value, row) => <Button size="sm" variant="outline" onClick={() => loadDetail(row.id)} disabled={saving || detailLoading}>View / Process</Button> },
    ];

    return (
        <MainLayout title="Business Operations" items={businessManagerMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Product Approvals"
                    description="Review product requests submitted by Business Staff and decide whether to approve or reject them."
                    actions={<Button variant="outline" onClick={() => setReload((value) => value + 1)} disabled={loading || saving}>Refresh</Button>}
                />

                {notice && <div role="status"><Alert type="success">{notice}</Alert></div>}
                <Card className="space-y-4">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                        <div>
                            <h2 className="font-semibold text-slate-900">Pending Approval Requests</h2>
                            <p className="mt-1 text-sm text-slate-500">Total: <strong>{data.totalElements ?? 0}</strong> requests awaiting review.</p>
                        </div>
                        <span className="rounded-xl bg-amber-50 px-3 py-2 text-xs text-amber-800">Only pending requests are shown</span>
                    </div>
                    {listError && <div role="alert"><Alert type="danger">{listError}</Alert></div>}
                    {loading ? <div role="status" className="flex items-center gap-3 py-8"><Spinner /> Loading requests...</div>
                        : !listError && data.items.length === 0 ? <EmptyState title="No Pending Requests" description="There are no product requests awaiting approval." />
                            : !listError ? <Table columns={columns} data={data.items} /> : null}
                    {!loading && !listError && data.totalPages > 1 && (
                        <Pagination page={page + 1} totalPages={data.totalPages} onChange={(value) => { setPage(value - 1); closeDetail(); }} />
                    )}
                </Card>

                <Card>
                    <h2 className="font-semibold text-slate-900">Lookup Processed Request</h2>
                    <p className="mt-1 text-sm text-slate-500">Enter the request number to review a previously approved or rejected request.</p>
                    <form onSubmit={lookupRequest} className="mt-3 flex max-w-xl flex-col items-stretch gap-2 sm:flex-row sm:items-end">
                        <div className="flex-1"><Input label="Request Number" value={lookupId} onChange={(event) => { setLookupId(event.target.value); setDetailError(""); }} placeholder="Example: 6" inputMode="numeric" /></div>
                        <Button type="submit" variant="outline" disabled={saving || detailLoading}>Search</Button>
                    </form>
                </Card>

                {detailLoading && <Card><div role="status" className="flex items-center gap-3"><Spinner /> Loading request details...</div></Card>}
                {detailError && <div role="alert"><Alert type="danger">{detailError}</Alert></div>}
                {detail && (
                    <Card className="space-y-5 border-orange-200" >
                        <div className="flex flex-wrap items-start justify-between gap-3">
                            <div className="min-w-0">
                                <h2 className="break-all text-lg font-bold text-slate-900">Details {detail.requestCode || `request #${detail.id}`}</h2>
                                <p className="mt-1 text-sm text-slate-500">Product: {detail.productName || "—"} · {detail.productCode || "—"} · Request #{detail.id}</p>
                            </div>
                            <Button variant="ghost" size="sm" onClick={closeDetail} disabled={saving}>Close</Button>
                        </div>
                        <div className="grid gap-4 rounded-xl bg-slate-50 p-4 sm:grid-cols-2 lg:grid-cols-4">
                            <DetailField label="Request Type" value={REQUEST_TYPES[detail.requestType] || detail.requestType} />
                            <div><dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Status</dt><dd className="mt-1"><ProductStatusBadge status={detail.status} /></dd></div>
                            <DetailField label="Requested By" value={detail.createdByName || (detail.createdById ? `User ID ${detail.createdById}` : "")} />
                            <DetailField label="Submitted At" value={formatProductDate(detail.createdAt)} />
                            <DetailField label="Reviewed By" value={detail.approvedByName || (detail.approvedById ? `User ID ${detail.approvedById}` : "Pending")} />
                            <DetailField label="Reviewed At" value={formatProductDate(detail.approvedAt)} />
                            <DetailField label="Product Reference ID" value={detail.productId ? String(detail.productId) : ""} />
                            <DetailField label="Request Code" value={detail.requestCode} />
                        </div>
                        {detail.requestReason && <Alert type="info"><strong>Request Reason:</strong> {detail.requestReason}</Alert>}
                        {detail.rejectionReason && <Alert type="danger"><strong>Rejection Reason:</strong> {detail.rejectionReason}</Alert>}

                        <div className="space-y-3">
                            <div>
                                <h3 className="font-semibold text-slate-900">Compare Product Information</h3>
                                <p className="mt-1 text-sm text-slate-500">Fields with proposed values different from the existing values are highlighted for comparison.</p>
                            </div>
                            {comparisonRows.length > 0 ? (
                                <div className="overflow-x-auto rounded-xl border border-slate-200">
                                    <table className="min-w-full divide-y divide-slate-200 text-sm">
                                        <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-600">
                                            <tr><th className="px-4 py-3">Field</th><th className="px-4 py-3">Current Value</th><th className="px-4 py-3">Proposed Value</th></tr>
                                        </thead>
                                        <tbody className="divide-y divide-slate-100">
                                            {comparisonRows.map((row) => (
                                                <tr key={row.key}>
                                                    <td className="px-4 py-3 font-medium text-slate-700">{row.label}</td>
                                                    <td className="max-w-xs break-all px-4 py-3 text-slate-600">{row.before}</td>
                                                    <td className={`max-w-xs break-all px-4 py-3 ${row.changed ? "bg-orange-50 font-semibold text-[#c94810]" : "text-slate-700"}`}>{row.after}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            ) : <Alert type="warning">This request does not contain enough information for comparison. Review the supporting data before deciding.</Alert>}
                        </div>

                        {detail.status === "PENDING" ? (
                            <div className="space-y-4 rounded-2xl border border-orange-200 bg-orange-50/50 p-4">
                                <div>
                                    <h3 className="font-semibold text-slate-900">Approval Decision</h3>
                                    <p className="mt-1 text-sm text-slate-600">Your decision will be saved. Review the information carefully before confirming.</p>
                                </div>
                                <div className="flex flex-wrap gap-2">
                                    <Button type="button" variant={action === "APPROVE" ? "success" : "outline"} onClick={() => selectAction("APPROVE")} disabled={saving}>Approve</Button>
                                    <Button type="button" variant={action === "REJECT" ? "danger" : "outline"} onClick={() => selectAction("REJECT")} disabled={saving}>Reject</Button>
                                    {action && <Button type="button" variant="ghost" onClick={() => selectAction("")} disabled={saving}>Cancel Action</Button>}
                                </div>
                                {action && (
                                    <form onSubmit={submitDecision} className="space-y-3 rounded-xl border border-slate-200 bg-white p-4">
                                        {action === "REJECT" && (
                                            <TextArea
                                                label="Rejection Reason * (maximum 500 characters)"
                                                rows={3}
                                                value={reason}
                                                maxLength={500}
                                                required
                                                onChange={(event) => { setReason(event.target.value); setConfirmed(false); setActionError(""); }}
                                                placeholder="Enter specific feedback so Business Staff can revise and resubmit the request"
                                            />
                                        )}
                                        <label className="flex items-start gap-2 text-sm text-slate-700">
                                            <input type="checkbox" className="mt-0.5 h-4 w-4 accent-[#f25d19]" checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} disabled={saving} />
                                            <span>I have reviewed request #{detail.id} and confirm the decision to {action === "APPROVE" ? "approved" : "rejected"} in the system.</span>
                                        </label>
                                        {actionError && <div role="alert"><Alert type="danger">{actionError}</Alert></div>}
                                        <Button type="submit" variant={action === "APPROVE" ? "success" : "danger"} disabled={saving || !confirmed || (action === "REJECT" && (!reason.trim() || reason.trim().length > 500))}>
                                            {saving ? "Processing..." : action === "APPROVE" ? "Confirm Approval" : "Confirm Rejection"}
                                        </Button>
                                    </form>
                                )}
                            </div>
                        ) : <Alert type="info">This request has already been processed and cannot be reviewed again.</Alert>}
                    </Card>
                )}
            </div>
        </MainLayout>
    );
}

