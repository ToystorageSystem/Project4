import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
    createStockTransferDraft,
    getStockTransferDetail,
    updateStockTransfer,
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
    canEditStockTransfer,
    productsWithCurrentReservation,
    stockTransferFormFromDetail,
    toUpdateStockTransferPayload,
    todayLocalIso,
    toStockTransferPayload,
    validateStockTransferDraftDetailed,
} from "./stockTransferDraft";

const ROOT = "/business/stock-transfers";
const numberLabel = new Intl.NumberFormat("en-GB");
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
    const [showValidationErrors, setShowValidationErrors] = useState(false);
    const [saving, setSaving] = useState(false);
    const [saveError, setSaveError] = useState("");
    const [uncertainSave, setUncertainSave] = useState(false);
    const [originalDetail, setOriginalDetail] = useState(null);
    const [loadingExisting, setLoadingExisting] = useState(isEditMode);
    const [existingError, setExistingError] = useState("");

    // Load the current state: Backend permits editing until picking begins.
    useEffect(() => {
        if (!isEditMode) return;
        const controller = new AbortController();
        const id = Number(transferId);
        setLoadingExisting(true);
        setExistingError("");
        setOriginalDetail(null);
        if (!Number.isSafeInteger(id) || id <= 0) {
            setExistingError("Invalid transfer identifier.");
            setLoadingExisting(false);
            return () => controller.abort();
        }
        getStockTransferDetail(id, controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;
                if (!data || Number(data.id) !== id || !Array.isArray(data.items)) {
                    throw new Error("Invalid transfer data format.");
                }
                if (!canEditStockTransfer(data.status)) {
                    throw new Error("The warehouse has started processing, or the transfer is already closed. Return to the details page.");
                }
                const loadedForm = stockTransferFormFromDetail(data);
                if (!loadedForm.items.length) throw new Error("This transfer contains no products.");
                nextRowId.current = loadedForm.items.length;
                setForm(loadedForm);
                setOriginalDetail(data);
                setShowValidationErrors(false);
            })
            .catch((error) => {
                if (!controller.signal.aborted) {
                    setExistingError(stockTransferErrorMessage(error, "Unable to load transfer for editing."));
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
                throw new Error("Invalid source warehouse or destination store data.");
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
                if (!Array.isArray(options)) throw new Error("Invalid product data.");
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
        { value: "", label: "-- Select Source Warehouse / Store --" },
        ...sources.map((item) => ({ value: String(item.id), label: `${item.name} (${item.code}) · ${item.type}` })),
    ];
    const destinationOptions = [
        { value: "", label: "-- Select Destination Store --" },
        ...destinations.filter((item) => String(item.id) !== form.fromWarehouseId).map((item) => ({
            value: String(item.id), label: `${item.name} (${item.code})`,
        })),
    ];

    const editingReservedTransfer = isEditMode
        && ["CREATED", "PENDING_SOURCE_CONFIRMATION"].includes(originalDetail?.status);
    const canChangeDestination = !isEditMode || originalDetail?.status === "DRAFT";
    const editableProducts = useMemo(
        () => productsWithCurrentReservation(products, originalDetail),
        [products, originalDetail],
    );
    const productById = useMemo(
        () => new Map(editableProducts.map((item) => [String(item.productId), item])),
        [editableProducts],
    );
    const totalQuantity = useMemo(() => form.items.reduce((sum, row) => {
        const value = Number(row.requestedQuantity);
        return sum + (Number.isSafeInteger(value) && value > 0 ? value : 0);
    }, 0), [form.items]);
    const validation = useMemo(
        () => validateStockTransferDraftDetailed(form, sources, destinations, editableProducts),
        [form, sources, destinations, editableProducts],
    );
    const { errors, fieldErrors } = validation;
    const getFieldError = (field) => showValidationErrors ? fieldErrors[field] : undefined;
    const getItemError = (clientKey, field) => showValidationErrors ? fieldErrors.items[clientKey]?.[field] : undefined;
    const minDate = todayLocalIso();

    // Remove stale server errors when the user edits; inline validation stays visible
    // after the first Save click and clears itself as fields are corrected.
    const clearSaveFeedback = () => setSaveError("");
    const focusFirstInvalid = () => {
        window.requestAnimationFrame(() => {
            document.querySelector('[data-stock-transfer-form] [aria-invalid="true"]')?.focus();
        });
    };
    const setField = (name, value) => {
        setForm((previous) => ({ ...previous, [name]: value }));
        clearSaveFeedback();
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
        clearSaveFeedback();
    };
    const changeRow = (key, field, value) => {
        setForm((previous) => ({
            ...previous,
            items: previous.items.map((row) => row.clientKey === key
                ? (field === "productId" ? { ...row, productId: value, requestedQuantity: value ? "1" : "" } : { ...row, [field]: value })
                : row),
        }));
        clearSaveFeedback();
    };
    const addRow = () => {
        setForm((previous) => ({ ...previous, items: [...previous.items, blankTransferRow(++nextRowId.current)] }));
        clearSaveFeedback();
    };
    const removeRow = (key) => {
        setForm((previous) => ({ ...previous, items: previous.items.filter((row) => row.clientKey !== key) }));
        clearSaveFeedback();
    };

    const handleSaveDraft = async () => {
        if (saving || uncertainSave || loading || loadingExisting || loadingProducts
            || loadError || existingError || productsError) return;
        setShowValidationErrors(true);
        setSaveError("");
        if (errors.length > 0) {
            focusFirstInvalid();
            return;
        }
        // Still re-check current stock before POST/PUT. Backend remains authoritative.
        setSaving(true);
        let latestProducts;
        try {
            latestProducts = await getStockTransferSourceProducts(form.fromWarehouseId);
            if (!Array.isArray(latestProducts)) throw new Error("Invalid inventory data.");
        } catch (error) {
            setSaveError(stockTransferErrorMessage(error, "Unable to verify available stock before saving."));
            setSaving(false);
            return;
        }
        const freshOptions = productsWithCurrentReservation(latestProducts, originalDetail);
        const freshProblems = validateStockTransferDraftDetailed(form, sources, destinations, freshOptions).errors;
        if (freshProblems.length) {
            setProducts(latestProducts);
            setSaveError("Available stock changed. Correct the highlighted fields before saving.");
            focusFirstInvalid();
            setSaving(false);
            return;
        }
        if (isEditMode) {
            // Avoid overwriting a transfer that another session has modified.
            try {
                const latest = await getStockTransferDetail(transferId);
                if (!canEditStockTransfer(latest?.status)
                    || latest.status !== originalDetail?.status
                    || Number(latest.id) !== Number(transferId)) {
                    setSaveError("The transfer status has changed, or warehouse processing has started. Return to the details and refresh.");
                    setSaving(false);
                    return;
                }
                if (originalDetail?.updatedAt && latest.updatedAt !== originalDetail.updatedAt) {
                    setSaveError("This transfer was updated elsewhere. Reload before editing to avoid overwriting changes.");
                    setSaving(false);
                    return;
                }
            } catch (error) {
                setSaveError(stockTransferErrorMessage(error, "Unable to verify the latest transfer status."));
                setSaving(false);
                return;
            }
        }
        try {
            const saved = isEditMode
                ? await updateStockTransfer(transferId, toUpdateStockTransferPayload(form))
                : await createStockTransferDraft(toStockTransferPayload(form));
            const expectedId = isEditMode ? Number(transferId) : Number(saved?.id);
            const expectedStatus = isEditMode ? originalDetail?.status : "DRAFT";
            if (!Number.isSafeInteger(Number(saved?.id)) || Number(saved.id) !== expectedId || saved.status !== expectedStatus) {
                // A successful POST/PUT may have changed data despite an unexpected response.
                setUncertainSave(true);
                setSaveError("Unable to confirm the transfer was saved. Check the transfer list before retrying.");
                return;
            }
            navigate(`${ROOT}/${saved.id}`, { replace: true });
        } catch (error) {
            if (!error?.response) {
                setUncertainSave(true);
                setSaveError("Connection lost while saving. The changes may have been applied. DO NOT retry before checking the transfer details.");
            } else {
                setSaveError(stockTransferErrorMessage(error, "Unable to save changes. Please try again."));
            }
        } finally {
            setSaving(false);
        }
    };

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-6" data-stock-transfer-form>
                <PageHeader
                    title={isEditMode ? `Edit Stock Transfer ${originalDetail?.transferCode || `#${transferId}`}` : "Create Stock Transfer"}
                    description={isEditMode
                        ? "Edit the transfer before warehouse picking begins."
                        : "Transfer stock from a main warehouse to a store or between stores."}
                    actions={<Button type="button" variant="outline" onClick={() => navigate(isEditMode ? `${ROOT}/${transferId}` : "/business/dashboard")}>← {isEditMode ? "Details" : "Dashboard"}</Button>}
                />
                {loadError && <div role="alert"><Alert type="danger">{loadError} <Button type="button" variant="outline" size="sm" onClick={() => setRefresh((count) => count + 1)}>Retry</Button></Alert></div>}
                {productsError && <div role="alert"><Alert type="danger">{productsError}</Alert></div>}
                {existingError && <div role="alert"><Alert type="danger">{existingError}</Alert></div>}

                {(loading || loadingExisting) ? <Card className="flex items-center gap-3"><Spinner /> Loading transfer and reference data...</Card> : !loadError && !existingError && (
                    <>
                        <Card className="space-y-5">
                            <h2 className="text-lg font-semibold text-slate-900">Source and Destination</h2>
                            <div className="grid gap-4 md:grid-cols-2">
                                <Select label="Source Warehouse / Store *" error={getFieldError("fromWarehouseId")} aria-invalid={!!getFieldError("fromWarehouseId")} disabled={saving || isEditMode} value={form.fromWarehouseId} options={sourceOptions} onChange={(event) => changeSource(event.target.value)} />
                                <Select label="Destination Store *" error={getFieldError("toWarehouseId")} aria-invalid={!!getFieldError("toWarehouseId")} disabled={saving || !canChangeDestination} value={form.toWarehouseId} options={destinationOptions} onChange={(event) => setField("toWarehouseId", event.target.value)} />
                            </div>
                            {source && <p className="text-sm text-slate-500">Source: {source.address || source.name} · Type: {source.type}</p>}
                            {destination && <p className="text-sm text-slate-500">Destination: {destination.address || destination.name}</p>}
                            <div className="grid gap-4 md:grid-cols-2">
                                <Input label="Expected Ship Date *" error={getFieldError("expectedShipmentDate")} aria-invalid={!!getFieldError("expectedShipmentDate")} disabled={saving} type="date" min={minDate} value={form.expectedShipmentDate} onChange={(event) => setField("expectedShipmentDate", event.target.value)} />
                                <Input label="Expected Receipt Date *" error={getFieldError("expectedReceiptDate")} aria-invalid={!!getFieldError("expectedReceiptDate")} disabled={saving} type="date" min={form.expectedShipmentDate || minDate} value={form.expectedReceiptDate} onChange={(event) => setField("expectedReceiptDate", event.target.value)} />
                            </div>
                            <Select label="Transfer Reason *" error={getFieldError("reasonCode")} aria-invalid={!!getFieldError("reasonCode")} disabled={saving} value={form.reasonCode} options={[{ value: "", label: "-- Select Reason --" }, ...TRANSFER_REASONS]} onChange={(event) => setField("reasonCode", event.target.value)} />
                            {form.reasonCode === "OTHER" && (
                                <TextArea disabled={saving} label="Other Reason Explanation *" error={getFieldError("reasonNote")} aria-invalid={!!getFieldError("reasonNote")} rows={2} value={form.reasonNote} onChange={(event) => setField("reasonNote", event.target.value)} />
                            )}
                            <TextArea disabled={saving} label="Notes (optional)" rows={2} value={form.notes} onChange={(event) => setField("notes", event.target.value)} />
                        </Card>

                        <Card className="space-y-4">
                            <div className="flex flex-wrap items-center justify-between gap-3">
                                <div>
                                    <h2 className="text-lg font-semibold text-slate-900">Transfer Items</h2>
                                    <p className="mt-1 text-sm text-slate-500">{editingReservedTransfer
                                        ? "Available-to-transfer quantity includes stock already reserved for this transfer. The system will validate quantities when saving."
                                        : "This is the available stock at the source, not the destination."}</p>
                                </div>
                                <Button type="button" variant="outline" disabled={saving || !form.fromWarehouseId || loadingProducts || products.length === 0} onClick={addRow}>+ Add Product</Button>
                            </div>
                            {loadingProducts && <p className="flex items-center gap-2 text-sm text-slate-500"><Spinner /> Loading available stock...</p>}
                            {getFieldError("itemList") && <p role="alert" className="text-sm font-medium text-red-600">{getFieldError("itemList")}</p>}
                            {form.fromWarehouseId && !loadingProducts && !productsError && editableProducts.length === 0 && <Alert type="warning">No available stock at the source. Check inventory or select another source.</Alert>}
                            {form.items.map((row, index) => {
                                const selectedProduct = productById.get(row.productId);
                                const otherSelected = new Set(form.items.filter((item) => item.clientKey !== row.clientKey).map((item) => item.productId));
                                const eligible = editableProducts.filter((item) => !otherSelected.has(String(item.productId)) && Number(item.availableQuantity) > 0);
                                const options = [
                                    { value: "", label: "-- Select Product --" },
                                    ...eligible.map((item) => ({
                                        value: String(item.productId),
                                        label: `${item.productCode} — ${item.productName} (${item.baseUnit || ""}) · Stock ${displayQuantity(item.availableQuantity)}`,
                                    })),
                                ];
                                const requested = Number(row.requestedQuantity);
                                const overStock = selectedProduct && row.requestedQuantity !== "" && Number.isSafeInteger(requested) && requested > Number(selectedProduct.availableQuantity);
                                return (
                                    <div key={row.clientKey} className="space-y-3 rounded-xl border border-slate-200 bg-slate-50 p-4">
                                        <div className="flex items-center justify-between gap-3">
                                            <span className="text-sm font-semibold text-slate-800">Product #{index + 1}</span>
                                            <Button type="button" size="sm" variant="outline" disabled={saving || form.items.length === 1} onClick={() => removeRow(row.clientKey)}>Remove Row</Button>
                                        </div>
                                        <div className="grid gap-4 md:grid-cols-3">
                                            <Select label="Product *" error={getItemError(row.clientKey, "productId")} aria-invalid={!!getItemError(row.clientKey, "productId")} value={row.productId} disabled={saving || !form.fromWarehouseId || loadingProducts} options={options} onChange={(event) => changeRow(row.clientKey, "productId", event.target.value)} />
                                            <Input label="Requested Quantity *" error={getItemError(row.clientKey, "requestedQuantity")} aria-invalid={!!getItemError(row.clientKey, "requestedQuantity")} type="number" min="1" step="1" max={selectedProduct?.availableQuantity ?? undefined} disabled={saving || !selectedProduct || loadingProducts} value={row.requestedQuantity} onChange={(event) => changeRow(row.clientKey, "requestedQuantity", event.target.value)} />
                                            <div className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm">
                                                <span className="block text-slate-500">{editingReservedTransfer ? "Available after accounting for this transfer's reservations (estimate)" : "Available Stock at Source"}</span>
                                                <strong className="mt-1 block text-xl text-slate-900">{selectedProduct ? displayQuantity(selectedProduct.availableQuantity) : "—"}</strong>
                                            </div>
                                        </div>
                                        {overStock && !showValidationErrors && <p role="alert" className="text-sm font-semibold text-red-600">Request {displayQuantity(requested)} exceeds available stock {displayQuantity(selectedProduct.availableQuantity)}.</p>}
                                        {selectedProduct && <p className="text-xs text-slate-500">Code: {selectedProduct.productCode} · Barcode: {selectedProduct.barcode || "—"} · Unit: {selectedProduct.baseUnit || "—"}</p>}
                                    </div>
                                );
                            })}
                            <div className="flex justify-end border-t border-slate-200 pt-4 text-right">
                                <div>
                                    <p className="text-sm text-slate-500">Total Requested Quantity</p>
                                    <strong className="text-2xl text-slate-900">{displayQuantity(totalQuantity)}</strong>
                                </div>
                            </div>
                        </Card>

                        {saveError && <div role="alert"><Alert type="danger">{saveError}</Alert></div>}
                        <div className="flex flex-wrap justify-end gap-3">
                            <Button type="button" variant="outline" disabled={saving || uncertainSave} onClick={() => {
                                const resetForm = isEditMode && originalDetail
                                    ? stockTransferFormFromDetail(originalDetail)
                                    : blankStockTransferForm();
                                setForm(resetForm);
                                nextRowId.current = resetForm.items.length;
                                setShowValidationErrors(false);
                                clearSaveFeedback();
                            }}>{isEditMode ? "Restore Data" : "Reset Form"}</Button>
                            <Button type="button" disabled={saving || loadingProducts || !!productsError || uncertainSave} onClick={handleSaveDraft}>
                                {saving ? "Saving..." : (isEditMode ? "Save Transfer Changes" : "Save Draft Transfer")}
                            </Button>
                        </div>
                    </>
                )}
            </div>
        </MainLayout>
    );
}

