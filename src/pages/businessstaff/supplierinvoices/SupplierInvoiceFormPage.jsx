
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
    createSupplierInvoice,
    getSupplierInvoiceDetail,
    getSupplierInvoicePurchaseOrderOptions,
    getSupplierInvoiceSupplierOptions,
    supplierInvoiceErrorMessage,
    updateSupplierInvoice,
    uploadSupplierInvoiceAttachment,
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
import {
    formatSupplierInvoiceMoney,
    SupplierInvoiceStatusBadge,
} from "./supplierInvoiceFormat";

const ROOT = "/business/supplier-invoices";
const EDITABLE = new Set(["DRAFT", "PENDING_VERIFICATION"]);

const FILE_EXTENSION_PATTERN = /\.(pdf|xml|jpe?g|png|webp)$/i;
const FILE_ACCEPT =
    ".pdf,.xml,.jpg,.jpeg,.png,.webp,application/pdf,application/xml,text/xml,image/jpeg,image/png,image/webp";

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

    return formatSupplierInvoiceMoney(
        Number(centsToInvoiceDecimal(Math.abs(cents)))
    ).replace(/^/, cents < 0 ? "−" : "");
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

    // File hóa đơn được chọn trên giao diện.
    const [attachmentFile, setAttachmentFile] = useState(null);

    // Ghi nhớ ID nếu hóa đơn đã lưu nhưng upload chưa thành công.
    const [savedInvoiceId, setSavedInvoiceId] = useState(null);
    const [attachmentWarning, setAttachmentWarning] = useState("");

    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setLoadError("");
        setOriginal(null);
        setSuppliers([]);
        setConfirmedWrite(false);
        setConfirmedMismatch(false);
        setAttachmentFile(null);
        setSavedInvoiceId(null);
        setAttachmentWarning("");

        if (
            editing &&
            (!Number.isSafeInteger(Number(invoiceId)) ||
                Number(invoiceId) <= 0)
        ) {
            setLoadError("Invalid invoice code.");
            setLoading(false);
            return () => controller.abort();
        }

        Promise.all([
            getSupplierInvoiceSupplierOptions(controller.signal),
            editing
                ? getSupplierInvoiceDetail(invoiceId, controller.signal)
                : Promise.resolve(null),
        ])
            .then(([items, loaded]) => {
                if (controller.signal.aborted) return;

                if (!Array.isArray(items)) {
                    throw new Error(
                        "Invalid supplier list data."
                    );
                }

                setSuppliers(items);

                if (editing) {
                    if (!loaded || !EDITABLE.has(loaded.status)) {
                        setLoadError(
                            "Only Draft or Pending Verification invoices can be edited."
                        );
                        return;
                    }

                    setOriginal(loaded);
                    setForm({
                        supplierId: String(loaded.supplierId ?? ""),
                        purchaseOrderId: String(
                            loaded.purchaseOrderId ?? ""
                        ),
                        invoiceNumber: loaded.invoiceNumber ?? "",
                        invoiceDate: loaded.invoiceDate ?? "",
                        dueDate: loaded.dueDate ?? "",
                        subtotal:
                            loaded.subtotal == null
                                ? ""
                                : String(loaded.subtotal),
                        taxAmount:
                            loaded.taxAmount == null
                                ? ""
                                : String(loaded.taxAmount),
                    });
                } else {
                    setForm(blankForm());
                }
            })
            .catch((err) => {
                if (!controller.signal.aborted) {
                    setLoadError(
                        supplierInvoiceErrorMessage(
                            err,
                            "Unable to load invoice form data."
                        )
                    );
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
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

        getSupplierInvoicePurchaseOrderOptions(
            form.supplierId,
            controller.signal
        )
            .then((items) => {
                if (controller.signal.aborted) return;

                if (!Array.isArray(items)) {
                    throw new Error(
                        "Invalid purchase order list data."
                    );
                }

                setPurchaseOrders(items);
            })
            .catch((err) => {
                if (!controller.signal.aborted) {
                    setOrderError(
                        supplierInvoiceErrorMessage(
                            err,
                            "Unable to load supplier purchase orders."
                        )
                    );
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoadingOrders(false);
                }
            });

        return () => controller.abort();
    }, [form.supplierId, loading, loadError]);

    const selectedOrder = useMemo(
        () =>
            purchaseOrders.find(
                (po) => String(po.id) === form.purchaseOrderId
            ),
        [purchaseOrders, form.purchaseOrderId]
    );

    const subtotalCents = parseInvoiceCents(form.subtotal);
    const taxCents = parseInvoiceCents(form.taxAmount);

    const totalCents =
        subtotalCents !== null && taxCents !== null
            ? subtotalCents + taxCents
            : null;

    const poCents = selectedOrder
        ? parseInvoiceCents(selectedOrder.totalAmount)
        : null;

    const amountDifference =
        subtotalCents !== null && poCents !== null
            ? subtotalCents - poCents
            : null;

    const mismatched =
        amountDifference !== null && amountDifference !== 0;

    const liveProblems = validateSupplierInvoiceDraft(
        form,
        purchaseOrders
    );

    // Kiểm tra định dạng và file rỗng trước khi upload.
    const attachmentError =
        attachmentFile &&
        (!FILE_EXTENSION_PATTERN.test(attachmentFile.name) ||
            attachmentFile.size <= 0)
            ? "Select a non-empty PDF, XML, JPG, JPEG, PNG, or WEBP file."
            : "";

    const changeField = (key, value) => {
        setForm((old) => ({
            ...old,
            [key]: value,
            ...(key === "supplierId"
                ? { purchaseOrderId: "" }
                : {}),
        }));

        setConfirmedWrite(false);
        setConfirmedMismatch(false);
        setSaveError("");
        setProblems([]);
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        if (
            saving ||
            savedInvoiceId ||
            loading ||
            loadingOrders ||
            loadError ||
            orderError
        ) {
            return;
        }

        const issues = validateSupplierInvoiceDraft(
            form,
            purchaseOrders
        );

        setProblems(issues);
        setSaveError("");

        if (
            issues.length ||
            attachmentError ||
            !confirmedWrite ||
            (mismatched && !confirmedMismatch)
        ) {
            return;
        }

        setSaving(true);

        try {
            const payload = supplierInvoiceDraftPayload(form);

            // Bước 1: Tạo hoặc cập nhật hóa đơn trong MySQL.
            const saved = editing
                ? await updateSupplierInvoice(invoiceId, payload)
                : await createSupplierInvoice(payload);

            if (
                !saved ||
                !Number.isSafeInteger(Number(saved.id)) ||
                Number(saved.id) <= 0
            ) {
                throw new Error(
                    "No invoice code was returned. Check the invoice list before creating another one."
                );
            }

            // Bước 2: Nếu có file, upload bằng ID hóa đơn vừa lưu.
            if (attachmentFile) {
                try {
                    const uploaded =
                        await uploadSupplierInvoiceAttachment(
                            saved.id,
                            attachmentFile
                        );

                    if (
                        !uploaded ||
                        Number(uploaded.id) !== Number(saved.id)
                    ) {
                        throw new Error(
                            "Unable to verify the attachment. Check the invoice details."
                        );
                    }
                } catch (uploadError) {
                    // Hóa đơn đã tồn tại. Không cho tạo trùng khi upload lỗi.
                    const message =
                        uploadError?.response || uploadError?.request
                            ? supplierInvoiceErrorMessage(
                                  uploadError,
                                  "File upload failed."
                              )
                            : uploadError?.message ||
                              "File upload failed.";

                    setSavedInvoiceId(Number(saved.id));

                    setAttachmentWarning(
                        `Invoice ${
                            editing
                                ? "was updated"
                                : "draft was created"
                        } successfully, but the attachment could not be verified: ${message} ` +
                            "Do not create the invoice again. Open its details to check and retry the upload if needed."
                    );

                    return;
                }
            }

            // Bước 3: Chuyển sang trang chi tiết.
            navigate(`${ROOT}/${saved.id}`, {
                replace: true,
            });
        } catch (err) {
            setSaveError(
                supplierInvoiceErrorMessage(
                    err,
                    "Unable to save invoice. Check the data."
                )
            );
        } finally {
            setSaving(false);
        }
    };

    return (
        <MainLayout
            title="Business Operations"
            items={businessStaffMenuItems}
        >
            <div className="space-y-6">
                <PageHeader
                    title={
                        editing
                            ? `Edit Invoice ${
                                  original?.invoiceNumber || ""
                              }`
                            : "Create Supplier Invoice"
                    }
                    description="Select a supplier and purchase order, then enter invoice details. The total is calculated automatically from the subtotal and tax."
                    actions={
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() =>
                                navigate(
                                    editing
                                        ? `${ROOT}/${invoiceId}`
                                        : ROOT
                                )
                            }
                        >
                            ← Back
                        </Button>
                    }
                />

                <Alert type="info">
                    A new invoice is created with status{" "}
                    <strong>Draft</strong>. Only invoices in{" "}
                    <strong>Draft</strong> or{" "}
                    <strong>Pending Verification</strong> can be edited. Saving changes updates the system and requires separate confirmation.
                </Alert>

                {loadError && (
                    <div role="alert">
                        <Alert type="danger">{loadError}</Alert>
                    </div>
                )}

                {saveError && (
                    <div role="alert">
                        <Alert type="danger">{saveError}</Alert>
                    </div>
                )}

                {attachmentWarning && (
                    <div role="alert">
                        <Alert type="warning">
                            <p>{attachmentWarning}</p>

                            <div className="mt-3">
                                <Button
                                    type="button"
                                    onClick={() =>
                                        navigate(
                                            `${ROOT}/${savedInvoiceId}`,
                                            { replace: true }
                                        )
                                    }
                                >
                                    Open Invoice Details to verify the attachment
                                </Button>
                            </div>
                        </Alert>
                    </div>
                )}

                {problems.length > 0 && (
                    <div role="alert">
                        <Alert type="danger">
                            <strong>Invalid information:</strong>

                            <ul className="mt-2 list-disc space-y-1 pl-5">
                                {problems.map((item, index) => (
                                    <li key={index}>{item}</li>
                                ))}
                            </ul>
                        </Alert>
                    </div>
                )}

                {loading ? (
                    <Card>
                        <span className="flex items-center gap-2 text-sm text-slate-600">
                            <Spinner />
                            Loading data...
                        </span>
                    </Card>
                ) : (
                    !loadError && (
                        <form
                            onSubmit={handleSubmit}
                            className="space-y-6"
                        >
                            {editing && original && (
                                <Card className="flex items-center justify-between gap-3">
                                    <span className="text-sm text-slate-600">
                                        Internal ID:{" "}
                                        <strong>
                                            {original.invoiceCode || "—"}
                                        </strong>
                                    </span>

                                    <SupplierInvoiceStatusBadge
                                        status={original.status}
                                    />
                                </Card>
                            )}

                            <Card>
                                <h2 className="mb-4 text-lg font-semibold text-slate-900">
                                    Supplier & Purchase Order
                                </h2>

                                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                                    <Select
                                        label="Supplier *"
                                        value={form.supplierId}
                                        options={[
                                            {
                                                value: "",
                                                label: "-- Select Supplier --",
                                            },
                                            ...suppliers.map((item) => ({
                                                value: String(item.id),
                                                label: `${item.supplierName} (${item.supplierCode})`,
                                            })),
                                        ]}
                                        onChange={(event) =>
                                            changeField(
                                                "supplierId",
                                                event.target.value
                                            )
                                        }
                                        disabled={saving}
                                    />

                                    <Select
                                        label="Linked Purchase Order *"
                                        value={form.purchaseOrderId}
                                        options={[
                                            {
                                                value: "",
                                                label: loadingOrders
                                                    ? "Loading purchase orders..."
                                                    : "-- Select Purchase Order --",
                                            },
                                            ...(!loadingOrders &&
                                            editing &&
                                            form.purchaseOrderId &&
                                            !selectedOrder
                                                ? [
                                                      {
                                                          value: form.purchaseOrderId,
                                                          label: `Purchase Order # ${form.purchaseOrderId} (no longer eligible)`,
                                                      },
                                                  ]
                                                : []),
                                            ...purchaseOrders.map(
                                                (po) => ({
                                                    value: String(po.id),
                                                    label: `${po.orderCode} · ${po.status} · ${formatSupplierInvoiceMoney(
                                                        po.totalAmount
                                                    )}`,
                                                })
                                            ),
                                        ]}
                                        onChange={(event) =>
                                            changeField(
                                                "purchaseOrderId",
                                                event.target.value
                                            )
                                        }
                                        disabled={
                                            !form.supplierId ||
                                            loadingOrders ||
                                            !!orderError ||
                                            saving
                                        }
                                    />
                                </div>

                                {orderError && (
                                    <div
                                        className="mt-3"
                                        role="alert"
                                    >
                                        <Alert type="danger">
                                            {orderError}
                                        </Alert>
                                    </div>
                                )}

                                {form.supplierId &&
                                    !loadingOrders &&
                                    !orderError &&
                                    purchaseOrders.length === 0 && (
                                        <p className="mt-3 text-sm text-amber-700">
                                            This supplier has no eligible purchase orders for invoicing.
                                        </p>
                                    )}

                                {selectedOrder && (
                                    <p className="mt-3 text-sm text-slate-600">
                                        Purchase Order Amount:{" "}
                                        <strong>
                                            {formatSupplierInvoiceMoney(
                                                selectedOrder.totalAmount
                                            )}
                                        </strong>
                                        . Expected Delivery Date:{" "}
                                        {selectedOrder.expectedDeliveryDate ||
                                            "—"}
                                        .
                                    </p>
                                )}
                            </Card>

                            <Card>
                                <h2 className="mb-4 text-lg font-semibold text-slate-900">
                                    Invoice Information
                                </h2>

                                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                                    <Input
                                        label="Invoice Number * (maximum 100 characters)"
                                        value={form.invoiceNumber}
                                        maxLength={100}
                                        placeholder="Enter the supplier's invoice number"
                                        onChange={(event) =>
                                            changeField(
                                                "invoiceNumber",
                                                event.target.value
                                            )
                                        }
                                    />

                                    <div className="self-end pb-2 text-sm text-slate-500">
                                        Invoice number must be unique for each supplier.
                                    </div>

                                    <Input
                                        label="Invoice Issue Date *"
                                        type="date"
                                        value={form.invoiceDate}
                                        onChange={(event) =>
                                            changeField(
                                                "invoiceDate",
                                                event.target.value
                                            )
                                        }
                                    />

                                    <Input
                                        label="Payment Due Date *"
                                        type="date"
                                        min={
                                            form.invoiceDate ||
                                            undefined
                                        }
                                        value={form.dueDate}
                                        onChange={(event) =>
                                            changeField(
                                                "dueDate",
                                                event.target.value
                                            )
                                        }
                                    />
                                </div>

                                {form.invoiceDate &&
                                    form.dueDate &&
                                    form.dueDate <
                                        form.invoiceDate && (
                                        <p
                                            className="mt-3 text-sm font-medium text-red-600"
                                            role="alert"
                                        >
                                            Payment due date cannot be before the invoice issue date.
                                        </p>
                                    )}
                            </Card>

                            <Card>
                                <h2 className="mb-2 text-lg font-semibold text-slate-900">
                                    Invoice Amount
                                </h2>

                                <p className="mb-4 text-sm text-slate-500">
                                    Enter amounts as numbers without thousands separators (e.g., 12000000). Up to two decimal places are supported.
                                </p>

                                <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                                    <Input
                                        label="Subtotal Before Tax (VND) *"
                                        type="text"
                                        inputMode="decimal"
                                        value={form.subtotal}
                                        placeholder="0"
                                        onChange={(event) =>
                                            changeField(
                                                "subtotal",
                                                event.target.value
                                            )
                                        }
                                    />

                                    <Input
                                        label="Tax Amount (VND) *"
                                        type="text"
                                        inputMode="decimal"
                                        value={form.taxAmount}
                                        placeholder="0"
                                        onChange={(event) =>
                                            changeField(
                                                "taxAmount",
                                                event.target.value
                                            )
                                        }
                                    />

                                    <Input
                                        label="Total Amount (calculated)"
                                        type="text"
                                        value={
                                            totalCents === null
                                                ? "—"
                                                : moneyLabel(
                                                      totalCents
                                                  )
                                        }
                                        readOnly
                                        className="bg-slate-50 font-semibold"
                                    />
                                </div>

                                {mismatched && selectedOrder && (
                                    <div
                                        className="mt-4"
                                        role="status"
                                    >
                                        <Alert type="warning">
                                            <strong>
                                                Difference from Purchase Order (before tax):
                                            </strong>{" "}
                                            {moneyLabel(
                                                amountDifference
                                            )}{" "}
                                            (invoice subtotal minus purchase order amount).

                                            <p className="mt-1">
                                                PO:{" "}
                                                {formatSupplierInvoiceMoney(
                                                    selectedOrder.totalAmount
                                                )}{" "}
                                                · Invoice Subtotal:{" "}
                                                {moneyLabel(
                                                    subtotalCents
                                                )}
                                                .
                                            </p>

                                            <div className="mt-3">
                                                <Checkbox
                                                    checked={
                                                        confirmedMismatch
                                                    }
                                                    onChange={(
                                                        event
                                                    ) =>
                                                        setConfirmedMismatch(
                                                            event
                                                                .target
                                                                .checked
                                                        )
                                                    }
                                                    label="I have reviewed the amount difference and still wish to save the invoice."
                                                />
                                            </div>
                                        </Alert>
                                    </div>
                                )}

                                {amountDifference === 0 && (
                                    <div className="mt-4">
                                        <Alert type="success">
                                            The invoice subtotal matches the purchase order amount.
                                        </Alert>
                                    </div>
                                )}
                            </Card>

                            {/* Phần mới: Đính kèm chứng từ hóa đơn */}
                            <Card>
                                <h2 className="mb-2 text-lg font-semibold text-slate-900">
                                    Invoice Supporting Document
                                </h2>

                                <p className="mb-4 text-sm text-slate-600">
                                    Supported files: PDF, XML, or images (JPG, PNG, WEBP). You can attach a document now or add it later on the invoice details page.
                                </p>

                                <label
                                    htmlFor="supplier-invoice-form-attachment"
                                    className="mb-2 block text-sm font-medium text-slate-800"
                                >
                                    Select Invoice File (optional)
                                </label>

                                <input
                                    key={
                                        editing
                                            ? invoiceId
                                            : "new"
                                    }
                                    id="supplier-invoice-form-attachment"
                                    type="file"
                                    accept={FILE_ACCEPT}
                                    disabled={
                                        saving ||
                                        !!savedInvoiceId
                                    }
                                    onChange={(event) => {
                                        setAttachmentFile(
                                            event.target.files?.[0] ||
                                                null
                                        );
                                        setSaveError("");
                                    }}
                                    className="block w-full rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700 file:mr-3 file:rounded-lg file:border-0 file:bg-[#fff1e9] file:px-3 file:py-2 file:font-semibold file:text-[#c94810]"
                                />

                                {attachmentFile && (
                                    <p className="mt-2 break-all text-xs text-slate-600">
                                        Selected:{" "}
                                        {attachmentFile.name} (
                                        {(
                                            attachmentFile.size /
                                            1024
                                        ).toFixed(1)}{" "}
                                        KB)
                                    </p>
                                )}

                                {attachmentError && (
                                    <div
                                        className="mt-3"
                                        role="alert"
                                    >
                                        <Alert type="danger">
                                            {attachmentError}
                                        </Alert>
                                    </div>
                                )}
                            </Card>

                            <Card>
                                <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
                                    <Checkbox
                                        checked={confirmedWrite}
                                        onChange={(event) =>
                                            setConfirmedWrite(
                                                event.target.checked
                                            )
                                        }
                                        label={
                                            attachmentFile
                                                ? "I agree to save the invoice and attach the supporting document."
                                                : "I have reviewed and agree to save the invoice to the system."
                                        }
                                    />
                                </div>

                                <div className="mt-4 flex flex-wrap items-center justify-end gap-3">
                                    <Button
                                        type="button"
                                        variant="outline"
                                        onClick={() =>
                                            navigate(
                                                editing
                                                    ? `${ROOT}/${invoiceId}`
                                                    : ROOT
                                            )
                                        }
                                    >
                                        Cancel
                                    </Button>

                                    <Button
                                        type="submit"
                                        disabled={
                                            saving ||
                                            !!savedInvoiceId ||
                                            !!attachmentError ||
                                            loadingOrders ||
                                            !!orderError ||
                                            !confirmedWrite ||
                                            (mismatched &&
                                                !confirmedMismatch)
                                        }
                                    >
                                        {saving
                                            ? "Saving..."
                                            : editing
                                              ? "Save Invoice Changes"
                                              : attachmentFile
                                                ? "Create Draft Invoice and Upload File"
                                                : "Create Draft Invoice"}
                                    </Button>
                                </div>

                                {liveProblems.length > 0 && (
                                    <p className="mt-3 text-xs text-slate-500">
                                        Complete all required fields with valid information before saving.
                                    </p>
                                )}
                            </Card>
                        </form>
                    )
                )}
            </div>
        </MainLayout>
    );
}
