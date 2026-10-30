
import { useEffect, useState } from "react";

import {
    getSupplierPriceComparison,
    decideSupplierPrice,
    supplierFeedbackErrorMessage,
} from "../../../api/business/supplierFeedback";


import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import EmptyState from "../../../components/ui/EmptyState";
import Spinner from "../../../components/ui/Spinner";
import Table from "../../../components/ui/Table";
import Modal from "../../../components/ui/Modal";
import TextArea from "../../../components/ui/TextArea";


// Issue #16 - Supplier Price Comparison.
// Chi doc du lieu, khong cap nhat gia hoac Purchase Order.

const PRICE_DECISION_WRITE_ENABLED =
    import.meta.env.VITE_ENABLE_SUPPLIER_PRICE_DECISION_WRITE === "true";
const PRICE_CHANGE_LABELS = {
    INCREASED: "Tăng giá",
    DECREASED: "Giảm giá",
    UNCHANGED: "Không thay đổi",
    NOT_QUOTED: "Chưa báo giá",
};

const PRICE_DECISION_UI_PREVIEW =
    import.meta.env.DEV &&
    import.meta.env.VITE_PREVIEW_SUPPLIER_PRICE_DECISION === "true";

const MOCK_PRICE_DECISION_ITEM = {
    purchaseOrderItemId: -1,
    productName: "Sản phẩm giả lập - chỉ kiểm thử giao diện",
    originalUnitPrice: 120000,
    confirmedUnitPrice: 130000,
};


const RESPONSE_STATUS_LABELS = {
    PENDING: "Đang chờ",
    AVAILABLE: "Có hàng",
    PARTIALLY_AVAILABLE: "Có một phần",
    OUT_OF_STOCK: "Hết hàng",
    DISCONTINUED: "Ngừng sản xuất",
    PRICE_CHANGED: "Thay đổi giá",
};

function formatNumber(value) {
    if (value == null || value === "") {
        return "—";
    }

    const number = Number(value);

    return Number.isFinite(number)
        ? number.toLocaleString("vi-VN")
        : String(value);
}

function formatMoney(value, signed = false) {
    if (value == null || value === "") {
        return "—";
    }

    const number = Number(value);

    if (!Number.isFinite(number)) {
        return String(value);
    }

    const prefix = signed && number > 0 ? "+" : "";

    return `${prefix}${number.toLocaleString("vi-VN", {
        maximumFractionDigits: 2,
    })} đ`;
}

function formatPercent(value) {
    if (value == null || value === "") {
        return "—";
    }

    const number = Number(value);

    if (!Number.isFinite(number)) {
        return String(value);
    }

    const prefix = number > 0 ? "+" : "";

    return `${prefix}${number.toLocaleString("vi-VN", {
        maximumFractionDigits: 2,
    })}%`;
}

function PriceChangeBadge({ value }) {
    const colors = {
        INCREASED: "bg-red-100 text-red-700",
        DECREASED: "bg-green-100 text-green-700",
        UNCHANGED: "bg-slate-100 text-slate-700",
        NOT_QUOTED: "bg-amber-100 text-amber-800",
    };

    return (
        <span
            className={`inline-block rounded-full px-3 py-1 text-xs font-semibold ${
                colors[value] || "bg-slate-100 text-slate-700"
            }`}
        >
            {PRICE_CHANGE_LABELS[value] || value || "—"}
        </span>
    );
}

const columns = [
    {
        key: "productName",
        label: "Sản phẩm",
        render: (value, item) => (
            <div className="space-y-1">
                <p className="font-semibold text-slate-800">
                    {value || "—"}
                </p>
                <p className="text-xs text-slate-500">
                    Product ID: {item.productId ?? "—"}
                </p>
            </div>
        ),
    },
    {
        key: "supplierResponseStatus",
        label: "Phản hồi NCC",
        render: (value) =>
            RESPONSE_STATUS_LABELS[value] || value || "—",
    },
    {
        key: "orderedQuantity",
        label: "SL đặt",
        render: formatNumber,
    },
    {
        key: "receivedQuantity",
        label: "SL đã nhận",
        render: formatNumber,
    },
    {
        key: "confirmedQuantity",
        label: "SL xác nhận",
        render: formatNumber,
    },
    {
        key: "originalUnitPrice",
        label: "Giá ban đầu",
        render: (value) => formatMoney(value),
    },
    {
        key: "confirmedUnitPrice",
        label: "Giá NCC báo",
        render: (value) => formatMoney(value),
    },
    {
        key: "differencePerUnit",
        label: "Chênh lệch / đơn vị",
        render: (value) => formatMoney(value, true),
    },
    {
        key: "differencePercent",
        label: "Tỷ lệ chênh lệch",
        render: formatPercent,
    },
    {
        key: "estimatedOrderDifference",
        label: "Chênh lệch ước tính",
        render: (value) => formatMoney(value, true),
    },
    {
        key: "priceChange",
        label: "Biến động",
        render: (value) => <PriceChangeBadge value={value} />,
    },
];

