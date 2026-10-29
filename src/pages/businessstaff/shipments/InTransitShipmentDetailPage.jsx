
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";

import {
    getInTransitShipmentDetail,
    inTransitShipmentErrorMessage,
} from "../../../api/business/inTransitShipments";

import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";

import Alert from "../../../components/ui/Alert";
import Card from "../../../components/ui/Card";
import EmptyState from "../../../components/ui/EmptyState";
import Spinner from "../../../components/ui/Spinner";
import Table from "../../../components/ui/Table";

// Issue #13 - Chi tiet hang dang van chuyen.
// Business Staff chi duoc xem du lieu.

const ROOT = "/business-staff/in-transit-shipments";

const STATUS_LABELS = {
    CREATED: "Đã tạo",
    ASSIGNED: "Đã phân công",
    ACCEPTED: "Đã nhận chuyến",
    REJECTED: "Đã từ chối",
    READY_TO_SHIP: "Sẵn sàng giao",
    IN_TRANSIT: "Đang vận chuyển",
    ARRIVED: "Đã đến điểm nhận",
    DELIVERED: "Đã giao thành công",
    FAILED: "Giao thất bại",
    CANCELLED: "Đã hủy",
};

const TRANSFER_TYPE_LABELS = {
    STORE_RETURN: "Cửa hàng trả về kho",
    MAIN_TO_STORE: "Kho tổng đến cửa hàng",
    STORE_TO_STORE: "Giữa các cửa hàng",
};

function formatNumber(value) {
    if (value == null || value === "") return "—";

    const number = Number(value);

    return Number.isFinite(number)
        ? number.toLocaleString("vi-VN")
        : String(value);
}

function formatDateTime(value) {
    if (!value) return "—";

    const date = new Date(value);

    return Number.isNaN(date.getTime())
        ? String(value)
        : date.toLocaleString("vi-VN", {
              hour12: false,
          });
}

function StatusBadge({ status }) {
    const colors = {
        DELIVERED: "bg-green-100 text-green-700",
        IN_TRANSIT: "bg-blue-100 text-blue-700",
        ARRIVED: "bg-teal-100 text-teal-700",
        FAILED: "bg-red-100 text-red-700",
        CANCELLED: "bg-red-100 text-red-700",
        ASSIGNED: "bg-purple-100 text-purple-700",
    };

    return (
        <span
            className={`inline-block rounded-full px-3 py-1 text-xs font-semibold ${
                colors[status] || "bg-slate-100 text-slate-700"
            }`}
        >
            {STATUS_LABELS[status] || status || "Chưa xác định"}
        </span>
    );
}

function InfoItem({ label, value }) {
    return (
        <div className="space-y-1">
            <p className="text-xs font-medium text-slate-500">
                {label}
            </p>

            <div className="break-words text-sm font-semibold text-slate-800">
                {value ?? "—"}
            </div>
        </div>
    );
}

// Bang danh sach phieu dieu chuyen.
const transferColumns = [
    {
        key: "transferCode",
        label: "Mã phiếu điều chuyển",
        render: (value, transfer) =>
            transfer.transferId != null ? (
                <Link
                    to={`/business-staff/stock-transfers/${transfer.transferId}`}
                    className="font-semibold text-[#c94810] hover:underline"
                >
                    {value || "—"}
                </Link>
            ) : (
                value || "—"
            ),
    },
    {
        key: "transferType",
        label: "Loại điều chuyển",
        render: (value) =>
            TRANSFER_TYPE_LABELS[value] || value || "—",
    },
    {
        key: "transferStatus",
        label: "Trạng thái",
        render: (value) => value || "—",
    },
    {
        key: "totalRequestedQuantity",
        label: "SL yêu cầu",
        render: formatNumber,
    },
    {
        key: "totalShippedQuantity",
        label: "SL đã xuất",
        render: formatNumber,
    },
    {
        key: "totalReceivedQuantity",
        label: "SL đã nhận",
        render: formatNumber,
    },
    {
        key: "expectedReceiptDate",
        label: "Dự kiến nhận",
        render: (value) => value || "—",
    },
];

