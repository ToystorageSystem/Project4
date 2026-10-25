import { useEffect, useState } from "react";
import MainLayout from "../../components/layout/MainLayout";
import PageHeader from "../../components/layout/PageHeader";
import MetricCard, { formatCount } from "../../components/business/MetricCard";
import { businessManagerMenuItems } from "../../components/icons/businessMenuItems";
import { getBusinessManagerInventory } from "../../api/business/businessDashboard";

export default function BusinessManagerDashboardPage() {
    const [report, setReport] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let cancelled = false;
        getBusinessManagerInventory()
            .then((data) => {
                if (!cancelled) {
                    if (!data || typeof data !== "object" || Array.isArray(data)) {
                        throw new Error("Invalid inventory report response");
                    }
                    setReport(data);
                }
            })
            .catch((err) => {
                if (!cancelled) {
                    setError(err.response?.status === 403
                        ? "Tài khoản không có quyền xem báo cáo tồn kho (403)."
                        : "Không tải được báo cáo tồn kho. Kiểm tra Backend và phiên đăng nhập.");
                }
            })
            .finally(() => { if (!cancelled) setLoading(false); });
        return () => { cancelled = true; };
    }, []);

    return (
        <MainLayout title="Business Manager" items={businessManagerMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Business Manager Dashboard"
                    description="Tổng quan tồn kho được truy vấn trực tiếp từ API báo cáo. Bước này chỉ đọc dữ liệu."
                />

                {loading && <p role="status" className="text-sm text-slate-500">Đang tải báo cáo tồn kho...</p>}
                {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}
                {!loading && !error && report && (
                    <>
                        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                            <MetricCard label="Sản phẩm trong kho" value={formatCount(report.totalProducts)} />
                            <MetricCard label="Tổng tồn vật lý" value={formatCount(report.totalQuantity)} />
                            <MetricCard label="Tồn khả dụng" value={formatCount(report.totalAvailableQuantity)} />
                            <MetricCard label="Tồn giữ chỗ" value={formatCount(report.totalReservedQuantity)} />
                        </div>
                        <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                            <h2 className="font-semibold text-slate-900">Tình trạng dữ liệu</h2>
                            <p className="mt-2 text-sm text-slate-600">Nguồn: GET /api/business-manager/reports/inventory</p>
                            <p className="mt-1 text-sm text-slate-500">Số dòng tồn kho theo vị trí: {formatCount(report.totalBalanceRecords)}</p>
                            {report.snapshotAt && <p className="mt-1 text-xs text-slate-500">Thời điểm báo cáo: {report.snapshotAt}</p>}
                        </section>
                    </>
                )}
            </div>
        </MainLayout>
    );
}
