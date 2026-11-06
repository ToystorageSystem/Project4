
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import MainLayout from "../../components/layout/MainLayout";
import PageHeader from "../../components/layout/PageHeader";
import MetricCard, { formatCount } from "../../components/business/MetricCard";
import { businessStaffMenuItems } from "../../components/icons/businessMenuItems";
import { getBusinessStaffSuppliers } from "../../api/business/businessDashboard";
import { listProducts } from "../../api/business/products";
import { listPurchaseOrders } from "../../api/business/purchaseOrders";
import { listRejectedProductRequests } from "../../api/business/rejectedProducts";


const QUICK_ACTIONS = [
    {
        title: "Thêm sản phẩm",
        description: "Tạo yêu cầu sản phẩm mới",
        to: "/business-staff/products/new",
        iconPath: "M12 5v14M5 12h14",
    },
    {
        title: "Tạo đơn mua hàng",
        description: "Lập đơn mua từ nhà cung cấp",
        to: "/business-staff/purchase-orders/new",
        iconPath: "M8 3h8l4 4v14H4V3h4m8 0v5h4M8 12h8M8 16h8",
    },
    {
        title: "Tạo phiếu điều chuyển",
        description: "Điều chuyển hàng giữa các kho",
        to: "/business-staff/stock-transfers/new",
        iconPath: "M4 7h16m0 0-4-4m4 4-4 4M20 17H4m0 0 4-4m-4 4 4 4",
    },
    {
        title: "Quản lý nhà cung cấp",
        description: "Xem và cập nhật nhà cung cấp",
        to: "/business-staff/suppliers",
        iconPath: "M3 21h18M5 21V7l7-4 7 4v14M9 11h6M9 15h6M9 19h6",
    },
];