// San pham ben trong phieu dieu chuyen.
const transferProductColumns = [
    {
        key: "productCode",
        label: "Mã sản phẩm",
    },
    {
        key: "productName",
        label: "Tên sản phẩm",
    },
    {
        key: "barcode",
        label: "Barcode",
    },
    {
        key: "baseUnit",
        label: "Đơn vị",
    },
    {
        key: "requestedQuantity",
        label: "Yêu cầu",
        render: formatNumber,
    },
    {
        key: "approvedQuantity",
        label: "Duyệt",
        render: formatNumber,
    },
    {
        key: "packedQuantity",
        label: "Đóng kiện",
        render: formatNumber,
    },
    {
        key: "shippedQuantity",
        label: "Đã xuất",
        render: formatNumber,
    },
    {
        key: "receivedQuantity",
        label: "Đã nhận",
        render: formatNumber,
    },
];

// San pham thuc te trong tung kien hang.
const packageProductColumns = [
    {
        key: "productCode",
        label: "Mã sản phẩm",
    },
    {
        key: "productName",
        label: "Tên sản phẩm",
    },
    {
        key: "barcode",
        label: "Barcode",
    },
    {
        key: "baseUnit",
        label: "Đơn vị",
    },
    {
        key: "quantity",
        label: "Số lượng trong kiện",
        render: formatNumber,
    },
];

