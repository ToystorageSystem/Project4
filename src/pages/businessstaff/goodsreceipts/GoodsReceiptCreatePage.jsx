import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
    createMainWarehouseGoodsReceipt,
    getActiveProductsForGoodsReceipt,
    getGoodsReceiptMainWarehouseOptions,
    getGoodsReceiptSupplierOptions,
    goodsReceiptErrorMessage,
} from "../../../api/business/goodsReceipts";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Checkbox from "../../../components/ui/Checkbox";
import Input from "../../../components/ui/Input";
import Modal from "../../../components/ui/Modal";
import Select from "../../../components/ui/Select";
import Spinner from "../../../components/ui/Spinner";
import TextArea from "../../../components/ui/TextArea";
import { formatReceiptMoney, receiptText as T } from "./receiptText";
import { newRow, normalizePrice, isPositivePrice, calculateLineTotal, validateForm, hasFormErrors } from "./receiptValidation";

const ROOT = "/business-staff/goods-receipts";
export default function GoodsReceiptCreatePage() {
    const navigate = useNavigate();
    const nextRowKey = useRef(2);
    const postInFlight = useRef(false);
    const [reload, setReload] = useState(0);
    const [loading, setLoading] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const [suppliers, setSuppliers] = useState([]);
    const [warehouses, setWarehouses] = useState([]);
    const [products, setProducts] = useState([]);
    const [nextProductPage, setNextProductPage] = useState(1);
    const [hasMoreProducts, setHasMoreProducts] = useState(false);
    const [loadError, setLoadError] = useState("");
    const [loadMoreError, setLoadMoreError] = useState("");
    const [form, setForm] = useState({ supplierId: "", warehouseId: "", note: "", items: [newRow(1)] });
    const [errors, setErrors] = useState({ items: {} });
    const [submitError, setSubmitError] = useState("");
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [confirmed, setConfirmed] = useState(false);
    const [saving, setSaving] = useState(false);
    const [savedWithoutId, setSavedWithoutId] = useState(null);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setLoadError("");
        Promise.all([
            getGoodsReceiptSupplierOptions(controller.signal),
            getGoodsReceiptMainWarehouseOptions(controller.signal),
            getActiveProductsForGoodsReceipt(0, controller.signal),
        ]).then(([supplierOptions, warehouseOptions, productPage]) => {
            if (controller.signal.aborted) return;
            if (!Array.isArray(supplierOptions) || !Array.isArray(warehouseOptions) || !Array.isArray(productPage?.content)) {
                throw new Error("Unexpected response format from Backend.");
            }
            setSuppliers(supplierOptions);
            setWarehouses(warehouseOptions);
            setProducts(productPage.content.filter((p) => p.status === "ACTIVE"));
            setNextProductPage(1);
            setHasMoreProducts(!productPage.last);
        }).catch((requestError) => {
            if (!controller.signal.aborted) setLoadError(goodsReceiptErrorMessage(requestError, T.loadError));
        }).finally(() => {
            if (!controller.signal.aborted) setLoading(false);
        });
        return () => controller.abort();
    }, [reload]);

    async function loadMoreProducts() {
        if (!hasMoreProducts || loadingMore) return;
        setLoadingMore(true);
        setLoadMoreError("");
        try {
            const page = await getActiveProductsForGoodsReceipt(nextProductPage);
            if (!Array.isArray(page?.content)) throw new Error("Unexpected product page response.");
            setProducts((current) => {
                const byId = new Map(current.map((p) => [p.id, p]));
                page.content.filter((p) => p.status === "ACTIVE").forEach((p) => byId.set(p.id, p));
                return [...byId.values()];
            });
            setNextProductPage((previous) => previous + 1);
            setHasMoreProducts(!page.last);
        } catch (error) {
            setLoadMoreError(goodsReceiptErrorMessage(error, T.loadError));
        } finally {
            setLoadingMore(false);
        }
    }

    const totalQuantity = useMemo(() => form.items.reduce((sum, item) => {
        const quantity = Number(item.expectedQuantity);
        return sum + (Number.isSafeInteger(quantity) && quantity > 0 ? quantity : 0);
    }, 0), [form.items]);

    const estimatedTotal = useMemo(() => form.items.reduce((sum, item) => sum + calculateLineTotal(item), 0), [form.items]);
    const supplierSelected = suppliers.find((s) => String(s.id) === form.supplierId);
    const warehouseSelected = warehouses.find((w) => String(w.id) === form.warehouseId);

    function setField(field, value) {
        setForm((current) => ({ ...current, [field]: value }));
        setErrors({ items: {} });
        setSubmitError("");
    }

    function changeItem(key, field, value) {
        setForm((current) => ({
            ...current,
            items: current.items.map((item) => {
                if (item.key !== key) return item;
                if (field === "productId") {
                    const product = products.find((p) => String(p.id) === value);
                    const suggestedPrice = product && isPositivePrice(product.purchasePrice) ? String(product.purchasePrice) : "";
                    return { ...item, productId: value, unitPrice: suggestedPrice };
                }
                return { ...item, [field]: value };
            }),
        }));
        setErrors({ items: {} });
        setSubmitError("");
    }

    function appendItem() {
        setForm((current) => ({ ...current, items: [...current.items, newRow(nextRowKey.current++)] }));
    }

    function deleteItem(key) {
        setForm((current) => ({
            ...current,
            items: current.items.length > 1 ? current.items.filter((item) => item.key !== key) : current.items,
        }));
        setErrors({ items: {} });
    }

    function prepareSubmit(event) {
        event.preventDefault();
        const found = validateForm(form, suppliers, warehouses, products);
        setErrors(found);
        if (hasFormErrors(found)) {
            setSubmitError(T.requiredFields);
            return;
        }
        setSubmitError("");
        setConfirmed(false);
        setConfirmOpen(true);
    }

    async function confirmCreate() {
        if (!confirmed || saving || postInFlight.current || savedWithoutId) return;
        postInFlight.current = true;
        setSaving(true);
        setSubmitError("");
        try {
            const payload = {
                supplierId: Number(form.supplierId),
                warehouseId: Number(form.warehouseId),
                note: form.note.trim() || null,
                items: form.items.map((item) => ({
                    productId: Number(item.productId),
                    expectedQuantity: Number(item.expectedQuantity),
                    unitPrice: normalizePrice(item.unitPrice),
                })),
            };
            const result = await createMainWarehouseGoodsReceipt(payload);
            const receiptId = Number(result?.id);
            if (Number.isSafeInteger(receiptId) && receiptId > 0) {
                navigate(`${ROOT}/${receiptId}`, { replace: true, state: { notice: T.success } });
            } else {
                // HTTP success is still a write: do not allow a second POST just because the response is incomplete.
                setSavedWithoutId(result || {});
                setConfirmOpen(false);
            }
        } catch (error) {
            setSubmitError(`${goodsReceiptErrorMessage(error, T.loadError)} ${T.timeoutWarning}`);
            setConfirmOpen(false);
        } finally {
            postInFlight.current = false;
            setSaving(false);
        }
    }

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader title={T.title} description={T.intro} />
                <div className="space-y-2">
                    <Alert type="info">{T.draftNote}</Alert>
                    <Alert type="warning">{T.autoPoNote}</Alert>
                </div>
                {savedWithoutId && (
                    <Alert type="success">{T.success} {T.receiptCode}: {savedWithoutId.receiptCode || "-"}. {T.savedWithoutId}</Alert>
                )}
                {loadError && (
                    <div role="alert" className="space-y-2"><Alert type="danger">{loadError}</Alert><Button variant="outline" onClick={() => setReload((n) => n + 1)}>{T.retry}</Button></div>
                )}
                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-6 text-sm text-slate-600"><Spinner />{T.loading}</div>
                ) : !loadError && (suppliers.length === 0 || warehouses.length === 0 || products.length === 0) ? (
                    <Alert type="warning">{[!suppliers.length && T.noSuppliers, !warehouses.length && T.noWarehouses, !products.length && T.noProducts].filter(Boolean).join(" ")}</Alert>
                ) : !loadError ? (
                    <form onSubmit={prepareSubmit} className="space-y-6" noValidate>
                        {submitError && <div role="alert"><Alert type="danger">{submitError}</Alert></div>}
                        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
                            <h2 className="mb-5 text-lg font-semibold text-slate-900">{T.supplier} / {T.warehouse}</h2>
                            <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
                                <Select label={T.supplier} value={form.supplierId} error={errors.supplierId}
                                    onChange={(e) => setField("supplierId", e.target.value)}
                                    options={[{ value: "", label: T.chooseSupplier }, ...suppliers.map((s) => ({ value: String(s.id), label: `${s.code} - ${s.name}` }))]} />
                                <Select label={T.warehouse} value={form.warehouseId} error={errors.warehouseId}
                                    onChange={(e) => setField("warehouseId", e.target.value)}
                                    options={[{ value: "", label: T.chooseWarehouse }, ...warehouses.map((w) => ({ value: String(w.id), label: `${w.code} - ${w.name}` }))]} />
                            </div>
                        </div>

                        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
                            <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
                                <h2 className="text-lg font-semibold text-slate-900">{T.items}</h2>
                                <Button type="button" size="sm" variant="outline" onClick={appendItem}>{T.addLine}</Button>
                            </div>
                            <p className="mb-4 text-sm text-slate-500">{T.productNote}</p>
                            <div className="space-y-4">
                                {form.items.map((item, index) => {
                                    const itemErrors = errors.items?.[item.key] || {};
                                    const available = products.filter((p) => !form.items.some((other) => other.key !== item.key && other.productId === String(p.id)));
                                    return (
                                        <div key={item.key} className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                                            <div className="mb-3 flex items-center justify-between gap-2">
                                                <h3 className="text-sm font-semibold text-slate-700">#{index + 1}</h3>
                                                <Button type="button" variant="ghost" size="sm" disabled={form.items.length === 1} onClick={() => deleteItem(item.key)}>{T.removeLine}</Button>
                                            </div>
                                            <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
                                                <div className="lg:col-span-5">
                                                    <Select label={T.product} value={item.productId} error={itemErrors.productId}
                                                        onChange={(e) => changeItem(item.key, "productId", e.target.value)}
                                                        options={[{ value: "", label: T.chooseProduct }, ...available.map((p) => ({ value: String(p.id), label: `${p.productCode} - ${p.name}` }))]} />
                                                </div>
                                                <div className="lg:col-span-3">
                                                    <Input label={T.quantity} type="number" min="1" step="1" value={item.expectedQuantity} error={itemErrors.expectedQuantity}
                                                        onChange={(e) => changeItem(item.key, "expectedQuantity", e.target.value)} />
                                                </div>
                                                <div className="lg:col-span-4">
                                                    <Input label={T.price} type="text" inputMode="decimal" value={item.unitPrice} error={itemErrors.unitPrice}
                                                        placeholder="120000" onChange={(e) => changeItem(item.key, "unitPrice", e.target.value)} />
                                                </div>
                                            </div>
                                            <p className="mt-3 text-right text-sm font-semibold text-slate-700">{T.subtotal}: {formatReceiptMoney(calculateLineTotal(item))}</p>
                                        </div>
                                    );
                                })}
                            </div>
                            {loadMoreError && <div className="mt-3" role="alert"><Alert type="danger">{loadMoreError}</Alert></div>}
                            {hasMoreProducts && (
                                <div className="mt-4"><Button type="button" variant="outline" size="sm" disabled={loadingMore} onClick={loadMoreProducts}>
                                    {loadingMore ? T.loadingMore : T.loadMore}
                                </Button></div>
                            )}
                        </div>

                        <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
                            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm lg:col-span-2">
                                <TextArea rows={4} maxLength={2000} label={T.note} placeholder={T.notePlaceholder}
                                    value={form.note} error={errors.note} onChange={(e) => setField("note", e.target.value)} />
                                <p className="mt-2 text-xs text-slate-500">{form.note.length}/2000 - {T.noteLimit}</p>
                            </div>
                            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                                <div className="space-y-3 text-sm">
                                    <div className="flex justify-between"><span className="text-slate-500">{T.totalLines}</span><strong>{form.items.length}</strong></div>
                                    <div className="flex justify-between"><span className="text-slate-500">{T.totalQuantity}</span><strong>{totalQuantity}</strong></div>
                                    <div className="border-t border-slate-200 pt-3"><span className="block text-slate-500">{T.total}</span>
                                        <strong className="mt-2 block text-2xl text-slate-900">{formatReceiptMoney(estimatedTotal)}</strong></div>
                                </div>
                            </div>
                        </div>
                        <div className="flex justify-end"><Button type="submit" size="lg" disabled={saving || Boolean(savedWithoutId)}>{T.create}</Button></div>
                    </form>
                ) : null}
            </div>
            <Modal open={confirmOpen} title={T.confirmTitle} onClose={() => { if (!saving) setConfirmOpen(false); }}
                footer={<>
                    <Button type="button" variant="ghost" disabled={saving} onClick={() => setConfirmOpen(false)}>{T.cancel}</Button>
                    <Button type="button" disabled={!confirmed || saving} onClick={confirmCreate}>{saving ? T.creating : T.create}</Button>
                </>}>
                <div className="space-y-3 text-sm text-slate-700">
                    <Alert type="warning">{T.confirmIntro}</Alert>
                    <p><strong>{T.supplier}:</strong> {supplierSelected?.name || "-"}</p>
                    <p><strong>{T.warehouse}:</strong> {warehouseSelected?.name || "-"}</p>
                    <p><strong>{T.totalLines}:</strong> {form.items.length}</p>
                    <p><strong>{T.total}:</strong> {formatReceiptMoney(estimatedTotal)}</p>
                    <Checkbox checked={confirmed} onChange={(e) => setConfirmed(e.target.checked)} label={T.confirmCheck} />
                </div>
            </Modal>
        </MainLayout>
    );
}
