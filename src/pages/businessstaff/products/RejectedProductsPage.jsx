import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
    getRejectedProductRequest,
    listRejectedProductRequests,
    resubmitRejectedProductRequest,
} from "../../../api/business/rejectedProducts";
import {
    getCategoryOptions,
    getProduct,
    productErrorMessage,
} from "../../../api/business/products";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import EmptyState from "../../../components/ui/EmptyState";
import Spinner from "../../../components/ui/Spinner";

const PAGE_SIZE = 10;
const EMPTY_FORM = {
    productCode: "",
    barcode: "",
    imageUrl: "",
    name: "",
    categoryId: "",
    brandId: "",
    baseUnit: "",
    purchasePrice: "",
    sellingPrice: "",
    requestReason: "",
};

const INPUT_CLASS =
    "mt-1 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100";

function asObject(value) {
    if (value && typeof value === "object" && !Array.isArray(value)) return value;
    if (typeof value === "string") {
        try {
            const parsed = JSON.parse(value);
            if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) return parsed;
        } catch {
            // Ignore an old, non-JSON snapshot. The user can fill missing values.
        }
    }
    return {};
}

function firstAvailable(sources, keys) {
    for (const source of sources) {
        const object = asObject(source);
        for (const key of keys) {
            const value = object[key];
            if (value !== null && value !== undefined) return String(value);
        }
    }
    return "";
}

function buildForm(request, product) {
    // The rejected request's proposed values take precedence over the saved product.
    const sources = [request?.newValue, product, request?.oldValue];
    return {
        productCode: firstAvailable(sources, ["productCode", "productsCode"]),
        barcode: firstAvailable(sources, ["barcode"]),
        imageUrl: firstAvailable(sources, ["imageUrl"]),
        name: firstAvailable(sources, ["name", "productName"]),
        categoryId: firstAvailable(sources, ["categoryId"]),
        brandId: firstAvailable(sources, ["brandId"]),
        baseUnit: firstAvailable(sources, ["baseUnit"]),
        purchasePrice: firstAvailable(sources, ["purchasePrice"]),
        sellingPrice: firstAvailable(sources, ["sellingPrice"]),
        requestReason: request?.requestReason || "",
    };
}

function validate(form) {
    const required = ["productCode", "barcode", "name", "baseUnit"];
    if (required.some((field) => !form[field].trim())) {
        return "Mã, barcode, tên sản phẩm và đơn vị tính không được để trống.";
    }
    const limits = {
        productCode: 50,
        barcode: 100,
        imageUrl: 500,
        name: 200,
        baseUnit: 30,
        requestReason: 500,
    };
    if (Object.entries(limits).some(([key, max]) => form[key].trim().length > max)) {
        return "Một hoặc nhiều trường đang vượt quá độ dài Backend cho phép.";
    }
    if (!/^[1-9]\d*$/.test(form.categoryId.trim())) {
        return "ID danh mục phải là số nguyên dương.";
    }
    if (form.brandId.trim() && !/^[1-9]\d*$/.test(form.brandId.trim())) {
        return "ID thương hiệu phải là số nguyên dương, hoặc để trống.";
    }
    if (["purchasePrice", "sellingPrice"].some((field) =>
        !form[field].trim() || !Number.isFinite(Number(form[field])) || Number(form[field]) < 0.01
    )) {
        return "Giá nhập và giá bán phải là số lớn hơn hoặc bằng 0,01.";
    }
    if (form.imageUrl.trim() && !/^https?:\/\//i.test(form.imageUrl.trim())) {
        return "Đường dẫn ảnh phải bắt đầu bằng http:// hoặc https://.";
    }
    return "";
}

function toPayload(form) {
    return {
        productCode: form.productCode.trim(),
        barcode: form.barcode.trim(),
        imageUrl: form.imageUrl.trim() || null,
        name: form.name.trim(),
        categoryId: Number(form.categoryId),
        brandId: form.brandId.trim() ? Number(form.brandId) : null,
        baseUnit: form.baseUnit.trim(),
        purchasePrice: Number(form.purchasePrice),
        sellingPrice: Number(form.sellingPrice),
        requestReason: form.requestReason.trim() || null,
    };
}

function formatDate(value) {
    if (!value) return "—";
    const date = new Date(value);
    return Number.isNaN(date.getTime())
        ? String(value)
        : new Intl.DateTimeFormat("vi-VN", {
            dateStyle: "short",
            timeStyle: "short",
        }).format(date);
}

