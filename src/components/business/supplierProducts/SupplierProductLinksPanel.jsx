import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listSuppliers } from "../../../api/business/suppliers";
import {
    createSupplierProductLink,
    deleteSupplierProductLink,
    getLinksByProduct,
    setDefaultSupplierLink,
    supplierProductErrorMessage,
    updateSupplierProductLink,
} from "../../../api/business/supplierProducts";
import Alert from "../../ui/Alert";
import Button from "../../ui/Button";
import Card from "../../ui/Card";
import Checkbox from "../../ui/Checkbox";
import EmptyState from "../../ui/EmptyState";
import Input from "../../ui/Input";
import Modal from "../../ui/Modal";
import Select from "../../ui/Select";
import StatusBadge from "../../ui/StatusBadge";
import Table from "../../ui/Table";

const EMPTY_FORM = {
    supplierId: "",
    supplierProductCode: "",
    purchasePrice: "",
    leadTimeDays: "0",
    minimumOrderQuantity: "1",
    status: "ACTIVE",
    defaultSupplier: false,
};
const formatMoney = (value) => value == null ? "—" : `${Number(value).toLocaleString("vi-VN")} ₫`;
const display = (value) => value == null || value === "" ? "—" : value;

function validationErrors(form, creating, linkedSupplierIds) {
    const errors = {};
    if (creating && (!Number.isSafeInteger(Number(form.supplierId)) || Number(form.supplierId) <= 0)) {
        errors.supplierId = "Chọn nhà cung cấp hợp lệ.";
    } else if (creating && linkedSupplierIds.has(Number(form.supplierId))) {
        errors.supplierId = "Nhà cung cấp này đã liên kết với sản phẩm (kể cả liên kết INACTIVE). Hãy sửa liên kết cũ.";
    }
    if (form.supplierProductCode.trim().length > 100) {
        errors.supplierProductCode = "Tối đa 100 ký tự.";
    }
    const rawPrice = form.purchasePrice.trim();
    if (!/^\d+(\.\d{1,2})?$/.test(rawPrice) || Number(rawPrice) < 0.01 || !Number.isFinite(Number(rawPrice))) {
        errors.purchasePrice = "Giá nhập phải >= 0.01 và tối đa 2 chữ số thập phân.";
    }
    if (!/^\d+$/.test(form.leadTimeDays) || !Number.isSafeInteger(Number(form.leadTimeDays))) {
        errors.leadTimeDays = "Thời gian giao hàng phải là số nguyên từ 0 trở lên.";
    }
    if (!/^\d+$/.test(form.minimumOrderQuantity) || !Number.isSafeInteger(Number(form.minimumOrderQuantity)) || Number(form.minimumOrderQuantity) < 1) {
        errors.minimumOrderQuantity = "MOQ phải là số nguyên từ 1 trở lên.";
    }
    if (!creating && !["ACTIVE", "INACTIVE"].includes(form.status)) {
        errors.status = "Trạng thái không hợp lệ.";
    }
    return errors;
}

