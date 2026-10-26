import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
    createSupplierInvoice,
    getSupplierInvoiceDetail,
    getSupplierInvoicePurchaseOrderOptions,
    getSupplierInvoiceSupplierOptions,
    supplierInvoiceErrorMessage,
    updateSupplierInvoice,
} from "../../../api/business/supplierInvoices";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import Checkbox from "../../../components/ui/Checkbox";
import Input from "../../../components/ui/Input";
import Select from "../../../components/ui/Select";
import Spinner from "../../../components/ui/Spinner";
import {
    centsToInvoiceDecimal,
    parseInvoiceCents,
    supplierInvoiceDraftPayload,
    validateSupplierInvoiceDraft,
} from "./supplierInvoiceDraft";
import { formatSupplierInvoiceMoney, SupplierInvoiceStatusBadge } from "./supplierInvoiceFormat";

const ROOT = "/business-staff/supplier-invoices";
const EDITABLE = new Set(["DRAFT", "PENDING_VERIFICATION"]);
const blankForm = () => ({
    supplierId: "",
    purchaseOrderId: "",
    invoiceNumber: "",
    invoiceDate: "",
    dueDate: "",
    subtotal: "",
    taxAmount: "",
});

function moneyLabel(cents) {
    if (!Number.isSafeInteger(cents)) return "—";
    return formatSupplierInvoiceMoney(Number(centsToInvoiceDecimal(Math.abs(cents))))
        .replace(/^/, cents < 0 ? "−" : "");
}

