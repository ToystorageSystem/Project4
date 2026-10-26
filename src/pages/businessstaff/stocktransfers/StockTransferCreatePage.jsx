import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
    createStockTransferDraft,
    getStockTransferDetail,
    updateStockTransferDraft,
    getStockTransferDestinations,
    getStockTransferSourceProducts,
    getStockTransferSources,
    stockTransferErrorMessage,
} from "../../../api/business/stockTransfers";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import Input from "../../../components/ui/Input";
import Select from "../../../components/ui/Select";
import Spinner from "../../../components/ui/Spinner";
import TextArea from "../../../components/ui/TextArea";
import {
    TRANSFER_REASONS,
    blankStockTransferForm,
    blankTransferRow,
    stockTransferFormFromDetail,
    toUpdateStockTransferPayload,
    todayLocalIso,
    toStockTransferPayload,
    validateStockTransferDraft,
} from "./stockTransferDraft";

const ROOT = "/business-staff/stock-transfers";
const numberLabel = new Intl.NumberFormat("vi-VN");
const displayQuantity = (quantity) => numberLabel.format(Number(quantity) || 0);

export default function StockTransferCreatePage() {
    const navigate = useNavigate();
    const { transferId } = useParams();
    const isEditMode = transferId !== undefined;
    const nextRowId = useRef(1);
    const [form, setForm] = useState(blankStockTransferForm);
    const [sources, setSources] = useState([]);
    const [destinations, setDestinations] = useState([]);
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [loadingProducts, setLoadingProducts] = useState(false);
    const [loadError, setLoadError] = useState("");
    const [productsError, setProductsError] = useState("");
    const [refresh, setRefresh] = useState(0);
    const [previewRequested, setPreviewRequested] = useState(false);
    const [confirmedDbWrite, setConfirmedDbWrite] = useState(false);
    const [saving, setSaving] = useState(false);
    const [saveError, setSaveError] = useState("");
    const [uncertainSave, setUncertainSave] = useState(false);
    const [originalDetail, setOriginalDetail] = useState(null);
    const [loadingExisting, setLoadingExisting] = useState(isEditMode);
    const [existingError, setExistingError] = useState("");

    // A separate GET initializes editing from an existing DRAFT only.
    useEffect(() => {
        if (!isEditMode) return;
        const controller = new AbortController();
        const id = Number(transferId);
        setLoadingExisting(true);
        setExistingError("");
        setOriginalDetail(null);
        if (!Number.isSafeInteger(id) || id <= 0) {
            setExistingError("Mã định danh phiếu không hợp lệ.");
            setLoadingExisting(false);
            return () => controller.abort();
        }
        getStockTransferDetail(id, controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;
                if (!data || Number(data.id) !== id || !Array.isArray(data.items)) {
                    throw new Error("Dữ liệu phiếu không đúng định dạng.");
                }
                if (data.status !== "DRAFT") {
                    throw new Error("Chỉ phiếu DRAFT mới được sửa tại trang này. Hãy quay lại chi tiết.");
                }
                const loadedForm = stockTransferFormFromDetail(data);
                if (!loadedForm.items.length) throw new Error("Phiếu không có sản phẩm.");
                nextRowId.current = loadedForm.items.length;
                setForm(loadedForm);
                setOriginalDetail(data);
                setPreviewRequested(false);
                setConfirmedDbWrite(false);
            })
            .catch((error) => {
                if (!controller.signal.aborted) {
                    setExistingError(stockTransferErrorMessage(error, "Không thể tải phiếu để sửa."));
                }
            })
            .finally(() => { if (!controller.signal.aborted) setLoadingExisting(false); });
        return () => controller.abort();
    }, [transferId, isEditMode]);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setLoadError("");
        Promise.all([
            getStockTransferSources(controller.signal),
            getStockTransferDestinations(controller.signal),
        ]).then(([sourceOptions, destinationOptions]) => {
            if (controller.signal.aborted) return;
            if (!Array.isArray(sourceOptions) || !Array.isArray(destinationOptions)) {
                throw new Error("Dữ liệu kho xuất hoặc cửa hàng nhận không đúng định dạng.");
            }
            setSources(sourceOptions);
            setDestinations(destinationOptions);
        }).catch((error) => {
            if (!controller.signal.aborted) setLoadError(stockTransferErrorMessage(error));
        }).finally(() => {
            if (!controller.signal.aborted) setLoading(false);
        });
        return () => controller.abort();
    }, [refresh]);

    useEffect(() => {
        const controller = new AbortController();
        setProducts([]);
        setProductsError("");
        if (!form.fromWarehouseId || loadError) {
            setLoadingProducts(false);
            return () => controller.abort();
        }
        setLoadingProducts(true);
        getStockTransferSourceProducts(form.fromWarehouseId, controller.signal)
            .then((options) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(options)) throw new Error("Dữ liệu sản phẩm không đúng định dạng.");
                setProducts(options);
            }).catch((error) => {
                if (!controller.signal.aborted) setProductsError(stockTransferErrorMessage(error));
            }).finally(() => {
                if (!controller.signal.aborted) setLoadingProducts(false);
            });
        return () => controller.abort();
    }, [form.fromWarehouseId, loadError, refresh]);

    const source = sources.find((item) => String(item.id) === form.fromWarehouseId);
    const destination = destinations.find((item) => String(item.id) === form.toWarehouseId);
    const sourceOptions = [
        { value: "", label: "-- Chọn kho/cửa hàng xuất --" },
        ...sources.map((item) => ({ value: String(item.id), label: `${item.name} (${item.code}) · ${item.type}` })),
    ];
    const destinationOptions = [
        { value: "", label: "-- Chọn cửa hàng nhận --" },
        ...destinations.filter((item) => String(item.id) !== form.fromWarehouseId).map((item) => ({
            value: String(item.id), label: `${item.name} (${item.code})`,
        })),
    ];

    const productById = useMemo(
        () => new Map(products.map((item) => [String(item.productId), item])),
        [products],
    );
    const totalQuantity = useMemo(() => form.items.reduce((sum, row) => {
        const value = Number(row.requestedQuantity);
        return sum + (Number.isSafeInteger(value) && value > 0 ? value : 0);
    }, 0), [form.items]);
    const errors = useMemo(() => validateStockTransferDraft(form, sources, destinations, products), [form, sources, destinations, products]);
    const previewReady = previewRequested && !loading && !loadingExisting && !loadingProducts
        && !loadError && !existingError && !productsError && errors.length === 0;
    const payload = useMemo(() => previewReady ? toStockTransferPayload(form) : null, [form, previewReady]);
    const minDate = todayLocalIso();

    // A material edit invalidates the previously confirmed preview and write consent.
    const invalidatePreview = () => {
        setPreviewRequested(false);
        setConfirmedDbWrite(false);
        setSaveError("");
    };
    const setField = (name, value) => {
        setForm((previous) => ({ ...previous, [name]: value }));
        invalidatePreview();
    };
    const changeSource = (value) => {
        if (isEditMode) return;
        setForm((previous) => ({
            ...previous,
            fromWarehouseId: value,
            toWarehouseId: previous.toWarehouseId === value ? "" : previous.toWarehouseId,
            items: [blankTransferRow(++nextRowId.current)],
        }));
        setProducts([]);
        invalidatePreview();
    };
    const changeRow = (key, field, value) => {
        setForm((previous) => ({
            ...previous,
            items: previous.items.map((row) => row.clientKey === key
                ? (field === "productId" ? { ...row, productId: value, requestedQuantity: value ? "1" : "" } : { ...row, [field]: value })
                : row),
        }));
        invalidatePreview();
    };
    const addRow = () => {
        setForm((previous) => ({ ...previous, items: [...previous.items, blankTransferRow(++nextRowId.current)] }));
        invalidatePreview();
    };
    const removeRow = (key) => {
        setForm((previous) => ({ ...previous, items: previous.items.filter((row) => row.clientKey !== key) }));
        invalidatePreview();
    };

    const handleSaveDraft = async () => {
        if (!previewReady || !confirmedDbWrite || saving || uncertainSave) return;
        // A second check before POST/PUT reduces the chance of relying on stale inventory.
        // Backend remains authoritative and rechecks stock inside its transaction.
        setSaving(true);
        setSaveError("");
        let latestProducts;
        try {
            latestProducts = await getStockTransferSourceProducts(form.fromWarehouseId);
            if (!Array.isArray(latestProducts)) throw new Error("Dữ liệu tồn kho không đúng định dạng.");
        } catch (error) {
            setSaveError(stockTransferErrorMessage(error, "Không kiểm tra lại được tồn kho trước khi lưu."));
            setSaving(false);
            return;
        }
        const freshProblems = validateStockTransferDraft(form, sources, destinations, latestProducts);
        if (freshProblems.length) {
            setProducts(latestProducts);
            setPreviewRequested(true);
            setConfirmedDbWrite(false);
            setSaveError("Dữ liệu vừa thay đổi. Hãy sửa các lỗi được hiển thị, rồi xem trước lại.");
            setSaving(false);
            return;
        }
        if (isEditMode) {
            // Avoid overwriting a transfer that another session has modified.
            try {
                const latest = await getStockTransferDetail(transferId);
                if (latest?.status !== "DRAFT" || Number(latest.id) !== Number(transferId)) {
                    setSaveError("Phiếu không còn ở trạng thái DRAFT. Hãy quay lại chi tiết và làm mới.");
                    setSaving(false);
                    return;
                }
                if (originalDetail?.updatedAt && latest.updatedAt !== originalDetail.updatedAt) {
                    setSaveError("Phiếu đã được cập nhật ở nơi khác. Hãy tải lại trước khi sửa để tránh ghi đè.");
                    setSaving(false);
                    return;
                }
            } catch (error) {
                setSaveError(stockTransferErrorMessage(error, "Không thể xác minh trạng thái mới nhất của phiếu."));
                setSaving(false);
                return;
            }
        }
        try {
            const saved = isEditMode
                ? await updateStockTransferDraft(transferId, toUpdateStockTransferPayload(form))
                : await createStockTransferDraft(toStockTransferPayload(form));
            const expectedId = isEditMode ? Number(transferId) : Number(saved?.id);
            if (!Number.isSafeInteger(Number(saved?.id)) || Number(saved.id) !== expectedId || saved.status !== "DRAFT") {
                // A successful POST/PUT may have changed data despite an unexpected response.
                setUncertainSave(true);
                setSaveError("Backend phản hồi không như dự kiến. Không bấm lưu lại; hãy kiểm tra chi tiết phiếu với nhóm.");
                return;
            }
            navigate(`${ROOT}/${saved.id}`, { replace: true });
        } catch (error) {
            if (!error?.response) {
                setUncertainSave(true);
                setSaveError("Mất kết nối lúc lưu. Dữ liệu có thể đã được ghi; KHÔNG bấm lại trước khi kiểm tra chi tiết phiếu.");
            } else {
                setSaveError(stockTransferErrorMessage(error, "Backend không thể lưu thay đổi DRAFT."));
            }
        } finally {
            setConfirmedDbWrite(false);
            setSaving(false);
        }
    };

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title={isEditMode ? `Sửa phiếu điều chuyển ${originalDetail?.transferCode || `#${transferId}`}` : "Tạo phiếu điều chuyển hàng"}
                    description="Issue #11 — Kho tổng → Cửa hàng hoặc Cửa hàng → Cửa hàng. Backend kiểm tra lại tồn khả dụng khi ghi phiếu."
                    actions={<Button type="button" variant="outline" onClick={() => navigate(isEditMode ? `${ROOT}/${transferId}` : "/business-staff/dashboard")}>← {isEditMode ? "Chi tiết" : "Dashboard"}</Button>}
                />
                <Alert type="info">
                    <strong>{isEditMode ? "Sửa phiếu DRAFT" : "Tạo phiếu DRAFT"}:</strong> Xem trước chỉ kiểm tra trên giao diện.
                    Thao tác lưu ghi dữ liệu thật vào MySQL chung, chưa giữ chỗ tồn kho.
                    Gửi phiếu từ trang chi tiết mới chuyển sang CREATED và giữ chỗ tồn kho.
                    {isEditMode && " Điểm xuất của phiếu đã tạo không thể thay đổi."}
                </Alert>
                {loadError && <div role="alert"><Alert type="danger">{loadError} <Button type="button" variant="outline" size="sm" onClick={() => setRefresh((count) => count + 1)}>Thử lại</Button></Alert></div>}
                {productsError && <div role="alert"><Alert type="danger">{productsError}</Alert></div>}
                {existingError && <div role="alert"><Alert type="danger">{existingError}</Alert></div>}

                {(loading || loadingExisting) ? <Card className="flex items-center gap-3"><Spinner /> Đang tải dữ liệu phiếu và danh mục...</Card> : !loadError && !existingError && (
                    <>
                        <Card className="space-y-5">
                            <h2 className="text-lg font-semibold text-slate-900">Điểm xuất và điểm nhận</h2>
                            <div className="grid gap-4 md:grid-cols-2">
                                <Select label="Kho / cửa hàng xuất *" disabled={saving || isEditMode} value={form.fromWarehouseId} options={sourceOptions} onChange={(event) => changeSource(event.target.value)} />
                                <Select label="Cửa hàng nhận *" disabled={saving} value={form.toWarehouseId} options={destinationOptions} onChange={(event) => setField("toWarehouseId", event.target.value)} />
                            </div>
                            {source && <p className="text-sm text-slate-500">Điểm xuất: {source.address || source.name} · Loại: {source.type}</p>}
                            {destination && <p className="text-sm text-slate-500">Điểm nhận: {destination.address || destination.name}</p>}
                            <div className="grid gap-4 md:grid-cols-2">
                                <Input label="Ngày dự kiến xuất hàng *" disabled={saving} type="date" min={minDate} value={form.expectedShipmentDate} onChange={(event) => setField("expectedShipmentDate", event.target.value)} />
                                <Input label="Ngày dự kiến nhận hàng *" disabled={saving} type="date" min={form.expectedShipmentDate || minDate} value={form.expectedReceiptDate} onChange={(event) => setField("expectedReceiptDate", event.target.value)} />
                            </div>
                            <Select label="Lý do điều chuyển *" disabled={saving} value={form.reasonCode} options={[{ value: "", label: "-- Chọn lý do --" }, ...TRANSFER_REASONS]} onChange={(event) => setField("reasonCode", event.target.value)} />
                            <TextArea disabled={saving} label={form.reasonCode === "OTHER" ? "Giải thích lý do khác *" : "Diễn giải lý do (nếu có)"} rows={2} value={form.reasonNote} onChange={(event) => setField("reasonNote", event.target.value)} />
                            <TextArea disabled={saving} label="Ghi chú (nếu có)" rows={2} value={form.notes} onChange={(event) => setField("notes", event.target.value)} />
                        </Card>

                        <Card className="space-y-4">
                            <div className="flex flex-wrap items-center justify-between gap-3">
                                <div>
                                    <h2 className="text-lg font-semibold text-slate-900">Sản phẩm điều chuyển</h2>
                                    <p className="mt-1 text-sm text-slate-500">Tồn khả dụng được tải từ Backend theo kho/cửa hàng xuất, không phải tồn của cửa hàng nhận.</p>
                                </div>
                                <Button type="button" variant="outline" disabled={saving || !form.fromWarehouseId || loadingProducts || products.length === 0} onClick={addRow}>+ Thêm sản phẩm</Button>
                            </div>
                            {loadingProducts && <p className="flex items-center gap-2 text-sm text-slate-500"><Spinner /> Đang tải tồn khả dụng...</p>}
                            {form.fromWarehouseId && !loadingProducts && !productsError && products.length === 0 && <Alert type="warning">Điểm xuất chưa có sản phẩm khả dụng. Hãy kiểm tra tồn kho hoặc chọn điểm xuất khác.</Alert>}
                            {form.items.map((row, index) => {
                                const selectedProduct = productById.get(row.productId);
                                const otherSelected = new Set(form.items.filter((item) => item.clientKey !== row.clientKey).map((item) => item.productId));
                                const eligible = products.filter((item) => !otherSelected.has(String(item.productId)) && Number(item.availableQuantity) > 0);
                                const options = [
                                    { value: "", label: "-- Chọn sản phẩm --" },
                                    ...eligible.map((item) => ({
                                        value: String(item.productId),
                                        label: `${item.productCode} — ${item.productName} (${item.baseUnit || ""}) · Tồn ${displayQuantity(item.availableQuantity)}`,
                                    })),
                                ];
                                const requested = Number(row.requestedQuantity);
                                const overStock = selectedProduct && row.requestedQuantity !== "" && Number.isSafeInteger(requested) && requested > Number(selectedProduct.availableQuantity);
                                return (
                                    <div key={row.clientKey} className="space-y-3 rounded-xl border border-slate-200 bg-slate-50 p-4">
                                        <div className="flex items-center justify-between gap-3">
                                            <span className="text-sm font-semibold text-slate-800">Sản phẩm #{index + 1}</span>
                                            <Button type="button" size="sm" variant="outline" disabled={saving || form.items.length === 1} onClick={() => removeRow(row.clientKey)}>Xóa dòng</Button>
                                        </div>
                                        <div className="grid gap-4 md:grid-cols-3">
                                            <Select label="Sản phẩm *" value={row.productId} disabled={saving || !form.fromWarehouseId || loadingProducts} options={options} onChange={(event) => changeRow(row.clientKey, "productId", event.target.value)} />
                                            <Input label="Số lượng yêu cầu *" type="number" min="1" step="1" max={selectedProduct?.availableQuantity ?? undefined} disabled={saving || !selectedProduct || loadingProducts} value={row.requestedQuantity} onChange={(event) => changeRow(row.clientKey, "requestedQuantity", event.target.value)} />
                                            <div className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm">
                                                <span className="block text-slate-500">Tồn khả dụng tại điểm xuất</span>
                                                <strong className="mt-1 block text-xl text-slate-900">{selectedProduct ? displayQuantity(selectedProduct.availableQuantity) : "—"}</strong>
                                            </div>
                                        </div>
                                        {overStock && <p role="alert" className="text-sm font-semibold text-red-600">Yêu cầu {displayQuantity(requested)} vượt tồn khả dụng {displayQuantity(selectedProduct.availableQuantity)}.</p>}
                                        {selectedProduct && <p className="text-xs text-slate-500">Mã: {selectedProduct.productCode} · Barcode: {selectedProduct.barcode || "—"} · Đơn vị: {selectedProduct.baseUnit || "—"}</p>}
                                    </div>
                                );
                            })}
                            <div className="flex justify-end border-t border-slate-200 pt-4 text-right">
                                <div>
                                    <p className="text-sm text-slate-500">Tổng số lượng yêu cầu</p>
                                    <strong className="text-2xl text-slate-900">{displayQuantity(totalQuantity)}</strong>
                                </div>
                            </div>
                        </Card>

                        <Card className="space-y-4">
                            <h2 className="text-lg font-semibold text-slate-900">Kiểm tra bản nháp trên giao diện</h2>
                            <p className="text-sm text-slate-600">Kiểm tra dữ liệu trên màn hình trước khi lưu. Việc xem trước không ghi vào MySQL.</p>
                            {previewRequested && errors.length > 0 && <div role="alert"><Alert type="danger">
                                <strong>Cần sửa các thông tin sau:</strong>
                                <ul className="mt-2 list-disc space-y-1 pl-5">{errors.map((message, index) => <li key={index}>{message}</li>)}</ul>
                            </Alert></div>}
                            {previewReady && <Alert type="success">
                                <strong>Kiểm tra đạt — chưa ghi dữ liệu.</strong>
                                <p className="mt-2">{source?.name} → {destination?.name}; {payload.items.length} sản phẩm, tổng {displayQuantity(totalQuantity)} đơn vị.</p>
                                <p>Ngày xuất: {payload.expectedShipmentDate}; ngày nhận: {payload.expectedReceiptDate}; lý do: {TRANSFER_REASONS.find((item) => item.value === payload.reasonCode)?.label}.</p>
                            </Alert>}
                            {saveError && <div role="alert"><Alert type="danger">{saveError}</Alert></div>}
                            {previewReady && !uncertainSave && <label className="flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-slate-700">
                                <input type="checkbox" className="mt-1 accent-orange-600" checked={confirmedDbWrite} disabled={saving} onChange={(event) => setConfirmedDbWrite(event.target.checked)} />
                                <span>Tôi xác nhận {isEditMode ? "cập nhật phiếu DRAFT" : "tạo phiếu DRAFT"} trên <strong>MySQL chung của nhóm</strong>. Thao tác ghi dữ liệu thật, chưa giữ chỗ tồn kho.</span>
                            </label>}
                            <div className="flex flex-wrap justify-end gap-3">
                                <Button type="button" variant="outline" disabled={saving || uncertainSave} onClick={() => {
                                    const resetForm = isEditMode && originalDetail
                                        ? stockTransferFormFromDetail(originalDetail)
                                        : blankStockTransferForm();
                                    setForm(resetForm);
                                    nextRowId.current = resetForm.items.length;
                                    invalidatePreview();
                                }}>{isEditMode ? "Khôi phục dữ liệu" : "Làm lại form"}</Button>
                                <Button type="button" disabled={saving || loadingProducts || !!productsError || uncertainSave} onClick={() => setPreviewRequested(true)}>Kiểm tra / Xem trước</Button>
                                <Button type="button" disabled={!previewReady || !confirmedDbWrite || saving || uncertainSave} onClick={handleSaveDraft}>
                                    {saving ? "Đang lưu..." : (isEditMode ? "Lưu thay đổi DRAFT" : "Lưu phiếu DRAFT")}
                                </Button>
                            </div>
                            <p className="text-xs text-slate-500">Gửi CREATED và Hủy phiếu được thực hiện riêng tại trang chi tiết, với xác nhận trước khi thay đổi MySQL. Không bật Seeder.</p>
                        </Card>
                    </>
                )}
            </div>
        </MainLayout>
    );
}
