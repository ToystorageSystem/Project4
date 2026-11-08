
import { useEffect, useState } from "react";

import {
    getSupplierFeedback,
    recordSupplierFeedback,
    supplierFeedbackErrorMessage,
} from "../../../api/business/supplierFeedback";


import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import Modal from "../../../components/ui/Modal";
import Input from "../../../components/ui/Input";
import Select from "../../../components/ui/Select";
import Spinner from "../../../components/ui/Spinner";
import TextArea from "../../../components/ui/TextArea";
import { todayVietnamIso } from "./purchaseOrderDraft";

// Issue #16 - Supplier Feedback.
// Giai doan 1: Nhap va xem truoc du lieu.
// KHONG goi PUT/POST, KHONG ghi database.


const SUPPLIER_FEEDBACK_WRITE_ENABLED =
    import.meta.env.VITE_ENABLE_SUPPLIER_FEEDBACK_WRITE === "true";
const ORDER_STATUSES = [
    { value: "", label: "-- Select Status --" },
    { value: "AVAILABLE", label: "Fully Available" },
    { value: "PARTIALLY_AVAILABLE", label: "Partially Available" },
    { value: "OUT_OF_STOCK", label: "Out of Stock" },
    { value: "DISCONTINUED", label: "Supply Discontinued" },
    { value: "PRICE_CHANGED", label: "Price Change" },
    { value: "DELAYED", label: "Delayed Delivery" },
    { value: "DECLINED", label: "Order Rejected" },
];

const ITEM_STATUSES = [
    { value: "PENDING", label: "Pending" },
    { value: "AVAILABLE", label: "Available" },
    { value: "PARTIALLY_AVAILABLE", label: "Partially Available" },
    { value: "OUT_OF_STOCK", label: "Out of Stock" },
    { value: "DISCONTINUED", label: "Supply Discontinued" },
    { value: "PRICE_CHANGED", label: "Price Change" },
];

function parseQuantity(text) {
    const value = String(text ?? "").trim();

    if (!value) return null;
    if (!/^\d+$/.test(value)) return NaN;

    const number = Number(value);
    return Number.isSafeInteger(number) ? number : NaN;
}

function parsePrice(text) {
    const value = String(text ?? "").trim();

    if (!value) return null;
    if (!/^\d+(\.\d{1,2})?$/.test(value)) return NaN;

    const number = Number(value);
    return Number.isFinite(number) ? number : NaN;
}

function isIsoDate(value) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
    const date = new Date(`${value}T00:00:00Z`);
    return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value;
}

// Tinh ngay som nhat NCC duoc phep de xuat.
function minSupplierProposedDate(status, requestedDate) {
    const today = todayVietnamIso();

    // Neu NCC giao tre, ngay moi phai sau ngay doanh nghiep yeu cau.
    if (status !== "DELAYED" || !isIsoDate(requestedDate)) {
        return today;
    }

    const date = new Date(`${requestedDate}T00:00:00Z`);
    date.setUTCDate(date.getUTCDate() + 1);

    const firstLateDate = date.toISOString().slice(0, 10);

    return firstLateDate > today ? firstLateDate : today;
}