export default function BusinessStaffDashboardPage() {
    const [suppliers, setSuppliers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const [extraStats, setExtraStats] = useState({
        products: null,
        draftOrders: null,
        rejectedRequests: null,
    });

    useEffect(() => {
        const controller = new AbortController();

        async function loadExtraStats() {
            const [productsResult, ordersResult, rejectedResult] =
                await Promise.allSettled([
                    listProducts(
                        { page: 0, size: 1 },
                        controller.signal
                    ),
                    listPurchaseOrders(
                        { status: "DRAFT" },
                        controller.signal
                    ),
                    listRejectedProductRequests(
                        { page: 0, size: 1 },
                        controller.signal
                    ),
                ]);

            if (controller.signal.aborted) return;

            const products =
                productsResult.status === "fulfilled" &&
                Number.isFinite(productsResult.value?.totalElements)
                    ? productsResult.value.totalElements
                    : null;

            const draftOrders =
                ordersResult.status === "fulfilled" &&
                Array.isArray(ordersResult.value)
                    ? ordersResult.value.length
                    : null;

            const rejectedRequests =
                rejectedResult.status === "fulfilled" &&
                Number.isFinite(rejectedResult.value?.totalElements)
                    ? rejectedResult.value.totalElements
                    : null;

            setExtraStats({
                products,
                draftOrders,
                rejectedRequests,
            });
        }

        loadExtraStats();

        return () => controller.abort();
    }, []);


    useEffect(() => {
        let cancelled = false;

        getBusinessStaffSuppliers()
            .then((data) => {
                if (!cancelled) {
                    if (!Array.isArray(data)) {
                        throw new Error("Dữ liệu nhà cung cấp chưa hợp lệ");
                    }
                    setSuppliers(data);
                }
            })
            .catch((err) => {
                if (!cancelled) {
                    setError(
                        err.response?.status === 403
                            ? "Tài khoản chưa được cấp quyền xem nhà cung cấp."
                            : "Không tải được danh sách nhà cung cấp. Vui lòng thử lại."
                    );
                }
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => {
            cancelled = true;
        };
    }, []);

    const activeCount = suppliers.filter(
        (s) => s.status === "ACTIVE"
    ).length;

    return (
        <MainLayout
            title="Business Staff"
            items={businessStaffMenuItems}
        >
            <div className="space-y-6">
                <PageHeader title="Tổng quan công việc" />

                {loading && (
                    <p role="status" className="text-sm text-slate-500">
                        Đang tải dữ liệu nhà cung cấp...
                    </p>
                )}

                {error && (
                    <div
                        role="alert"
                        className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700"
                    >
                        {error}
                    </div>
                )}

                {!loading && !error && (
                    <>
                        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                            <MetricCard
                                label="Tổng nhà cung cấp"
                                value={formatCount(suppliers.length)}
                            />
                            <MetricCard
                                label="Đang hoạt động"
                                value={formatCount(activeCount)}
                            />
                            <MetricCard
                                label="Ngừng hoạt động"
                                value={formatCount(suppliers.length - activeCount)}
                            />
                            <MetricCard
                                label="Tổng sản phẩm"
                                value={formatCount(extraStats.products)}
                            />
                            <MetricCard
                                label="Đơn mua hàng nháp"
                                value={formatCount(extraStats.draftOrders)}
                            />
                            <MetricCard
                                label="Yêu cầu bị từ chối"
                                value={formatCount(extraStats.rejectedRequests)}
                            />
                        </div>

                        {/* Thao tac nhanh */}
                        <section
                            className="space-y-4"
                            aria-labelledby="quick-actions-title"
                        >
                            <div className="flex flex-wrap items-center justify-between gap-2">
                                <h2
                                    id="quick-actions-title"
                                    className="text-lg font-bold text-slate-900"
                                >
                                    Thao tác nhanh
                                </h2>
                                <p className="text-xs text-slate-500">
                                    Truy cập nhanh các chức năng thường dùng
                                </p>
                            </div>

                            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                                {QUICK_ACTIONS.map((action) => (
                                    <Link
                                        key={action.to}
                                        to={action.to}
                                        className="group flex items-center gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:border-orange-200 hover:bg-orange-50/40 hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-orange-400"
                                    >
                                        <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-orange-50 text-[#f25d19]">
                                            <svg
                                                viewBox="0 0 24 24"
                                                fill="none"
                                                stroke="currentColor"
                                                strokeWidth="1.8"
                                                strokeLinecap="round"
                                                strokeLinejoin="round"
                                                className="h-5 w-5"
                                                aria-hidden="true"
                                            >
                                                <path d={action.iconPath} />
                                            </svg>
                                        </span>

                                        <span className="min-w-0 flex-1">
                                            <span className="block text-sm font-semibold text-slate-900">
                                                {action.title}
                                            </span>
                                            <span className="mt-1 block text-xs leading-5 text-slate-500">
                                                {action.description}
                                            </span>
                                        </span>

                                        <span
                                            className="text-lg text-slate-400 group-hover:text-[#f25d19]"
                                            aria-hidden="true"
                                        >
                                            →
                                        </span>
                                    </Link>
                                ))}
                            </div>
                        </section>

                        {/* Cong viec can xu ly */}
                        <section
                            className="space-y-4"
                            aria-labelledby="pending-tasks-title"
                        >
                            <h2
                                id="pending-tasks-title"
                                className="text-lg font-bold text-slate-900"
                            >
                                Công việc cần xử lý
                            </h2>

                            <div className="grid gap-3 lg:grid-cols-2">
                                <Link
                                    to="/business-staff/purchase-orders"
                                    className="group flex items-center gap-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm transition hover:border-orange-200 hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-orange-400"
                                >
                                    <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-700">
                                        <svg
                                            viewBox="0 0 24 24"
                                            fill="none"
                                            stroke="currentColor"
                                            strokeWidth="1.8"
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                            className="h-5 w-5"
                                            aria-hidden="true"
                                        >
                                            <path d="M8 3h8l4 4v14H4V3h4Zm8 0v5h4M8 12h8M8 16h5" />
                                        </svg>
                                    </span>

                                    <span className="min-w-0 flex-1">
                                        <span className="block text-sm font-semibold text-slate-900">
                                            Đơn mua hàng nháp
                                        </span>

                                        <span className="mt-1 block text-xs leading-5 text-slate-500">
                                            {extraStats.draftOrders === null
                                                ? "Chưa tải được số liệu."
                                                : extraStats.draftOrders > 0
                                                    ? "Có đơn nháp cần tiếp tục hoàn thiện."
                                                    : "Hiện không có đơn nháp."}
                                        </span>
                                    </span>

                                    <span className="rounded-full bg-orange-50 px-3 py-1 text-sm font-bold text-[#c94810]">
                                        {formatCount(extraStats.draftOrders)}
                                    </span>

                                    <span
                                        className="text-slate-400 group-hover:text-[#f25d19]"
                                        aria-hidden="true"
                                    >
                                        →
                                    </span>
                                </Link>

                                <Link
                                    to="/business-staff/products/rejected"
                                    className="group flex items-center gap-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm transition hover:border-orange-200 hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-orange-400"
                                >
                                    <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-700">
                                        <svg
                                            viewBox="0 0 24 24"
                                            fill="none"
                                            stroke="currentColor"
                                            strokeWidth="1.8"
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                            className="h-5 w-5"
                                            aria-hidden="true"
                                        >
                                            <path d="M8 3h8l4 4v14H4V3h4Zm8 0v5h4M8 12h8M8 16h5" />
                                        </svg>
                                    </span>

                                    <span className="min-w-0 flex-1">
                                        <span className="block text-sm font-semibold text-slate-900">
                                            Yêu cầu sản phẩm bị từ chối
                                        </span>

                                        <span className="mt-1 block text-xs leading-5 text-slate-500">
                                            {extraStats.rejectedRequests === null
                                                ? "Chưa tải được số liệu."
                                                : extraStats.rejectedRequests > 0
                                                    ? "Xem lý do từ chối và chỉnh sửa để gửi lại."
                                                    : "Không có yêu cầu bị từ chối."}
                                        </span>
                                    </span>

                                    <span className="rounded-full bg-orange-50 px-3 py-1 text-sm font-bold text-[#c94810]">
                                        {formatCount(extraStats.rejectedRequests)}
                                    </span>

                                    <span
                                        className="text-slate-400 group-hover:text-[#f25d19]"
                                        aria-hidden="true"
                                    >
                                        →
                                    </span>
                                </Link>
                            </div>
                        </section>

                        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                            <div className="border-b border-slate-200 p-5">
                                <div className="flex flex-wrap items-center justify-between gap-3">
                                    <h2 className="font-semibold text-slate-900">
                                        Nhà cung cấp
                                    </h2>

                                    <Link
                                        to="/business-staff/suppliers"
                                        className="text-sm font-semibold text-[#c94810] hover:underline"
                                    >
                                        Xem tất cả →
                                    </Link>
                                </div>
                            </div>

                            {suppliers.length === 0 ? (
                                <p className="p-5 text-sm text-slate-500">
                                    Không có nhà cung cấp nào.
                                </p>
                            ) : (
                                <div className="overflow-x-auto">
                                    <table className="w-full text-left text-sm">
                                        <thead className="bg-slate-50 text-slate-500">
                                            <tr>
                                                <th className="px-5 py-3">Mã nhà cung cấp</th>
                                                <th className="px-5 py-3">Tên nhà cung cấp</th>
                                                <th className="px-5 py-3">Email</th>
                                                <th className="px-5 py-3">Trạng thái</th>
                                            </tr>
                                        </thead>

                                        <tbody className="divide-y divide-slate-100">
                                            {suppliers.slice(0, 5).map((supplier) => (
                                                <tr key={supplier.id}>
                                                    <td className="px-5 py-3">
                                                        {supplier.suppliersCode || "—"}
                                                    </td>
                                                    <td className="px-5 py-3 font-medium text-slate-900">
                                                        {supplier.name || "—"}
                                                    </td>
                                                    <td className="px-5 py-3">
                                                        {supplier.email || "—"}
                                                    </td>
                                                    <td className="px-5 py-3">
                                                        {supplier.status || "—"}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </section>
                    </>
                )}
            </div>
        </MainLayout>
    );
}