export default function SupplierProductLinksPanel({ productId, productStatus }) {
    const [links, setLinks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState("");
    const [notice, setNotice] = useState("");
    const [error, setError] = useState("");
    const [reload, setReload] = useState(0);
    const [action, setAction] = useState(null); // create | edit | default | delete
    const [form, setForm] = useState(EMPTY_FORM);
    const [errors, setErrors] = useState({});
    const [confirmed, setConfirmed] = useState(false);
    const [deleteText, setDeleteText] = useState("");
    const [saving, setSaving] = useState(false);
    const [supplierOptions, setSupplierOptions] = useState([]);
    const [supplierPage, setSupplierPage] = useState(0);
    const [supplierTotalPages, setSupplierTotalPages] = useState(0);
    const [supplierKeywordDraft, setSupplierKeywordDraft] = useState("");
    const [supplierKeyword, setSupplierKeyword] = useState("");
    const [supplierLoading, setSupplierLoading] = useState(false);
    const [supplierError, setSupplierError] = useState("");
    const canManage = productStatus === "ACTIVE";

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setLoadError("");
        getLinksByProduct(productId, controller.signal)
            .then((result) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(result)) throw new Error("Invalid supplier product links response");
                setLinks(result);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setLoadError(supplierProductErrorMessage(requestError, "Không tải được nhà cung cấp liên kết."));
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [productId, reload]);

    useEffect(() => {
        if (action?.type !== "create") return;
        const controller = new AbortController();
        setSupplierLoading(true);
        setSupplierError("");
        listSuppliers({ keyword: supplierKeyword || undefined, status: "ACTIVE", page: supplierPage, size: 20, sortBy: "name", direction: "ASC" }, controller.signal)
            .then((result) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(result?.content)) throw new Error("Invalid supplier list response");
                setSupplierOptions(result.content);
                setSupplierTotalPages(result.totalPages ?? 0);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setSupplierError(supplierProductErrorMessage(requestError, "Không tải được danh sách nhà cung cấp."));
            })
            .finally(() => { if (!controller.signal.aborted) setSupplierLoading(false); });
        return () => controller.abort();
    }, [action?.type, supplierKeyword, supplierPage]);

    const openAction = (type, link = null) => {
        if (!canManage) return;
        setNotice("");
        setError("");
        setErrors({});
        setConfirmed(false);
        setDeleteText("");
        if (type === "create") {
            setSupplierPage(0);
            setSupplierKeyword("");
            setSupplierKeywordDraft("");
            setSupplierOptions([]);
            setForm({ ...EMPTY_FORM });
        } else if (type === "edit" && link) {
            setForm({
                supplierId: String(link.supplierId ?? ""),
                supplierProductCode: link.supplierProductCode ?? "",
                purchasePrice: String(link.purchasePrice ?? ""),
                leadTimeDays: String(link.leadTimeDays ?? 0),
                minimumOrderQuantity: String(link.minimumOrderQuantity ?? 1),
                status: link.status ?? "ACTIVE",
                defaultSupplier: Boolean(link.defaultSupplier),
            });
        }
        setAction({ type, link });
    };

    const change = (field) => (event) => {
        const value = event.target.type === "checkbox" ? event.target.checked : event.target.value;
        setForm((current) => ({ ...current, [field]: value }));
        setErrors((current) => ({ ...current, [field]: undefined }));
    };

    const saveForm = async (event) => {
        event.preventDefault();
        if (saving || !confirmed || !canManage || !action || !["create", "edit"].includes(action.type)) return;
        const creating = action.type === "create";
        const nextErrors = validationErrors(form, creating, new Set(links.map((link) => Number(link.supplierId))));
        if (!creating && action.link?.defaultSupplier && form.status === "INACTIVE") {
            nextErrors.status = "Không thể ngừng hoạt động NCC mặc định. Hãy chọn một NCC mặc định khác trước.";
        }
        setErrors(nextErrors);
        if (Object.keys(nextErrors).length) return;
        const payload = {
            supplierProductCode: form.supplierProductCode.trim() || null,
            purchasePrice: Number(form.purchasePrice),
            leadTimeDays: Number(form.leadTimeDays),
            minimumOrderQuantity: Number(form.minimumOrderQuantity),
        };
        setSaving(true);
        setError("");
        try {
            if (creating) {
                await createSupplierProductLink({
                    ...payload,
                    productId: Number(productId),
                    supplierId: Number(form.supplierId),
                    defaultSupplier: form.defaultSupplier,
                });
            } else {
                await updateSupplierProductLink(action.link.id, { ...payload, status: form.status });
            }
            setNotice(creating ? "Đã tạo liên kết nhà cung cấp." : "Đã cập nhật liên kết nhà cung cấp.");
            setAction(null);
            setReload((current) => current + 1);
        } catch (requestError) {
            setError(supplierProductErrorMessage(requestError, "Không thể lưu liên kết."));
        } finally {
            setSaving(false);
        }
    };

    const confirmAction = async () => {
        if (saving || !confirmed || !canManage || !action?.link) return;
        if (action.type === "default" && (action.link.status !== "ACTIVE" || action.link.supplierStatus !== "ACTIVE")) return;
        if (action.type === "delete" && (deleteText !== "XOA" || action.link.defaultSupplier)) return;
        setSaving(true);
        setError("");
        try {
            if (action.type === "default") {
                await setDefaultSupplierLink(action.link.id);
                setNotice(`Đã chọn ${action.link.supplierName} làm nhà cung cấp mặc định.`);
            } else if (action.type === "delete") {
                await deleteSupplierProductLink(action.link.id);
                setNotice("Đã xóa vĩnh viễn liên kết.");
            } else return;
            setAction(null);
            setReload((current) => current + 1);
        } catch (requestError) {
            setError(supplierProductErrorMessage(requestError, "Không thể thực hiện thao tác."));
        } finally {
            setSaving(false);
        }
    };

    const columns = [
        {
            key: "supplierName", label: "Nhà cung cấp",
            render: (_value, link) => (
                <div className="space-y-1">
                    <Link to={`/business-staff/suppliers/${link.supplierId}`} className="font-semibold text-[#c94810] hover:underline">{link.supplierName || link.supplierCode || "—"}</Link>
                    <div className="text-xs text-slate-500">{display(link.supplierCode)} · {display(link.linkCode)}</div>
                    <StatusBadge status={link.supplierStatus} />
                </div>
            ),
        },
        { key: "supplierProductCode", label: "Mã SP phía NCC", render: display },
        { key: "purchasePrice", label: "Giá nhập", render: formatMoney },
        { key: "minimumOrderQuantity", label: "MOQ", render: display },
        { key: "leadTimeDays", label: "Giao (ngày)", render: display },
        { key: "status", label: "Liên kết", render: (value, link) => <div className="space-y-1"><StatusBadge status={value} />{link.defaultSupplier && <div className="text-xs font-semibold text-[#c94810]">★ NCC mặc định</div>}</div> },
        {
            key: "id", label: "Thao tác",
            render: (_value, link) => (
                <div className="flex flex-wrap gap-2">
                    <Button type="button" variant="outline" size="sm" disabled={!canManage} onClick={() => openAction("edit", link)}>Sửa</Button>
                    {!link.defaultSupplier && <Button type="button" variant="secondary" size="sm" disabled={!canManage || link.status !== "ACTIVE" || link.supplierStatus !== "ACTIVE"} onClick={() => openAction("default", link)}>Đặt mặc định</Button>}
                    <Button type="button" variant="danger" size="sm" disabled={!canManage || Boolean(link.defaultSupplier)} onClick={() => openAction("delete", link)}>Xóa</Button>
                </div>
            ),
        },
    ];

    const editingForm = action?.type === "create" || action?.type === "edit";
    const creating = action?.type === "create";
    return (
        <section className="space-y-4">
            <Card>
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                        <h2 className="text-lg font-bold text-slate-900">Nhà cung cấp liên kết ({links.length})</h2>
                        <p className="mt-1 text-sm text-slate-500">Giá nhập, MOQ, thời gian giao hàng và nhà cung cấp mặc định · Issue #8</p>
                    </div>
                    <Button type="button" disabled={!canManage} onClick={() => openAction("create")}>+ Liên kết nhà cung cấp</Button>
                </div>
                {!canManage && <div className="mt-4"><Alert type="warning">Chỉ cho phép thay đổi liên kết khi sản phẩm ở trạng thái ACTIVE. Các liên kết hiện có vẫn được hiển thị để tra cứu.</Alert></div>}
                {notice && <div role="status" className="mt-4"><Alert type="success">{notice}</Alert></div>}
                {loadError && <div role="alert" className="mt-4"><Alert type="danger">{loadError}</Alert></div>}
                {!loading && !loadError && <p className="mt-4 text-xs text-slate-500">Có {links.filter((link) => link.status === "ACTIVE").length} liên kết đang hoạt động. Liên kết INACTIVE vẫn được giữ để có thể khôi phục qua chức năng Sửa.</p>}
            </Card>
            {loading ? <p role="status" className="text-sm text-slate-600">Đang tải liên kết nhà cung cấp...</p>
                : !loadError && (links.length ? <Table columns={columns} data={links} />
                    : <EmptyState title="Chưa liên kết nhà cung cấp" description="Sản phẩm chưa có nhà cung cấp. Sử dụng nút Liên kết nhà cung cấp để tạo mới." />)}
            <Modal
                open={Boolean(action)}
                title={creating ? "Thêm liên kết nhà cung cấp" : action?.type === "edit" ? "Cập nhật liên kết" : action?.type === "default" ? "Đặt nhà cung cấp mặc định" : "Xóa vĩnh viễn liên kết"}
                onClose={() => { if (!saving) { setAction(null); setError(""); } }}
                footer={<>
                    <Button type="button" variant="ghost" disabled={saving} onClick={() => { setAction(null); setError(""); }}>Hủy</Button>
                    <Button
                        type={editingForm ? "submit" : "button"}
                        form={editingForm ? "supplier-product-link-form" : undefined}
                        variant={action?.type === "delete" ? "danger" : "primary"}
                        disabled={saving || !confirmed || (action?.type === "delete" && deleteText !== "XOA")}
                        onClick={editingForm ? undefined : confirmAction}
                    >{saving ? "Đang xử lý..." : creating ? "Tạo liên kết" : action?.type === "edit" ? "Lưu thay đổi" : action?.type === "default" ? "Xác nhận mặc định" : "Xóa vĩnh viễn"}</Button>
                </>}
            >
                <div className="max-h-[65vh] space-y-4 overflow-y-auto pr-1">
                    {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                    {editingForm ? (
                        <form id="supplier-product-link-form" onSubmit={saveForm} className="space-y-4" noValidate>
                            {creating ? (
                                <div className="space-y-3 rounded-xl border border-slate-200 p-3">
                                    <div>
                                        <Input
                                            label="Tìm nhà cung cấp đang hoạt động"
                                            placeholder="Nhập tên hoặc mã NCC"
                                            value={supplierKeywordDraft}
                                            onChange={(event) => setSupplierKeywordDraft(event.target.value)}
                                            onKeyDown={(event) => {
                                                // Enter in the search field must never submit the link-creation form.
                                                if (event.key === "Enter") {
                                                    event.preventDefault();
                                                    setSupplierPage(0);
                                                    setSupplierKeyword(supplierKeywordDraft.trim());
                                                    setForm((current) => ({ ...current, supplierId: "" }));
                                                }
                                            }}
                                        />
                                        <Button type="button" size="sm" variant="outline" className="mt-2" onClick={() => { setSupplierPage(0); setSupplierKeyword(supplierKeywordDraft.trim()); setForm((current) => ({ ...current, supplierId: "" })); }}>Tìm NCC</Button>
                                    </div>
                                    {supplierError && <Alert type="danger">{supplierError}</Alert>}
                                    {supplierLoading ? <p className="text-sm text-slate-500">Đang tải NCC...</p> : <Select
                                        label="Nhà cung cấp *" value={form.supplierId} error={errors.supplierId}
                                        onChange={change("supplierId")}
                                        options={[
    {
        value: "",
        label: "-- Chọn nhà cung cấp --",
    },
    ...supplierOptions
        .filter(
            (supplier) =>
                !links.some(
                    (link) =>
                        Number(link.supplierId) === Number(supplier.id)
                )
        )
        .map((supplier) => ({
            value: String(supplier.id),
            label: `${supplier.name} (${supplier.supplierCode})`,
        })),
]}
                                    />}
                                    <div className="flex items-center justify-between text-xs text-slate-500">
                                        <span>Trang {supplierPage + 1}/{Math.max(1, supplierTotalPages)}</span>
                                        <div className="flex gap-2">
                                            <Button type="button" size="sm" variant="ghost" disabled={supplierPage <= 0 || supplierLoading} onClick={() => { setSupplierPage((page) => page - 1); setForm((current) => ({ ...current, supplierId: "" })); }}>Trước</Button>
                                            <Button type="button" size="sm" variant="ghost" disabled={supplierPage + 1 >= supplierTotalPages || supplierLoading} onClick={() => { setSupplierPage((page) => page + 1); setForm((current) => ({ ...current, supplierId: "" })); }}>Sau</Button>
                                        </div>
                                    </div>
                                </div>
                            ) : <p className="text-sm text-slate-700">Liên kết với <strong>{action.link?.supplierName}</strong> (ID {action.link?.id}). Muốn đổi NCC, hãy tạo liên kết mới.</p>}
                            <Input label="Mã sản phẩm phía NCC (tối đa 100 ký tự)" value={form.supplierProductCode} maxLength={100} onChange={change("supplierProductCode")} error={errors.supplierProductCode} />
                            <Input label="Giá nhập *" type="number" min="0.01" step="0.01" value={form.purchasePrice} onChange={change("purchasePrice")} error={errors.purchasePrice} />
                            <Input label="Thời gian giao hàng (ngày) *" type="number" min="0" step="1" value={form.leadTimeDays} onChange={change("leadTimeDays")} error={errors.leadTimeDays} />
                            <Input label="MOQ (số lượng đặt tối thiểu) *" type="number" min="1" step="1" value={form.minimumOrderQuantity} onChange={change("minimumOrderQuantity")} error={errors.minimumOrderQuantity} />
                            {creating ? <Checkbox checked={form.defaultSupplier} onChange={change("defaultSupplier")} label="Đặt làm nhà cung cấp mặc định ngay khi tạo (sẽ bỏ mặc định cũ)." />
                                : <Select label="Trạng thái liên kết" value={form.status} onChange={change("status")} error={errors.status} options={[{ value: "ACTIVE", label: "ACTIVE – Hoạt động" }, { value: "INACTIVE", label: "INACTIVE – Ngừng hoạt động" }]} />}
                            <p className="text-xs text-slate-500">Chỉ sửa điều kiện mua hàng và trạng thái của liên kết. Không thay đổi chính sản phẩm hoặc nhà cung cấp.</p>
                        </form>
                    ) : action?.type === "default" ? (
                        <p className="text-sm text-slate-700">Chọn <strong>{action.link?.supplierName}</strong> làm nhà cung cấp mặc định của sản phẩm? Backend sẽ bỏ mặc định của NCC cũ.</p>
                    ) : action?.type === "delete" ? (
                        <div className="space-y-3 text-sm text-slate-700">
                            <Alert type="danger">Cảnh báo: API DELETE xóa vĩnh viễn liên kết trên MySQL chung và hiện chưa ghi lịch sử xóa trong activity_logs. Chỉ thực hiện khi nhóm đã đồng ý.</Alert>
                            <p>Xóa liên kết với <strong>{action.link?.supplierName}</strong> (mã {action.link?.linkCode})?</p>
                            <Input label='Gõ XOA để xác nhận' value={deleteText} onChange={(event) => setDeleteText(event.target.value)} placeholder="XOA" />
                        </div>
                    ) : null}
                    <div className="rounded-xl border border-amber-200 bg-amber-50 p-3">
                        <Checkbox checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} label="Tôi xác nhận thao tác này ghi dữ liệu thật vào MySQL dùng chung của nhóm." />
                    </div>
                </div>
            </Modal>
        </section>
    );
}