function validateAndBuild(rows, orderStatus, orderNote, proposedDate, agreedDate, requestedDate) {
    const errors = [];

    if (proposedDate && proposedDate < todayVietnamIso()) {
    errors.push("Supplier proposed date cannot be in the past.");
    }

    if (agreedDate && agreedDate < todayVietnamIso()) {
        errors.push("Agreed delivery date cannot be in the past.");
    }

    if (
        orderStatus === "DELAYED" &&
        agreedDate &&
        requestedDate &&
        agreedDate <= requestedDate
    ) {
        errors.push(
            "For delayed deliveries, the agreed delivery date must be later than the requested delivery date."
        );
    }

    if (!orderStatus) {
        errors.push("Select a supplier response status.");
    }
    if (proposedDate && !isIsoDate(proposedDate)) errors.push("Invalid supplier proposed date.");
    if (agreedDate && !isIsoDate(agreedDate)) errors.push("Invalid agreed delivery date.");
    if (orderStatus === "DELAYED") {
        if (!proposedDate) errors.push("Supplier proposed date is required for delayed delivery.");
        else if (isIsoDate(proposedDate) && requestedDate && proposedDate <= requestedDate) {
            errors.push("Supplier proposed date must be after the requested delivery date.");
        }
    }
    if (agreedDate && ["PENDING_RESPONSE", "DECLINED"].includes(orderStatus)) {
        errors.push("An agreed delivery date cannot be recorded for pending or declined feedback.");
    }

    const items = rows.map((row) => {
        const quantity = parseQuantity(row.confirmedQuantity);
        const price = parsePrice(row.confirmedUnitPrice);

        const ordered = Number(row.orderedQuantity);
        const received = Number(row.receivedQuantity);
        const originalPrice = Number(row.originalUnitPrice);

        const name = row.productName || `Item ${row.id}`;

        if (
            quantity !== null &&
            (!Number.isSafeInteger(quantity) ||
                quantity < received ||
                quantity > ordered)
        ) {
            errors.push(
                `${name}: confirmed quantity must be between ${received} and ${ordered}.`
            );
        }

        if (
            price !== null &&
            (!Number.isFinite(price) || price <= 0)
        ) {
            errors.push(
                `${name}: confirmed price must be greater than zero, with at most two decimal places.`
            );
        }

        if (
            row.status === "AVAILABLE" &&
            quantity !== ordered
        ) {
            errors.push(
                `${name}: Available status requires confirmation of the full ordered quantity ${ordered}.`
            );
        }

        if (
            row.status === "PARTIALLY_AVAILABLE" &&
            (quantity === null || quantity <= 0 ||
                quantity >= ordered)
        ) {
            errors.push(
                `${name}: Partially Available status requires a confirmed quantity greater than zero and less than the ordered quantity.`
            );
        }

        if (
            ["OUT_OF_STOCK", "DISCONTINUED"].includes(row.status) &&
            quantity !== received
        ) {
            errors.push(
                `${name}: confirmed quantity must match the quantity already received (${received}).`
            );
        }

        if (
            row.status === "PRICE_CHANGED" &&
            (price === null || !Number.isFinite(price) ||
                price === originalPrice)
        ) {
            errors.push(
                `${name}: for a price change, the new price must differ from the current price.`
            );
        }

        return {
            purchaseOrderItemId: row.id,
            supplierResponseStatus: row.status,
            confirmedQuantity: quantity,
            confirmedUnitPrice: price,
            supplierResponseNote:
                row.note.trim(),
        };
    });

    const statuses = items.map(
        (item) => item.supplierResponseStatus
    );

    // Kiem tra phan hoi toan don: PARTIALLY_AVAILABLE
    if (orderStatus === "PARTIALLY_AVAILABLE") {
        const hasShortage = items.some((item) =>
            [
                "PARTIALLY_AVAILABLE",
                "OUT_OF_STOCK",
                "DISCONTINUED",
            ].includes(item.supplierResponseStatus)
        );

        const hasSupply = items.some((item) =>
            [
                "AVAILABLE",
                "PARTIALLY_AVAILABLE",
                "PRICE_CHANGED",
            ].includes(item.supplierResponseStatus) &&
            Number.isSafeInteger(item.confirmedQuantity) &&
            item.confirmedQuantity > 0
        );

        if (!hasShortage || !hasSupply) {
            errors.push(
                "Partially Available requires at least one item with a shortage, out-of-stock, or supply-discontinued status and at least one item with a positive confirmed quantity."
            );
        }
    }


    if (
        orderStatus === "PENDING_RESPONSE" &&
        statuses.some((status) => status !== "PENDING")
    ) {
        errors.push(
            "Pending Response requires all items to be Pending."
        );
    }

    if (
        orderStatus === "AVAILABLE" &&
        statuses.some((status) => status !== "AVAILABLE")
    ) {
        errors.push(
            "Fully Available requires all items to be Available."
        );
    }

    if (
        orderStatus === "OUT_OF_STOCK" &&
        statuses.some((status) => status !== "OUT_OF_STOCK")
    ) {
        errors.push(
            "Out of Stock requires all items to be Out of Stock."
        );
    }

    if (
        orderStatus === "DISCONTINUED" &&
        !statuses.includes("DISCONTINUED")
    ) {
        errors.push(
            "Supply Discontinued requires at least one discontinued item."
        );
    }

    if (
        orderStatus === "PRICE_CHANGED" &&
        !statuses.includes("PRICE_CHANGED")
    ) {
        errors.push(
            "Price Change requires at least one item with a changed price."
        );
    }

    return {
        errors,
        payload: {
            supplierResponseStatus: orderStatus,
            supplierResponseNote: orderNote.trim(),
            supplierProposedDeliveryDate: proposedDate || null,
            agreedDeliveryDate: agreedDate || null,
            items,
        },
    };
}

