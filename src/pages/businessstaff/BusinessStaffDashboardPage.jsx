import { formatBusinessStatus } from "../../utils/viStatus";

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
        title: "Add Product",
        description: "Submit New Product Request",
        to: "/business/products/new",
        iconPath: "M12 5v14M5 12h14",
    },
    {
        title: "Create Purchase Order",
        description: "Create Supplier Purchase Order",
        to: "/business/purchase-orders/new",
        iconPath: "M8 3h8l4 4v14H4V3h4m8 0v5h4M8 12h8M8 16h8",
    },
    {
        title: "Create Stock Transfer",
        description: "Transfer Stock Between Warehouses",
        to: "/business/stock-transfers/new",
        iconPath: "M4 7h16m0 0-4-4m4 4-4 4M20 17H4m0 0 4-4m-4 4 4 4",
    },
    {
        title: "Supplier Management",
        description: "View and update supplier information",
        to: "/business/suppliers",
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
                        throw new Error("Invalid supplier data");
                    }
                    setSuppliers(data);
                }
            })
            .catch((err) => {
                if (!cancelled) {
                    setError(
                        err.response?.status === 403
                            ? "You do not have permission to view suppliers."
                            : "Unable to load suppliers. Please try again."
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
            title="Business Operations"
            items={businessStaffMenuItems}
        >
            <div className="space-y-6">
                <PageHeader title="Work Overview" />

                {loading && (
                    <p role="status" className="text-sm text-slate-500">
                        Loading supplier data...
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
                                label="Total Suppliers"
                                value={formatCount(suppliers.length)}
                            />
                            <MetricCard
                                label="Active"
                                value={formatCount(activeCount)}
                            />
                            <MetricCard
                                label="Inactive"
                                value={formatCount(suppliers.length - activeCount)}
                            />
                            <MetricCard
                                label="Total Products"
                                value={formatCount(extraStats.products)}
                            />
                            <MetricCard
                                label="Draft Purchase Orders"
                                value={formatCount(extraStats.draftOrders)}
                            />
                            <MetricCard
                                label="Rejected Requests"
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
                                    Quick Actions
                                </h2>
                                <p className="text-xs text-slate-500">
                                    Quick access to frequently used features
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
                                Action Required
                            </h2>

                            <div className="grid gap-3 lg:grid-cols-2">
                                <Link
                                    to="/business/purchase-orders"
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
                                            Draft Purchase Orders
                                        </span>

                                        <span className="mt-1 block text-xs leading-5 text-slate-500">
                                            {extraStats.draftOrders === null
                                                ? "No data available."
                                                : extraStats.draftOrders > 0
                                                    ? "Draft purchase orders require completion."
                                                    : "No draft purchase orders."}
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
                                    to="/business/products/rejected"
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
                                            Rejected Product Requests
                                        </span>

                                        <span className="mt-1 block text-xs leading-5 text-slate-500">
                                            {extraStats.rejectedRequests === null
                                                ? "No data available."
                                                : extraStats.rejectedRequests > 0
                                                    ? "Review rejection reasons and revise requests for resubmission."
                                                    : "No rejected requests."}
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
                                        Suppliers
                                    </h2>

                                    <Link
                                        to="/business/suppliers"
                                        className="text-sm font-semibold text-[#c94810] hover:underline"
                                    >
                                        View All →
                                    </Link>
                                </div>
                            </div>

                            {suppliers.length === 0 ? (
                                <p className="p-5 text-sm text-slate-500">
                                    No suppliers found.
                                </p>
                            ) : (
                                <div className="overflow-x-auto">
                                    <table className="w-full text-left text-sm">
                                        <thead className="bg-slate-50 text-slate-500">
                                            <tr>
                                                <th className="px-5 py-3">Supplier Code</th>
                                                <th className="px-5 py-3">Supplier Name</th>
                                                <th className="px-5 py-3">Email</th>
                                                <th className="px-5 py-3">Status</th>
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
                                                        {supplier.status ? formatBusinessStatus(supplier.status) : "—"}
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

