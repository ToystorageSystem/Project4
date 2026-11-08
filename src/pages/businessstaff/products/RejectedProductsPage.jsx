import { formatBusinessStatus } from "../../../utils/viStatus";

import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import {
    closeRejectedProductRequest,
    getRejectedProductRequest,
    listRejectedProductRequests,
    resubmitRejectedProductRequest,
} from "../../../api/business/rejectedProducts";
import {
    getCategoryOptions,
    getProduct,
    productErrorMessage,
} from "../../../api/business/products";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import EmptyState from "../../../components/ui/EmptyState";
import Spinner from "../../../components/ui/Spinner";

const PAGE_SIZE = 10;

const EMPTY_FORM = {
    productCode: "",
    barcode: "",
    imageUrl: "",
    name: "",
    categoryId: "",
    brandId: "",
    baseUnit: "",
    purchasePrice: "",
    sellingPrice: "",
    requestReason: "",
};

const INPUT_CLASS =
    "mt-1 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100";

function asObject(value) {
    if (value && typeof value === "object" && !Array.isArray(value)) {
        return value;
    }

    if (typeof value === "string") {
        try {
            const parsed = JSON.parse(value);

            if (
                parsed &&
                typeof parsed === "object" &&
                !Array.isArray(parsed)
            ) {
                return parsed;
            }
        } catch {
            // Bỏ qua dữ liệu snapshot cũ không phải JSON.
        }
    }

    return {};
}

function firstAvailable(sources, keys) {
    for (const source of sources) {
        const object = asObject(source);

        for (const key of keys) {
            const value = object[key];

            if (value !== null && value !== undefined) {
                return String(value);
            }
        }
    }

    return "";
}

function buildForm(request, product) {
    // Ưu tiên dữ liệu đề xuất trước dữ liệu sản phẩm hiện tại.
    const sources = [
        request?.newValue,
        product,
        request?.oldValue,
    ];

    return {
        productCode: firstAvailable(sources, [
            "productCode",
            "productsCode",
        ]),
        barcode: firstAvailable(sources, ["barcode"]),
        imageUrl: firstAvailable(sources, ["imageUrl"]),
        name: firstAvailable(sources, [
            "name",
            "productName",
        ]),
        categoryId: firstAvailable(sources, ["categoryId"]),
        brandId: firstAvailable(sources, ["brandId"]),
        baseUnit: firstAvailable(sources, ["baseUnit"]),
        purchasePrice: firstAvailable(sources, ["purchasePrice"]),
        sellingPrice: firstAvailable(sources, ["sellingPrice"]),
        requestReason: request?.requestReason || "",
    };
}

function validate(form) {
    const required = [
        "productCode",
        "barcode",
        "name",
        "baseUnit",
    ];

    if (required.some((field) => !form[field].trim())) {
        return "Product code, barcode, name, and unit of measure are required.";
    }

    const limits = {
        productCode: 50,
        barcode: 100,
        imageUrl: 500,
        name: 200,
        baseUnit: 30,
        requestReason: 500,
    };

    if (
        Object.entries(limits).some(
            ([key, max]) => form[key].trim().length > max
        )
    ) {
        return "Some fields exceed the allowed length. Shorten the information and try again.";
    }

    if (!/^[1-9]\d*$/.test(form.categoryId.trim())) {
        return "Category ID must be a positive integer.";
    }

    if (
        form.brandId.trim() &&
        !/^[1-9]\d*$/.test(form.brandId.trim())
    ) {
        return "Brand ID must be a positive integer or left blank.";
    }

    if (
        ["purchasePrice", "sellingPrice"].some(
            (field) =>
                !form[field].trim() ||
                !Number.isFinite(Number(form[field])) ||
                Number(form[field]) < 0.01
        )
    ) {
        return "Purchase and selling prices must be at least 0.01.";
    }

    if (
        form.imageUrl.trim() &&
        !/^https?:\/\//i.test(form.imageUrl.trim())
    ) {
        return "Image URL must start with http:// or https://.";
    }

    return "";
}

