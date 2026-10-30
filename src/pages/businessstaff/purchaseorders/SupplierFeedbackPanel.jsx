
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

// Issue #16 - Supplier Feedback.
// Giai doan 1: Nhap va xem truoc du lieu.
// KHONG goi PUT/POST, KHONG ghi database.


const SUPPLIER_FEEDBACK_WRITE_ENABLED =
    import.meta.env.VITE_ENABLE_SUPPLIER_FEEDBACK_WRITE === "true";
const ORDER_STATUSES = [
    { value: "", label: "-- Chọn trạng thái --" },
    { value: "PENDING_RESPONSE", label: "Đang chờ phản hồi" },
    { value: "AVAILABLE", label: "Có đủ hàng" },
    { value: "PARTIALLY_AVAILABLE", label: "Có một phần hàng" },
    { value: "OUT_OF_STOCK", label: "Hết hàng" },
    { value: "DISCONTINUED", label: "Ngừng cung cấp" },
    { value: "PRICE_CHANGED", label: "Thay đổi giá" },
    { value: "DELAYED", label: "Giao hàng trễ" },
    { value: "DECLINED", label: "Từ chối đơn hàng" },
];

const ITEM_STATUSES = [
    { value: "PENDING", label: "Đang chờ" },
    { value: "AVAILABLE", label: "Có hàng" },
    { value: "PARTIALLY_AVAILABLE", label: "Có một phần" },
    { value: "OUT_OF_STOCK", label: "Hết hàng" },
    { value: "DISCONTINUED", label: "Ngừng cung cấp" },
    { value: "PRICE_CHANGED", label: "Thay đổi giá" },
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

function validateAndBuild(rows, orderStatus, orderNote) {
    const errors = [];

    if (!orderStatus) {
        errors.push("Chưa chọn trạng thái phản hồi toàn đơn.");
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
                `${name}: số lượng xác nhận phải từ ${received} đến ${ordered}.`
            );
        }

        if (
            price !== null &&
            (!Number.isFinite(price) || price <= 0)
        ) {
            errors.push(
                `${name}: giá xác nhận phải lớn hơn 0, tối đa 2 chữ số thập phân.`
            );
        }

        if (
            row.status === "AVAILABLE" &&
            quantity !== ordered
        ) {
            errors.push(
                `${name}: trạng thái Có hàng yêu cầu xác nhận đủ ${ordered}.`
            );
        }

        if (
            row.status === "PARTIALLY_AVAILABLE" &&
            (quantity === null || quantity <= 0 ||
                quantity >= ordered)
        ) {
            errors.push(
                `${name}: trạng thái Có một phần cần số lượng lớn hơn 0 và nhỏ hơn số lượng đặt.`
            );
        }

        if (
            ["OUT_OF_STOCK", "DISCONTINUED"].includes(row.status) &&
            quantity !== received
        ) {
            errors.push(
                `${name}: số lượng xác nhận phải bằng số đã nhận (${received}).`
            );
        }

        if (
            row.status === "PRICE_CHANGED" &&
            (price === null || !Number.isFinite(price) ||
                price === originalPrice)
        ) {
            errors.push(
                `${name}: khi thay đổi giá, giá mới phải khác giá hiện tại.`
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
                "Có một phần hàng yêu cầu ít nhất một sản phẩm thiếu/hết/ngừng cung cấp và ít nhất một sản phẩm có số lượng xác nhận lớn hơn 0."
            );
        }
    }


    if (
        orderStatus === "PENDING_RESPONSE" &&
        statuses.some((status) => status !== "PENDING")
    ) {
        errors.push(
            "Đang chờ phản hồi yêu cầu tất cả sản phẩm ở trạng thái Đang chờ."
        );
    }

    if (
        orderStatus === "AVAILABLE" &&
        statuses.some((status) => status !== "AVAILABLE")
    ) {
        errors.push(
            "Có đủ hàng yêu cầu tất cả sản phẩm ở trạng thái Có hàng."
        );
    }

    if (
        orderStatus === "OUT_OF_STOCK" &&
        statuses.some((status) => status !== "OUT_OF_STOCK")
    ) {
        errors.push(
            "Hết hàng yêu cầu tất cả sản phẩm ở trạng thái Hết hàng."
        );
    }

    if (
        orderStatus === "DISCONTINUED" &&
        !statuses.includes("DISCONTINUED")
    ) {
        errors.push(
            "Ngừng cung cấp cần ít nhất một sản phẩm ngừng cung cấp."
        );
    }

    if (
        orderStatus === "PRICE_CHANGED" &&
        !statuses.includes("PRICE_CHANGED")
    ) {
        errors.push(
            "Thay đổi giá cần ít nhất một sản phẩm có giá thay đổi."
        );
    }

    return {
        errors,
        payload: {
            supplierResponseStatus: orderStatus,
            supplierResponseNote: orderNote.trim(),
            items,
        },
    };
}

