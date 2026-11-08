
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
    INCREASED: "Price Increase",
    DECREASED: "Price Decrease",
    UNCHANGED: "No Change",
    NOT_QUOTED: "Not Quoted",
};

const PRICE_DECISION_UI_PREVIEW =
    import.meta.env.DEV &&
    import.meta.env.VITE_PREVIEW_SUPPLIER_PRICE_DECISION === "true";

const MOCK_PRICE_DECISION_ITEM = {
    purchaseOrderItemId: -1,
    productName: "Demo Product - UI Test Only",
    originalUnitPrice: 120000,
    confirmedUnitPrice: 130000,
};


const RESPONSE_STATUS_LABELS = {
    PENDING: "Pending",
    AVAILABLE: "Available",
    PARTIALLY_AVAILABLE: "Partially Available",
    OUT_OF_STOCK: "Out of Stock",
    DISCONTINUED: "Discontinued",
    PRICE_CHANGED: "Price Change",
};

function formatNumber(value) {
    if (value == null || value === "") {
        return "—";
    }

    const number = Number(value);

    return Number.isFinite(number)
        ? number.toLocaleString("en-GB")
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

    return `${prefix}${number.toLocaleString("en-GB", {
        maximumFractionDigits: 2,
    })} VND`;
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

    return `${prefix}${number.toLocaleString("en-GB", {
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
        label: "Product",
        render: (value, item) => (
            <div className="space-y-1">
                <p className="font-semibold text-slate-800">
                    {value || "—"}
                </p>
                <p className="text-xs text-slate-500">
                    Product Code: {item.productId ?? "—"}
                </p>
            </div>
        ),
    },
    {
        key: "supplierResponseStatus",
        label: "Supplier Response",
        render: (value) =>
            RESPONSE_STATUS_LABELS[value] || value || "—",
    },
    {
        key: "orderedQuantity",
        label: "Ordered Quantity",
        render: formatNumber,
    },
    {
        key: "receivedQuantity",
        label: "Received Qty",
        render: formatNumber,
    },
    {
        key: "confirmedQuantity",
        label: "Confirmed Qty",
        render: formatNumber,
    },
    {
        key: "originalUnitPrice",
        label: "Original Price",
        render: (value) => formatMoney(value),
    },
    {
        key: "confirmedUnitPrice",
        label: "Supplier Quoted Price",
        render: (value) => formatMoney(value),
    },
    {
        key: "differencePerUnit",
        label: "Price Difference / Unit",
        render: (value) => formatMoney(value, true),
    },
    {
        key: "differencePercent",
        label: "Price Difference (%)",
        render: formatPercent,
    },
    {
        key: "estimatedOrderDifference",
        label: "Estimated Price Impact",
        render: (value) => formatMoney(value, true),
    },
    {
        key: "priceChange",
        label: "Change",
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
            setError("Invalid purchase order ID.");
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
                        "Invalid price comparison data."
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
                              "Unable to load price comparison."
                          )
                        : requestError?.message ||
                          "Unable to load price comparison.";

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
            setDecisionError("Please enter a reason for your decision.");
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
                "This product is no longer eligible for a price decision. Reload the data."
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
                    ? "New supplier price accepted successfully."
                    : "New supplier price rejected successfully."
            );

            setDecisionDraft(null);
            setDecisionReason("");

            // Tai lai bang so sanh gia tu Backend.
            setReload((value) => value + 1);
        } catch (error) {
            setDecisionError(
                supplierFeedbackErrorMessage(
                    error,
                    "Unable to complete the price decision."
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
            label: "Price Decision",
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
                            Accept Price
                        </Button>



                        <Button
                            type="button"
                            variant="outline"
                            disabled={!canReviewDecision}
                            onClick={() => openDecisionPreview(item, "REJECT")}
                        >
                            Reject Price
                        </Button>




                        <p className="text-xs text-slate-500">
                            {item.priceDecision === "ACCEPT"
                                ? "Price Accepted"
                                : item.priceDecision === "REJECT"
                                ? "Price Rejected"
                                : canReviewDecision
                                    ? PRICE_DECISION_WRITE_ENABLED
                                        ? "Preliminarily Eligible - Decision Available"
                                        : "Preliminarily Eligible - Preview Only"
                                    : "Not Eligible for Price Decision"}
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
                        Supplier Price Comparison
                    </h2>
                    <p className="mt-1 text-sm text-slate-500">
                        Compare the original purchase price with the supplier's newly proposed price.
                    </p>
                </div>

                <Button
                    type="button"
                    variant="ghost"
                    disabled={loading}
                    onClick={() => setReload((v) => v + 1)}
                >
                    Refresh
                </Button>
            </div>

            <div className="mb-5">

                    <Alert type="info">
                        {PRICE_DECISION_WRITE_ENABLED
                            ? "You can accept or reject the new price. If accepted, the purchase unit price will be updated. Every decision is recorded."
                            : "The proposed price is for reference only. Price changes cannot be confirmed at this time."}
                    </Alert>

            </div>

            {loading ? (
                <div
                    role="status"
                    className="flex items-center gap-3 py-6 text-sm text-slate-600"
                >
                    <Spinner />
                    Loading price comparison...
                </div>
            ) : error ? (
                <div role="alert">
                    <Alert type="danger">{error}</Alert>
                </div>
            ) : items.length === 0 ? (
                <EmptyState
                    title="No Price Comparison Data"
                    description="The purchase order contains no products to compare."
                />
            ) : (
                <div className="space-y-4">
                    <div className="text-sm text-slate-600">
                        <p>
                            Purchase Order:{" "}
                            <strong>
                                {comparison.orderCode || "—"}
                            </strong>
                        </p>
                        <p>
                            Supplier:{" "}
                            <strong>
                                {comparison.supplierName || "—"}
                            </strong>
                        </p>
                    </div>

                    {/* Thong bao dieu kien quyet dinh gia */}
                    <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
                        {hasReceivedGoods
                            ? "Prices cannot be changed after goods have been received. Contact a manager if further action is required."
                            : purchaseOrderStatus !== "ORDERED"
                            ? "A price decision is not allowed until the order has been submitted to the supplier."
                            : !hasNoReceivedItems
                                ? "A price decision is unavailable because received quantities have not been fully determined."
                                : "Only products with a valid supplier price change proposal are eligible for review."}
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
                                UI Test Mode - Sample Data
                            </p>

                            <p className="text-xs text-amber-800">
                                The buttons below only open a preview dialog. No changes will be saved.
                            </p>

                            <div className="flex flex-wrap gap-2">
                                <Button
                                    type="button"
                                    variant="outline"
                                    onClick={() => openMockDecisionPreview("ACCEPT")}
                                >
                                    Preview Price Acceptance
                                </Button>

                                <Button
                                    type="button"
                                    variant="outline"
                                    onClick={() => openMockDecisionPreview("REJECT")}
                                >
                                    Preview Price Rejection
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
                        The system estimates the price difference using the ordered quantity. If the supplier confirms only part of the quantity, this is not the final price impact.
                    </p>
                </div>
            )}

            <Modal
                open={Boolean(decisionDraft)}
                title={
                    decisionDraft?.decision === "ACCEPT"
                        ? "Confirm New Price Acceptance"
                        : "Confirm New Price Rejection"
                }
                onClose={closeDecisionPreview}
                footer={
                    <>
                        <Button
                            type="button"
                            variant="outline"
                            onClick={closeDecisionPreview}
                        >
                            Close
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
                                ? "Confirm Decision (disabled)"
                                : decisionSubmitting
                                ? "Processing..."
                                : "Confirm Decision"}
                        </Button>

                    </>
                }
            >
                {decisionDraft && (
                    <div className="space-y-4">
                        <p className="text-sm text-slate-700">
                            Product:{" "}
                            <strong>
                                {decisionDraft.item.productName}
                            </strong>
                        </p>

                        <p className="text-sm text-slate-700">
                            Current Purchase Price:{" "}
                            <strong>
                                {formatMoney(
                                    decisionDraft.item.originalUnitPrice
                                )}
                            </strong>
                        </p>

                        <p className="text-sm text-slate-700">
                            Supplier Quoted Price:{" "}
                            <strong>
                                {formatMoney(
                                    decisionDraft.item.confirmedUnitPrice
                                )}
                            </strong>
                        </p>

                        <p className="text-sm font-medium text-slate-800">
                            Decision:{" "}
                            {decisionDraft.decision === "ACCEPT"
                                ? "ACCEPT"
                                : "REJECT"}
                        </p>

                        <TextArea
                            label="Decision Reason *"
                            rows={3}
                            value={decisionReason}
                            onChange={(event) =>
                                setDecisionReason(event.target.value)
                            }
                            placeholder="Enter the reason for accepting or rejecting the price..."
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
                                ? "This is sample data. No changes have been saved."
                                : PRICE_DECISION_WRITE_ENABLED
                                ? "Review the price and reason before confirming. Acceptance will update the purchase order price and supplier information."
                                : "This is a preview only. Price decisions are currently disabled."}
                        </Alert>

                    </div>
                )}
            </Modal>

        </Card>
    );
}

