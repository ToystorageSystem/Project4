
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";

import {
    getShipmentManifestDetail,
    shipmentManifestErrorMessage,
} from "../../../api/business/shipmentManifests";

import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";

import Alert from "../../../components/ui/Alert";
import Card from "../../../components/ui/Card";
import EmptyState from "../../../components/ui/EmptyState";
import Spinner from "../../../components/ui/Spinner";
import Table from "../../../components/ui/Table";

const ROOT = "/business-staff/shipment-manifests";

const formatNumber = (value) =>
    Number(value ?? 0).toLocaleString("vi-VN");

function formatDate(value) {
    if (!value) return "—";

    const date = new Date(value);

    return Number.isNaN(date.getTime())
        ? value
        : date.toLocaleString("vi-VN");
}

const transportLabels = {
    CREATED: "Mới tạo",
    ASSIGNED: "Đã phân công",
    ACCEPTED: "Đã nhận chuyến",
    REJECTED: "Đã từ chối",
    READY_TO_SHIP: "Sẵn sàng giao",
    IN_TRANSIT: "Đang vận chuyển",
    ARRIVED: "Đã đến nơi",
    DELIVERED: "Đã giao",
    FAILED: "Giao thất bại",
    CANCELLED: "Đã hủy",
};

function StatusBadge({ status }) {
    const colors = {
        DELIVERED: "bg-green-100 text-green-700",
        IN_TRANSIT: "bg-blue-100 text-blue-700",
        FAILED: "bg-red-100 text-red-700",
        CANCELLED: "bg-red-100 text-red-700",
    };

    return (
        <span
            className={`inline-block rounded-full px-3 py-1 text-xs font-semibold ${
                colors[status] || "bg-slate-100 text-slate-700"
            }`}
        >
            {transportLabels[status] || status || "Chưa có trạng thái"}
        </span>
    );
}

function InfoItem({ label, value }) {
    return (
        <div className="space-y-1">
            <p className="text-xs font-medium text-slate-500">
                {label}
            </p>
            <p className="break-words text-sm font-semibold text-slate-800">
                {value ?? "—"}
            </p>
        </div>
    );
}

const transferColumns = [
    {
        key: "transferCode",
        label: "Mã phiếu điều chuyển",
        render: (value, item) => (
            <Link
                to={`/business-staff/stock-transfers/${item.transferId}`}
                className="font-semibold text-[#c94810] hover:underline"
            >
                {value}
            </Link>
        ),
    },
    {
        key: "transferType",
        label: "Loại điều chuyển",
    },
    {
        key: "transferStatus",
        label: "Trạng thái",
    },
    {
        key: "totalProducts",
        label: "Số sản phẩm",
        render: formatNumber,
    },
    {
        key: "totalRequestedQuantity",
        label: "SL yêu cầu",
        render: formatNumber,
    },
    {
        key: "expectedShipmentDate",
        label: "Dự kiến xuất",
        render: (value) => value || "—",
    },
    {
        key: "expectedReceiptDate",
        label: "Dự kiến nhận",
        render: (value) => value || "—",
    },
];

const productColumns = [
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
        label: "Số lượng",
        render: formatNumber,
    },
];