function toPayload(form) {
    return {
        productCode: form.productCode.trim(),
        barcode: form.barcode.trim(),
        imageUrl: form.imageUrl.trim() || null,
        name: form.name.trim(),
        categoryId: Number(form.categoryId),
        brandId: form.brandId.trim()
            ? Number(form.brandId)
            : null,
        baseUnit: form.baseUnit.trim(),
        purchasePrice: Number(form.purchasePrice),
        sellingPrice: Number(form.sellingPrice),
        requestReason: form.requestReason.trim() || null,
    };
}

function formatDate(value) {
    if (!value) return "—";

    const date = new Date(value);

    return Number.isNaN(date.getTime())
        ? String(value)
        : new Intl.DateTimeFormat("en-GB", {
              dateStyle: "short",
              timeStyle: "short",
          }).format(date);
}

function FormField({
    label,
    value,
    onChange,
    type = "text",
    required = false,
    ...rest
}) {
    return (
        <label className="block text-sm font-medium text-slate-700">
            {label}
            {required ? " *" : ""}

            <input
                className={INPUT_CLASS}
                type={type}
                value={value}
                onChange={(event) =>
                    onChange(event.target.value)
                }
                required={required}
                {...rest}
            />
        </label>
    );
}

// =====================================================
// PRODUCT CHANGE COMPARISON
// Chỉ thay cách hiển thị dữ liệu JSON.
// Không thay đổi API hoặc nghiệp vụ.
// =====================================================

const COMPARISON_FIELDS = [
    {
        key: "productCode",
        label: "Product Code",
    },
    {
        key: "barcode",
        label: "Barcode",
    },
    {
        key: "name",
        label: "Product Name",
    },
    {
        key: "categoryId",
        nameKey: "categoryName",
        label: "Categories",
    },
    {
        key: "brandId",
        nameKey: "brandName",
        label: "Brand",
    },
    {
        key: "baseUnit",
        label: "Unit of Measure",
    },
    {
        key: "purchasePrice",
        label: "Purchase Price",
        price: true,
    },
    {
        key: "sellingPrice",
        label: "Selling Price",
        price: true,
    },
    {
        key: "imageUrl",
        label: "Image",
        image: true,
    },
];

function comparisonValue(value, field, snapshot) {
    if (field.image) {
        return value
            ? "Image Available"
            : "No Image";
    }

    if (field.nameKey && snapshot[field.nameKey]) {
        return String(snapshot[field.nameKey]);
    }

    if (
        value === null ||
        value === undefined ||
        value === ""
    ) {
        return "Not Available";
    }

    if (field.price) {
        const number = Number(value);

        if (Number.isFinite(number)) {
            return new Intl.NumberFormat("en-GB", {
                style: "currency",
                currency: "VND",
                maximumFractionDigits: 2,
            }).format(number);
        }
    }

    return String(value);
}

function hasChanged(field, oldData, newData) {
    if (
        !Object.prototype.hasOwnProperty.call(
            newData,
            field.key
        )
    ) {
        return false;
    }

    const before = oldData[field.key];
    const after = newData[field.key];

    if (
        field.price &&
        before != null &&
        after != null
    ) {
        const beforePrice = Number(before);
        const afterPrice = Number(after);

        if (
            Number.isFinite(beforePrice) &&
            Number.isFinite(afterPrice)
        ) {
            return beforePrice !== afterPrice;
        }
    }

    return (
        String(before ?? "").trim() !==
        String(after ?? "").trim()
    );
}