export default function SupplierPriceComparisonPanel({
    purchaseOrderId,
    purchaseOrderStatus,
}) {
    const [comparison, setComparison] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [reload, setReload] = useState(0);
    const [decisionDraft, setDecisionDraft] = useState(null);
    const [decisionReason, setDecisionReason] = useState("");
    const [decisionSubmitting, setDecisionSubmitting] = useState(false);
    const [decisionError, setDecisionError] = useState("");
    const [decisionSuccess, setDecisionSuccess] = useState("");

    useEffect(() => {
        const controller = new AbortController();
        const numericId = Number(purchaseOrderId);

        setComparison(null);
        setError("");
        setLoading(true);

        if (
            !Number.isSafeInteger(numericId) ||
            numericId <= 0
        ) {
            setError("ID đơn mua hàng không hợp lệ.");
            setLoading(false);
            return () => controller.abort();
        }

        getSupplierPriceComparison(
            numericId,
            controller.signal
        )
            .then((data) => {
                if (controller.signal.aborted) return;

                if (!Array.isArray(data?.items)) {
                    throw new Error(
                        "Dữ liệu so sánh giá không hợp lệ."
                    );
                }

                setComparison(data);
            })
            .catch((requestError) => {
                if (controller.signal.aborted) return;

                const message =
                    requestError?.isAxiosError
                        ? supplierFeedbackErrorMessage(
                              requestError,
                              "Không tải được bảng so sánh giá."
                          )
                        : requestError?.message ||
                          "Không tải được bảng so sánh giá.";

                setError(message);
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [purchaseOrderId, reload]);

    const items = comparison?.items || [];
    const hasReceivedGoods = items.some((item) => {
    if (item.receivedQuantity == null) {
        return false;
    }

    const received = Number(item.receivedQuantity);

    return Number.isSafeInteger(received) && received > 0;
    });
    const hasNoReceivedItems =
        items.length > 0 &&
        items.every((item) => {
            if (item.receivedQuantity == null) {
                return false;
            }

            const received = Number(item.receivedQuantity);

            return Number.isSafeInteger(received) && received === 0;
        });

    const orderEligibleForPriceDecision =
        purchaseOrderStatus === "ORDERED" &&
        hasNoReceivedItems;

    function openDecisionPreview(item, decision) {
        const originalPrice = Number(item?.originalUnitPrice);
        const quotedPrice = Number(item?.confirmedUnitPrice);

        const validPrice =
            item?.originalUnitPrice != null &&
            item?.confirmedUnitPrice != null &&
            Number.isFinite(originalPrice) &&
            Number.isFinite(quotedPrice) &&
            quotedPrice > 0 &&
            quotedPrice !== originalPrice &&
            /^\d+(\.\d{1,2})?$/.test(
                String(item.confirmedUnitPrice)
            );


        if (
            !orderEligibleForPriceDecision ||
            ["ACCEPT", "REJECT"].includes(item?.priceDecision) ||
            item?.supplierResponseStatus !== "PRICE_CHANGED" ||
            !validPrice ||
            !["ACCEPT", "REJECT"].includes(decision)
        ) {
            return;
        }


        setDecisionDraft({ item, decision });
        setDecisionReason("");
    }

    function closeDecisionPreview() {
        setDecisionDraft(null);
        setDecisionReason("");
    }

    function openMockDecisionPreview(decision) {
        if (
            !PRICE_DECISION_UI_PREVIEW ||
            !["ACCEPT", "REJECT"].includes(decision)
        ) {
            return;
        }

        setDecisionDraft({
            item: MOCK_PRICE_DECISION_ITEM,
            decision,
        });

        setDecisionReason("");
        setDecisionError("");
    }


    async function handleConfirmPriceDecision() {
        // Khong cho phep POST khi chua bat chuc nang.
        if (
            PRICE_DECISION_UI_PREVIEW ||
            !PRICE_DECISION_WRITE_ENABLED ||
            decisionSubmitting ||
            !decisionDraft ||
            !orderEligibleForPriceDecision ||
            !["ACCEPT", "REJECT"].includes(decisionDraft.decision)
        ) {
            return;
        }


        const reason = decisionReason.trim();

        if (!reason) {
            setDecisionError("Vui lòng nhập lý do quyết định.");
            return;
        }

        const currentItem = items.find(
            (item) =>
                item.purchaseOrderItemId ===
                decisionDraft.item.purchaseOrderItemId
        );

        const originalPrice = Number(
            currentItem?.originalUnitPrice
        );

        const quotedPrice = Number(
            currentItem?.confirmedUnitPrice
        );

        const validItem =
            currentItem &&
            !["ACCEPT", "REJECT"].includes(currentItem.priceDecision) &&
            currentItem.supplierResponseStatus === "PRICE_CHANGED" &&
            currentItem.originalUnitPrice != null &&
            currentItem.confirmedUnitPrice != null &&
            Number.isFinite(originalPrice) &&
            Number.isFinite(quotedPrice) &&
            quotedPrice > 0 &&
            quotedPrice !== originalPrice &&
            /^\d+(\.\d{1,2})?$/.test(
                String(currentItem.confirmedUnitPrice)
            );

        if (!validItem) {
            setDecisionError(
                "Sản phẩm không còn đủ điều kiện quyết định giá. Vui lòng tải lại dữ liệu."
            );
            return;
        }

        setDecisionSubmitting(true);
        setDecisionError("");
        setDecisionSuccess("");

        try {
            await decideSupplierPrice(
                purchaseOrderId,
                currentItem.purchaseOrderItemId,
                {
                    decision: decisionDraft.decision,
                    reason,
                }
            );

            setDecisionSuccess(
                decisionDraft.decision === "ACCEPT"
                    ? "Đã chấp nhận giá mới thành công."
                    : "Đã từ chối giá mới thành công."
            );

            setDecisionDraft(null);
            setDecisionReason("");

            // Tai lai bang so sanh gia tu Backend.
            setReload((value) => value + 1);
        } catch (error) {
            setDecisionError(
                supplierFeedbackErrorMessage(
                    error,
                    "Không thực hiện được quyết định giá."
                )
            );
        } finally {
            setDecisionSubmitting(false);
        }
    }


    const decisionColumns = [
        ...columns,
        {
            key: "priceDecision",
            label: "Quyết định giá",
            render: (_value, item) => {
                const originalPrice = Number(item.originalUnitPrice);
                const quotedPrice = Number(item.confirmedUnitPrice);

                const validPrice =
                    item.originalUnitPrice != null &&
                    item.confirmedUnitPrice != null &&
                    Number.isFinite(originalPrice) &&
                    Number.isFinite(quotedPrice) &&
                    quotedPrice > 0 &&
                    quotedPrice !== originalPrice &&
                    /^\d+(\.\d{1,2})?$/.test(
                        String(item.confirmedUnitPrice)
                    );


                const alreadyDecided =
                    item.priceDecision === "ACCEPT" ||
                    item.priceDecision === "REJECT";

                const canReviewDecision =
                    !alreadyDecided &&
                    orderEligibleForPriceDecision &&
                    item.supplierResponseStatus === "PRICE_CHANGED" &&
                    validPrice;


                return (
                    <div className="min-w-40 space-y-2">

                        <Button
                            type="button"
                            variant="outline"
                            disabled={!canReviewDecision}
                            onClick={() => openDecisionPreview(item, "ACCEPT")}
                        >
                            Chấp nhận giá
                        </Button>



                        <Button
                            type="button"
                            variant="outline"
                            disabled={!canReviewDecision}
                            onClick={() => openDecisionPreview(item, "REJECT")}
                        >
                            Từ chối giá
                        </Button>




                        <p className="text-xs text-slate-500">
                            {item.priceDecision === "ACCEPT"
                                ? "Đã chấp nhận giá"
                                : item.priceDecision === "REJECT"
                                ? "Đã từ chối giá"
                                : canReviewDecision
                                    ? PRICE_DECISION_WRITE_ENABLED
                                        ? "Đủ điều kiện sơ bộ - có thể xem xét quyết định"
                                        : "Đủ điều kiện sơ bộ - chỉ xem trước"
                                    : "Chưa đủ điều kiện quyết định giá"}
                        </p>


                    </div>
                );
            },
        },
    ];


    return (
        <Card>
            <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
                <div>
                    <h2 className="text-lg font-bold text-slate-900">
                        So sánh giá nhà cung cấp
                    </h2>
                    <p className="mt-1 text-sm text-slate-500">
                        Issue #16 — Giá nhập ban đầu và giá
                        nhà cung cấp phản hồi.
                    </p>
                </div>

                <Button
                    type="button"
                    variant="ghost"
                    disabled={loading}
                    onClick={() => setReload((v) => v + 1)}
                >
                    Làm mới
                </Button>
            </div>

            <div className="mb-5">

                    <Alert type="info">
                        {PRICE_DECISION_WRITE_ENABLED
                            ? "Chức năng quyết định giá đã được kích hoạt. Chấp nhận giá sẽ cập nhật giá PO và giá nhập nhà cung cấp. Từ chối giá sẽ không thay đổi giá. Mọi quyết định đều được ghi lịch sử."
                            : "Giá mới hiện chỉ dùng để xem và so sánh. Chức năng gửi quyết định ACCEPT/REJECT đang được khóa để tránh thay đổi dữ liệu."}
                    </Alert>

            </div>

            {loading ? (
                <div
                    role="status"
                    className="flex items-center gap-3 py-6 text-sm text-slate-600"
                >
                    <Spinner />
                    Đang tải dữ liệu so sánh giá...
                </div>
            ) : error ? (
                <div role="alert">
                    <Alert type="danger">{error}</Alert>
                </div>
            ) : items.length === 0 ? (
                <EmptyState
                    title="Chưa có dữ liệu so sánh giá"
                    description="Purchase Order chưa có sản phẩm để so sánh."
                />
            ) : (
                <div className="space-y-4">
                    <div className="text-sm text-slate-600">
                        <p>
                            Đơn mua hàng:{" "}
                            <strong>
                                {comparison.orderCode || "—"}
                            </strong>
                        </p>
                        <p>
                            Nhà cung cấp:{" "}
                            <strong>
                                {comparison.supplierName || "—"}
                            </strong>
                        </p>
                    </div>

                    {/* Thong bao dieu kien quyet dinh gia */}
                    <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
                        {hasReceivedGoods
                            ? "Chưa thể quyết định giá: Đơn mua hàng đã nhận hàng. Backend không cho phép ACCEPT/REJECT giá mới sau khi đã nhận hàng."
                            : purchaseOrderStatus !== "ORDERED"
                            ? "Chưa thể quyết định giá: Purchase Order không ở trạng thái ORDERED."
                            : !hasNoReceivedItems
                                ? "Chưa thể quyết định giá: Chưa xác định đầy đủ số lượng hàng đã nhận."
                                : "Đơn hàng đủ điều kiện ban đầu để xem xét giá mới. Chỉ sản phẩm PRICE_CHANGED có báo giá hợp lệ mới được ACCEPT/REJECT."}
                    </div>

                    {decisionSuccess && (
                        <div role="status">
                            <Alert type="success">
                                {decisionSuccess}
                            </Alert>
                        </div>
                    )}

                    {PRICE_DECISION_UI_PREVIEW && (
                        <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 space-y-3">
                            <p className="text-sm font-semibold text-amber-900">
                                Chế độ kiểm thử giao diện - dữ liệu giả lập
                            </p>

                            <p className="text-xs text-amber-800">
                                Hai nút bên dưới chỉ mở Modal để kiểm tra.
                                Không gửi POST và không thay đổi MySQL.
                            </p>

                            <div className="flex flex-wrap gap-2">
                                <Button
                                    type="button"
                                    variant="outline"
                                    onClick={() => openMockDecisionPreview("ACCEPT")}
                                >
                                    Test Modal ACCEPT
                                </Button>

                                <Button
                                    type="button"
                                    variant="outline"
                                    onClick={() => openMockDecisionPreview("REJECT")}
                                >
                                    Test Modal REJECT
                                </Button>
                            </div>
                        </div>
                    )}


                    {/* Bang so sanh gia hien tai */}
                    <div className="overflow-x-auto">
                        <Table
                            columns={decisionColumns}
                            data={items}
                            rowKey="purchaseOrderItemId"
                        />
                    </div>

                    <p className="text-xs text-slate-500">
                        Chênh lệch ước tính được Backend
                        tính theo số lượng đặt mua.
                        Nếu nhà cung cấp chỉ xác nhận một
                        phần số lượng, giá trị này chưa
                        phải tổng chênh lệch cuối cùng.
                    </p>
                </div>
            )}

            <Modal
                open={Boolean(decisionDraft)}
                title={
                    decisionDraft?.decision === "ACCEPT"
                        ? "Xác nhận chấp nhận giá mới"
                        : "Xác nhận từ chối giá mới"
                }
                onClose={closeDecisionPreview}
                footer={
                    <>
                        <Button
                            type="button"
                            variant="outline"
                            onClick={closeDecisionPreview}
                        >
                            Đóng
                        </Button>


                        <Button
                            type="button"
                            onClick={handleConfirmPriceDecision}
                            disabled={
                                PRICE_DECISION_UI_PREVIEW ||
                                !PRICE_DECISION_WRITE_ENABLED ||
                                decisionSubmitting ||
                                !decisionDraft ||
                                !decisionReason.trim() ||
                                !orderEligibleForPriceDecision
                            }
                        >
                            {!PRICE_DECISION_WRITE_ENABLED
                                ? "Xác nhận quyết định (chưa kích hoạt)"
                                : decisionSubmitting
                                ? "Đang xử lý..."
                                : "Xác nhận quyết định"}
                        </Button>

                    </>
                }
            >
                {decisionDraft && (
                    <div className="space-y-4">
                        <p className="text-sm text-slate-700">
                            Sản phẩm:{" "}
                            <strong>
                                {decisionDraft.item.productName}
                            </strong>
                        </p>

                        <p className="text-sm text-slate-700">
                            Giá PO hiện tại:{" "}
                            <strong>
                                {formatMoney(
                                    decisionDraft.item.originalUnitPrice
                                )}
                            </strong>
                        </p>

                        <p className="text-sm text-slate-700">
                            Giá nhà cung cấp báo:{" "}
                            <strong>
                                {formatMoney(
                                    decisionDraft.item.confirmedUnitPrice
                                )}
                            </strong>
                        </p>

                        <p className="text-sm font-medium text-slate-800">
                            Quyết định:{" "}
                            {decisionDraft.decision === "ACCEPT"
                                ? "CHẤP NHẬN"
                                : "TỪ CHỐI"}
                        </p>

                        <TextArea
                            label="Lý do quyết định *"
                            rows={3}
                            value={decisionReason}
                            onChange={(event) =>
                                setDecisionReason(event.target.value)
                            }
                            placeholder="Nhập lý do chấp nhận hoặc từ chối giá..."
                        />
                        {decisionError && (
                            <div role="alert">
                                <Alert type="danger">
                                    {decisionError}
                                </Alert>
                            </div>
                        )}



                        <Alert type="info">
                            {PRICE_DECISION_UI_PREVIEW
                                ? "Đây là dữ liệu giả lập để kiểm thử giao diện. Không gửi quyết định lên Backend."
                                : PRICE_DECISION_WRITE_ENABLED
                                ? "Hãy kiểm tra kỹ quyết định và lý do. Khi xác nhận, hệ thống sẽ ghi lịch sử. ACCEPT còn cập nhật giá PO và giá nhập nhà cung cấp."
                                : "Đây chỉ là bước xem trước. Chức năng gửi quyết định giá đang bị khóa."}
                        </Alert>

                    </div>
                )}
            </Modal>

        </Card>
    );
}