export default function InTransitShipmentDetailPage() {
    const { deliveryId } = useParams();

    const [delivery, setDelivery] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    // 1. Goi API chi tiet chuyen giao.
    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError("");
        setDelivery(null);

        getInTransitShipmentDetail(
            deliveryId,
            controller.signal
        )
            .then((data) => {
                if (controller.signal.aborted) return;

                if (data?.deliveryId == null) {
                    throw new Error(
                        "Dữ liệu chuyến vận chuyển không hợp lệ."
                    );
                }

                setDelivery(data);
            })
            .catch((err) => {
                if (controller.signal.aborted) return;

                const message = err?.isAxiosError
                    ? inTransitShipmentErrorMessage(err)
                    : err?.message ||
                      "Không tải được chi tiết chuyến giao.";

                setError(message);
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [deliveryId]);

    // 2. Xu ly danh sach long nhau an toan.
    const transfers = Array.isArray(delivery?.transfers)
        ? delivery.transfers
        : [];

    const packages = Array.isArray(delivery?.packages)
        ? delivery.packages
        : [];

    const history = Array.isArray(delivery?.statusHistory)
        ? [...delivery.statusHistory].sort((a, b) => {
              const timeA = Date.parse(a?.changedAt || "");
              const timeB = Date.parse(b?.changedAt || "");

              if (
                  !Number.isFinite(timeA) ||
                  !Number.isFinite(timeB)
              ) {
                  return 0;
              }

              return timeA - timeB;
          })
        : [];

    return (
        <MainLayout
            title="Business Staff"
            items={businessStaffMenuItems}
        >
            <div className="space-y-6">
                <PageHeader
                    title="Chi tiết chuyến vận chuyển"
                    description="Theo dõi thông tin giao hàng, sản phẩm, kiện hàng và lịch sử trạng thái."
                    actions={
                        <Link
                            to={ROOT}
                            className="rounded-xl border border-orange-500 px-4 py-2 text-sm font-medium text-orange-600 hover:bg-orange-50"
                        >
                            Quay lại danh sách
                        </Link>
                    }
                />

                {loading ? (
                    <div
                        role="status"
                        className="flex items-center gap-3 rounded-xl bg-white p-8 text-slate-600"
                    >
                        <Spinner />
                        Đang tải chi tiết chuyến vận chuyển...
                    </div>
                ) : error ? (
                    <div role="alert">
                        <Alert type="danger">{error}</Alert>
                    </div>
                ) : !delivery ? (
                    <EmptyState
                        title="Không tìm thấy chuyến vận chuyển"
                        description="Không có dữ liệu chi tiết để hiển thị."
                    />
                ) : (
                    <>
                        {/* Quyen han Business Staff */}
                        <Alert type="info">
                            Business Staff chỉ được xem và theo
                            dõi chuyến vận chuyển. Việc cập
                            nhật trạng thái giao hàng thuộc về
                            các vai trò được phân quyền.
                        </Alert>

                        {/* Canh bao qua han */}
                        {delivery.overdue === true && (
                            <div role="alert">
                                <Alert type="danger">
                                    <strong>
                                        Cảnh báo: Quá hạn giao hàng!
                                    </strong>{" "}
                                    Chuyến vận chuyển đã vượt
                                    thời gian dự kiến nhận:{" "}
                                    {formatDateTime(
                                        delivery.expectedDeliveryAt
                                    )}.
                                    Trạng thái hiện tại vẫn là{" "}
                                    {STATUS_LABELS[
                                        delivery.deliveryStatus
                                    ] || delivery.deliveryStatus}.
                                </Alert>
                            </div>
                        )}

                        {/* Thong tin chung */}
                        <Card>
                            <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
                                <div>
                                    <p className="text-sm text-slate-500">
                                        Mã chuyến giao
                                    </p>

                                    <h2 className="text-xl font-bold text-slate-900">
                                        {delivery.deliveryCode || "—"}
                                    </h2>
                                </div>

                                <StatusBadge
                                    status={delivery.deliveryStatus}
                                />
                            </div>

                            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                                <InfoItem
                                    label="Mã bảng kê"
                                    value={
                                        delivery.manifestId != null ? (
                                            <Link
                                                to={`/business-staff/shipment-manifests/${delivery.manifestId}`}
                                                className="text-[#c94810] hover:underline"
                                            >
                                                {delivery.manifestCode || "—"}
                                            </Link>
                                        ) : (
                                            delivery.manifestCode || "—"
                                        )
                                    }
                                />

                                <InfoItem
                                    label="Trạng thái bảng kê"
                                    value={delivery.manifestStatus}
                                />

                                <InfoItem
                                    label="Ngày tạo chuyến"
                                    value={formatDateTime(
                                        delivery.createdAt
                                    )}
                                />

                                <InfoItem
                                    label="Cập nhật gần nhất"
                                    value={formatDateTime(
                                        delivery.updatedAt
                                    )}
                                />
                            </div>

                            {delivery.manifestNote && (
                                <p className="mt-4 text-sm text-slate-600">
                                    <strong>Ghi chú bảng kê:</strong>{" "}
                                    {delivery.manifestNote}
                                </p>
                            )}
                        </Card>

                        {/* Diem xuat va diem nhan */}
                        <div className="grid gap-4 md:grid-cols-2">
                            <Card>
                                <h3 className="mb-4 text-lg font-bold text-slate-800">
                                    Điểm xuất
                                </h3>

                                <div className="space-y-4">
                                    <InfoItem
                                        label="Kho / cửa hàng"
                                        value={delivery.fromWarehouseName}
                                    />

                                    <InfoItem
                                        label="Mã điểm xuất"
                                        value={delivery.fromWarehouseCode}
                                    />

                                    <InfoItem
                                        label="Địa chỉ"
                                        value={delivery.fromWarehouseAddress}
                                    />
                                </div>
                            </Card>

                            <Card>
                                <h3 className="mb-4 text-lg font-bold text-slate-800">
                                    Điểm nhận
                                </h3>

                                <div className="space-y-4">
                                    <InfoItem
                                        label="Kho / cửa hàng"
                                        value={delivery.toWarehouseName}
                                    />

                                    <InfoItem
                                        label="Mã điểm nhận"
                                        value={delivery.toWarehouseCode}
                                    />

                                    <InfoItem
                                        label="Địa chỉ"
                                        value={delivery.toWarehouseAddress}
                                    />
                                </div>
                            </Card>
                        </div>

                        {/* So lieu tong hop */}
                        <div className="grid gap-4 sm:grid-cols-3">
                            {[
                                [
                                    "Tổng phiếu điều chuyển",
                                    delivery.totalTransfers,
                                ],
                                [
                                    "Tổng số kiện hàng",
                                    delivery.totalPackages,
                                ],
                                [
                                    "Tổng số lượng sản phẩm",
                                    delivery.totalProductQuantity,
                                ],
                            ].map(([label, value]) => (
                                <Card key={label}>
                                    <p className="text-sm text-slate-500">
                                        {label}
                                    </p>

                                    <p className="mt-2 text-3xl font-bold text-[#c94810]">
                                        {formatNumber(value)}
                                    </p>
                                </Card>
                            ))}
                        </div>

                        {/* Thong tin nguoi phu trach */}
                        <Card>
                            <h3 className="mb-4 text-lg font-bold text-slate-800">
                                Tài xế và thông tin bàn giao
                            </h3>

                            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                                <InfoItem
                                    label="Tài xế"
                                    value={delivery.driverName}
                                />

                                <InfoItem
                                    label="Mã tài xế"
                                    value={delivery.driverCode}
                                />

                                <InfoItem
                                    label="Người bàn giao"
                                    value={delivery.handedOverByName}
                                />

                                <InfoItem
                                    label="Mã người bàn giao"
                                    value={delivery.handedOverByCode}
                                />

                                <InfoItem
                                    label="Dự kiến lấy hàng"
                                    value={formatDateTime(
                                        delivery.expectedPickupAt
                                    )}
                                />

                                <InfoItem
                                    label="Thời gian bàn giao"
                                    value={formatDateTime(
                                        delivery.handedOverAt
                                    )}
                                />

                                <InfoItem
                                    label="Bắt đầu vận chuyển"
                                    value={formatDateTime(
                                        delivery.startedAt
                                    )}
                                />

                                <InfoItem
                                    label="Dự kiến giao hàng"
                                    value={formatDateTime(
                                        delivery.expectedDeliveryAt
                                    )}
                                />

                                <InfoItem
                                    label="Thời gian giao hoàn tất"
                                    value={formatDateTime(
                                        delivery.deliveredAt
                                    )}
                                />
                            </div>
                        </Card>

                        {/* Danh sach phieu dieu chuyen */}
                        <section className="space-y-4">
                            <h3 className="text-lg font-bold text-slate-800">
                                Danh sách phiếu điều chuyển
                            </h3>

                            {transfers.length === 0 ? (
                                <EmptyState
                                    title="Chưa có phiếu điều chuyển"
                                />
                            ) : (
                                <>
                                    <Table
                                        columns={transferColumns}
                                        data={transfers}
                                        rowKey="transferId"
                                    />

                                    {transfers.map((transfer, index) => {
                                        const products =
                                            Array.isArray(transfer.products)
                                                ? transfer.products
                                                : [];

                                        return (
                                            <Card
                                                key={
                                                    transfer.transferId ??
                                                    index
                                                }
                                            >
                                                <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                                                    <h4 className="font-bold text-slate-800">
                                                        Sản phẩm của phiếu{" "}
                                                        {transfer.transferCode}
                                                    </h4>

                                                    <span className="rounded-lg bg-slate-100 px-3 py-1 text-sm font-medium text-slate-700">
                                                        {transfer.transferStatus || "—"}
                                                    </span>
                                                </div>

                                                {products.length > 0 ? (
                                                    <Table
                                                        columns={
                                                            transferProductColumns
                                                        }
                                                        data={products}
                                                        rowKey="productId"
                                                    />
                                                ) : (
                                                    <EmptyState
                                                        title="Phiếu chưa có sản phẩm"
                                                    />
                                                )}
                                            </Card>
                                        );
                                    })}
                                </>
                            )}
                        </section>

                        {/* Danh sach kien hang */}
                        <section className="space-y-4">
                            <h3 className="text-lg font-bold text-slate-800">
                                Danh sách kiện hàng
                            </h3>

                            {packages.length === 0 ? (
                                <EmptyState
                                    title="Chưa có kiện hàng"
                                />
                            ) : (
                                packages.map((pkg, index) => {
                                    const products =
                                        Array.isArray(pkg.products)
                                            ? pkg.products
                                            : [];

                                    return (
                                        <Card
                                            key={pkg.packageId ?? index}
                                        >
                                            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                                                <h4 className="text-lg font-bold text-slate-800">
                                                    {pkg.packageCode || "—"}
                                                </h4>

                                                <span className="rounded-lg bg-slate-100 px-3 py-1 text-sm font-medium text-slate-700">
                                                    {pkg.packageStatus || "—"}
                                                </span>
                                            </div>

                                            <div className="mb-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                                                <InfoItem
                                                    label="Mã niêm phong"
                                                    value={pkg.sealNumber}
                                                />

                                                <InfoItem
                                                    label="Thời gian đóng kiện"
                                                    value={formatDateTime(
                                                        pkg.packedAt
                                                    )}
                                                />

                                                <InfoItem
                                                    label="Thời gian niêm phong"
                                                    value={formatDateTime(
                                                        pkg.sealedAt
                                                    )}
                                                />

                                                <InfoItem
                                                    label="Tổng số lượng"
                                                    value={formatNumber(
                                                        pkg.totalQuantity
                                                    )}
                                                />
                                            </div>

                                            <h5 className="mb-3 font-semibold text-slate-800">
                                                Sản phẩm trong kiện
                                            </h5>

                                            {products.length > 0 ? (
                                                <Table
                                                    columns={
                                                        packageProductColumns
                                                    }
                                                    data={products}
                                                    rowKey="productId"
                                                />
                                            ) : (
                                                <EmptyState
                                                    title="Kiện chưa có sản phẩm"
                                                />
                                            )}
                                        </Card>
                                    );
                                })
                            )}
                        </section>

                        {/* Timeline lich su trang thai */}
                        <Card>
                            <h3 className="mb-4 text-lg font-bold text-slate-800">
                                Lịch sử trạng thái vận chuyển
                            </h3>

                            {history.length === 0 ? (
                                <EmptyState
                                    title="Chưa có lịch sử trạng thái"
                                />
                            ) : (
                                <ol className="space-y-4">
                                    {history.map((event, index) => (
                                        <li
                                            key={`${event.status}-${event.changedAt}-${index}`}
                                            className="relative border-l-2 border-orange-200 py-1 pl-5"
                                        >
                                            <span
                                                aria-hidden="true"
                                                className="absolute -left-[7px] top-2 h-3 w-3 rounded-full bg-[#c94810]"
                                            />

                                            <div className="flex flex-wrap items-center gap-3">
                                                <StatusBadge
                                                    status={event.status}
                                                />

                                                <span className="text-sm font-semibold text-slate-600">
                                                    {formatDateTime(
                                                        event.changedAt
                                                    )}
                                                </span>
                                            </div>

                                            <p className="mt-2 text-sm text-slate-700">
                                                <strong>Người thực hiện:</strong>{" "}
                                                {event.changedByName || "Không có thông tin"}
                                            </p>

                                            {event.note && (
                                                <p className="mt-1 text-sm text-slate-600">
                                                    <strong>Ghi chú:</strong>{" "}
                                                    {event.note}
                                                </p>
                                            )}
                                        </li>
                                    ))}
                                </ol>
                            )}

                            <p className="mt-4 text-xs text-slate-500">
                                Lịch sử này được hiển thị theo
                                những sự kiện Backend cung cấp,
                                không tự bổ sung các trạng thái
                                chưa được ghi nhận.
                            </p>
                        </Card>
                    </>
                )}
            </div>
        </MainLayout>
    );
}