function ProductChangeComparison({ request }) {
    const oldData = asObject(request.oldValue);
    const newData = asObject(request.newValue);

    const isCreate = request.requestType === "CREATE";

    const visibleFields = COMPARISON_FIELDS.filter(
        (field) =>
            isCreate
                ? Object.prototype.hasOwnProperty.call(
                      newData,
                      field.key
                  )
                : hasChanged(field, oldData, newData)
    );

    return (
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 bg-slate-50 px-4 py-3">
                <div>
                    <h3 className="text-sm font-semibold text-slate-900">
                        {isCreate
                            ? "Proposed Product Information"
                            : "Compare Product Information"}
                    </h3>

                    <p className="mt-1 text-xs text-slate-500">
                        {isCreate
                            ? "Information submitted for approval."
                            : "Only proposed changes are shown."}
                    </p>
                </div>

                <span className="rounded-full bg-orange-50 px-3 py-1 text-xs font-semibold text-orange-700">
                    {visibleFields.length} fields
                </span>
            </div>

            {visibleFields.length === 0 ? (
                <p className="px-4 py-5 text-sm text-slate-600">
                    There are no changes to the main product fields.
                </p>
            ) : (
                <div className="overflow-x-auto">
                    <table className="w-full min-w-[580px] text-left text-sm">
                        <thead className="bg-slate-50 text-slate-600">
                            <tr>
                                <th className="px-4 py-3 font-semibold">
                                    Information
                                </th>

                                {!isCreate && (
                                    <th className="px-4 py-3 font-semibold">
                                        Current
                                    </th>
                                )}

                                <th className="px-4 py-3 font-semibold">
                                    Proposed
                                </th>
                            </tr>
                        </thead>

                        <tbody>
                            {visibleFields.map((field) => (
                                <tr
                                    key={field.key}
                                    className="border-t border-slate-100 align-top"
                                >
                                    <td className="px-4 py-3 font-medium text-slate-700">
                                        {field.label}
                                    </td>

                                    {!isCreate && (
                                        <td className="break-words px-4 py-3 text-slate-600">
                                            {comparisonValue(
                                                oldData[field.key],
                                                field,
                                                oldData
                                            )}
                                        </td>
                                    )}

                                    <td className="break-words px-4 py-3 font-medium text-orange-700">
                                        {comparisonValue(
                                            newData[field.key],
                                            field,
                                            newData
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </section>
    );
}

// =====================================================
// REJECTED PRODUCTS PAGE
// Giữ nguyên các chức năng hiện tại.
// =====================================================

export default function RejectedProductsPage() {
    const { user } = useAuth();

    const [page, setPage] = useState(0);
    const [showClosed, setShowClosed] = useState(false);
    const [reload, setReload] = useState(0);

    const [data, setData] = useState({
        items: [],
        totalPages: 0,
        totalElements: 0,
    });

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");

    const [detail, setDetail] = useState(null);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailError, setDetailError] = useState("");

    const [editing, setEditing] = useState(false);
    const [form, setForm] = useState({ ...EMPTY_FORM });
    const [categories, setCategories] = useState([]);
    const [formError, setFormError] = useState("");
    const [confirmed, setConfirmed] = useState(false);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError("");

        listRejectedProductRequests(
            {
                page,
                size: PAGE_SIZE,
                closed: showClosed,
            },
            controller.signal
        )
            .then((result) => {
                if (controller.signal.aborted) return;

                if (!Array.isArray(result?.items)) {
                    throw new Error(
                        "Unable to read rejected requests."
                    );
                }

                setData(result);
            })
            .catch((requestError) => {
                if (controller.signal.aborted) return;

                setError(
                    productErrorMessage(
                        requestError,
                        "Unable to load rejected requests."
                    )
                );

                setData({
                    items: [],
                    totalPages: 0,
                    totalElements: 0,
                });
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [page, reload, showClosed]);

    const openDetail = async (row) => {
        if (detailLoading || saving) return;

        setDetail(null);
        setDetailLoading(true);
        setDetailError("");
        setEditing(false);
        setFormError("");
        setConfirmed(false);
        setNotice("");

        try {
            const request = await getRejectedProductRequest(
                row.id
            );

            // Nếu CREATE bị từ chối, có thể không đọc được
            // sản phẩm trực tiếp. Dùng snapshot để điền form.
            const [product, options] = await Promise.all([
                request.productId
                    ? getProduct(request.productId).catch(
                          () => null
                      )
                    : Promise.resolve(null),

                getCategoryOptions().catch(() => []),
            ]);

            setCategories(
                Array.isArray(options) ? options : []
            );

            setForm(buildForm(request, product));
            setDetail(request);
        } catch (requestError) {
            setDetailError(
                productErrorMessage(
                    requestError,
                    "Unable to load request details."
                )
            );
        } finally {
            setDetailLoading(false);
        }
    };

    const closeRequest = async () => {
        if (!detail || saving || showClosed) return;

        if (
            !window.confirm(
                "Stop processing this request? Its rejection history will be retained."
            )
        ) {
            return;
        }

        setSaving(true);
        setDetailError("");

        try {
            await closeRejectedProductRequest(detail.id);

            setNotice(
                "Request closed. Approval history has been retained."
            );

            setDetail(null);
            setEditing(false);
            setPage(0);
            setReload((current) => current + 1);
        } catch (requestError) {
            setDetailError(
                productErrorMessage(
                    requestError,
                    "Unable to close the request. Please try again."
                )
            );
        } finally {
            setSaving(false);
        }
    };

    const changeForm = (field, value) => {
        setForm((current) => ({
            ...current,
            [field]: value,
        }));

        setConfirmed(false);
        setFormError("");
    };

    const submitResubmission = async (event) => {
        event.preventDefault();

        if (
            !detail ||
            saving ||
            !confirmed ||
            detail.status !== "REJECTED" ||
            detail.closedAt
        ) {
            return;
        }

        const validationError = validate(form);

        if (validationError) {
            setFormError(validationError);
            return;
        }

        setSaving(true);
        setFormError("");

        try {
            const response =
                await resubmitRejectedProductRequest(
                    detail.id,
                    toPayload(form)
                );

            setNotice(
                `Approval request resubmitted successfully${
                    response?.requestCode
                        ? ` (${response.requestCode})`
                        : ""
                }.`
            );

            setDetail(null);
            setEditing(false);
            setConfirmed(false);
            setPage(0);
            setReload((value) => value + 1);
        } catch (requestError) {
            setFormError(
                productErrorMessage(
                    requestError,
                    "Unable to resubmit the approval request."
                )
            );
        } finally {
            setSaving(false);
        }
    };

    const rows = data.items || [];
    const totalPages = Number(data.totalPages) || 0;

    return (
        <MainLayout
            title="Business Operations"
            items={businessStaffMenuItems}
        >
            <div className="space-y-6">
                <PageHeader
                    title="Rejected Products"
                    description="Review rejection reasons, resubmit if needed, or select Do Not Continue. History is retained."
                />

                {notice && (
                    <div role="status" className="space-y-2">
                        <Alert type="success">
                            {notice}
                        </Alert>

                        <Link
                            className="text-sm font-medium text-orange-700 underline"
                            to="/business/products/requests"
                        >
                            View My Request History
                        </Link>
                    </div>
                )}

                {error && (
                    <div role="alert">
                        <Alert type="danger">
                            {error}
                        </Alert>
                    </div>
                )}

                <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                    <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                        <p className="text-sm text-slate-600">
                            {showClosed
                                ? "Closed:"
                                : "Requires Action:"}{" "}
                            <strong>
                                {data.totalElements ?? 0}
                            </strong>{" "}
                            requests.
                        </p>

                        <div className="flex flex-wrap items-center gap-2">
                            <Button
                                variant={
                                    !showClosed
                                        ? "primary"
                                        : "outline"
                                }
                                onClick={() => {
                                    setShowClosed(false);
                                    setPage(0);
                                    setDetail(null);
                                }}
                            >
                                Requires Action
                            </Button>

                            <Button
                                variant={
                                    showClosed
                                        ? "primary"
                                        : "outline"
                                }
                                onClick={() => {
                                    setShowClosed(true);
                                    setPage(0);
                                    setDetail(null);
                                }}
                            >
                                Closed
                            </Button>

                            <Button
                                variant="outline"
                                onClick={() =>
                                    setReload(
                                        (current) => current + 1
                                    )
                                }
                            >
                                Refresh
                            </Button>
                        </div>
                    </div>

                    {loading ? (
                        <div className="flex items-center gap-3 text-sm text-slate-600">
                            <Spinner />
                            Loading rejected products...
                        </div>
                    ) : !error && rows.length === 0 ? (
                        <EmptyState
                            title={
                                showClosed
                                    ? "No Closed Requests"
                                    : "No Rejected Requests Require Action"
                            }
                            description={
                                showClosed
                                    ? "No requests have been marked Do Not Continue."
                                    : "There are no requests requiring review."
                            }
                        />
                    ) : !error ? (
                        <div className="overflow-x-auto">
                            <table className="w-full min-w-[700px] border-collapse text-left text-sm">
                                <thead className="bg-slate-50 text-slate-600">
                                    <tr>
                                        <th className="px-3 py-3">
                                            Request Code
                                        </th>
                                        <th className="px-3 py-3">
                                            Products
                                        </th>
                                        <th className="px-3 py-3">
                                            Type
                                        </th>
                                        <th className="px-3 py-3">
                                            Rejection Reason
                                        </th>
                                        <th className="px-3 py-3">
                                            Processed At
                                        </th>
                                        <th className="px-3 py-3">
                                            Actions
                                        </th>
                                    </tr>
                                </thead>

                                <tbody>
                                    {rows.map((row) => (
                                        <tr
                                            key={row.id}
                                            className="border-t border-slate-100 align-top"
                                        >
                                            <td className="px-3 py-3 font-medium">
                                                {row.requestCode ||
                                                    `#${row.id}`}
                                            </td>

                                            <td className="px-3 py-3">
                                                <span className="font-medium">
                                                    {row.productName ||
                                                        "—"}
                                                </span>

                                                <span className="mt-1 block text-xs text-slate-500">
                                                    {row.productCode ||
                                                        "—"}
                                                </span>
                                            </td>

                                            <td className="px-3 py-3">
                                                {row.requestType ||
                                                    "—"}
                                            </td>

                                            <td className="max-w-64 whitespace-normal px-3 py-3 text-red-700">
                                                {row.rejectionReason ||
                                                    "No reason provided"}
                                            </td>

                                            <td className="px-3 py-3">
                                                {formatDate(
                                                    row.approvedAt ||
                                                        row.createdAt
                                                )}
                                            </td>

                                            <td className="px-3 py-3">
                                                <Button
                                                    variant="outline"
                                                    size="sm"
                                                    onClick={() =>
                                                        openDetail(row)
                                                    }
                                                >
                                                    {showClosed
                                                        ? "View History"
                                                        : "View / Resubmit"}
                                                </Button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    ) : null}

                    {!loading &&
                        !error &&
                        totalPages > 1 && (
                            <div className="mt-5 flex items-center justify-end gap-3">
                                <Button
                                    variant="outline"
                                    disabled={page === 0}
                                    onClick={() =>
                                        setPage((p) => p - 1)
                                    }
                                >
                                    Previous
                                </Button>

                                <span className="text-sm text-slate-600">
                                    Trang {page + 1}/{totalPages}
                                </span>

                                <Button
                                    variant="outline"
                                    disabled={
                                        page + 1 >= totalPages
                                    }
                                    onClick={() =>
                                        setPage((p) => p + 1)
                                    }
                                >
                                    Sau
                                </Button>
                            </div>
                        )}
                </div>

                {detailLoading && (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-5 text-sm">
                        <Spinner />
                        Loading request details...
                    </div>
                )}

                {detailError && (
                    <div role="alert">
                        <Alert type="danger">
                            {detailError}
                        </Alert>
                    </div>
                )}

                {detail && (
                    <section className="space-y-5 rounded-2xl border border-orange-200 bg-white p-5 shadow-sm">
                        <div className="flex flex-wrap items-start justify-between gap-3">
                            <div>
                                <h2 className="text-xl font-semibold text-slate-900">
                                    Request Details{" "}
                                    {detail.requestCode ||
                                        `#${detail.id}`}
                                </h2>

                                <p className="mt-1 text-sm text-slate-500">
                                    Product:{" "}
                                    {detail.productName || "—"} ·{" "}
                                    {detail.productCode || "—"}
                                </p>
                            </div>

                            <Button
                                variant="ghost"
                                disabled={saving}
                                onClick={() => {
                                    setDetail(null);
                                    setEditing(false);
                                }}
                            >
                                Close
                            </Button>
                        </div>

                        <dl className="grid gap-3 text-sm sm:grid-cols-2">
                            <div>
                                <dt className="text-slate-500">
                                    Status
                                </dt>
                                <dd className="font-medium">
                                    {detail.status ? formatBusinessStatus(detail.status) : "—"}
                                </dd>
                            </div>

                            <div>
                                <dt className="text-slate-500">
                                    Request Type
                                </dt>
                                <dd className="font-medium">
                                    {detail.requestType || "—"}
                                </dd>
                            </div>

                            <div>
                                <dt className="text-slate-500">
                                    Rejected By
                                </dt>
                                <dd className="font-medium">
                                    {detail.approvedByName || "—"}
                                </dd>
                            </div>

                            <div>
                                <dt className="text-slate-500">
                                    Rejected At
                                </dt>
                                <dd className="font-medium">
                                    {formatDate(
                                        detail.approvedAt
                                    )}
                                </dd>
                            </div>
                        </dl>

                        <div className="rounded-xl border border-red-200 bg-red-50 p-4">
                            <p className="text-sm font-semibold text-red-800">
                                Business Manager's Rejection Reason
                            </p>

                            <p className="mt-1 whitespace-pre-wrap text-sm text-red-800">
                                {detail.rejectionReason ||
                                    "No rejection reason provided."}
                            </p>
                        </div>

                        <div className="text-sm text-slate-600">
                            <strong>
                                Previous Submission Reason:
                            </strong>{" "}
                            {detail.requestReason || "—"}
                        </div>

                        {/* Hiển thị bảng thay vì dữ liệu JSON */}
                        <ProductChangeComparison request={detail} />

                        {detail.status === "REJECTED" &&
                            !editing &&
                            !detail.closedAt &&
                            !showClosed && (
                                <div className="flex flex-wrap gap-2">
                                    <Button
                                        onClick={() => {
                                            setEditing(true);
                                            setFormError("");
                                            setConfirmed(false);
                                        }}
                                    >
                                        Edit and Resubmit for Approval
                                    </Button>

                                    {String(
                                        detail.createdById
                                    ) ===
                                        String(user?.id) && (
                                        <Button
                                            variant="outline"
                                            disabled={saving}
                                            onClick={closeRequest}
                                        >
                                            Do Not Continue
                                        </Button>
                                    )}
                                </div>
                            )}

                        {detail.closedAt && (
                            <p className="text-sm text-slate-600">
                                Tracking closed at{" "}
                                {formatDate(detail.closedAt)}. The request remains recorded as rejected.
                            </p>
                        )}

                        {editing &&
                            detail.status === "REJECTED" &&
                            !detail.closedAt && (
                                <form
                                    onSubmit={submitResubmission}
                                    className="space-y-4 border-t border-slate-200 pt-5"
                                >
                                    <h3 className="text-lg font-semibold">
                                        Revise details based on the feedback
                                    </h3>

                                    {formError && (
                                        <div role="alert">
                                            <Alert type="danger">
                                                {formError}
                                            </Alert>
                                        </div>
                                    )}

                                    <div className="grid gap-4 sm:grid-cols-2">
                                        <FormField
                                            label="Product Code"
                                            required
                                            maxLength={50}
                                            value={form.productCode}
                                            onChange={(v) =>
                                                changeForm(
                                                    "productCode",
                                                    v
                                                )
                                            }
                                        />

                                        <FormField
                                            label="Barcode"
                                            required
                                            maxLength={100}
                                            value={form.barcode}
                                            onChange={(v) =>
                                                changeForm(
                                                    "barcode",
                                                    v
                                                )
                                            }
                                        />

                                        <FormField
                                            label="Product Name"
                                            required
                                            maxLength={200}
                                            value={form.name}
                                            onChange={(v) =>
                                                changeForm(
                                                    "name",
                                                    v
                                                )
                                            }
                                        />

                                        <FormField
                                            label="Unit of Measure"
                                            required
                                            maxLength={30}
                                            value={form.baseUnit}
                                            onChange={(v) =>
                                                changeForm(
                                                    "baseUnit",
                                                    v
                                                )
                                            }
                                        />

                                        <div>
                                            <FormField
                                                label="Category ID"
                                                required
                                                inputMode="numeric"
                                                list="issue4-category-options"
                                                value={form.categoryId}
                                                onChange={(v) =>
                                                    changeForm(
                                                        "categoryId",
                                                        v
                                                    )
                                                }
                                            />

                                            <datalist id="issue4-category-options">
                                                {categories
                                                    .filter(
                                                        (row) =>
                                                            row.status ===
                                                            "ACTIVE"
                                                    )
                                                    .map((row) => (
                                                        <option
                                                            key={row.id}
                                                            value={String(
                                                                row.id
                                                            )}
                                                        >
                                                            {row.name}
                                                        </option>
                                                    ))}
                                            </datalist>

                                            <p className="mt-1 text-xs text-slate-500">
                                                Select an active category.
                                            </p>
                                        </div>

                                        <FormField
                                            label="Brand ID (optional)"
                                            inputMode="numeric"
                                            value={form.brandId}
                                            onChange={(v) =>
                                                changeForm(
                                                    "brandId",
                                                    v
                                                )
                                            }
                                        />

                                        <FormField
                                            label="Purchase Price"
                                            required
                                            type="number"
                                            min="0.01"
                                            step="0.01"
                                            value={form.purchasePrice}
                                            onChange={(v) =>
                                                changeForm(
                                                    "purchasePrice",
                                                    v
                                                )
                                            }
                                        />

                                        <FormField
                                            label="Selling Price"
                                            required
                                            type="number"
                                            min="0.01"
                                            step="0.01"
                                            value={form.sellingPrice}
                                            onChange={(v) =>
                                                changeForm(
                                                    "sellingPrice",
                                                    v
                                                )
                                            }
                                        />
                                    </div>

                                    <FormField
                                        label="Image URL (optional)"
                                        maxLength={500}
                                        value={form.imageUrl}
                                        onChange={(v) =>
                                            changeForm(
                                                "imageUrl",
                                                v
                                            )
                                        }
                                    />

                                    <label className="block text-sm font-medium text-slate-700">
                                        Resubmission Notes for Business Manager

                                        <textarea
                                            className={INPUT_CLASS}
                                            rows={3}
                                            maxLength={500}
                                            value={form.requestReason}
                                            onChange={(event) =>
                                                changeForm(
                                                    "requestReason",
                                                    event.target.value
                                                )
                                            }
                                            placeholder="I have revised the information based on the feedback..."
                                        />
                                    </label>

                                    <label className="flex items-start gap-2 text-sm text-slate-700">
                                        <input
                                            type="checkbox"
                                            className="mt-1"
                                            checked={confirmed}
                                            onChange={(event) =>
                                                setConfirmed(
                                                    event.target.checked
                                                )
                                            }
                                        />

                                        <span>
                                            I confirm the revised information and agree to resubmit the request for approval.
                                        </span>
                                    </label>

                                    <div className="flex flex-wrap gap-2">
                                        <button
                                            type="submit"
                                            disabled={
                                                saving || !confirmed
                                            }
                                            className="rounded-xl bg-orange-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-orange-700 disabled:cursor-not-allowed disabled:opacity-50"
                                        >
                                            {saving
                                                ? "Submitting..."
                                                : "Resubmit for Approval"}
                                        </button>

                                        <button
                                            type="button"
                                            disabled={saving}
                                            onClick={() => {
                                                setEditing(false);
                                                setFormError("");
                                                setConfirmed(false);
                                            }}
                                            className="rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-700 disabled:opacity-50"
                                        >
                                            Cancel Editing
                                        </button>
                                    </div>
                                </form>
                            )}
                    </section>
                )}
            </div>
        </MainLayout>
    );
}
