
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
            setLoadError("Mã hóa đơn không hợp lệ.");
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
                        "Danh sách nhà cung cấp không hợp lệ."
                    );
                }

                setSuppliers(items);

                if (editing) {
                    if (!loaded || !EDITABLE.has(loaded.status)) {
                        setLoadError(
                            "Chỉ hóa đơn nháp hoặc đang chờ kiểm tra mới được sửa."
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
                            "Không tải được dữ liệu tạo/sửa hóa đơn."
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
                        "Danh sách đơn mua hàng không hợp lệ."
                    );
                }

                setPurchaseOrders(items);
            })
            .catch((err) => {
                if (!controller.signal.aborted) {
                    setOrderError(
                        supplierInvoiceErrorMessage(
                            err,
                            "Không tải được đơn mua hàng của nhà cung cấp."
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
            ? "Vui lòng chọn file PDF, XML, JPG, JPEG, PNG hoặc WEBP không rỗng."
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
                    "Chưa nhận được mã hóa đơn. Vui lòng kiểm tra danh sách trước khi tạo lại."
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
                            "Chưa xác nhận được tệp đã đính kèm. Vui lòng kiểm tra chi tiết hóa đơn."
                        );
                    }
                } catch (uploadError) {
                    // Hóa đơn đã tồn tại. Không cho tạo trùng khi upload lỗi.
                    const message =
                        uploadError?.response || uploadError?.request
                            ? supplierInvoiceErrorMessage(
                                  uploadError,
                                  "Tải tệp lên thất bại."
                              )
                            : uploadError?.message ||
                              "Tải tệp lên thất bại.";

                    setSavedInvoiceId(Number(saved.id));

                    setAttachmentWarning(
                        `Hóa đơn ${
                            editing
                                ? "đã được cập nhật"
                                : "nháp đã được tạo"
                        } thành công, nhưng chưa xác nhận được việc đính kèm file: ${message} ` +
                            "Không tạo lại hóa đơn. Hãy mở chi tiết để kiểm tra và tải lại nếu cần."
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
                    "Không thể lưu hóa đơn. Hãy kiểm tra dữ liệu."
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
                            ? `Sửa hóa đơn ${
                                  original?.invoiceNumber || ""
                              }`
                            : "Tạo hóa đơn nhà cung cấp"
                    }
                    description="Chọn nhà cung cấp, đơn mua hàng và nhập thông tin hóa đơn. Tổng tiền được tính tự động từ số tiền trước thuế và tiền thuế."
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
                            ← Quay lại
                        </Button>
                    }
                />

                <Alert type="info">
                    Tạo hóa đơn mới ở trạng thái{" "}
                    <strong>Nháp</strong>. Chỉ hóa đơn{" "}
                    <strong>Nháp</strong> hoặc{" "}
                    <strong>Chờ xác minh</strong> mới được
                    sửa. Mọi thao tác lưu sẽ ghi dữ liệu thật lên
                    hệ thống và cần xác nhận riêng.
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
                                    Mở chi tiết hóa đơn để kiểm tra file
                                </Button>
                            </div>
                        </Alert>
                    </div>
                )}

                {problems.length > 0 && (
                    <div role="alert">
                        <Alert type="danger">
                            <strong>Thông tin không hợp lệ:</strong>

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
                            Đang tải dữ liệu...
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
                                        Mã nội bộ:{" "}
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
                                    Nhà cung cấp và đơn mua hàng
                                </h2>

                                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                                    <Select
                                        label="Nhà cung cấp *"
                                        value={form.supplierId}
                                        options={[
                                            {
                                                value: "",
                                                label: "-- Chọn nhà cung cấp --",
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
                                        label="Đơn mua hàng liên kết *"
                                        value={form.purchaseOrderId}
                                        options={[
                                            {
                                                value: "",
                                                label: loadingOrders
                                                    ? "Đang tải đơn mua hàng..."
                                                    : "-- Chọn đơn mua hàng --",
                                            },
                                            ...(!loadingOrders &&
                                            editing &&
                                            form.purchaseOrderId &&
                                            !selectedOrder
                                                ? [
                                                      {
                                                          value: form.purchaseOrderId,
                                                          label: `Đơn mua hàng số ${form.purchaseOrderId} (không còn đủ điều kiện)`,
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
                                            Nhà cung cấp này chưa có đơn mua hàng phù hợp để lập hóa đơn.
                                        </p>
                                    )}

                                {selectedOrder && (
                                    <p className="mt-3 text-sm text-slate-600">
                                        Giá trị đơn mua hàng:{" "}
                                        <strong>
                                            {formatSupplierInvoiceMoney(
                                                selectedOrder.totalAmount
                                            )}
                                        </strong>
                                        . Ngày giao dự kiến:{" "}
                                        {selectedOrder.expectedDeliveryDate ||
                                            "—"}
                                        .
                                    </p>
                                )}
                            </Card>

                            <Card>
                                <h2 className="mb-4 text-lg font-semibold text-slate-900">
                                    Thông tin hóa đơn
                                </h2>

                                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                                    <Input
                                        label="Số hóa đơn * (tối đa 100 ký tự)"
                                        value={form.invoiceNumber}
                                        maxLength={100}
                                        placeholder="Nhập số hóa đơn từ nhà cung cấp"
                                        onChange={(event) =>
                                            changeField(
                                                "invoiceNumber",
                                                event.target.value
                                            )
                                        }
                                    />

                                    <div className="self-end pb-2 text-sm text-slate-500">
                                        Số hóa đơn không được trùng với hóa đơn khác của cùng nhà cung cấp.
                                    </div>

                                    <Input
                                        label="Ngày phát hành hóa đơn *"
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
                                        label="Hạn thanh toán *"
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
                                            Hạn thanh toán không được
                                            trước ngày phát hành.
                                        </p>
                                    )}
                            </Card>

                            <Card>
                                <h2 className="mb-2 text-lg font-semibold text-slate-900">
                                    Giá trị hóa đơn
                                </h2>

                                <p className="mb-4 text-sm text-slate-500">
                                    Nhập tiền bằng số, không có dấu
                                    phân cách hàng nghìn (ví dụ:
                                    12000000). Hỗ trợ tối đa 2 chữ số
                                    thập phân.
                                </p>

                                <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                                    <Input
                                        label="Giá trị trước thuế (VND) *"
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
                                        label="Tiền thuế (VND) *"
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
                                        label="Tổng tiền (tự tính)"
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
                                                Chênh lệch với đơn mua hàng (trước thuế):
                                            </strong>{" "}
                                            {moneyLabel(
                                                amountDifference
                                            )}{" "}
                                            (giá trị hóa đơn trừ giá trị đơn mua hàng).

                                            <p className="mt-1">
                                                PO:{" "}
                                                {formatSupplierInvoiceMoney(
                                                    selectedOrder.totalAmount
                                                )}{" "}
                                                · Hóa đơn trước thuế:{" "}
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
                                                    label="Tôi đã kiểm tra số tiền chênh lệch và vẫn muốn lưu hóa đơn."
                                                />
                                            </div>
                                        </Alert>
                                    </div>
                                )}

                                {amountDifference === 0 && (
                                    <div className="mt-4">
                                        <Alert type="success">
                                            Số tiền trước thuế khớp
                                            với giá trị đơn mua hàng.
                                        </Alert>
                                    </div>
                                )}
                            </Card>

                            {/* Phần mới: Đính kèm chứng từ hóa đơn */}
                            <Card>
                                <h2 className="mb-2 text-lg font-semibold text-slate-900">
                                    Chứng từ hóa đơn
                                </h2>

                                <p className="mb-4 text-sm text-slate-600">
                                    Hỗ trợ tệp PDF, XML và ảnh (JPG, PNG, WEBP).
                                    Bạn có thể đính kèm chứng từ ngay bây giờ
                                    hoặc bổ sung sau tại trang chi tiết hóa đơn.
                                </p>

                                <label
                                    htmlFor="supplier-invoice-form-attachment"
                                    className="mb-2 block text-sm font-medium text-slate-800"
                                >
                                    Chọn file hóa đơn (tùy chọn)
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
                                        Đã chọn:{" "}
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
                                                ? "Tôi đồng ý lưu hóa đơn và đính kèm tệp chứng từ."
                                                : "Tôi đã kiểm tra và đồng ý lưu hóa đơn vào hệ thống."
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
                                        Hủy
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
                                              ? "Lưu thay đổi hóa đơn"
                                              : attachmentFile
                                                ? "Tạo hóa đơn nháp và tải file"
                                                : "Tạo hóa đơn nháp"}
                                    </Button>
                                </div>

                                {liveProblems.length > 0 && (
                                    <p className="mt-3 text-xs text-slate-500">
                                        Form cần điền đủ các trường
                                        bắt buộc và giá trị hợp lệ
                                        trước khi lưu.
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
