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
    CREATE: "Thêm mới",
    UPDATE: "Chỉnh sửa",
    DEACTIVATE: "Ẩn sản phẩm",
};
const FIELD_LABELS = {
    productCode: "Mã sản phẩm",
    barcode: "Barcode",
    name: "Tên sản phẩm",
    categoryId: "ID danh mục",
    categoryName: "Tên danh mục",
    brandId: "ID thương hiệu",
    brandName: "Tên thương hiệu",
    baseUnit: "Đơn vị cơ sở",
    purchasePrice: "Giá nhập",
    sellingPrice: "Giá bán",
    imageUrl: "Đường dẫn hình ảnh",
    status: "Trạng thái sản phẩm",
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
    if (typeof value === "boolean") return value ? "Có" : "Không";
    if (typeof value === "object") return JSON.stringify(value);
    return String(value);
}

function makeComparisonRows(detail) {
    const oldData = asObject(detail?.oldValue);
    const newData = asObject(detail?.newValue);
    const isCreate = detail?.requestType === "CREATE";
    return Object.entries(FIELD_LABELS)
        .filter(([key]) => Object.hasOwn(oldData, key) || Object.hasOwn(newData, key))
        .map(([key, label]) => {
            const hasOld = Object.hasOwn(oldData, key);
            const hasNew = Object.hasOwn(newData, key);
            const before = hasOld ? formatValue(key, oldData[key]) : "—";
            const after = hasNew ? formatValue(key, newData[key]) : "Không đề xuất đổi";
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
                if (!Array.isArray(result?.items)) throw new Error("Invalid approval page response");
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
                    setListError(productErrorMessage(error, "Không tải được các yêu cầu chờ duyệt."));
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
                if (!result || !result.id) throw new Error("Invalid approval detail response");
                setDetail(result);
            }
        } catch (error) {
            if (!controller.signal.aborted) {
                setDetailError(productErrorMessage(error, "Không tải được chi tiết yêu cầu."));
            }
        } finally {
            if (!controller.signal.aborted) setDetailLoading(false);
        }
    };

    const lookupRequest = (event) => {
        event.preventDefault();
        if (saving) return;
        if (!/^[1-9]\d*$/.test(lookupId.trim())) {
            setDetailError("Nhập ID yêu cầu là số nguyên dương (ví dụ: 6).");
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
            setActionError("Lý do từ chối bắt buộc, tối đa 500 ký tự.");
            return;
        }
        setSaving(true);
        setActionError("");
        try {
            const result = action === "APPROVE"
                ? await approveProductApprovalRequest(detail.id)
                : await rejectProductApprovalRequest(detail.id, trimmedReason);
            setDetail(result);
            setNotice(`Đã ${action === "APPROVE" ? "phê duyệt" : "từ chối"} yêu cầu ${result?.requestCode || `#${detail.id}`} thành công. Business Staff có thể xem kết quả trong mục Yêu cầu sản phẩm của tôi.`);
            setAction("");
            setReason("");
            setConfirmed(false);
            setReload((value) => value + 1);
        } catch (error) {
            setActionError(productErrorMessage(error, "Không xử lý được yêu cầu. Có thể yêu cầu đã được người khác xử lý."));
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
        { key: "id", label: "ID", render: (value) => <span className="font-semibold text-slate-800">#{value}</span> },
        { key: "requestCode", label: "Mã yêu cầu", render: (value) => <span className="break-all font-medium text-slate-900">{value || "—"}</span> },
        { key: "requestType", label: "Loại yêu cầu", render: (value) => <TypeBadge type={value} /> },
        { key: "productName", label: "Sản phẩm", render: (value, row) => <div><p className="font-medium text-slate-900">{value || "—"}</p><p className="text-xs text-slate-500">{row.productCode || "—"}</p></div> },
        { key: "createdByName", label: "Người gửi", render: (value) => value || "—" },
        { key: "createdAt", label: "Ngày gửi", render: formatProductDate },
        { key: "status", label: "Trạng thái", render: (value) => <ProductStatusBadge status={value} /> },
        { key: "id-action", label: "Thao tác", render: (_value, row) => <Button size="sm" variant="outline" onClick={() => loadDetail(row.id)} disabled={saving || detailLoading}>Xem / Xử lý</Button> },
    ];

    return (
        <MainLayout title="Business Manager" items={businessManagerMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Duyệt sản phẩm"
                    description="Xem yêu cầu Business Staff gửi lên, kiểm tra dữ liệu và quyết định duyệt hoặc từ chối."
                    actions={<Button variant="outline" onClick={() => setReload((value) => value + 1)} disabled={loading || saving}>Làm mới</Button>}
                />

                {notice && <div role="status"><Alert type="success">{notice}</Alert></div>}
                <Card className="space-y-4">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                        <div>
                            <h2 className="font-semibold text-slate-900">Danh sách yêu cầu chờ duyệt</h2>
                            <p className="mt-1 text-sm text-slate-500">Tổng số: <strong>{data.totalElements ?? 0}</strong> yêu cầu đang PENDING.</p>
                        </div>
                        <span className="rounded-xl bg-amber-50 px-3 py-2 text-xs text-amber-800">Chỉ hiển thị yêu cầu chờ duyệt</span>
                    </div>
                    {listError && <div role="alert"><Alert type="danger">{listError}</Alert></div>}
                    {loading ? <div role="status" className="flex items-center gap-3 py-8"><Spinner /> Đang tải yêu cầu...</div>
                        : !listError && data.items.length === 0 ? <EmptyState title="Không có yêu cầu chờ duyệt" description="Hiện chưa có yêu cầu sản phẩm nào cần Business Manager xử lý." />
                            : !listError ? <Table columns={columns} data={data.items} /> : null}
                    {!loading && !listError && data.totalPages > 1 && (
                        <Pagination page={page + 1} totalPages={data.totalPages} onChange={(value) => { setPage(value - 1); closeDetail(); }} />
                    )}
                </Card>

                <Card>
                    <h2 className="font-semibold text-slate-900">Tra cứu yêu cầu đã xử lý theo ID</h2>
                    <p className="mt-1 text-sm text-slate-500">Backend hỗ trợ xem lại yêu cầu theo ID, kể cả APPROVED hoặc REJECTED. Chưa có API danh sách lịch sử toàn hệ thống.</p>
                    <form onSubmit={lookupRequest} className="mt-3 flex max-w-xl flex-col items-stretch gap-2 sm:flex-row sm:items-end">
                        <div className="flex-1"><Input label="ID yêu cầu" value={lookupId} onChange={(event) => { setLookupId(event.target.value); setDetailError(""); }} placeholder="Ví dụ: 6" inputMode="numeric" /></div>
                        <Button type="submit" variant="outline" disabled={saving || detailLoading}>Tra cứu</Button>
                    </form>
                </Card>

                {detailLoading && <Card><div role="status" className="flex items-center gap-3"><Spinner /> Đang tải chi tiết yêu cầu...</div></Card>}
                {detailError && <div role="alert"><Alert type="danger">{detailError}</Alert></div>}
                {detail && (
                    <Card className="space-y-5 border-orange-200" >
                        <div className="flex flex-wrap items-start justify-between gap-3">
                            <div className="min-w-0">
                                <h2 className="break-all text-lg font-bold text-slate-900">Chi tiết {detail.requestCode || `yêu cầu #${detail.id}`}</h2>
                                <p className="mt-1 text-sm text-slate-500">Sản phẩm: {detail.productName || "—"} · {detail.productCode || "—"} · ID yêu cầu #{detail.id}</p>
                            </div>
                            <Button variant="ghost" size="sm" onClick={closeDetail} disabled={saving}>Đóng</Button>
                        </div>
                        <div className="grid gap-4 rounded-xl bg-slate-50 p-4 sm:grid-cols-2 lg:grid-cols-4">
                            <DetailField label="Loại yêu cầu" value={REQUEST_TYPES[detail.requestType] || detail.requestType} />
                            <div><dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Trạng thái</dt><dd className="mt-1"><ProductStatusBadge status={detail.status} /></dd></div>
                            <DetailField label="Người yêu cầu" value={detail.createdByName || (detail.createdById ? `ID ${detail.createdById}` : "")} />
                            <DetailField label="Ngày gửi" value={formatProductDate(detail.createdAt)} />
                            <DetailField label="Người duyệt / từ chối" value={detail.approvedByName || (detail.approvedById ? `ID ${detail.approvedById}` : "Chưa xử lý")} />
                            <DetailField label="Thời điểm xử lý" value={formatProductDate(detail.approvedAt)} />
                            <DetailField label="ID sản phẩm" value={detail.productId ? String(detail.productId) : ""} />
                            <DetailField label="Mã yêu cầu" value={detail.requestCode} />
                        </div>
                        {detail.requestReason && <Alert type="info"><strong>Lý do đề nghị:</strong> {detail.requestReason}</Alert>}
                        {detail.rejectionReason && <Alert type="danger"><strong>Lý do đã từ chối:</strong> {detail.rejectionReason}</Alert>}

                        <div className="space-y-3">
                            <div>
                                <h3 className="font-semibold text-slate-900">So sánh thông tin sản phẩm</h3>
                                <p className="mt-1 text-sm text-slate-500">Những trường có dữ liệu đề xuất khác dữ liệu cũ sẽ được đánh dấu để dễ đối chiếu.</p>
                            </div>
                            {comparisonRows.length > 0 ? (
                                <div className="overflow-x-auto rounded-xl border border-slate-200">
                                    <table className="min-w-full divide-y divide-slate-200 text-sm">
                                        <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-600">
                                            <tr><th className="px-4 py-3">Thuộc tính</th><th className="px-4 py-3">Dữ liệu cũ</th><th className="px-4 py-3">Dữ liệu đề xuất</th></tr>
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
                            ) : <Alert type="warning">Yêu cầu này chưa có dữ liệu so sánh. Kiểm tra JSON chi tiết trước khi xử lý.</Alert>}
                            <details className="rounded-xl border border-slate-200 p-3">
                                <summary className="cursor-pointer text-sm font-medium text-slate-700">Xem toàn bộ dữ liệu gốc (JSON)</summary>
                                <div className="mt-3 grid gap-3 md:grid-cols-2">
                                    <div className="min-w-0"><p className="mb-1 text-xs font-semibold text-slate-500">Dữ liệu cũ</p><pre className="max-h-72 overflow-auto rounded-lg bg-slate-50 p-3 text-xs">{JSON.stringify(detail.oldValue ?? null, null, 2)}</pre></div>
                                    <div className="min-w-0"><p className="mb-1 text-xs font-semibold text-slate-500">Dữ liệu đề xuất</p><pre className="max-h-72 overflow-auto rounded-lg bg-slate-50 p-3 text-xs">{JSON.stringify(detail.newValue ?? null, null, 2)}</pre></div>
                                </div>
                            </details>
                        </div>

                        {detail.status === "PENDING" ? (
                            <div className="space-y-4 rounded-2xl border border-orange-200 bg-orange-50/50 p-4">
                                <div>
                                    <h3 className="font-semibold text-slate-900">Quyết định phê duyệt</h3>
                                    <p className="mt-1 text-sm text-slate-600">Thao tác thực sự cập nhật MySQL chung, chỉ thực hiện sau khi kiểm tra đầy đủ nội dung.</p>
                                </div>
                                <div className="flex flex-wrap gap-2">
                                    <Button type="button" variant={action === "APPROVE" ? "success" : "outline"} onClick={() => selectAction("APPROVE")} disabled={saving}>Phê duyệt</Button>
                                    <Button type="button" variant={action === "REJECT" ? "danger" : "outline"} onClick={() => selectAction("REJECT")} disabled={saving}>Từ chối</Button>
                                    {action && <Button type="button" variant="ghost" onClick={() => selectAction("")} disabled={saving}>Hủy thao tác</Button>}
                                </div>
                                {action && (
                                    <form onSubmit={submitDecision} className="space-y-3 rounded-xl border border-slate-200 bg-white p-4">
                                        {action === "REJECT" && (
                                            <TextArea
                                                label="Lý do từ chối * (tối đa 500 ký tự)"
                                                rows={3}
                                                value={reason}
                                                maxLength={500}
                                                required
                                                onChange={(event) => { setReason(event.target.value); setConfirmed(false); setActionError(""); }}
                                                placeholder="Nhập góp ý cụ thể để Business Staff chỉnh sửa và gửi lại"
                                            />
                                        )}
                                        <label className="flex items-start gap-2 text-sm text-slate-700">
                                            <input type="checkbox" className="mt-0.5 h-4 w-4 accent-[#f25d19]" checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} disabled={saving} />
                                            <span>Tôi đã xem thông tin yêu cầu #{detail.id} và xác nhận {action === "APPROVE" ? "phê duyệt" : "từ chối"} trên MySQL chung.</span>
                                        </label>
                                        {actionError && <div role="alert"><Alert type="danger">{actionError}</Alert></div>}
                                        <Button type="submit" variant={action === "APPROVE" ? "success" : "danger"} disabled={saving || !confirmed || (action === "REJECT" && (!reason.trim() || reason.trim().length > 500))}>
                                            {saving ? "Đang xử lý..." : action === "APPROVE" ? "Xác nhận phê duyệt" : "Xác nhận từ chối"}
                                        </Button>
                                    </form>
                                )}
                            </div>
                        ) : <Alert type="info">Yêu cầu này đã được xử lý. Các thao tác duyệt/từ chối chỉ khả dụng khi trạng thái là PENDING.</Alert>}
                    </Card>
                )}
            </div>
        </MainLayout>
    );
}