export default function SupplierFeedbackPanel({ purchaseOrder, onSaved }) {
    const [rows, setRows] = useState([]);
    const [orderStatus, setOrderStatus] = useState("");
    const [orderNote, setOrderNote] = useState("");
    const [proposedDate, setProposedDate] = useState("");
    const [agreedDate, setAgreedDate] = useState("");

    const [loading, setLoading] = useState(false);
    const [loadError, setLoadError] = useState("");
    const [errors, setErrors] = useState([]);
    const [preview, setPreview] = useState(null);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [saving, setSaving] = useState(false);
    const [saveError, setSaveError] = useState("");
    const [saveSuccess, setSaveSuccess] = useState("");


    const [reload, setReload] = useState(0);

    const eligible = [
        "ORDERED",
        "PARTIALLY_RECEIVED",
    ].includes(purchaseOrder?.status);

    useEffect(() => {
        const controller = new AbortController();

        setRows([]);
        setOrderStatus("");
        setOrderNote("");
        setProposedDate("");
        setAgreedDate("");
        setErrors([]);
        setPreview(null);
        setConfirmOpen(false);
        setLoadError("");

        if (!eligible) {
            setLoading(false);
            return () => controller.abort();
        }

        setLoading(true);


        getSupplierFeedback(
            purchaseOrder.id,
            controller.signal
        )
            .then((data) => {
                if (controller.signal.aborted) return;

                if (
                    !Array.isArray(data?.items) ||
                    Number(data.purchaseOrderId) !==
                        Number(purchaseOrder.id)
                ) {
                    throw new Error(
                        "Invalid supplier response data."
                    );
                }

                const items = purchaseOrder.items || [];

                const feedbackById = new Map(
                    data.items.map((item) => [
                        String(item.purchaseOrderItemId),
                        item,
                    ])
                );

                if (
                    !items.length ||
                    items.some(
                        (item) =>
                            !feedbackById.has(String(item.id))
                    )
                ) {
                    throw new Error(
                        "Supplier response items do not match the purchase order items."
                    );
                }

                // Khoi phuc trang thai va ghi chu toan don.
                setOrderStatus(
                    data.supplierResponseStatus === "PENDING_RESPONSE"
                        ? "" : data.supplierResponseStatus ?? ""
                );

                setOrderNote(
                    data.supplierResponseNote ?? ""
                );
                setProposedDate(data.supplierProposedDeliveryDate ?? "");
                setAgreedDate(data.agreedDeliveryDate ?? "");

                // Khoi phuc phan hoi tung san pham.
                setRows(
                    items.map((item) => {
                        const feedback = feedbackById.get(
                            String(item.id)
                        );

                        return {
                            id: item.id,
                            productName: item.productName,
                            productCode: item.productCode,
                            orderedQuantity: item.orderedQuantity,
                            receivedQuantity: item.receivedQuantity,
                            originalUnitPrice: item.unitPrice,

                            status:
                                feedback.supplierResponseStatus ||
                                "PENDING",

                            confirmedQuantity:
                                feedback.confirmedQuantity == null
                                    ? ""
                                    : String(
                                          feedback.confirmedQuantity
                                      ),

                            confirmedUnitPrice:
                                feedback.confirmedUnitPrice == null
                                    ? ""
                                    : String(
                                          feedback.confirmedUnitPrice
                                      ),

                            note:
                                feedback.supplierResponseNote ?? "",
                        };
                    })
                );
            })

            .catch((error) => {
                if (controller.signal.aborted) return;

                setLoadError(
                    error?.isAxiosError
                        ? supplierFeedbackErrorMessage(
                              error,
                              "Unable to load supplier item responses."
                          )
                        : error?.message ||
                          "Unable to load supplier item responses."
                );
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [purchaseOrder, eligible, reload]);

    function updateRow(index, field, value) {
        setRows((current) =>
            current.map((row, rowIndex) =>
                rowIndex === index
                    ? { ...row, [field]: value }
                    : row
            )
        );

        setErrors([]);
        setPreview(null);
    }


    function handleReviewSave() {
        setConfirmOpen(false);

        const result = validateAndBuild(
            rows,
            orderStatus,
            orderNote,
            proposedDate,
            agreedDate,
            purchaseOrder.expectedDeliveryDate
        );

        setErrors(result.errors);

        if (result.errors.length > 0) {
            setPreview(null);
            return;
        }

        setPreview(result.payload);
        setConfirmOpen(true);
    }

    async function handleConfirmSave() {
        // Khoa thao tac ghi neu chua duoc cho phep.
        if (
            !SUPPLIER_FEEDBACK_WRITE_ENABLED ||
            saving ||
            loading ||
            !preview
        ) {
            return;
        }

        // Kiem tra lai du lieu truoc khi gui Backend.
        const result = validateAndBuild(
            rows,
            orderStatus,
            orderNote,
            proposedDate,
            agreedDate,
            purchaseOrder.expectedDeliveryDate
        );

        if (result.errors.length > 0) {
            setErrors(result.errors);
            setPreview(null);
            setConfirmOpen(false);
            return;
        }

        // Khong gui neu du lieu da khac ban xem truoc.
        if (
            JSON.stringify(result.payload) !==
            JSON.stringify(preview)
        ) {
            setConfirmOpen(false);
            setPreview(null);
            setSaveError(
                "The data has changed. Review and confirm again."
            );
            return;
        }

        setSaving(true);
        setSaveError("");
        setSaveSuccess("");

        try {
            await recordSupplierFeedback(
                purchaseOrder.id,
                result.payload
            );

            setConfirmOpen(false);
            setPreview(null);

            setSaveSuccess(
                "Supplier response recorded successfully."
            );

            // Doc lai du lieu moi nhat tu Backend va cap nhat trang chi tiet PO.
            setReload((value) => value + 1);
            onSaved?.();
        } catch (error) {
            setSaveError(
                supplierFeedbackErrorMessage(
                    error,
                    "Unable to save supplier response."
                )
            );
        } finally {
            setSaving(false);
        }
    }



    if (!eligible) {
        return (
            <Card>
                <h2 className="text-lg font-bold text-slate-900">
                    Supplier Response
                </h2>

                <div className="mt-4">
                    <Alert type="info">

                            {saveError && (
                                <div role="alert">
                                    <Alert type="danger">
                                        {saveError}
                                    </Alert>
                                </div>
                            )}

                            {saveSuccess && (
                                <div
                                    role="status"
                                    className="rounded-xl border border-green-200 bg-green-50 p-3 text-sm text-green-700"
                                >
                                    {saveSuccess}
                                </div>
                            )}
                        Supplier responses can be recorded only for orders already submitted to the supplier or partially received.
                    </Alert>
                </div>
            </Card>
        );
    }

    return (
        <Card>
            <div className="space-y-6">
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                        <h2 className="text-lg font-bold text-slate-900">
                            Record Supplier Response
                        </h2>

                        <p className="mt-1 text-sm text-slate-500">
                            Record the supplier's response for each product on the purchase order.
                        </p>
                    </div>

                    <Button
                        type="button"
                        variant="ghost"
                        disabled={loading}
                        onClick={() => setReload((value) => value + 1)}
                    >
                        Reload
                    </Button>
                </div>



                <Alert type="info">
                    {SUPPLIER_FEEDBACK_WRITE_ENABLED
                        ? "Supplier feedback is updated when you select Confirm Save."
                        : "You may review and edit the response first. Saving is currently unavailable."}
                </Alert>



                <p className="text-xs text-slate-500">
                    You can review previous response statuses, quantities, unit prices, and notes before updating.
                </p>


                {loading ? (
                    <div className="flex items-center gap-3 py-5">
                        <Spinner />
                        <span className="text-sm text-slate-600">
                            Loading supplier response data...
                        </span>
                    </div>
                ) : loadError ? (
                    <div role="alert">
                        <Alert type="danger">{loadError}</Alert>
                    </div>
                ) : (
                    <>
                        <div className="grid gap-4 md:grid-cols-2">
                            <Select
                                label="Overall Supplier Response Status *"
                                value={orderStatus}
                                options={ORDER_STATUSES}
                                onChange={(event) => {
                                    setOrderStatus(event.target.value);
                                    setErrors([]);
                                    setPreview(null);
                                }}
                            />

                            <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-700">
                                <p className="text-xs font-semibold text-slate-500">
                                    PURCHASE ORDER
                                </p>
                                <strong>
                                    {purchaseOrder.orderCode}
                                </strong>
                                <p className="mt-1">
                                    {purchaseOrder.supplierName}
                                </p>
                            </div>
                        </div>

                        <p className="text-sm text-slate-600">
                            Requested Delivery Date: <strong>{purchaseOrder.expectedDeliveryDate || "Not specified"}</strong>
                        </p>
                        {purchaseOrder.status === "ORDERED" && !orderStatus && (
                            <p className="text-sm text-amber-700">
                                Awaiting supplier confirmation. Record a response only after the supplier has replied.
                            </p>
                        )}
                        <div className="grid gap-4 md:grid-cols-2">
                            <Input
                                type="date"
                                label="Supplier Proposed Delivery Date"
                                min={minSupplierProposedDate(
                                    orderStatus,
                                    purchaseOrder.expectedDeliveryDate
                                )}
                                value={proposedDate}
                                onChange={(event) => {
                                    setProposedDate(event.target.value);
                                    setAgreedDate("");
                                    setPreview(null);
                                }}
                            />
                            <Input
                                type="date"
                                label="Agreed Delivery Date (only when both parties agree)"
                                min={minSupplierProposedDate(
                                    orderStatus,
                                    purchaseOrder.expectedDeliveryDate
                                )}
                                value={agreedDate}
                                onChange={(event) => {
                                    setAgreedDate(event.target.value);
                                    setPreview(null);
                                }}
                            />
                        </div>
                        <TextArea
                            label="Overall Response Notes"
                            rows={3}
                            value={orderNote}
                            onChange={(event) => {
                                setOrderNote(event.target.value);
                                setPreview(null);
                            }}
                            placeholder="Example: Supplier agreed to deliver on the confirmed date..."
                        />

                        <div className="space-y-4">
                            <h3 className="font-semibold text-slate-900">
                                Item-Level Responses
                            </h3>

                            {rows.map((row, index) => (
                                <div
                                    key={row.id}
                                    className="space-y-4 rounded-xl border border-slate-200 p-4"
                                >
                                    <div>
                                        <h4 className="font-semibold text-slate-900">
                                            {row.productName}
                                        </h4>

                                        <p className="mt-1 text-xs text-slate-500">
                                            {row.productCode} | Item ID: {row.id}
                                        </p>

                                        <p className="mt-2 text-sm text-slate-600">
                                            Ordered: {row.orderedQuantity} | Received: {row.receivedQuantity} | Order Unit Price: {Number(
                                                row.originalUnitPrice
                                            ).toLocaleString("en-GB")} VND
                                        </p>
                                    </div>

                                    <div className="grid gap-4 md:grid-cols-3">
                                        <Select
                                            label="Product Availability Status *"
                                            options={ITEM_STATUSES}
                                            value={row.status}
                                            onChange={(event) =>
                                                updateRow(
                                                    index,
                                                    "status",
                                                    event.target.value
                                                )
                                            }
                                        />

                                        <Input
                                            type="number"
                                            min="0"
                                            step="1"
                                            label="Confirmed Qty"
                                            value={row.confirmedQuantity}
                                            onChange={(event) =>
                                                updateRow(
                                                    index,
                                                    "confirmedQuantity",
                                                    event.target.value
                                                )
                                            }
                                            placeholder="Enter quantity"
                                        />

                                        <Input
                                            type="number"
                                            min="0.01"
                                            step="0.01"
                                            label="Supplier Quoted Price (VND)"
                                            value={row.confirmedUnitPrice}
                                            onChange={(event) =>
                                                updateRow(
                                                    index,
                                                    "confirmedUnitPrice",
                                                    event.target.value
                                                )
                                            }
                                            placeholder="Enter new price"
                                        />
                                    </div>

                                    <TextArea
                                        label="Item Notes"
                                        rows={2}
                                        value={row.note}
                                        onChange={(event) =>
                                            updateRow(
                                                index,
                                                "note",
                                                event.target.value
                                            )
                                        }
                                        placeholder="Supplier notes about this product..."
                                    />
                                </div>
                            ))}
                        </div>

                        {errors.length > 0 && (
                            <div role="alert">
                                <Alert type="danger">
                                    <div className="space-y-1">
                                        {errors.map((message, index) => (
                                            <p key={index}>{message}</p>
                                        ))}
                                    </div>
                                </Alert>
                            </div>
                        )}


                        <div className="flex justify-end">
                            <Button
                                type="button"
                                onClick={handleReviewSave}
                                disabled={loading || rows.length === 0}
                            >
                                Save Response
                            </Button>
                        </div>
                  </>
             )}


            <Modal
                open={confirmOpen && Boolean(preview)}
                title="Confirm Supplier Response"
                onClose={() => setConfirmOpen(false)}
                footer={
                    <div className="flex justify-end gap-3">
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => setConfirmOpen(false)}
                        >
                            Close
                        </Button>


                        <Button
                            type="button"
                            onClick={handleConfirmSave}
                            disabled={
                                !SUPPLIER_FEEDBACK_WRITE_ENABLED ||
                                saving ||
                                loading ||
                                !preview
                            }
                        >
                            {!SUPPLIER_FEEDBACK_WRITE_ENABLED
                                ? "Confirm Save (disabled)"
                                : saving
                                ? "Saving..."
                                : "Confirm Save"}
                        </Button>

                    </div>
                }
            >
                <div className="space-y-4">
                    <Alert type="info">
                        {SUPPLIER_FEEDBACK_WRITE_ENABLED
                            ? "Review the response carefully. Selecting Confirm Save will update the system."
                            : "This is a confirmation preview. Saving is not enabled."}
                    </Alert>


                    <p className="text-sm text-slate-700">
                        Purchase Order:{" "}
                        <strong>{purchaseOrder.orderCode}</strong>
                    </p>

                    <p className="text-sm text-slate-700">
                        Supplier:{" "}
                        <strong>{purchaseOrder.supplierName}</strong>
                    </p>

                    <p className="text-sm text-slate-700">
                        Responded Items:{" "}
                        <strong>{preview?.items?.length ?? 0}</strong>
                    </p>


                    <div className="max-h-72 space-y-4 overflow-y-auto rounded-xl bg-slate-50 p-4">
                        <div className="space-y-2 text-sm">
                            <p>
                                Response Status:{" "}
                                <strong>
                                    {ORDER_STATUSES.find(
                                        (s) => s.value === preview?.supplierResponseStatus
                                    )?.label || "—"}
                                </strong>
                            </p>

                            <p>
                                Overall Notes:{" "}
                                {preview?.supplierResponseNote || "None"}
                            </p>
                            <p>Supplier proposed delivery: {preview?.supplierProposedDeliveryDate || "—"}</p>
                            <p>Agreed delivery: {preview?.agreedDeliveryDate || "—"}</p>
                        </div>

                        <div className="border-t border-slate-200 pt-3">
                            <h4 className="mb-3 font-semibold text-slate-900">
                                Product Details
                            </h4>

                            <div className="space-y-3">
                                {(preview?.items || []).map((item) => {
                                    const product = rows.find(
                                        (row) =>
                                            String(row.id) ===
                                            String(item.purchaseOrderItemId)
                                    );

                                    return (
                                        <div
                                            key={item.purchaseOrderItemId}
                                            className="space-y-1 rounded-lg border border-slate-200 bg-white p-3 text-sm"
                                        >
                                            <p className="font-semibold text-slate-900">
                                                {product?.productName || "Product"}
                                            </p>

                                            <p>
                                                Status:{" "}
                                                {ITEM_STATUSES.find(
                                                    (s) =>
                                                        s.value ===
                                                        item.supplierResponseStatus
                                                )?.label || "—"}
                                            </p>

                                            <p>
                                                Confirmed Quantity:{" "}
                                                {item.confirmedQuantity ?? "—"}
                                            </p>

                                            <p>
                                                Original Purchase Price:{" "}
                                                {Number(
                                                    product?.originalUnitPrice ?? 0
                                                ).toLocaleString("en-GB")} VND
                                            </p>

                                            <p>
                                                Supplier Quoted Price:{" "}
                                                {item.confirmedUnitPrice == null
                                                    ? "Not Quoted"
                                                    : `${Number(
                                                        item.confirmedUnitPrice
                                                    ).toLocaleString("en-GB")} VND`}
                                            </p>

                                            <p>
                                                Notes:{" "}
                                                {item.supplierResponseNote || "None"}
                                            </p>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    </div>

                </div>
            </Modal>

            </div>
        </Card>
    );
}
