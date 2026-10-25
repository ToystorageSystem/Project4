import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import MainLayout from "../../components/layout/MainLayout";
import PageHeader from "../../components/layout/PageHeader";
import MetricCard, { formatCount } from "../../components/business/MetricCard";
import { businessStaffMenuItems } from "../../components/icons/businessMenuItems";
import { getBusinessStaffSuppliers } from "../../api/business/businessDashboard";

export default function BusinessStaffDashboardPage() {
    const [suppliers, setSuppliers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let cancelled = false;
        getBusinessStaffSuppliers()
            .then((data) => {
                if (!cancelled) {
                    if (!Array.isArray(data)) throw new Error("Invalid supplier list response");
                    setSuppliers(data);
                }
            })
            .catch((err) => {
                if (!cancelled) {
                    setError(err.response?.status === 403
                        ? "Tài khoản không có quyền xem nhà cung cấp (403)."
                        : "Không tải được nhà cung cấp. Kiểm tra Backend và phiên đăng nhập.");
                }
            })
            .finally(() => { if (!cancelled) setLoading(false); });
        return () => { cancelled = true; };
    }, []);

    const activeCount = suppliers.filter((s) => s.status === "ACTIVE").length;

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Business Staff Dashboard"
                    description="Theo dõi dữ liệu nhà cung cấp từ Backend Spring Boot. Bước này chỉ đọc dữ liệu."
                />

                {loading && <p role="status" className="text-sm text-slate-500">Đang tải dữ liệu nhà cung cấp...</p>}
                {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}

                {!loading && !error && (
                    <>
                        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                            <MetricCard label="Tổng nhà cung cấp" value={formatCount(suppliers.length)} />
                            <MetricCard label="Đang hoạt động" value={formatCount(activeCount)} />
                            <MetricCard label="Ngừng hoạt động" value={formatCount(suppliers.length - activeCount)} />
                        </div>
                        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                            <div className="border-b border-slate-200 p-5">
                                <div className="flex flex-wrap items-center justify-between gap-3">
                                    <h2 className="font-semibold text-slate-900">Nhà cung cấp (5 bản ghi đầu)</h2>
                                    <Link to="/business-staff/suppliers" className="text-sm font-semibold text-[#c94810] hover:underline">Xem tất cả →</Link>
                                </div>
                                <p className="mt-1 text-sm text-slate-500">Nguồn: GET /api/business-staff/suppliers</p>
                            </div>
                            {suppliers.length === 0 ? (
                                <p className="p-5 text-sm text-slate-500">Không có nhà cung cấp nào.</p>
                            ) : (
                                <div className="overflow-x-auto">
                                    <table className="w-full text-left text-sm">
                                        <thead className="bg-slate-50 text-slate-500">
                                            <tr><th className="px-5 py-3">Mã NCC</th><th className="px-5 py-3">Tên NCC</th><th className="px-5 py-3">Email</th><th className="px-5 py-3">Trạng thái</th></tr>
                                        </thead>
                                        <tbody className="divide-y divide-slate-100">
                                            {suppliers.slice(0, 5).map((supplier) => (
                                                <tr key={supplier.id}>
                                                    <td className="px-5 py-3">{supplier.suppliersCode || "—"}</td>
                                                    <td className="px-5 py-3 font-medium text-slate-900">{supplier.name || "—"}</td>
                                                    <td className="px-5 py-3">{supplier.email || "—"}</td>
                                                    <td className="px-5 py-3">{supplier.status || "—"}</td>
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