function FormField({ label, value, onChange, type = "text", required = false, ...rest }) {
    return (
        <label className="block text-sm font-medium text-slate-700">
            {label}{required ? " *" : ""}
            <input
                className={INPUT_CLASS}
                type={type}
                value={value}
                onChange={(event) => onChange(event.target.value)}
                required={required}
                {...rest}
            />
        </label>
    );
}

export default function RejectedProductsPage() {
    const [page, setPage] = useState(0);
    const [reload, setReload] = useState(0);
    const [data, setData] = useState({ items: [], totalPages: 0, totalElements: 0 });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");

    const [detail, setDetail] = useState(null);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailError, setDetailError] = useState("");
    const [editing, setEditing] = useState(false);
    const [form, setForm] = useState({ ...EMPTY_FORM });
    const [categories, setCategories] = useState([]);
    const [formError, setFormError] = useState("");
    const [confirmed, setConfirmed] = useState(false);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        listRejectedProductRequests({ page, size: PAGE_SIZE }, controller.signal)
            .then((result) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(result?.items)) {
                    throw new Error("Backend response does not contain items");
                }
                setData(result);
            })
            .catch((requestError) => {
                if (controller.signal.aborted) return;
                setError(productErrorMessage(requestError, "Không tải được danh sách bị từ chối."));
                setData({ items: [], totalPages: 0, totalElements: 0 });
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoading(false);
            });
        return () => controller.abort();
    }, [page, reload]);

    const openDetail = async (row) => {
        if (detailLoading || saving) return;
        setDetail(null);
        setDetailLoading(true);
        setDetailError("");
        setEditing(false);
        setFormError("");
        setConfirmed(false);
        setNotice("");
        try {
            const request = await getRejectedProductRequest(row.id);
            // A product may not be directly viewable after a rejected CREATE.
            // In that case, populate the form from newValue / oldValue alone.
            const [product, options] = await Promise.all([
                request.productId ? getProduct(request.productId).catch(() => null) : Promise.resolve(null),
                getCategoryOptions().catch(() => []),
            ]);
            setCategories(Array.isArray(options) ? options : []);
            setForm(buildForm(request, product));
            setDetail(request);
        } catch (requestError) {
            setDetailError(productErrorMessage(requestError, "Không lấy được chi tiết yêu cầu."));
        } finally {
            setDetailLoading(false);
        }
    };

    const changeForm = (field, value) => {
        setForm((current) => ({ ...current, [field]: value }));
        setConfirmed(false);
        setFormError("");
    };

    const submitResubmission = async (event) => {
        event.preventDefault();
        if (!detail || saving || !confirmed || detail.status !== "REJECTED") return;
        const validationError = validate(form);
        if (validationError) {
            setFormError(validationError);
            return;
        }
        setSaving(true);
        setFormError("");
        try {
            const response = await resubmitRejectedProductRequest(detail.id, toPayload(form));
            setNotice(`Đã gửi lại yêu cầu phê duyệt thành công${response?.requestCode ? ` (${response.requestCode})` : ""}.`);
            setDetail(null);
            setEditing(false);
            setConfirmed(false);
            setPage(0);
            setReload((value) => value + 1);
        } catch (requestError) {
            setFormError(productErrorMessage(requestError, "Không gửi lại được yêu cầu phê duyệt."));
        } finally {
            setSaving(false);
        }
    };

    const rows = data.items || [];
    const totalPages = Number(data.totalPages) || 0;

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Sản phẩm bị từ chối"
                    description="Xem lý do từ chối, chỉnh sửa theo góp ý và gửi lại Business Manager phê duyệt."
                />

                {notice && (
                    <div role="status" className="space-y-2">
                        <Alert type="success">{notice}</Alert>
                        <Link className="text-sm font-medium text-orange-700 underline" to="/business-staff/products/requests">
                            Xem lịch sử các yêu cầu của tôi
                        </Link>
                    </div>
                )}
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}

                <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                    <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                        <p className="text-sm text-slate-600">
                            Tổng số: <strong>{data.totalElements ?? 0}</strong> yêu cầu bị từ chối.
                        </p>
                        <Button variant="outline" onClick={() => setReload((current) => current + 1)}>
                            Làm mới
                        </Button>
                    </div>

                    {loading ? (
                        <div className="flex items-center gap-3 text-sm text-slate-600">
                            <Spinner /> Đang tải sản phẩm bị từ chối...
                        </div>
                    ) : !error && rows.length === 0 ? (
                        <EmptyState
                            title="Không có sản phẩm bị từ chối"
                            description="Hiện chưa có yêu cầu bị từ chối cần xử lý."
                        />
                    ) : !error ? (
                        <div className="overflow-x-auto">
                            <table className="w-full min-w-[700px] border-collapse text-left text-sm">
                                <thead className="bg-slate-50 text-slate-600">
                                    <tr>
                                        <th className="px-3 py-3">Mã yêu cầu</th>
                                        <th className="px-3 py-3">Sản phẩm</th>
                                        <th className="px-3 py-3">Loại</th>
                                        <th className="px-3 py-3">Lý do từ chối</th>
                                        <th className="px-3 py-3">Ngày xử lý</th>
                                        <th className="px-3 py-3">Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {rows.map((row) => (
                                        <tr key={row.id} className="border-t border-slate-100 align-top">
                                            <td className="px-3 py-3 font-medium">{row.requestCode || `#${row.id}`}</td>
                                            <td className="px-3 py-3">
                                                <span className="font-medium">{row.productName || "—"}</span>
                                                <span className="mt-1 block text-xs text-slate-500">{row.productCode || "—"}</span>
                                            </td>
                                            <td className="px-3 py-3">{row.requestType || "—"}</td>
                                            <td className="max-w-64 whitespace-normal px-3 py-3 text-red-700">
                                                {row.rejectionReason || "Chưa có lý do"}
                                            </td>
                                            <td className="px-3 py-3">{formatDate(row.approvedAt || row.createdAt)}</td>
                                            <td className="px-3 py-3">
                                                <Button variant="outline" size="sm" onClick={() => openDetail(row)}>
                                                    Xem / Gửi lại
                                                </Button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    ) : null}
                    {!loading && !error && totalPages > 1 && (
                        <div className="mt-5 flex items-center justify-end gap-3">
                            <Button variant="outline" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                                Trước
                            </Button>
                            <span className="text-sm text-slate-600">Trang {page + 1}/{totalPages}</span>
                            <Button variant="outline" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>
                                Sau
                            </Button>
                        </div>
                    )}
                </div>

                {detailLoading && (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-5 text-sm">
                        <Spinner /> Đang tải chi tiết yêu cầu...
                    </div>
                )}
                {detailError && <div role="alert"><Alert type="danger">{detailError}</Alert></div>}
                {detail && (
                    <section className="space-y-5 rounded-2xl border border-orange-200 bg-white p-5 shadow-sm">
                        <div className="flex flex-wrap items-start justify-between gap-3">
                            <div>
                                <h2 className="text-xl font-semibold text-slate-900">
                                    Chi tiết yêu cầu {detail.requestCode || `#${detail.id}`}
                                </h2>
                                <p className="mt-1 text-sm text-slate-500">
                                    Sản phẩm: {detail.productName || "—"} · {detail.productCode || "—"}
                                </p>
                            </div>
                            <Button variant="ghost" disabled={saving} onClick={() => { setDetail(null); setEditing(false); }}>
                                Đóng
                            </Button>
                        </div>
                        <dl className="grid gap-3 text-sm sm:grid-cols-2">
                            <div><dt className="text-slate-500">Trạng thái</dt><dd className="font-medium">{detail.status || "—"}</dd></div>
                            <div><dt className="text-slate-500">Loại yêu cầu</dt><dd className="font-medium">{detail.requestType || "—"}</dd></div>
                            <div><dt className="text-slate-500">Người từ chối</dt><dd className="font-medium">{detail.approvedByName || "—"}</dd></div>
                            <div><dt className="text-slate-500">Thời gian từ chối</dt><dd className="font-medium">{formatDate(detail.approvedAt)}</dd></div>
                        </dl>
                        <div className="rounded-xl border border-red-200 bg-red-50 p-4">
                            <p className="text-sm font-semibold text-red-800">Lý do Business Manager từ chối</p>
                            <p className="mt-1 whitespace-pre-wrap text-sm text-red-800">
                                {detail.rejectionReason || "Chưa có lý do từ chối."}
                            </p>
                        </div>
                        <div className="text-sm text-slate-600">
                            <strong>Lý do gửi trước đó:</strong> {detail.requestReason || "—"}
                        </div>
                        <details className="rounded-xl border border-slate-200 p-3 text-sm">
                            <summary className="cursor-pointer font-medium">Xem dữ liệu sản phẩm trước và sau đề xuất</summary>
                            <div className="mt-3 grid gap-3 lg:grid-cols-2">
                                <div>
                                    <p className="mb-1 font-medium">Dữ liệu cũ</p>
                                    <pre className="max-h-60 overflow-auto rounded-lg bg-slate-50 p-3 text-xs">{JSON.stringify(detail.oldValue ?? null, null, 2)}</pre>
                                </div>
                                <div>
                                    <p className="mb-1 font-medium">Dữ liệu đề xuất</p>
                                    <pre className="max-h-60 overflow-auto rounded-lg bg-slate-50 p-3 text-xs">{JSON.stringify(detail.newValue ?? null, null, 2)}</pre>
                                </div>
                            </div>
                        </details>

                        {detail.status === "REJECTED" && !editing && (
                            <Button onClick={() => { setEditing(true); setFormError(""); setConfirmed(false); }}>
                                Chỉnh sửa và gửi lại phê duyệt
                            </Button>
                        )}
                        {editing && detail.status === "REJECTED" && (
                            <form onSubmit={submitResubmission} className="space-y-4 border-t border-slate-200 pt-5">
                                <h3 className="text-lg font-semibold">Chỉnh sửa thông tin theo góp ý</h3>
                                {formError && <div role="alert"><Alert type="danger">{formError}</Alert></div>}
                                <div className="grid gap-4 sm:grid-cols-2">
                                    <FormField label="Mã sản phẩm" required maxLength={50} value={form.productCode} onChange={(v) => changeForm("productCode", v)} />
                                    <FormField label="Barcode" required maxLength={100} value={form.barcode} onChange={(v) => changeForm("barcode", v)} />
                                    <FormField label="Tên sản phẩm" required maxLength={200} value={form.name} onChange={(v) => changeForm("name", v)} />
                                    <FormField label="Đơn vị tính" required maxLength={30} value={form.baseUnit} onChange={(v) => changeForm("baseUnit", v)} />
                                    <div>
                                        <FormField label="ID danh mục" required inputMode="numeric" list="issue4-category-options" value={form.categoryId} onChange={(v) => changeForm("categoryId", v)} />
                                        <datalist id="issue4-category-options">
                                            {categories.filter((row) => row.status === "ACTIVE").map((row) => (
                                                <option key={row.id} value={String(row.id)}>{row.name}</option>
                                            ))}
                                        </datalist>
                                        <p className="mt-1 text-xs text-slate-500">Chọn danh mục đang hoạt động.</p>
                                    </div>
                                    <FormField label="ID thương hiệu (không bắt buộc)" inputMode="numeric" value={form.brandId} onChange={(v) => changeForm("brandId", v)} />
                                    <FormField label="Giá nhập" required type="number" min="0.01" step="0.01" value={form.purchasePrice} onChange={(v) => changeForm("purchasePrice", v)} />
                                    <FormField label="Giá bán" required type="number" min="0.01" step="0.01" value={form.sellingPrice} onChange={(v) => changeForm("sellingPrice", v)} />
                                </div>
                                <FormField label="URL ảnh (không bắt buộc)" maxLength={500} value={form.imageUrl} onChange={(v) => changeForm("imageUrl", v)} />
                                <label className="block text-sm font-medium text-slate-700">
                                    Ghi chú khi gửi lại cho Business Manager
                                    <textarea
                                        className={INPUT_CLASS}
                                        rows={3}
                                        maxLength={500}
                                        value={form.requestReason}
                                        onChange={(event) => changeForm("requestReason", event.target.value)}
                                        placeholder="Đã sửa thông tin theo góp ý..."
                                    />
                                </label>
                                <label className="flex items-start gap-2 text-sm text-slate-700">
                                    <input
                                        type="checkbox"
                                        className="mt-1"
                                        checked={confirmed}
                                        onChange={(event) => setConfirmed(event.target.checked)}
                                    />
                                    <span>Tôi xác nhận thông tin đã sửa và đồng ý gửi lại yêu cầu phê duyệt.</span>
                                </label>
                                <div className="flex flex-wrap gap-2">
                                    <button
                                        type="submit"
                                        disabled={saving || !confirmed}
                                        className="rounded-xl bg-orange-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-orange-700 disabled:cursor-not-allowed disabled:opacity-50"
                                    >
                                        {saving ? "Đang gửi..." : "Gửi lại phê duyệt"}
                                    </button>
                                    <button
                                        type="button"
                                        disabled={saving}
                                        onClick={() => { setEditing(false); setFormError(""); setConfirmed(false); }}
                                        className="rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-700 disabled:opacity-50"
                                    >
                                        Hủy chỉnh sửa
                                    </button>
                                </div>
                            </form>
                        )}
                    </section>
                )}
            </div>
        </MainLayout>
    );
}
