import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getMyProductRequests, productErrorMessage } from "../../../api/business/products";
import ProductStatusBadge, { formatProductDate } from "../../../components/business/products/ProductStatusBadge";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import EmptyState from "../../../components/ui/EmptyState";
import Pagination from "../../../components/ui/Pagination";
import Spinner from "../../../components/ui/Spinner";
import Table from "../../../components/ui/Table";

const ROOT = "/business-staff/products";
const TYPES = { CREATE: "Tạo mới", UPDATE: "Đề nghị sửa", DEACTIVATE: "Ẩn" };

export default function ProductRequestsPage() {
    const navigate = useNavigate();
    const [page, setPage] = useState(0);
    const [reload, setReload] = useState(0);
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        getMyProductRequests({ page, size: 10 }, controller.signal)
            .then((result) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(result?.items)) throw new Error("Invalid request page response");
                setData(result);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setError(productErrorMessage(requestError, "Không lấy được danh sách yêu cầu."));
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [page, reload]);

    const columns = [
        { key: "requestCode", label: "Mã yêu cầu", render: (v) => <span className="font-medium text-slate-900">{v || "—"}</span> },
        { key: "requestType", label: "Loại", render: (v) => TYPES[v] || v || "—" },
        { key: "productCode", label: "Mã sản phẩm" },
        { key: "productName", label: "Sản phẩm" },
        { key: "status", label: "Trạng thái", render: (v) => <ProductStatusBadge status={v} /> },
        { key: "rejectionReason", label: "Lý do từ chối", render: (v) => v || "—" },
        { key: "createdAt", label: "Thời gian", render: formatProductDate },
        { key: "productId", label: "Chi tiết", render: (v) => v ? <Link to={`${ROOT}/${v}`} className="text-[#c94810] hover:underline">Xem SP</Link> : "—" },
    ];

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader title="Yêu cầu sản phẩm của tôi" description="Theo dõi yêu cầu tạo/sửa, trạng thái duyệt và lý do từ chối do Backend cung cấp."
                    actions={<div className="flex gap-2"><Button variant="ghost" onClick={() => navigate(ROOT)}>← Danh sách</Button><Button variant="outline" onClick={() => setReload((v) => v + 1)}>Làm mới</Button></div>}
                />
                <Alert type="info">Yêu cầu sửa được duyệt mới cập nhật dữ liệu sản phẩm. Trạng thái PENDING nghĩa là Business Manager chưa xử lý.</Alert>
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                {loading ? <div role="status" className="flex items-center gap-3 rounded-2xl bg-white p-5"><Spinner /> Đang tải yêu cầu...</div>
                    : !error && (data?.items?.length || 0) === 0 ? <EmptyState title="Chưa có yêu cầu" description="Bạn chưa gửi yêu cầu nào trong danh sách này." />
                        : !error ? <Table columns={columns} data={data.items} /> : null}
                {!loading && !error && data && data.totalPages > 0 && (
                    <Pagination page={page + 1} totalPages={data.totalPages} onChange={(value) => setPage(value - 1)} />
                )}
            </div>
        </MainLayout>
    );
}