export default function ShipmentManifestDetailPage() {
    const { manifestId } = useParams();

    const [manifest, setManifest] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError("");
        setManifest(null);

        getShipmentManifestDetail(
            manifestId,
            controller.signal
        )
            .then((data) => {
                if (controller.signal.aborted) return;

                if (!data?.manifestId) {
                    throw new Error("Dữ liệu bảng kê không hợp lệ.");
                }

                setManifest(data);
            })
            .catch((err) => {
                if (controller.signal.aborted) return;

                setError(
                    err?.isAxiosError
                        ? shipmentManifestErrorMessage(err)
                        : err.message || "Không tải được chi tiết bảng kê."
                );
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [manifestId]);

    return (
        <MainLayout
            title="Business Staff"
            items={businessStaffMenuItems}
        >
            <div className="space-y-6">
                <PageHeader
                    title="Chi tiết bảng kê đi hàng"
                    description="Theo dõi thông tin vận chuyển, phiếu điều chuyển và các kiện hàng."
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
                        Đang tải chi tiết bảng kê...
                    </div>
                ) : error ? (
                    <div role="alert">
                        <Alert type="danger">{error}</Alert>
                    </div>
                ) : !manifest ? (
                    <EmptyState
                        title="Không tìm thấy bảng kê"
                        description="Không có dữ liệu chi tiết để hiển thị."
                    />
                ) : (
                    <>
                        <Alert type="info">
                            Business Staff chỉ được xem và theo dõi
                            bảng kê. Không được chỉnh sửa, hủy hoặc
                            thay đổi nội dung kiện hàng.
                        </Alert>

                        <Card>
                            <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
                                <div>
                                    <p className="text-sm text-slate-500">
                                        Mã bảng kê
                                    </p>
                                    <h2 className="text-xl font-bold text-slate-900">
                                        {manifest.manifestCode}
                                    </h2>
                                </div>

                                <StatusBadge
                                    status={manifest.transportStatus}
                                />
                            </div>

                            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                                <InfoItem
                                    label="Trạng thái bảng kê"
                                    value={manifest.manifestStatus}
                                />
                                <InfoItem
                                    label="Người tạo"
                                    value={manifest.createdByName}
                                />
                                <InfoItem
                                    label="Ngày tạo"
                                    value={formatDate(manifest.createdAt)}
                                />
                                <InfoItem
                                    label="Mã người tạo"
                                    value={manifest.createdByCode}
                                />
                            </div>
                        </Card>

                        <div className="grid gap-4 md:grid-cols-2">
                            <Card>
                                <h3 className="mb-4 text-lg font-bold text-slate-800">
                                    Điểm xuất
                                </h3>
                                <InfoItem
                                    label="Kho / cửa hàng"
                                    value={manifest.fromWarehouseName}
                                />
                                <div className="mt-3">
                                    <InfoItem
                                        label="Mã điểm xuất"
                                        value={manifest.fromWarehouseCode}
                                    />
                                </div>
                                <div className="mt-3">
                                    <InfoItem
                                        label="Địa chỉ"
                                        value={manifest.fromWarehouseAddress}
                                    />
                                </div>
                            </Card>

                            <Card>
                                <h3 className="mb-4 text-lg font-bold text-slate-800">
                                    Điểm nhận
                                </h3>
                                <InfoItem
                                    label="Kho / cửa hàng"
                                    value={manifest.toWarehouseName}
                                />
                                <div className="mt-3">
                                    <InfoItem
                                        label="Mã điểm nhận"
                                        value={manifest.toWarehouseCode}
                                    />
                                </div>
                                <div className="mt-3">
                                    <InfoItem
                                        label="Địa chỉ"
                                        value={manifest.toWarehouseAddress}
                                    />
                                </div>
                            </Card>
                        </div>

                        <div className="grid gap-4 sm:grid-cols-3">
                            {[
                                ["Tổng phiếu điều chuyển", manifest.totalTransfers],
                                ["Tổng số kiện", manifest.totalPackages],
                                ["Tổng số lượng sản phẩm", manifest.totalProductQuantity],
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

                        <Card>
                            <h3 className="mb-4 text-lg font-bold text-slate-800">
                                Thông tin vận chuyển
                            </h3>

                            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                                <InfoItem
                                    label="Mã chuyến giao"
                                    value={manifest.deliveryCode}
                                />
                                <InfoItem
                                    label="Tài xế"
                                    value={manifest.driverName}
                                />
                                <InfoItem
                                    label="Thời gian bắt đầu"
                                    value={formatDate(manifest.startedAt)}
                                />
                                <InfoItem
                                    label="Thời gian giao xong"
                                    value={formatDate(manifest.deliveredAt)}
                                />
                            </div>

                            {manifest.note && (
                                <p className="mt-4 text-sm text-slate-600">
                                    <strong>Ghi chú:</strong> {manifest.note}
                                </p>
                            )}
                        </Card>

                        <div className="space-y-3">
                            <h3 className="text-lg font-bold text-slate-800">
                                Danh sách phiếu điều chuyển
                            </h3>

                            {manifest.transfers?.length ? (
                                <Table
                                    columns={transferColumns}
                                    data={manifest.transfers}
                                    rowKey="transferId"
                                />
                            ) : (
                                <EmptyState
                                    title="Chưa có phiếu điều chuyển"
                                />
                            )}
                        </div>

                        <div className="space-y-4">
                            <h3 className="text-lg font-bold text-slate-800">
                                Danh sách kiện hàng
                            </h3>

                            {!manifest.packages?.length ? (
                                <EmptyState title="Chưa có kiện hàng" />
                            ) : (
                                manifest.packages.map((pkg) => (
                                    <Card key={pkg.packageId}>
                                        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                                            <h4 className="text-lg font-bold text-slate-800">
                                                {pkg.packageCode}
                                            </h4>

                                            <span className="rounded-lg bg-slate-100 px-3 py-1 text-sm font-medium text-slate-700">
                                                {pkg.packageStatus}
                                            </span>
                                        </div>

                                        <div className="mb-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                                            <InfoItem
                                                label="Mã niêm phong"
                                                value={pkg.sealNumber}
                                            />
                                            <InfoItem
                                                label="Người đóng kiện"
                                                value={pkg.packedByName}
                                            />
                                            <InfoItem
                                                label="Thời gian đóng kiện"
                                                value={formatDate(pkg.packedAt)}
                                            />
                                            <InfoItem
                                                label="Tổng số lượng"
                                                value={formatNumber(pkg.totalQuantity)}
                                            />
                                        </div>

                                        <h5 className="mb-3 font-semibold text-slate-800">
                                            Sản phẩm trong kiện
                                        </h5>

                                        {pkg.products?.length ? (
                                            <Table
                                                columns={productColumns}
                                                data={pkg.products}
                                                rowKey="productId"
                                            />
                                        ) : (
                                            <EmptyState
                                                title="Kiện chưa có sản phẩm"
                                            />
                                        )}
                                    </Card>
                                ))
                            )}
                        </div>
                    </>
                )}
            </div>
        </MainLayout>
    );
}