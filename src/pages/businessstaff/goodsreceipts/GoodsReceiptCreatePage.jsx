import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
    createMainWarehouseGoodsReceipt,
    goodsReceiptErrorMessage,
} from "../../../api/business/goodsReceipts";
import { getPurchaseOrderDetail, listPurchaseOrders } from "../../../api/business/purchaseOrders";
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
import { formatReceiptMoney, receiptText as T } from "./receiptText";

const ROOT = "/business-staff/goods-receipts";
const AVAILABLE_STATES = new Set(["ORDERED", "PARTIALLY_RECEIVED"]);
const MAX_QUANTITY = 2147483647;

function remainingQuantity(item) {
    return Math.max(0, Number(item?.orderedQuantity ?? 0) - Number(item?.receivedQuantity ?? 0));
}

function quantityValid(text, remaining) {
    return /^(0|[1-9]\d*)$/.test(String(text))
        && Number(text) <= remaining
        && Number(text) <= MAX_QUANTITY;
}

export default function GoodsReceiptCreatePage() {
    const navigate = useNavigate();
    const postInFlight = useRef(false);
    const [reload, setReload] = useState(0);
    const [loading, setLoading] = useState(true);
    const [orders, setOrders] = useState([]);
    const [loadError, setLoadError] = useState("");
    const [orderId, setOrderId] = useState("");
    const [order, setOrder] = useState(null);
    const [loadingOrder, setLoadingOrder] = useState(false);
    const [orderError, setOrderError] = useState("");
    const [quantities, setQuantities] = useState({});
    const [submitError, setSubmitError] = useState("");
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [confirmed, setConfirmed] = useState(false);
    const [saving, setSaving] = useState(false);
    const [savedWithoutId, setSavedWithoutId] = useState(null);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setLoadError("");
        listPurchaseOrders({}, controller.signal).then((data) => {
            if (controller.signal.aborted) return;
            if (!Array.isArray(data)) throw new Error("Danh sách đơn mua hàng chưa hợp lệ.");
            setOrders(data.filter((po) => AVAILABLE_STATES.has(po.status)));
        }).catch((error) => {
            if (!controller.signal.aborted) setLoadError(goodsReceiptErrorMessage(error, T.loadError));
        }).finally(() => {
            if (!controller.signal.aborted) setLoading(false);
        });
        return () => controller.abort();
    }, [reload]);

    useEffect(() => {
        const controller = new AbortController();
        setOrder(null);
        setOrderError("");
        setLoadingOrder(false);
        setQuantities({});
        setSubmitError("");
        if (!orderId) return () => controller.abort();

        setLoadingOrder(true);
        getPurchaseOrderDetail(orderId, controller.signal).then((data) => {
            if (controller.signal.aborted) return;
            if (!data || !AVAILABLE_STATES.has(data.status) || !Array.isArray(data.items)) {
                throw new Error(T.invalidOrder);
            }
            setOrder(data);
            setQuantities(Object.fromEntries(data.items.map((item) => [
                String(item.productId), String(remainingQuantity(item)),
            ])));
        }).catch((error) => {
            if (!controller.signal.aborted) setOrderError(
                error?.response ? goodsReceiptErrorMessage(error, T.loadError) : (error?.message || T.loadError)
            );
        }).finally(() => {
            if (!controller.signal.aborted) setLoadingOrder(false);
        });
        return () => controller.abort();
    }, [orderId]);

    const items = order?.items ?? [];
    const availableItems = useMemo(() => items.filter((item) => remainingQuantity(item) > 0), [items]);
    const fieldsValid = availableItems.every((item) =>
        quantityValid(quantities[String(item.productId)] ?? "", remainingQuantity(item)));
    const selectedItems = availableItems.filter((item) => {
        const count = quantities[String(item.productId)] ?? "";
        return quantityValid(count, remainingQuantity(item)) && Number(count) > 0;
    });
    const estimatedTotal = selectedItems.reduce((total, item) =>
        total + Number(quantities[String(item.productId)]) * Number(item.unitPrice || 0), 0);
    const totalQuantity = selectedItems.reduce((total, item) =>
        total + Number(quantities[String(item.productId)]), 0);

    function prepareSubmit(event) {
        event.preventDefault();
        if (!order || !AVAILABLE_STATES.has(order.status) || !fieldsValid || !selectedItems.length) {
            setSubmitError(T.invalidQuantity);
            return;
        }
        setSubmitError("");
        setConfirmed(false);
        setConfirmOpen(true);
    }

    async function confirmCreate() {
        if (!confirmed || saving || postInFlight.current || savedWithoutId || !order) return;
        postInFlight.current = true;
        setSaving(true);
        setSubmitError("");
        try {
            const payload = {
                purchaseOrderId: Number(order.id),
                items: selectedItems.map((item) => ({
                    productId: Number(item.productId),
                    expectedQuantity: Number(quantities[String(item.productId)]),
                })),
            };
            const result = await createMainWarehouseGoodsReceipt(payload);
            const receiptId = Number(result?.id);
            if (Number.isSafeInteger(receiptId) && receiptId > 0) {
                navigate(`${ROOT}/${receiptId}`, { replace: true, state: { notice: T.success } });
            } else {
                // Server may have already saved: never retry blindly.
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
                <PageHeader title={T.title} description={T.intro}
                    actions={<Button type="button" variant="outline" onClick={() => navigate(`${ROOT}/list`)}>
                        ← Theo dõi phiếu nhập kho
                    </Button>} />
                <Alert type="info">{T.draftNote}</Alert>
                <Alert type="warning">{T.fromPurchaseOrderNote}</Alert>
                {savedWithoutId && (
                    <Alert type="success">{T.success} {T.receiptCode}: {savedWithoutId.receiptCode || "-"}. {T.savedWithoutId}</Alert>
                )}
                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-6 text-sm text-slate-600"><Spinner />{T.loading}</div>
                ) : loadError ? (
                    <div role="alert" className="space-y-2"><Alert type="danger">{loadError}</Alert>
                        <Button variant="outline" onClick={() => setReload((n) => n + 1)}>{T.retry}</Button>
                    </div>
                ) : !orders.length ? (
                    <Alert type="warning">{T.noPurchaseOrders}</Alert>
                ) : (
                    <form onSubmit={prepareSubmit} className="space-y-6" noValidate>
                        {submitError && <div role="alert"><Alert type="danger">{submitError}</Alert></div>}
                        <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
                            <h2 className="mb-5 text-lg font-semibold text-slate-900">{T.selectPurchaseOrder}</h2>
                            <Select label={T.purchaseOrderRequired} value={orderId}
                                onChange={(event) => setOrderId(event.target.value)}
                                options={[{ value: "", label: T.choosePurchaseOrder }, ...orders.map((po) => ({
                                    value: String(po.id),
                                    label: `${po.orderCode} - ${po.supplierName} - ${po.warehouseName} (${po.status})`,
                                }))]} />
                            {loadingOrder && <div className="mt-3 flex items-center gap-2 text-sm text-slate-600"><Spinner />{T.loading}</div>}
                            {orderError && <div className="mt-3" role="alert"><Alert type="danger">{orderError}</Alert></div>}
                            {order && (
                                <div className="mt-5 grid grid-cols-1 gap-4 text-sm sm:grid-cols-3">
                                    <div className="rounded-xl bg-slate-50 p-4"><span className="block text-slate-500">{T.supplier}</span><strong>{order.supplierName}</strong></div>
                                    <div className="rounded-xl bg-slate-50 p-4"><span className="block text-slate-500">{T.warehouse}</span><strong>{order.warehouseName}</strong></div>
                                    <div className="rounded-xl bg-slate-50 p-4"><span className="block text-slate-500">{T.purchaseOrderStatus}</span><strong>{order.status}</strong></div>
                                    {order.note && <div className="rounded-xl bg-slate-50 p-4 sm:col-span-3"><span className="block text-slate-500">{T.purchaseOrderNote}</span>{order.note}</div>}
                                </div>
                            )}
                        </section>

                        {order && <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
                            <h2 className="mb-2 text-lg font-semibold text-slate-900">{T.items}</h2>
                            <p className="mb-4 text-sm text-slate-500">{T.productNote}</p>
                            {!availableItems.length && <Alert type="warning">{T.noRemainingProducts}</Alert>}
                            <div className="space-y-4">
                                {availableItems.map((item) => {
                                    const key = String(item.productId);
                                    const remaining = remainingQuantity(item);
                                    const value = quantities[key] ?? "";
                                    const error = quantityValid(value, remaining) ? "" : T.invalidQuantity;
                                    return <div key={item.id ?? key} className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                                        <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
                                            <div className="lg:col-span-5">
                                                <span className="block text-sm font-semibold text-slate-800">{item.productCode} - {item.productName}</span>
                                                <span className="mt-1 block text-xs text-slate-500">{T.ordered}: {item.orderedQuantity} · {T.received}: {item.receivedQuantity || 0} · {T.remaining}: {remaining}</span>
                                            </div>
                                            <div className="lg:col-span-3">
                                                <Input type="number" min="0" max={remaining} step="1" label={T.quantityInput}
                                                    value={value} error={error} onChange={(event) => {
                                                        setQuantities((current) => ({ ...current, [key]: event.target.value }));
                                                        setSubmitError("");
                                                    }} />
                                            </div>
                                            <div className="lg:col-span-4">
                                                <span className="block text-sm text-slate-600">{T.price}</span>
                                                <strong className="mt-2 block">{formatReceiptMoney(item.unitPrice)}</strong>
                                                <span className="block text-xs text-slate-500">{T.subtotal}: {formatReceiptMoney(Number(value || 0) * Number(item.unitPrice || 0))}</span>
                                            </div>
                                        </div>
                                    </div>;
                                })}
                            </div>
                        </section>}

                        {order && <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
                            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3 text-sm">
                                <p>{T.totalLines}: <strong>{selectedItems.length}</strong></p>
                                <p>{T.totalQuantity}: <strong>{totalQuantity}</strong></p>
                                <p>{T.total}: <strong>{formatReceiptMoney(estimatedTotal)}</strong></p>
                            </div>
                            <div className="mt-5 flex justify-end"><Button type="submit" size="lg"
                                disabled={saving || Boolean(savedWithoutId) || !fieldsValid || !selectedItems.length}>{T.create}</Button></div>
                        </section>}
                    </form>
                )}
            </div>
            <Modal open={confirmOpen} title={T.confirmTitle} onClose={() => { if (!saving) setConfirmOpen(false); }}
                footer={<>
                    <Button type="button" variant="ghost" disabled={saving} onClick={() => setConfirmOpen(false)}>{T.cancel}</Button>
                    <Button type="button" disabled={!confirmed || saving} onClick={confirmCreate}>{saving ? T.creating : T.create}</Button>
                </>}>
                <div className="space-y-3 text-sm text-slate-700">
                    <Alert type="warning">{T.confirmIntro}</Alert>
                    <p><strong>{T.purchaseOrder}:</strong> {order?.orderCode || "-"}</p>
                    <p><strong>{T.totalLines}:</strong> {selectedItems.length}</p>
                    <p><strong>{T.total}:</strong> {formatReceiptMoney(estimatedTotal)}</p>
                    <Checkbox checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} label={T.confirmCheck} />
                </div>
            </Modal>
        </MainLayout>
    );
}