export default function SupplierInvoiceFormPage() {
    const { invoiceId } = useParams();
    const editing = invoiceId !== undefined;
    const navigate = useNavigate();
    const [form, setForm] = useState(blankForm);
    const [original, setOriginal] = useState(null);
    const [suppliers, setSuppliers] = useState([]);
    const [purchaseOrders, setPurchaseOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [loadingOrders, setLoadingOrders] = useState(false);
    const [loadError, setLoadError] = useState("");
    const [orderError, setOrderError] = useState("");
    const [saveError, setSaveError] = useState("");
    const [problems, setProblems] = useState([]);
    const [confirmedWrite, setConfirmedWrite] = useState(false);
    const [confirmedMismatch, setConfirmedMismatch] = useState(false);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setLoadError("");
        setOriginal(null);
        setSuppliers([]);
        setConfirmedWrite(false);
        setConfirmedMismatch(false);
        if (editing && (!Number.isSafeInteger(Number(invoiceId)) || Number(invoiceId) <= 0)) {
            setLoadError("ID hóa đơn không hợp lệ.");
            setLoading(false);
            return () => controller.abort();
        }

        Promise.all([
            getSupplierInvoiceSupplierOptions(controller.signal),
            editing ? getSupplierInvoiceDetail(invoiceId, controller.signal) : Promise.resolve(null),
        ]).then(([items, loaded]) => {
            if (controller.signal.aborted) return;
            if (!Array.isArray(items)) throw new Error("Danh sách nhà cung cấp không hợp lệ.");
            setSuppliers(items);
            if (editing) {
                if (!loaded || !EDITABLE.has(loaded.status)) {
                    setLoadError("Chỉ hóa đơn DRAFT hoặc PENDING_VERIFICATION mới được sửa.");
                    return;
                }
                setOriginal(loaded);
                setForm({
                    supplierId: String(loaded.supplierId ?? ""),
                    purchaseOrderId: String(loaded.purchaseOrderId ?? ""),
                    invoiceNumber: loaded.invoiceNumber ?? "",
                    invoiceDate: loaded.invoiceDate ?? "",
                    dueDate: loaded.dueDate ?? "",
                    subtotal: loaded.subtotal == null ? "" : String(loaded.subtotal),
                    taxAmount: loaded.taxAmount == null ? "" : String(loaded.taxAmount),
                });
            } else {
                setForm(blankForm());
            }
        }).catch((err) => {
            if (!controller.signal.aborted) {
                setLoadError(supplierInvoiceErrorMessage(err, "Không tải được dữ liệu tạo/sửa hóa đơn."));
            }
        }).finally(() => {
            if (!controller.signal.aborted) setLoading(false);
        });
        return () => controller.abort();
    }, [editing, invoiceId]);

    useEffect(() => {
        const controller = new AbortController();
        setPurchaseOrders([]);
        setOrderError("");
        if (!form.supplierId || loading || loadError) {
            setLoadingOrders(false);
            return () => controller.abort();
        }
        setLoadingOrders(true);
        getSupplierInvoicePurchaseOrderOptions(form.supplierId, controller.signal)
            .then((items) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(items)) throw new Error("Danh sách đơn mua hàng không hợp lệ.");
                setPurchaseOrders(items);
            })
            .catch((err) => {
                if (!controller.signal.aborted) {
                    setOrderError(supplierInvoiceErrorMessage(err, "Không tải được đơn mua hàng của nhà cung cấp."));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoadingOrders(false);
            });
        return () => controller.abort();
    }, [form.supplierId, loading, loadError]);

    const selectedOrder = useMemo(
        () => purchaseOrders.find((po) => String(po.id) === form.purchaseOrderId),
        [purchaseOrders, form.purchaseOrderId]
    );
    const subtotalCents = parseInvoiceCents(form.subtotal);
    const taxCents = parseInvoiceCents(form.taxAmount);
    const totalCents = subtotalCents !== null && taxCents !== null ? subtotalCents + taxCents : null;
    const poCents = selectedOrder ? parseInvoiceCents(selectedOrder.totalAmount) : null;
    const amountDifference = subtotalCents !== null && poCents !== null ? subtotalCents - poCents : null;
    const mismatched = amountDifference !== null && amountDifference !== 0;
    const liveProblems = validateSupplierInvoiceDraft(form, purchaseOrders);

    const changeField = (key, value) => {
        setForm((old) => ({
            ...old,
            [key]: value,
            ...(key === "supplierId" ? { purchaseOrderId: "" } : {}),
        }));
        setConfirmedWrite(false);
        setConfirmedMismatch(false);
        setSaveError("");
        setProblems([]);
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        if (saving || loading || loadingOrders || loadError || orderError) return;
        const issues = validateSupplierInvoiceDraft(form, purchaseOrders);
        setProblems(issues);
        setSaveError("");
        if (issues.length || !confirmedWrite || (mismatched && !confirmedMismatch)) return;
        setSaving(true);
        try {
            const payload = supplierInvoiceDraftPayload(form);
            const saved = editing
                ? await updateSupplierInvoice(invoiceId, payload)
                : await createSupplierInvoice(payload);
            if (!saved || !Number.isSafeInteger(Number(saved.id)) || Number(saved.id) <= 0) {
                throw new Error("Backend không trả về mã định danh hóa đơn hợp lệ.");
            }
            navigate(`${ROOT}/${saved.id}`, { replace: true });
        } catch (err) {
            setSaveError(supplierInvoiceErrorMessage(err, "Không thể lưu hóa đơn. Hãy kiểm tra dữ liệu."));
        } finally {
            setSaving(false);
        }
    };

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title={editing ? `Sửa hóa đơn ${original?.invoiceNumber || ""}` : "Tạo hóa đơn nhà cung cấp"}
                    description="Issue #10 — Liên kết hóa đơn với nhà cung cấp và đơn mua hàng. Tổng tiền = giá trị trước thuế + tiền thuế."
                    actions={<Button type="button" variant="outline" onClick={() => navigate(editing ? `${ROOT}/${invoiceId}` : ROOT)}>← Quay lại</Button>}
                />
                <Alert type="info">
                    Tạo hóa đơn mới ở trạng thái <strong>DRAFT</strong>. Chỉ hóa đơn <strong>DRAFT</strong> hoặc <strong>PENDING_VERIFICATION</strong> mới được sửa.
                    Mọi thao tác lưu sẽ ghi dữ liệu thật lên MySQL chung và cần xác nhận riêng.
                </Alert>
                {loadError && <div role="alert"><Alert type="danger">{loadError}</Alert></div>}
                {saveError && <div role="alert"><Alert type="danger">{saveError}</Alert></div>}
                {problems.length > 0 && <div role="alert"><Alert type="danger">
                    <strong>Thông tin chưa hợp lệ:</strong>
                    <ul className="mt-2 list-disc space-y-1 pl-5">{problems.map((item, index) => <li key={index}>{item}</li>)}</ul>
                </Alert></div>}
                {loading ? <Card><span className="flex items-center gap-2 text-sm text-slate-600"><Spinner /> Đang tải dữ liệu...</span></Card> : !loadError && (
                    <form onSubmit={handleSubmit} className="space-y-6">
                        {editing && original && <Card className="flex items-center justify-between gap-3">
                            <span className="text-sm text-slate-600">Mã nội bộ: <strong>{original.invoiceCode || "—"}</strong></span>
                            <SupplierInvoiceStatusBadge status={original.status} />
                        </Card>}
                        <Card>
                            <h2 className="mb-4 text-lg font-semibold text-slate-900">Nhà cung cấp và đơn mua hàng</h2>
                            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                                <Select
                                    label="Nhà cung cấp *"
                                    value={form.supplierId}
                                    options={[
                                        { value: "", label: "-- Chọn nhà cung cấp --" },
                                        ...suppliers.map((item) => ({ value: String(item.id), label: `${item.supplierName} (${item.supplierCode})` })),
                                    ]}
                                    onChange={(event) => changeField("supplierId", event.target.value)}
                                    disabled={saving}
                                />
                                <Select
                                    label="Đơn mua hàng liên kết *"
                                    value={form.purchaseOrderId}
                                    options={[
                                        { value: "", label: loadingOrders ? "Đang tải đơn mua hàng..." : "-- Chọn đơn mua hàng --" },
                                        ...(!loadingOrders && editing && form.purchaseOrderId && !selectedOrder ? [{ value: form.purchaseOrderId, label: `PO ID ${form.purchaseOrderId} (không còn đủ điều kiện)` }] : []),
                                        ...purchaseOrders.map((po) => ({ value: String(po.id), label: `${po.orderCode} · ${po.status} · ${formatSupplierInvoiceMoney(po.totalAmount)}` })),
                                    ]}
                                    onChange={(event) => changeField("purchaseOrderId", event.target.value)}
                                    disabled={!form.supplierId || loadingOrders || !!orderError || saving}
                                />
                            </div>
                            {orderError && <div className="mt-3" role="alert"><Alert type="danger">{orderError}</Alert></div>}
                            {form.supplierId && !loadingOrders && !orderError && purchaseOrders.length === 0 && (
                                <p className="mt-3 text-sm text-amber-700">NCC chưa có Purchase Order đủ điều kiện (ORDERED, PARTIALLY_RECEIVED hoặc COMPLETED).</p>
                            )}
                            {selectedOrder && <p className="mt-3 text-sm text-slate-600">
                                Giá trị Purchase Order: <strong>{formatSupplierInvoiceMoney(selectedOrder.totalAmount)}</strong>. Ngày giao dự kiến: {selectedOrder.expectedDeliveryDate || "—"}.
                            </p>}
                        </Card>
                        <Card>
                            <h2 className="mb-4 text-lg font-semibold text-slate-900">Thông tin hóa đơn</h2>
                            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                                <Input
                                    label="Số hóa đơn * (tối đa 100 ký tự)"
                                    value={form.invoiceNumber}
                                    maxLength={100}
                                    placeholder="Nhập số hóa đơn từ nhà cung cấp"
                                    onChange={(event) => changeField("invoiceNumber", event.target.value)}
                                />
                                <div className="text-sm text-slate-500 self-end pb-2">Số hóa đơn phải duy nhất trong cùng NCC, không phân biệt hoa/thường.</div>
                                <Input label="Ngày phát hành hóa đơn *" type="date" value={form.invoiceDate} onChange={(event) => changeField("invoiceDate", event.target.value)} />
                                <Input label="Hạn thanh toán *" type="date" min={form.invoiceDate || undefined} value={form.dueDate} onChange={(event) => changeField("dueDate", event.target.value)} />
                            </div>
                            {form.invoiceDate && form.dueDate && form.dueDate < form.invoiceDate && (
                                <p className="mt-3 text-sm font-medium text-red-600" role="alert">Hạn thanh toán không được trước ngày phát hành.</p>
                            )}
                        </Card>
                        <Card>
                            <h2 className="mb-2 text-lg font-semibold text-slate-900">Giá trị hóa đơn</h2>
                            <p className="mb-4 text-sm text-slate-500">Nhập tiền bằng số, không có dấu phân cách hàng nghìn (ví dụ: 12000000). Hỗ trợ tối đa 2 chữ số thập phân.</p>
                            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                                <Input label="Giá trị trước thuế (VND) *" type="text" inputMode="decimal" value={form.subtotal} placeholder="0" onChange={(event) => changeField("subtotal", event.target.value)} />
                                <Input label="Tiền thuế (VND) *" type="text" inputMode="decimal" value={form.taxAmount} placeholder="0" onChange={(event) => changeField("taxAmount", event.target.value)} />
                                <Input label="Tổng tiền (tự tính)" type="text" value={totalCents === null ? "—" : moneyLabel(totalCents)} readOnly className="bg-slate-50 font-semibold" />
                            </div>
                            {mismatched && selectedOrder && <div className="mt-4" role="status">
                                <Alert type="warning">
                                    <strong>Cảnh báo chênh lệch PO (trước thuế):</strong> {moneyLabel(amountDifference)} (hóa đơn trừ PO).
                                    <p className="mt-1">PO: {formatSupplierInvoiceMoney(selectedOrder.totalAmount)} · Hóa đơn trước thuế: {moneyLabel(subtotalCents)}.</p>
                                    <div className="mt-3">
                                        <Checkbox checked={confirmedMismatch} onChange={(event) => setConfirmedMismatch(event.target.checked)}
                                            label="Tôi đã kiểm tra chênh lệch với PO và vẫn muốn lưu hóa đơn." />
                                    </div>
                                </Alert>
                            </div>}
                            {amountDifference === 0 && <div className="mt-4"><Alert type="success">Giá trị trước thuế khớp với tổng tiền PO.</Alert></div>}
                        </Card>
                        <Card>
                            <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
                                <Checkbox checked={confirmedWrite} onChange={(event) => setConfirmedWrite(event.target.checked)}
                                    label="Tôi xác nhận thao tác này sẽ ghi dữ liệu thật vào MySQL chung của nhóm." />
                            </div>
                            <div className="mt-4 flex flex-wrap items-center justify-end gap-3">
                                <Button type="button" variant="outline" onClick={() => navigate(editing ? `${ROOT}/${invoiceId}` : ROOT)}>Hủy</Button>
                                <Button type="submit" disabled={saving || loadingOrders || !!orderError || !confirmedWrite || (mismatched && !confirmedMismatch)}>
                                    {saving ? "Đang lưu..." : editing ? "Lưu thay đổi hóa đơn" : "Tạo hóa đơn nháp"}
                                </Button>
                            </div>
                            {liveProblems.length > 0 && <p className="mt-3 text-xs text-slate-500">Form cần điền đủ các trường bắt buộc và giá trị hợp lệ trước khi lưu.</p>}
                        </Card>
                    </form>
                )}
            </div>
        </MainLayout>
    );
}
