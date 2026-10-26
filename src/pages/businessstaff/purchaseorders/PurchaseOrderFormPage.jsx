import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
    createPurchaseOrder,
    getPurchaseOrderDetail,
    getPurchaseOrderProductsBySupplier,
    getPurchaseOrderSuppliers,
    getPurchaseOrderWarehouses,
    purchaseOrderErrorMessage,
    updatePurchaseOrder,
} from "../../../api/business/purchaseOrders";
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
import TextArea from "../../../components/ui/TextArea";
import { formatPurchaseOrderMoney } from "./purchaseOrderFormat";
import { toPurchaseOrderPayload, validatePurchaseOrderDraft } from "./purchaseOrderDraft";

const ROOT = "/business-staff/purchase-orders";
const blankRow = (clientKey) => ({ clientKey, productId: "", orderedQuantity: "" });
const initialForm = () => ({
    supplierId: "",
    warehouseId: "",
    expectedDeliveryDate: "",
    note: "",
    items: [blankRow(1)],
});

export default function PurchaseOrderFormPage() {
    const { orderId } = useParams();
    const editing = orderId !== undefined;
    const navigate = useNavigate();
    const nextKey = useRef(1);
    const [form, setForm] = useState(initialForm);
    const [supplierOptions, setSupplierOptions] = useState([]);
    const [warehouseOptions, setWarehouseOptions] = useState([]);
    const [productOptions, setProductOptions] = useState([]);
    const [detail, setDetail] = useState(null);
    const [loading, setLoading] = useState(true);
    const [loadingProducts, setLoadingProducts] = useState(false);
    const [loadError, setLoadError] = useState("");
    const [productError, setProductError] = useState("");
    const [saveError, setSaveError] = useState("");
    const [saveProblems, setSaveProblems] = useState([]);
    const [saving, setSaving] = useState(false);
    const [confirmed, setConfirmed] = useState(false);
    const [confirmedPriceChange, setConfirmedPriceChange] = useState(false);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setLoadError("");
        setDetail(null);
        setConfirmed(false);
        setConfirmedPriceChange(false);

        if (editing && (!Number.isSafeInteger(Number(orderId)) || Number(orderId) <= 0)) {
            setLoadError("ID đơn mua hàng không hợp lệ.");
            setLoading(false);
            return () => controller.abort();
        }

        Promise.all([
            getPurchaseOrderSuppliers(controller.signal),
            getPurchaseOrderWarehouses(controller.signal),
            editing ? getPurchaseOrderDetail(orderId, controller.signal) : Promise.resolve(null),
        ]).then(([suppliers, warehouses, loadedDetail]) => {
            if (controller.signal.aborted) return;
            if (!Array.isArray(suppliers) || !Array.isArray(warehouses)) {
                throw new Error("Danh sách nhà cung cấp hoặc kho không hợp lệ.");
            }
            setSupplierOptions(suppliers);
            setWarehouseOptions(warehouses);
            if (editing) {
                if (loadedDetail?.status !== "DRAFT") {
                    setLoadError("Chỉ có thể sửa đơn mua hàng đang ở trạng thái DRAFT.");
                    return;
                }
                const loadedItems = Array.isArray(loadedDetail.items) ? loadedDetail.items : [];
                nextKey.current = Math.max(1, loadedItems.length);
                setForm({
                    supplierId: String(loadedDetail.supplierId ?? ""),
                    warehouseId: String(loadedDetail.warehouseId ?? ""),
                    expectedDeliveryDate: loadedDetail.expectedDeliveryDate || "",
                    note: loadedDetail.note || "",
                    items: loadedItems.length ? loadedItems.map((item, index) => ({
                        clientKey: index + 1,
                        productId: String(item.productId ?? ""),
                        orderedQuantity: String(item.orderedQuantity ?? ""),
                    })) : [blankRow(1)],
                });
                setDetail(loadedDetail);
            } else {
                nextKey.current = 1;
                setForm(initialForm());
            }
        }).catch((error) => {
            if (!controller.signal.aborted) {
                setLoadError(purchaseOrderErrorMessage(error, "Không tải được dữ liệu tạo/sửa đơn mua hàng."));
            }
        }).finally(() => {
            if (!controller.signal.aborted) setLoading(false);
        });
        return () => controller.abort();
    }, [editing, orderId]);

    useEffect(() => {
        const controller = new AbortController();
        setProductOptions([]);
        setProductError("");
        if (!form.supplierId || loading || loadError) {
            setLoadingProducts(false);
            return () => controller.abort();
        }
        setLoadingProducts(true);
        getPurchaseOrderProductsBySupplier(form.supplierId, controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(data)) throw new Error("Danh sách sản phẩm không hợp lệ.");
                setProductOptions(data);
            })
            .catch((error) => {
                if (!controller.signal.aborted) {
                    setProductError(purchaseOrderErrorMessage(error, "Không thể tải sản phẩm của nhà cung cấp."));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoadingProducts(false);
            });
        return () => controller.abort();
    }, [form.supplierId, loading, loadError]);

    const setField = (key, value) => {
        setForm((old) => ({ ...old, [key]: value }));
        setConfirmed(false);
        setConfirmedPriceChange(false);
        setSaveError("");
        setSaveProblems([]);
    };

    const changeSupplier = (value) => {
        nextKey.current += 1;
        setForm((old) => ({
            ...old,
            supplierId: value,
            // Products are supplier-specific: never silently retain old supplier's rows.
            items: [blankRow(nextKey.current)],
        }));
        setConfirmed(false);
        setConfirmedPriceChange(false);
        setSaveProblems([]);
        setSaveError("");
    };

    const updateRow = (clientKey, key, value) => {
        setForm((old) => ({
            ...old,
            items: old.items.map((row) => {
                if (row.clientKey !== clientKey) return row;
                if (key === "productId") {
                    const option = productOptions.find((item) => String(item.productId) === value);
                    return { ...row, productId: value, orderedQuantity: option ? String(Math.max(1, Number(option.minimumOrderQuantity || 1))) : "" };
                }
                return { ...row, [key]: value };
            }),
        }));
        setConfirmed(false);
        setConfirmedPriceChange(false);
        setSaveProblems([]);
        setSaveError("");
    };

    const addRow = () => {
        nextKey.current += 1;
        setForm((old) => ({ ...old, items: [...old.items, blankRow(nextKey.current)] }));
        setConfirmed(false);
        setSaveProblems([]);
    };

    const removeRow = (clientKey) => {
        setForm((old) => ({ ...old, items: old.items.filter((row) => row.clientKey !== clientKey) }));
        setConfirmed(false);
        setConfirmedPriceChange(false);
        setSaveProblems([]);
    };

    const selectedProductIds = new Set(form.items.filter((row) => row.productId).map((row) => String(row.productId)));
    const productById = useMemo(() => new Map(productOptions.map((option) => [String(option.productId), option])), [productOptions]);
    const oldPrices = useMemo(() => new Map((detail?.items || []).map((item) => [String(item.productId), Number(item.unitPrice)])), [detail]);
    const priceDiffers = editing && detail && String(detail.supplierId) === form.supplierId && form.items.some((row) => {
        const current = productById.get(row.productId);
        return current && oldPrices.has(row.productId) && Number(current.purchasePrice) !== oldPrices.get(row.productId);
    });
    const canAdd = Boolean(form.supplierId) && productOptions.length > selectedProductIds.size;
    const provisionalTotal = form.items.reduce((sum, row) => {
        const option = productById.get(row.productId);
        const qty = Number(row.orderedQuantity);
        const price = Number(option?.purchasePrice);
        return sum + (option && Number.isFinite(qty) && qty > 0 && Number.isFinite(price) ? qty * price : 0);
    }, 0);

    const save = async (event) => {
        event.preventDefault();
        if (saving || loading || loadingProducts || loadError || productError) return;
        setSaveError("");
        const problems = validatePurchaseOrderDraft(form, productOptions);
        setSaveProblems(problems);
        if (problems.length || !confirmed || (priceDiffers && !confirmedPriceChange)) return;
        setSaving(true);
        try {
            const payload = toPurchaseOrderPayload(form);
            const saved = editing
                ? await updatePurchaseOrder(orderId, payload)
                : await createPurchaseOrder(payload);
            if (!saved?.id) throw new Error("Backend không trả về ID đơn mua hàng.");
            navigate(`${ROOT}/${saved.id}`, {
                replace: true,
                state: { notice: editing ? "Đã cập nhật đơn nháp." : "Đã tạo đơn mua hàng ở trạng thái DRAFT. Chưa gửi đặt hàng và chưa thay đổi tồn kho." },
            });
        } catch (error) {
            setSaveError(purchaseOrderErrorMessage(error, "Không lưu được đơn mua hàng."));
        } finally {
            setSaving(false);
        }
    };

    const blocked = Boolean(loadError);
    const supplierSelectOptions = [{ value: "", label: "-- Chọn nhà cung cấp --" }, ...supplierOptions.map((item) => ({ value: String(item.id), label: `${item.name} (${item.code})` }))];
    const warehouseSelectOptions = [{ value: "", label: "-- Chọn kho tổng --" }, ...warehouseOptions.map((item) => ({ value: String(item.id), label: `${item.name} (${item.code})` }))];

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title={editing ? `Sửa đơn nháp ${detail?.orderCode || ""}` : "Tạo đơn mua hàng"}
                    description="Issue #9 — Chọn NCC, kho tổng, các sản phẩm đã liên kết và số lượng. Giá nhập lấy trực tiếp từ liên kết NCC."
                    actions={<Button type="button" variant="outline" onClick={() => navigate(editing ? `${ROOT}/${orderId}` : ROOT)}>← Quay lại</Button>}
                />
                <Alert type="info">Tạo đơn chỉ lưu trạng thái DRAFT, không nhập tồn kho. Sau khi lưu, việc gửi hoặc hủy đơn được thực hiện trong trang chi tiết và phải xác nhận riêng. Không sử dụng dữ liệu thử trên MySQL chung khi chưa thống nhất với nhóm.</Alert>
                {loadError && <div role="alert"><Alert type="danger">{loadError}</Alert></div>}
                {productError && <div role="alert"><Alert type="danger">{productError}</Alert></div>}
                {saveError && <div role="alert"><Alert type="danger">{saveError}</Alert></div>}
                {saveProblems.length > 0 && <div role="alert"><Alert type="danger"><strong>Thông tin chưa hợp lệ:</strong><ul className="mt-2 list-disc space-y-1 pl-5">{saveProblems.map((text, index) => <li key={index}>{text}</li>)}</ul></Alert></div>}

                {loading ? <Card className="flex items-center gap-3"><Spinner /> Đang tải thông tin đơn mua hàng...</Card> : blocked ? null : (
                    <form onSubmit={save} className="space-y-6">
                        <Card className="space-y-5">
                            <h2 className="text-lg font-semibold text-slate-900">Nhà cung cấp và kho tổng</h2>
                            <div className="grid gap-4 md:grid-cols-2">
                                <Select label="Nhà cung cấp *" value={form.supplierId} options={supplierSelectOptions} disabled={saving} onChange={(event) => changeSupplier(event.target.value)} />
                                <Select label="Kho tổng nhận hàng *" value={form.warehouseId} options={warehouseSelectOptions} disabled={saving} onChange={(event) => setField("warehouseId", event.target.value)} />
                                <Input label="Ngày dự kiến giao hàng *" type="date" value={form.expectedDeliveryDate} disabled={saving} onChange={(event) => setField("expectedDeliveryDate", event.target.value)} />
                            </div>
                            <TextArea label="Ghi chú (nếu có)" value={form.note} disabled={saving} rows={3} onChange={(event) => setField("note", event.target.value)} />
                            {editing && <p className="text-sm text-slate-500">Chỉ đơn DRAFT mới được sửa. Nếu đổi nhà cung cấp, danh sách sản phẩm sẽ được yêu cầu chọn lại.</p>}
                        </Card>

                        <Card className="space-y-4">
                            <div className="flex flex-wrap items-center justify-between gap-3">
                                <div>
                                    <h2 className="text-lg font-semibold text-slate-900">Sản phẩm đặt mua</h2>
                                    <p className="mt-1 text-sm text-slate-500">Chỉ sản phẩm ACTIVE có liên kết ACTIVE với nhà cung cấp. Giá nhập do Backend quyết định, không nhập thủ công.</p>
                                </div>
                                <Button type="button" variant="outline" disabled={!canAdd || saving || loadingProducts} onClick={addRow}>+ Thêm sản phẩm</Button>
                            </div>
                            {loadingProducts && <p role="status" className="text-sm text-slate-500">Đang tải sản phẩm liên kết...</p>}
                            {!loadingProducts && form.supplierId && !productError && productOptions.length === 0 && <Alert type="warning">Nhà cung cấp chưa có sản phẩm ACTIVE được phép đặt hàng.</Alert>}
                            {form.items.map((row, index) => {
                                const option = productById.get(row.productId);
                                const otherIds = new Set(form.items.filter((item) => item.clientKey !== row.clientKey).map((item) => item.productId));
                                const selectOptions = [
                                    { value: "", label: "-- Chọn sản phẩm --" },
                                    ...productOptions.filter((item) => !otherIds.has(String(item.productId))).map((item) => ({
                                        value: String(item.productId), label: `${item.productCode} — ${item.productName} (${item.baseUnit || ""})`,
                                    })),
                                ];
                                // Keep stale selections visible for review; validation prevents saving them.
                                if (row.productId && !option) {
                                    selectOptions.push({ value: row.productId, label: `SP ID ${row.productId} (không còn liên kết ACTIVE)` });
                                }
                                const quantity = Number(row.orderedQuantity);
                                const total = option && Number.isFinite(quantity) && quantity > 0 ? quantity * Number(option.purchasePrice) : 0;
                                return (
                                    <div key={row.clientKey} className="space-y-3 rounded-xl border border-slate-200 bg-slate-50 p-4">
                                        <div className="flex items-center justify-between gap-2">
                                            <span className="text-sm font-semibold text-slate-800">Sản phẩm #{index + 1}</span>
                                            <Button type="button" size="sm" variant="outline" disabled={saving || form.items.length === 1} onClick={() => removeRow(row.clientKey)}>Xóa dòng</Button>
                                        </div>
                                        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                                            <Select label="Sản phẩm *" disabled={!form.supplierId || loadingProducts || saving} value={row.productId} options={selectOptions} onChange={(event) => updateRow(row.clientKey, "productId", event.target.value)} />
                                            <Input label="Số lượng đặt *" type="number" min="1" step="1" disabled={!option || saving} value={row.orderedQuantity} onChange={(event) => updateRow(row.clientKey, "orderedQuantity", event.target.value)} />
                                            <div className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm">
                                                <span className="block text-slate-500">Đơn giá nhập từ NCC</span>
                                                <strong className="mt-1 block text-slate-900">{option ? formatPurchaseOrderMoney(option.purchasePrice) : "—"}</strong>
                                            </div>
                                        </div>
                                        {option && row.orderedQuantity !== "" && Number.isFinite(quantity) &&
                                            quantity > 0 && quantity < Math.max(1, Number(option.minimumOrderQuantity || 1)) && (
                                                <p role="alert" className="text-sm font-medium text-rose-700">
                                                    Số lượng đặt phải đạt tối thiểu {option.minimumOrderQuantity || 1} (MOQ).
                                                </p>
                                            )}
                                        {option && <div className="flex flex-wrap justify-between gap-3 text-sm text-slate-600">
                                            <span>MOQ: <b>{option.minimumOrderQuantity}</b> · Giao dự kiến: <b>{option.leadTimeDays} ngày</b>{option.defaultSupplier ? " · NCC mặc định" : ""}</span>
                                            <span>Thành tiền tạm tính: <b className="text-slate-900">{formatPurchaseOrderMoney(total)}</b></span>
                                        </div>}
                                    </div>
                                );
                            })}
                            <div className="flex justify-end border-t border-slate-200 pt-4 text-right">
                                <div>
                                    <p className="text-sm text-slate-500">Tổng tiền tạm tính</p>
                                    <strong className="text-2xl text-slate-900">{formatPurchaseOrderMoney(provisionalTotal)}</strong>
                                </div>
                            </div>
                        </Card>
                        {priceDiffers && <Alert type="warning">
                            Đơn nháp này có đơn giá cũ khác giá liên kết NCC hiện tại. Nếu lưu sửa, Backend sẽ lấy lại giá nhập hiện tại của NCC; vui lòng xác nhận với nhóm trước khi tiếp tục.
                            <div className="mt-3"><Checkbox checked={confirmedPriceChange} onChange={(event) => setConfirmedPriceChange(event.target.checked)} label="Tôi hiểu đơn giá sẽ được cập nhật theo liên kết NCC hiện tại." /></div>
                        </Alert>}
                        <Card className="space-y-4">
                            <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
                                <Checkbox checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} label="Tôi xác nhận thao tác này sẽ ghi dữ liệu thật vào MySQL chung của nhóm." />
                            </div>
                            <div className="flex flex-wrap justify-end gap-3">
                                <Button type="button" variant="outline" onClick={() => navigate(editing ? `${ROOT}/${orderId}` : ROOT)}>Hủy</Button>
                                <Button type="submit" disabled={saving || loadingProducts || !!productError || !confirmed || (priceDiffers && !confirmedPriceChange)}>{saving ? "Đang lưu..." : editing ? "Lưu thay đổi DRAFT" : "Tạo đơn nháp"}</Button>
                            </div>
                        </Card>
                    </form>
                )}
            </div>
        </MainLayout>
    );
}