export default function SupplierFeedbackPanel({ purchaseOrder }) {
    const [rows, setRows] = useState([]);
    const [orderStatus, setOrderStatus] = useState("");
    const [orderNote, setOrderNote] = useState("");

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
                        "Dữ liệu phản hồi không hợp lệ."
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
                        "Dữ liệu sản phẩm PO và phản hồi không khớp."
                    );
                }

                // Khoi phuc trang thai va ghi chu toan don.
                setOrderStatus(
                    data.supplierResponseStatus ?? ""
                );

                setOrderNote(
                    data.supplierResponseNote ?? ""
                );

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
                              "Không tải được phản hồi sản phẩm."
                          )
                        : error?.message ||
                          "Không tải được phản hồi sản phẩm."
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
            orderNote
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
            orderNote
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
                "Dữ liệu đã thay đổi. Vui lòng kiểm tra và xác nhận lại."
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
                "Đã ghi nhận phản hồi nhà cung cấp thành công."
            );

            // Doc lai du lieu moi nhat tu Backend.
            setReload((value) => value + 1);
        } catch (error) {
            setSaveError(
                supplierFeedbackErrorMessage(
                    error,
                    "Không lưu được phản hồi nhà cung cấp."
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
                    Phản hồi nhà cung cấp
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
                        Chức năng ghi nhận phản hồi chỉ áp dụng
                        cho Purchase Order ở trạng thái ORDERED
                        hoặc PARTIALLY_RECEIVED.
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
                            Ghi nhận phản hồi nhà cung cấp
                        </h2>

                        <p className="mt-1 text-sm text-slate-500">
                            Issue #16 — Nhập phản hồi theo từng
                            sản phẩm trong Purchase Order.
                        </p>
                    </div>

                    <Button
                        type="button"
                        variant="ghost"
                        disabled={loading}
                        onClick={() => setReload((value) => value + 1)}
                    >
                        Tải lại
                    </Button>
                </div>



                <Alert type="info">
                    {SUPPLIER_FEEDBACK_WRITE_ENABLED
                        ? "Dữ liệu phản hồi được tải từ Backend. Khi nhấn Xác nhận lưu, thông tin phản hồi sẽ được cập nhật vào MySQL chung của nhóm."
                        : "Dữ liệu phản hồi được tải từ Backend. Chế độ hiện tại chỉ cho phép chỉnh sửa và xem trước; chức năng xác nhận lưu đang bị khóa, không ghi dữ liệu mới vào MySQL."}
                </Alert>



                <p className="text-xs text-slate-500">
                    Thông tin phản hồi đã lưu được tải trực tiếp
                    từ Backend. Người dùng có thể kiểm tra
                    trạng thái, số lượng, giá và ghi chú trước đó.
                    Mọi thay đổi hiện chỉ được xem trước,
                    chưa gửi lên hệ thống.
                </p>


                {loading ? (
                    <div className="flex items-center gap-3 py-5">
                        <Spinner />
                        <span className="text-sm text-slate-600">
                            Đang tải dữ liệu phản hồi...
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
                                label="Trạng thái phản hồi toàn đơn *"
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
                                    ĐƠN MUA HÀNG
                                </p>
                                <strong>
                                    {purchaseOrder.orderCode}
                                </strong>
                                <p className="mt-1">
                                    {purchaseOrder.supplierName}
                                </p>
                            </div>
                        </div>

                        <TextArea
                            label="Ghi chú phản hồi toàn đơn"
                            rows={3}
                            value={orderNote}
                            onChange={(event) => {
                                setOrderNote(event.target.value);
                                setPreview(null);
                            }}
                            placeholder="Ví dụ: NCC xác nhận giao hàng theo lịch..."
                        />

                        <div className="space-y-4">
                            <h3 className="font-semibold text-slate-900">
                                Phản hồi từng sản phẩm
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
                                            {row.productCode} |
                                            PO Item ID: {row.id}
                                        </p>

                                        <p className="mt-2 text-sm text-slate-600">
                                            Đặt: {row.orderedQuantity} |
                                            Đã nhận: {row.receivedQuantity} |
                                            Giá PO: {Number(
                                                row.originalUnitPrice
                                            ).toLocaleString("vi-VN")} đ
                                        </p>
                                    </div>

                                    <div className="grid gap-4 md:grid-cols-3">
                                        <Select
                                            label="Trạng thái sản phẩm *"
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
                                            label="SL xác nhận"
                                            value={row.confirmedQuantity}
                                            onChange={(event) =>
                                                updateRow(
                                                    index,
                                                    "confirmedQuantity",
                                                    event.target.value
                                                )
                                            }
                                            placeholder="Nhập số lượng"
                                        />

                                        <Input
                                            type="number"
                                            min="0.01"
                                            step="0.01"
                                            label="Giá NCC báo (VNĐ)"
                                            value={row.confirmedUnitPrice}
                                            onChange={(event) =>
                                                updateRow(
                                                    index,
                                                    "confirmedUnitPrice",
                                                    event.target.value
                                                )
                                            }
                                            placeholder="Nhập giá mới"
                                        />
                                    </div>

                                    <TextArea
                                        label="Ghi chú sản phẩm"
                                        rows={2}
                                        value={row.note}
                                        onChange={(event) =>
                                            updateRow(
                                                index,
                                                "note",
                                                event.target.value
                                            )
                                        }
                                        placeholder="Thông tin NCC phản hồi về sản phẩm..."
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
                                Lưu phản hồi
                            </Button>
                        </div>
                  </>
             )}


            <Modal
                open={confirmOpen && Boolean(preview)}
                title="Xác nhận phản hồi nhà cung cấp"
                onClose={() => setConfirmOpen(false)}
                footer={
                    <div className="flex justify-end gap-3">
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => setConfirmOpen(false)}
                        >
                            Đóng
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
                                ? "Xác nhận lưu (chưa kích hoạt)"
                                : saving
                                ? "Đang lưu..."
                                : "Xác nhận lưu"}
                        </Button>

                    </div>
                }
            >
                <div className="space-y-4">
                    <Alert type="info">
                        {SUPPLIER_FEEDBACK_WRITE_ENABLED
                            ? "Vui lòng kiểm tra kỹ thông tin phản hồi. Khi nhấn Xác nhận lưu, dữ liệu sẽ được cập nhật vào hệ thống."
                            : "Đây là bước xem trước xác nhận. Chức năng ghi dữ liệu chưa được kích hoạt."}
                    </Alert>


                    <p className="text-sm text-slate-700">
                        Đơn mua hàng:{" "}
                        <strong>{purchaseOrder.orderCode}</strong>
                    </p>

                    <p className="text-sm text-slate-700">
                        Nhà cung cấp:{" "}
                        <strong>{purchaseOrder.supplierName}</strong>
                    </p>

                    <p className="text-sm text-slate-700">
                        Số sản phẩm phản hồi:{" "}
                        <strong>{preview?.items?.length ?? 0}</strong>
                    </p>


                    <div className="max-h-72 space-y-4 overflow-y-auto rounded-xl bg-slate-50 p-4">
                        <div className="space-y-2 text-sm">
                            <p>
                                Trạng thái phản hồi:{" "}
                                <strong>
                                    {ORDER_STATUSES.find(
                                        (s) => s.value === preview?.supplierResponseStatus
                                    )?.label || "—"}
                                </strong>
                            </p>

                            <p>
                                Ghi chú toàn đơn:{" "}
                                {preview?.supplierResponseNote || "Không có"}
                            </p>
                        </div>

                        <div className="border-t border-slate-200 pt-3">
                            <h4 className="mb-3 font-semibold text-slate-900">
                                Chi tiết sản phẩm
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
                                                {product?.productName || "Sản phẩm"}
                                            </p>

                                            <p>
                                                Trạng thái:{" "}
                                                {ITEM_STATUSES.find(
                                                    (s) =>
                                                        s.value ===
                                                        item.supplierResponseStatus
                                                )?.label || "—"}
                                            </p>

                                            <p>
                                                Số lượng xác nhận:{" "}
                                                {item.confirmedQuantity ?? "—"}
                                            </p>

                                            <p>
                                                Giá PO ban đầu:{" "}
                                                {Number(
                                                    product?.originalUnitPrice ?? 0
                                                ).toLocaleString("vi-VN")} đ
                                            </p>

                                            <p>
                                                Giá NCC báo:{" "}
                                                {item.confirmedUnitPrice == null
                                                    ? "Chưa báo giá"
                                                    : `${Number(
                                                        item.confirmedUnitPrice
                                                    ).toLocaleString("vi-VN")} đ`}
                                            </p>

                                            <p>
                                                Ghi chú:{" "}
                                                {item.supplierResponseNote || "Không có"}
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
